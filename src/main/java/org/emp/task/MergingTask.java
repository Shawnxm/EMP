package org.emp.task;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.SensorDataChunk;
import org.emp.data.SensorDataHandler;
import org.emp.edge.PtClMerger;

import java.nio.FloatBuffer;
import java.util.*;

/**
 * A task to merge sensor data chunk or frame into a central view.
 */
public class MergingTask extends Task {
  private static final Logger LOGGER = LogManager.getLogger(MergingTask.class);
  private final PtClMerger merger = new PtClMerger();
  private SensorDataChunk dataChunk;
  private int frameIdForCleanup;
  private final boolean isImmediateMerging;
  private final SensorDataHandler sensorDataHandler;

  // Constructor for immediate merging after decoding
  public MergingTask(Object dataChunk, SensorDataHandler sensorDataHandler) {
    super(TaskType.MERGING);
    isImmediateMerging = true;
    this.sensorDataHandler = sensorDataHandler;
    this.dataChunk = (SensorDataChunk) dataChunk;
  }
  // Constructor for merging after receiving primary oxts to clean decoded but unmerged chunks
  public MergingTask(int frameIdForCleanup, SensorDataHandler sensorDataHandler) {
    super(TaskType.MERGING);
    isImmediateMerging = false;
    this.sensorDataHandler = sensorDataHandler;
    this.frameIdForCleanup = frameIdForCleanup;
  }

  @Override
  public TaskResult call() throws Exception {
    if (isImmediateMerging) {  // immediate merging
      // Save the data chunk (will skip if it has been saved after decoding)
      sensorDataHandler.saveDataChunk(dataChunk);

      int vehicleId = dataChunk.getVehicleId();
      int frameId = dataChunk.getFrameId();
      int chunkId = dataChunk.getChunkId();
      LOGGER.debug("[Immediate] vehicle: " + vehicleId + "; frame: " + frameId + "; chunk: " + chunkId);
      if (vehicleId == 1) {  // No need to merge point cloud from the primary vehicle
        long t1 = System.currentTimeMillis();
        sensorDataHandler.statHandler.logStartMergingTime(frameId, vehicleId, chunkId, t1);

        float[] pointsPrimary = dataChunk.getDecodedPointCloud();
        FloatBuffer mergedPointCloud = sensorDataHandler.getMergedPointCloud(frameId);
        int offset = sensorDataHandler.updateMergedPointCloudOffset(frameId, pointsPrimary.length);
        LOGGER.debug("[Offset] vehicle: " + vehicleId + "; frame: " + frameId + "; chunk: " + chunkId + "; offset: " + offset + "; size: " + pointsPrimary.length);

        for (int i = 0; i < pointsPrimary.length; i++) {
          mergedPointCloud.put(offset + i, pointsPrimary[i]);
        }

        sensorDataHandler.saveMergedPointCloud(frameId, Collections.singleton(vehicleId));
        long t2 = System.currentTimeMillis();
        sensorDataHandler.statHandler.logEndMergingTime(frameId, vehicleId, chunkId, t2);
        sensorDataHandler.statHandler.logMergingTime(frameId, vehicleId, chunkId, t2-t1);
        LOGGER.info("[Immediate][Yes] vehicle: " + vehicleId + "; frame: " + frameId + "; chunk: " + chunkId);
      }
      else if (sensorDataHandler.shouldRunMerging(frameId)) {  // flag indicating the primary location has been received
        long t1 = System.currentTimeMillis();
        sensorDataHandler.statHandler.logStartMergingTime(frameId, vehicleId, chunkId, t1);

        float[] oxtsPrimary = sensorDataHandler.getPrimaryLocation(frameId).getOxtsData();
        float[] points = dataChunk.getDecodedPointCloud();
        List<float[]> pointsSecondary = Collections.singletonList(points);
        List<float[]> oxtsSecondary = Collections.singletonList(sensorDataHandler.getVehicleLocation(vehicleId, frameId).getOxtsData());
        FloatBuffer mergedPointCloud = sensorDataHandler.getMergedPointCloud(frameId);
        int offset = sensorDataHandler.updateMergedPointCloudOffset(frameId, points.length);
        LOGGER.debug("[Offset] vehicle: " + vehicleId + "; frame: " + frameId + "; chunk: " + chunkId + "; offset: " + offset + "; size: " + points.length);

        merger.fbMergeNoPrimary(oxtsPrimary, pointsSecondary, oxtsSecondary, mergedPointCloud, offset);

        sensorDataHandler.saveMergedPointCloud(frameId, Collections.singleton(vehicleId));
        long t2 = System.currentTimeMillis();
        sensorDataHandler.statHandler.logEndMergingTime(frameId, vehicleId, chunkId, t2);
        sensorDataHandler.statHandler.logMergingTime(frameId, vehicleId, chunkId, t2-t1);
        LOGGER.info("[Immediate][Yes] vehicle: " + vehicleId + "; frame: " + frameId + "; chunk: " + chunkId);
      }
      else {  // if not received, save the data to a space (sensorDataHandler) for future merging
        sensorDataHandler.pushUnmergedData(vehicleId, frameId, chunkId);
        LOGGER.info("[Immediate][No] vehicle: " + vehicleId + "; frame: " + frameId + "; chunk: " + chunkId);
      }
    }
    else {  // cleanup merging
      Map<Integer, Set<Integer>> indexMap = sensorDataHandler.popUnmergedData(frameIdForCleanup);
      LOGGER.debug("[Cleanup] frame: " + frameIdForCleanup);
      if (indexMap != null) {
        float[] oxtsPrimary = sensorDataHandler.getPrimaryLocation(frameIdForCleanup).getOxtsData();
        List<float[]> pointsSecondary = new ArrayList<>();
        List<float[]> oxtsSecondary = new ArrayList<>();
        Set<Integer> vehicleIds = new HashSet<>();
        int dataSize = 0;
        for (Integer vehicleId : indexMap.keySet()) {
          float[] concated = new float[0];
          for (Integer chunkId : indexMap.get(vehicleId)) {
            concated = ArrayUtils.addAll(concated, sensorDataHandler.getDataChunk(vehicleId, frameIdForCleanup, chunkId));
          }
          pointsSecondary.add(concated);
          oxtsSecondary.add(sensorDataHandler.getVehicleLocation(vehicleId, frameIdForCleanup).getOxtsData());
          vehicleIds.add(vehicleId);
          dataSize += concated.length;
        }
        FloatBuffer mergedPointCloud = sensorDataHandler.getMergedPointCloud(frameIdForCleanup);
        int offset = sensorDataHandler.updateMergedPointCloudOffset(frameIdForCleanup, dataSize);
        LOGGER.debug("[Offset] frame: " + frameIdForCleanup + "; offset: " + offset + "; size: " + dataSize);

        merger.fbMergeNoPrimary(oxtsPrimary, pointsSecondary, oxtsSecondary, mergedPointCloud, offset);

        sensorDataHandler.saveMergedPointCloud(frameIdForCleanup, vehicleIds);
        LOGGER.info("[Cleanup][Yes] frame: " + frameIdForCleanup);
      }
      else {
        LOGGER.info("[Cleanup][No] frame: " + frameIdForCleanup);
      }
    }

    return new MergingTaskResult();
  }

  /**
   * TaskResult from a merging task
   */
  public class MergingTaskResult extends TaskResult {

    public MergingTaskResult() {
      super(TaskType.MERGING);
    }

    @Override
    Object getResult() {
      return null;
    }
  }
}
