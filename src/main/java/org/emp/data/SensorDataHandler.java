package org.emp.data;

import org.emp.network.NonBlockingNetworkServer;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.Set;

/**
 * Stores metadata of multiple sensor data chunks from different vehicles
 * for scheduling tasks in the processing pipeline
 */
public interface SensorDataHandler {

  StatHandler statHandler = new StatHandler();

  /**
   * Save the decoded sensor data chunk to the metadata.
   *
   * @param dataChunk  Decoded sensor data chunk.
   */
  void saveDataChunk(SensorDataChunk dataChunk);

  void saveMergedPointCloud(int frameId, Set<Integer> vehicleIds);

  /**
   *
   * @param vehicleId  vehicle ID.
   * @param frameId  frame ID.
   * @param location  vehicle location for the frame.
   */
  void updateVehicleLocation(int vehicleId, int frameId, VehicleLocation location);

  void pushUnmergedData(int vehicleId, int frameId, int chunkId);

  Map<Integer, Set<Integer>> popUnmergedData(int frameId);

  float[] getDataChunk(int vehicleId, int frameId, int chunkId);

  VehicleLocation getVehicleLocation(int vehicleId, int frameId);

  VehicleLocation getPrimaryLocation(int frameId);

  FloatBuffer getMergedPointCloud (int frameId);

  int updateMergedPointCloudOffset (int frameId, int dataSize);

  void updatePartitioningDecisions(int frameId) throws IOException;

  boolean shouldRunMerging(int frameId);

  /**
   * Determines if the object detection should run based on the metadata.
   *
   * @return  {@code true} if should run; {@code false} otherwise.
   */
  boolean shouldRunObjectDetection() throws IOException;

  void setNetworkServer(NonBlockingNetworkServer networkServer);
}
