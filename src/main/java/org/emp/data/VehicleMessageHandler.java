package org.emp.data;

import java.nio.ByteBuffer;

/**
 * A handler to decode vehicle data message sent by vehicles over the network.
 */
public interface VehicleMessageHandler {
  /**
   * Handles the data received on the edge, sent by a vehicle.
   *
   * @param byteBuffer  The data currently received on the edge.  It can be partial message.
   * @return vehicle ID.
   */
  int handle(ByteBuffer byteBuffer);
}
