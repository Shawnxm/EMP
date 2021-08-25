package org.emp.network;

import java.nio.ByteBuffer;
import org.emp.data.VehicleMessageHandler;

class TestVehicleMessageHandler implements VehicleMessageHandler {
  private int maxPosition;

  @Override
  public int handle(ByteBuffer byteBuffer) {
    maxPosition = Math.max(byteBuffer.position(), maxPosition);
    if (byteBuffer.position() > 0) {
      return byteBuffer.get(0);
    }
    return -1;
  }

  public int getMaxPosition() {
    return maxPosition;
  }
}