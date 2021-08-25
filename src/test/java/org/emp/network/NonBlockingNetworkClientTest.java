package org.emp.network;

import static com.google.common.truth.Truth.assertThat;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.EdgeMessageHandler;
import org.emp.utils.EmpUnitTest;
import org.junit.jupiter.api.Test;

public class NonBlockingNetworkClientTest extends EmpUnitTest {
  private static final Logger LOGGER = LogManager.getLogger(NonBlockingNetworkClientTest.class);

  @Test
  public void testNonBlockingNetworkClient() throws UnknownHostException {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    String serverIp = "127.0.0.1";
    int serverPort = 31904;
    int clientPort = 50000;
    TestVehicleMessageHandler vehicleMessageHandler = new TestVehicleMessageHandler();
    NonBlockingNetworkServer server = new NonBlockingNetworkServer(serverPort, 1, vehicleMessageHandler);
    LOGGER.info("Start non-blocking network server ...");
    Future serverFuture = executor.submit(server);

    TestEdgeMessageHandler edgeMessageHandler = new TestEdgeMessageHandler();
    NonBlockingNetworkClient client = new NonBlockingNetworkClient(serverIp, serverPort, clientPort, edgeMessageHandler);
    LOGGER.info("Start non-blocking network client ...");
    Future clientFuture = executor.submit(client);

    int vehicleId = 1;
    TestDataGenerator sendDataGenerator = new TestDataGenerator(vehicleId);
    TestDataGenerator receiveDataGenerator = new TestDataGenerator(2);
    int sendBytes = 600000;
    int receiveBytes = 512000;
    int numMessages = 6;

    for (int i = 0; i < numMessages; i++) {
      LOGGER.info("-> Send message " + i + " from client to edge server");
      // Messages from client to edge server
      client.putDataToSendByteBufferQueue(sendDataGenerator.generate(sendBytes));
      // Messages from edge server to client
      boolean isServerSendSuccessful = false;
      while (!isServerSendSuccessful) {
        try {
          server.putDataToSendByteBufferQueue(vehicleId, receiveDataGenerator.generate(receiveBytes));
          LOGGER.info("<- Send message " + i + " from edge server to client");
          isServerSendSuccessful = true;
        } catch (IllegalStateException e) {
          // Skip the exception and try again next
        }
      }
    }

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    LOGGER.info("Stop non-blocking network client ...");
    client.stopRunning();
    LOGGER.info("Stop non-blocking network server ...");
    server.stopRunning();

    while (!clientFuture.isDone()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    while (!serverFuture.isDone()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    LOGGER.info("Verifying results");
    // Verify data sent by client
    assertThat(server.getNumClients()).isEqualTo(1);
    ByteBuffer serverReceiveByteBuffer = server.getReceiveByteBuffer(0);
    assertThat(serverReceiveByteBuffer.position()).isEqualTo(sendBytes * numMessages);
    sendDataGenerator.verify(serverReceiveByteBuffer, sendBytes);

    // Verify data received by client
    ByteBuffer clientReceiveBuffer = client.getReceiveByteBuffer();
    assertThat(clientReceiveBuffer.position()).isEqualTo(receiveBytes * numMessages);
    assertThat(edgeMessageHandler.getMaxPosition()).isEqualTo(receiveBytes * numMessages);
    receiveDataGenerator.verify(clientReceiveBuffer, receiveBytes);
    LOGGER.info("Finish");
  }

  class TestEdgeMessageHandler implements EdgeMessageHandler {
    private int maxPosition;

    @Override
    public void handle(ByteBuffer byteBuffer) {
      maxPosition = Math.max(byteBuffer.position(), maxPosition);
    }

    public int getMaxPosition() {
      return maxPosition;
    }
  }
}
