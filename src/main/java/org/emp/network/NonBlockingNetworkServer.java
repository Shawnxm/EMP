package org.emp.network;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.VehicleMessageHandler;
import org.emp.data.VehicleMessageHandlerImpl;

/**
 * A non-blocking network server that receives data from multiple network connections
 */
public class NonBlockingNetworkServer implements Runnable {
  private static final Logger LOGGER = LogManager.getLogger(NonBlockingNetworkServer.class);
  // Maximum number of clients to accept
  private static final int MAX_NUM_CLIENTS = 32;
  // Buffer size for each client connection
  private static final int BYTE_BUFFER_CAPACITY = 4 * 1024 * 1024; // 4MB

  // Server address
  private final InetSocketAddress serverAddress;
  // The handler to process receive vehicle data
  private final VehicleMessageHandler vehicleMessageHandler;
  // Should the server run.  When false, the server stops receiving data and exits.
  private boolean shouldRun;
  // Number of clients accepted
  private int numClients;
  // Max number of clients
  public final int maxNumClients;
  // The server has broadcast the "start" signal or not
  private boolean isStarted;
  // A list of existing socket channel
  private List<SocketChannel> socketChannelList;
  // A list of byte buffer to store received data
  private List<ByteBuffer> receiveByteBufferList;
  // A list of byte buffer queue for storing the data to send.
  // Each byte buffer in the queue represent one chunk of data to send
  // For the same index, the elements in `receiveByteBufferList` and `sendByteBufferList`
  // are for the same socket
  private final List<Queue<ByteBuffer>> sendByteBufferQueueList;
  // The mapping of the socket channel to the index of ByteBuffer in the `receiveByteBufferList`
  private Map<SocketChannel, Integer> socketToBufferIndexMap;
  // The mapping of vehicle ID to the index of ByteBuffer in the `sendByteBufferList`
  private Map<Integer, Integer> vehicleIdToBufferIndexMap;

  public NonBlockingNetworkServer(int port, int maxNumClients, VehicleMessageHandler vehicleMessageHandler)
      throws UnknownHostException {
    serverAddress = new InetSocketAddress(port);
    this.vehicleMessageHandler = vehicleMessageHandler;
    shouldRun = true;
    numClients = 0;
    this.maxNumClients = maxNumClients;
    isStarted = false;
    socketChannelList = new ArrayList<>(MAX_NUM_CLIENTS);
    receiveByteBufferList = IntStream.range(0, MAX_NUM_CLIENTS).boxed()
        .map(i -> ByteBuffer.allocate(BYTE_BUFFER_CAPACITY)).collect(Collectors.toList());
    sendByteBufferQueueList = IntStream.range(0, MAX_NUM_CLIENTS).boxed()
        .map(i -> new ArrayDeque<ByteBuffer>()).collect(Collectors.toList());
    socketToBufferIndexMap = new HashMap<>();
    vehicleIdToBufferIndexMap = new HashMap<>();
  }

  public void stopRunning() {
    shouldRun = false;
  }

  public int getNumClients() {
    return numClients;
  }

  public ByteBuffer getReceiveByteBuffer(int i) {
    return receiveByteBufferList.get(i);
  }

  /**
   * Puts the data to send into a {@link ByteBuffer} instance and add it to the queue
   *
   * @param vehicleId  Vehicle ID.
   * @param data  The byte array to send.
   */
  public void putDataToSendByteBufferQueue(int vehicleId, byte[] data) {
    Integer bufferIndex = vehicleIdToBufferIndexMap.get(vehicleId);

    if (bufferIndex == null) {
      throw new IllegalStateException("Cannot find the buffer for vehicle: " + vehicleId);
    }

    synchronized (sendByteBufferQueueList) {
      Queue<ByteBuffer> queue = sendByteBufferQueueList.get(bufferIndex);
      queue.add(ByteBuffer.wrap(data));
    }
  }

  @Override
  public void run() {
    ServerSocketChannel serverSocketChannel = null;
    
    try {
      Selector socketSelector = Selector.open();
      serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      serverSocketChannel.configureBlocking(false);
      serverSocketChannel.bind(serverAddress);
      serverSocketChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

      boolean hasSendData = true;
      while (shouldRun || hasSendData) {
        if (socketSelector.selectNow() <= 0) {
          continue;
        }

        Set<SelectionKey> selectionKeys = socketSelector.selectedKeys();
        for (SelectionKey selectionKey : selectionKeys) {
          // Accept a new connection from a client
          if (selectionKey.isAcceptable()) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(
                socketSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            socketChannelList.add(socketChannel);
            socketToBufferIndexMap.put(socketChannel, numClients);
            numClients++;
            LOGGER.info("Connection accepted: " + socketChannel.getRemoteAddress());
          }

          // Read data from a socket when available
          if (selectionKey.isReadable()) {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            Integer bufferIndex = socketToBufferIndexMap.get(socketChannel);

            if (bufferIndex == null) {
              throw new IllegalStateException(
                  "Cannot find the buffer for: " + socketChannel.getLocalAddress());
            }

            ByteBuffer byteBuffer = receiveByteBufferList.get(bufferIndex);
            int numBytes = socketChannel.read(byteBuffer);
            LOGGER.debug("Socket " + bufferIndex + " Read: " + numBytes);

            if (numBytes < 0) {
              SocketAddress socketAddress = socketChannel.getLocalAddress();
              socketChannel.close();
              LOGGER.info("Connection closed: " + socketAddress);
            }

            // TODO: only process vehicle ID for a handshake message
            int vehicleId = vehicleMessageHandler.handle(byteBuffer);
            if (vehicleId >= 0) {
              vehicleIdToBufferIndexMap.put(vehicleId, bufferIndex);
              if (numClients >= maxNumClients && !isStarted) {
                byte[] startMsg = VehicleMessageHandlerImpl.encodeMessage(new byte[]{}, 0, 'S');
                for (Integer vId : vehicleIdToBufferIndexMap.keySet()) {
                  LOGGER.info("Notify vehicle: " + vId + " to start");
                  putDataToSendByteBufferQueue(vId, startMsg);
                }
                isStarted = true;
              }
              LOGGER.debug(
                  "Vehicle to buffer index mapping added: " + vehicleId + " -> " + bufferIndex);
            }
          }

          if (selectionKey.isWritable()) {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            Integer bufferIndex = socketToBufferIndexMap.get(socketChannel);

            if (bufferIndex == null) {
              throw new IllegalStateException(
                  "Cannot find the buffer for: " + socketChannel.getLocalAddress());
            }

            synchronized (sendByteBufferQueueList) {
              Queue<ByteBuffer> queue = sendByteBufferQueueList.get(bufferIndex);
              if (!queue.isEmpty()) {
                ByteBuffer byteBuffer = queue.peek();
                int writeBytes = socketChannel.write(byteBuffer);
                LOGGER.debug("Socket " + bufferIndex + " Write: " + writeBytes
                    + " Buffer remaining: " + byteBuffer.remaining());

                if (!byteBuffer.hasRemaining()) {
                  queue.poll();
                }
              }
            }
          }
        }
        socketSelector.selectedKeys().clear();

        hasSendData = false;
        for (Queue<ByteBuffer> queue : sendByteBufferQueueList) {
          if (!queue.isEmpty()) {
            hasSendData = true;
            break;
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
      try {
        serverSocketChannel.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    for (SocketChannel socketChannel : socketChannelList) {
      if (socketChannel.isOpen()) {
        try {
          socketChannel.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    LOGGER.info("Server finished.");
  }
}
