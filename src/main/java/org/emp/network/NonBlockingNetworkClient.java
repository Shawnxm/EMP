package org.emp.network;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.EdgeMessageHandler;

/**
 * A non-blocking network client that connects to the edge server
 */
public class NonBlockingNetworkClient implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(NonBlockingNetworkClient.class);
    // Buffer size for the client connection
    private static final int BYTE_BUFFER_CAPACITY = 4 * 1024 * 1024; // 4MB

    private final InetSocketAddress serverAddress;
    private final InetSocketAddress clientAddress;
    private final EdgeMessageHandler edgeMessageHandler;
    // Byte buffer queue for storing the point data to send.
    // Each byte buffer in the queue represent one chunk of data to send
    private final Queue<ByteBuffer> sendByteBufferQueue;
    // Byte buffer to store received data
    private final ByteBuffer receiveByteBuffer;
    SocketChannel clientSocketChannel;
    // Should the server run.  When false, the server stops receiving data and exits.
    private boolean shouldRun;

    public NonBlockingNetworkClient(String serverIp, int serverPort, int clientPort, EdgeMessageHandler edgeMessageHandler)
        throws UnknownHostException {
        this.edgeMessageHandler = edgeMessageHandler;
        serverAddress = new InetSocketAddress(serverIp, serverPort);
        clientAddress = new InetSocketAddress(clientPort);
        shouldRun = true;
        sendByteBufferQueue = new ArrayDeque<>();
        receiveByteBuffer = ByteBuffer.allocate(BYTE_BUFFER_CAPACITY);
    }

    public void stopRunning() {
        shouldRun = false;
    }

    public ByteBuffer getReceiveByteBuffer() {
        return receiveByteBuffer;
    }

    /**
     * Puts the data to the send byte buffer queue.
     *
     * @param data  Byte array containing the data.  This is used as the underlying byte array
     *              in the {@link ByteBuffer} instance.
     * @throws Exception
     */
    public void putDataToSendByteBufferQueue(byte[] data) {
        synchronized (sendByteBufferQueue) {
            sendByteBufferQueue.add(ByteBuffer.wrap(data));
        }
    }

    @Override
    public void run() {
        try {
            Selector socketSelector = Selector.open();
            clientSocketChannel = SocketChannel.open();
            clientSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            clientSocketChannel.bind(clientAddress);
            LOGGER.info("Client connecting ...");
            clientSocketChannel.connect(serverAddress);
            LOGGER.info("Client connected to the edge server");
            clientSocketChannel.configureBlocking(false);
            clientSocketChannel.register(socketSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            while (shouldRun || !sendByteBufferQueue.isEmpty()) {
                if (socketSelector.selectNow() <= 0) {
                    continue;
                }

                Set<SelectionKey> selectionKeys = socketSelector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    // Send data
                    if (selectionKey.isWritable() && !sendByteBufferQueue.isEmpty()) {
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

                        synchronized (sendByteBufferQueue) {
                            ByteBuffer byteBuffer = sendByteBufferQueue.peek();
                            int writeBytes = socketChannel.write(byteBuffer);
                            LOGGER.debug("Client Write: " + writeBytes
                                + " Buffer remaining: " + byteBuffer.remaining());

                            if (!byteBuffer.hasRemaining()) {
                                sendByteBufferQueue.poll();
                            }
                        }
                    }

                    // Receive data
                    if (selectionKey.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                        int readBytes = socketChannel.read(receiveByteBuffer);
                        LOGGER.debug("Client Read: " + readBytes);

                        if (readBytes < 0) {
                            SocketAddress socketAddress = socketChannel.getLocalAddress();
                            socketChannel.close();
                            LOGGER.info("Connection closed: " + socketAddress);
                        }

                        edgeMessageHandler.handle(receiveByteBuffer);
                    }
                }

                socketSelector.selectedKeys().clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOGGER.info("Client finished.");
    }
}
