package org.emp.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Stores information of a sensor data frame from a vehicle.
 *
 * <p> A sensor data frame is composed of one or more chunks.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class SensorDataFrame {
  int vehicleId;
  int frameId;
  VehicleLocation vehicleLocation;
  Map<Integer, SensorDataChunk> chunks;
  boolean isMerged;

  public int getLatestChunkId() {
    if (chunks == null || chunks.isEmpty()) { return 0; }
    else {return Collections.max(chunks.keySet()); }
  }
}
