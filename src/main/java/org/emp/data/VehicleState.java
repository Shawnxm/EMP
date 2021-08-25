package org.emp.data;

import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Stores the state of a vehicle
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class VehicleState {
  int vehicleId;
  boolean isOnline;
  String objectPrediction;
  Map<Integer, SensorDataFrame> frames;
  double bwEstimationKbps;
  Set<Integer> neighborIds;
  Set<Integer> neighborInProgressIds;

  public int getLatestFrameId() {
    if (frames == null) { return -1; }
    else {return Collections.max(frames.keySet()); }
  }

  public int getLatestMergedFrameId() {
    if (frames == null) {
      return -1;
    }
    else {
      int latestFrame = Collections.max(frames.keySet());
      while (latestFrame > -1 && (frames.get(latestFrame) == null || !frames.get(latestFrame).isMerged())) {
        latestFrame--;
      }
      return latestFrame;
    }
  }
}
