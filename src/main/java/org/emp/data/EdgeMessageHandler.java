package org.emp.data;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A handler to decode data messages sent by the edge over the network.
 */
public interface EdgeMessageHandler {
  /**
   * Handles the data received on the vehicle, sent by the edge.
   *
   * @param byteBuffer  The data currently received on the vehicle.  It can be partial message.
   */
  void handle(ByteBuffer byteBuffer) throws IOException, ClassNotFoundException;
}
