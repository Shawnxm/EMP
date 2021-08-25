package org.emp.network;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.utils.EmpUnitTest;
import org.junit.jupiter.api.Test;

public class NonBlockingNetworkServerTest extends EmpUnitTest {
  private static final Logger LOGGER = LogManager.getLogger(NonBlockingNetworkServerTest.class);
  private static final int NUM_RECEIVED_MESSAGES = 3;

  private byte[] prepareReceiveData() {
    int size = 512;
    byte[] expectedReceiveData = new byte[size];
    new Random().nextBytes(expectedReceiveData);
    return expectedReceiveData;
  }

  @Test
  public void testNonBlockingNetworkServer() throws UnknownHostException {
    int numThreads = 6;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    int port = 31904;
    TestVehicleMessageHandler handler = new TestVehicleMessageHandler();
    NonBlockingNetworkServer server = new NonBlockingNetworkServer(port, 2, handler);
    // Number of clients to emulate
    int numClients = numThreads - 1;
    // Number of bytes to send from each client
    Integer[] numBytes = IntStream.range(0, numClients).boxed()
        .map(i -> (i + 1) * 512000).toArray(Integer[]::new);
    byte[] expectedReceiveData = prepareReceiveData();

    LOGGER.info("Start non-blocking network server ...");
    Future serverFuture = executor.submit(server);

    List<Future> clientFutures = new ArrayList<>();
    for (int i = 0; i < numClients; i++) {
      clientFutures.add(executor.submit(
          new TestClientRunnable(i, port, numBytes[i], 5, prepareReceiveData())));
      // Create wait time to ensure the server accepts the clients in the original order
      // for verification
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    for (int i = 0; i < numClients; i++) {
      // For each vehicle, send three messages from edge to vehicle
      for (int j = 0; j < NUM_RECEIVED_MESSAGES; j++) {
        boolean isDataSent = false;
        while (!isDataSent) {
          try {
            server.putDataToSendByteBufferQueue(i, expectedReceiveData);
            isDataSent = true;
          } catch (IllegalStateException e) {
            // Skip the exception and try again next
          }
        }
      }
    }

    for (Future future : clientFutures) {
      while (!future.isDone()) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    LOGGER.info("Stop non-blocking network server ...");
    server.stopRunning();
    while (!serverFuture.isDone()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    LOGGER.info("Verifying results");
    assertThat(server.getNumClients()).isEqualTo(numClients);
    for (int i = 0; i < numClients; i++) {
      TestDataGenerator testDataGenerator = new TestDataGenerator(i);
      ByteBuffer byteBuffer = server.getReceiveByteBuffer(i);
      // Verify data size
      assertThat(byteBuffer.position()).isEqualTo(numBytes[i]);
      // Verify content
      testDataGenerator.verify(byteBuffer);
    }
    assertThat(handler.getMaxPosition()).isEqualTo(numBytes[numClients-1]);
  }

  /**
   * Test client to connect to the server
   */
  class TestClientRunnable implements Runnable {
    private final Logger LOGGER = LogManager.getLogger(TestClientRunnable.class);
    int id;
    InetSocketAddress serverAddress;
    byte[] data;
    int batches;
    byte[] expectedReceiveData;

    public TestClientRunnable(int id, int port, int bytes, int batches, byte[] expectedReceiveData)
        throws UnknownHostException {
      this.id = id;
      serverAddress = new InetSocketAddress(InetAddress.getByName("localhost"), port);
      data = new TestDataGenerator(id).generate(bytes);
      this.batches = batches;
      this.expectedReceiveData = expectedReceiveData;
    }

    @Override
    public void run() {
      LOGGER.debug("start TestClientRunnable " + id);
      SocketChannel socketChannel = null;
      try {
        socketChannel = SocketChannel.open();
        socketChannel.connect(serverAddress);

        int start, end = 0, numBatchBytes = data.length / batches;

        LOGGER.debug("Vehicle " + id + " sending data...");
        // Send data
        for (int i = 0; i < batches; i++) {
          start = end;
          if (i == batches - 1) {
            end = data.length;
          } else {
            end += numBatchBytes;
          }
          ByteBuffer byteBuffer = ByteBuffer.wrap(Arrays.copyOfRange(data, start, end));
          socketChannel.write(byteBuffer);

          // Create artificial delay to emulate network congestion
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }

        }

        LOGGER.debug("Vehicle " + id + " data sending finishes. Receiving data...");

        // Receive data
        ByteBuffer readBuffer = ByteBuffer.allocate(10240);
        int receiveBytes = 0;
        while (receiveBytes < expectedReceiveData.length) {
          int bytes = socketChannel.read(readBuffer);
          if (bytes >= 0) {
            receiveBytes += bytes;
            LOGGER.debug("Vehicle " + id + " received " + receiveBytes + " bytes.");
          }
        }

        // Verify received data
        assertThat(readBuffer.position())
            .isEqualTo(expectedReceiveData.length * NUM_RECEIVED_MESSAGES);
        for (int i = 0; i < readBuffer.position(); i++) {
          assertThat(readBuffer.get(i)).isEqualTo(
              expectedReceiveData[i % expectedReceiveData.length]);
        }
        LOGGER.debug("Vehicle " + id + " received data verified.");
      } catch (IOException e) {
        e.printStackTrace();
      }

      if (socketChannel != null && socketChannel.isOpen()) {
        try {
          socketChannel.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      LOGGER.debug("end TestClientRunnable " + id);
    }
  }
}
