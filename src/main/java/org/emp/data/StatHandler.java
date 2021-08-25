package org.emp.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * A EMP statistics handler to store system statistics during running
 */
public class StatHandler {
  private static final Logger LOGGER = LogManager.getLogger(StatHandler.class);

  // Frame ID to earliest start time in milliseconds
  Map<Integer, Integer> frameStartTimeMs = new HashMap<>();
  // Frame ID to latest end time in milliseconds
  Map<Integer, Integer> frameEndTimeMs = new HashMap<>();
  // Each log in the queue represent one piece of result to save
  // private final Queue<ByteBuffer> logQueue;

  // frameID -> duration >
  Map<Integer, Long> timeFrameStart;
  Map<Integer, Long> timeFrameEnd;
  Map<Integer, Long> timeGroundRemoval;
  Map<Integer, Long> timePartitioning;
  Map<Integer, Long> timeDataSaving;
  // frameID -> < chunkID -> duration/timestamp >
  Map<Integer, Map<Integer, Long>> timeDracoEncoding;
  Map<Integer, Map<Integer, Long>> timePointCloudSending;
  // frameID -> < vehicleID -> < chunkID -> duration/timestamp > >
  final Map<Integer, Map<Integer, Map<Integer, Long>>> timePointCloudReceiving;
  final Map<Integer, Map<Integer, Map<Integer, Long>>> timeDracoDecoding;
  final Map<Integer, Map<Integer, Map<Integer, Long>>> timeStartDracoDecoding;
  final Map<Integer, Map<Integer, Map<Integer, Long>>> timeEndDracoDecoding;
  final Map<Integer, Map<Integer, Map<Integer, Long>>> timeMerging;
  final Map<Integer, Map<Integer, Map<Integer, Long>>> timeStartMerging;
  final Map<Integer, Map<Integer, Map<Integer, Long>>> timeEndMerging;
  // frameID -> < vehicleID -> chunkID >
  final Map<Integer, Map<Integer, Integer>> uploadingStatus;

  public StatHandler() {
    timeFrameStart = new HashMap<>();
    timeFrameEnd = new HashMap<>();
    timeGroundRemoval = new HashMap<>();
    timePartitioning = new HashMap<>();
    timeDataSaving = new HashMap<>();
    timeDracoEncoding = new HashMap<>();
    timePointCloudSending = new HashMap<>();
    timePointCloudReceiving = new HashMap<>();
    timeDracoDecoding = new HashMap<>();
    timeStartDracoDecoding = new HashMap<>();
    timeEndDracoDecoding = new HashMap<>();
    timeMerging = new HashMap<>();
    timeStartMerging = new HashMap<>();
    timeEndMerging = new HashMap<>();
    uploadingStatus = new HashMap<>();
    // logQueue = new ArrayDeque<>();
  }

  public void putDataToLogQueue() {
    // TODO: Convert the handler to a runnable and log results to files
    // synchronized (logQueue) {
      // logQueue.add();
    // }
  }

  // Vehicle
  public void logFrameStartTime(int frameId, long timestamp) {
    timeFrameStart.put(frameId, timestamp);
    LOGGER.info("[Start] frame: " + frameId + ", time: " + timestamp);
  }

  public void logGroundRemovalTime(int frameId, long duration) {
    timeGroundRemoval.put(frameId, duration);
    LOGGER.info("[Ground] frame: " + frameId + ", time: " + duration);
  }

  public void logPartitioningTime(int frameId, long duration) {
    timePartitioning.put(frameId, duration);
    LOGGER.info("[Partition] frame: " + frameId + ", time: " + duration);
  }

  public void logDracoEncodingTime(int frameId, int chunkId, long duration) {
    Map<Integer, Long> chunkIdToDuration = timeDracoEncoding.computeIfAbsent(frameId, k -> new HashMap<>());
    chunkIdToDuration.put(chunkId, duration);
    LOGGER.info("[Encode] frame: " + frameId + ", chunk: " + chunkId + ", time: " + duration);
  }

  public void logMsgEncodingTime(int frameId, String msgType, long duration) {
    //TODO
    LOGGER.info("[MsgEncode] frame: " + frameId + ", msg encode time: " + duration);
  }

  public void logSendTime(int frameId, int chunkId, long timestamp) {
    Map<Integer, Long> chunkIdToTimestamp = timePointCloudSending.computeIfAbsent(frameId, k -> new HashMap<>());
    chunkIdToTimestamp.put(chunkId, timestamp);
    LOGGER.info("[Send] frame: " + frameId + ", chunk: " + chunkId + ", time: " + timestamp);
  }

  public void printVehicleStats() {
    //TODO: important
  }

  public void saveVehicleStats() {
    //TODO
  }

  // Edge
  public void logReceiveTime(int frameId, int vehicleId, int chunkId, long timestamp) {
    synchronized (timePointCloudReceiving) {
      Map<Integer, Map<Integer, Long>> vehicleIdToChunkId = timePointCloudReceiving.computeIfAbsent(frameId, k -> new HashMap<>());
      Map<Integer, Long> chunkIdToTimestamp = vehicleIdToChunkId.computeIfAbsent(vehicleId, k -> new HashMap<>());
      vehicleIdToChunkId.put(vehicleId, chunkIdToTimestamp);
      chunkIdToTimestamp.put(chunkId, timestamp);
    }
    LOGGER.info("[Receive] frame: " + frameId + ", vehicle: " + vehicleId + ", chunk: " + chunkId + ", time: " + timestamp);
  }

  public void logMsgDecodingTime(int frameId, long duration) {
    //TODO
  }

  public void logDracoDecodingTime(int frameId, int vehicleId, int chunkId, long duration) {
    synchronized (timeDracoDecoding) {
      Map<Integer, Map<Integer, Long>> vehicleIdToChunkId = timeDracoDecoding.computeIfAbsent(frameId, k -> new HashMap<>());
      Map<Integer, Long> chunkIdToTimestamp = vehicleIdToChunkId.computeIfAbsent(vehicleId, k -> new HashMap<>());
      vehicleIdToChunkId.put(vehicleId, chunkIdToTimestamp);
      chunkIdToTimestamp.put(chunkId, duration);
    }
    LOGGER.info("[Decode] frame: " + frameId + ", vehicle: " + vehicleId + ", chunk: " + chunkId + ", time: " + duration);
  }

  public void logStartDracoDecodingTime(int frameId, int vehicleId, int chunkId, long timestamp) {
    synchronized (timeStartDracoDecoding) {
      Map<Integer, Map<Integer, Long>> vehicleIdToChunkId = timeStartDracoDecoding.computeIfAbsent(frameId, k -> new HashMap<>());
      Map<Integer, Long> chunkIdToTimestamp = vehicleIdToChunkId.computeIfAbsent(vehicleId, k -> new HashMap<>());
      vehicleIdToChunkId.put(vehicleId, chunkIdToTimestamp);
      chunkIdToTimestamp.put(chunkId, timestamp);
    }
    LOGGER.info("[DecodeStart] frame: " + frameId + ", vehicle: " + vehicleId + ", chunk: " + chunkId + ", time: " + timestamp);
  }

  public void logEndDracoDecodingTime(int frameId, int vehicleId, int chunkId, long timestamp) {
    synchronized (timeEndDracoDecoding) {
      Map<Integer, Map<Integer, Long>> vehicleIdToChunkId = timeEndDracoDecoding.computeIfAbsent(frameId, k -> new HashMap<>());
      Map<Integer, Long> chunkIdToTimestamp = vehicleIdToChunkId.computeIfAbsent(vehicleId, k -> new HashMap<>());
      vehicleIdToChunkId.put(vehicleId, chunkIdToTimestamp);
      chunkIdToTimestamp.put(chunkId, timestamp);
    }
    LOGGER.info("[DecodeEnd] frame: " + frameId + ", vehicle: " + vehicleId + ", chunk: " + chunkId + ", time: " + timestamp);
  }

  public void logMergingTime(int frameId, int vehicleId, int chunkId, long duration) {
    synchronized (timeMerging) {
      Map<Integer, Map<Integer, Long>> vehicleIdToChunkId = timeMerging.computeIfAbsent(frameId, k -> new HashMap<>());
      Map<Integer, Long> chunkIdToTimestamp = vehicleIdToChunkId.computeIfAbsent(vehicleId, k -> new HashMap<>());
      vehicleIdToChunkId.put(vehicleId, chunkIdToTimestamp);
      chunkIdToTimestamp.put(chunkId, duration);
    }
    LOGGER.info("[Merge] frame: " + frameId + ", vehicle: " + vehicleId + ", chunk: " + chunkId + ", time: " + duration);
  }

  public void logStartMergingTime(int frameId, int vehicleId, int chunkId, long timestamp) {
    synchronized (timeStartMerging) {
      Map<Integer, Map<Integer, Long>> vehicleIdToChunkId = timeStartMerging.computeIfAbsent(frameId, k -> new HashMap<>());
      Map<Integer, Long> chunkIdToTimestamp = vehicleIdToChunkId.computeIfAbsent(vehicleId, k -> new HashMap<>());
      vehicleIdToChunkId.put(vehicleId, chunkIdToTimestamp);
      chunkIdToTimestamp.put(chunkId, timestamp);
    }
    LOGGER.info("[MergeStart] frame: " + frameId + ", vehicle: " + vehicleId + ", chunk: " + chunkId + ", time: " + timestamp);
  }

  public void logEndMergingTime(int frameId, int vehicleId, int chunkId, long timestamp) {
    synchronized (timeEndMerging) {
      Map<Integer, Map<Integer, Long>> vehicleIdToChunkId = timeEndMerging.computeIfAbsent(frameId, k -> new HashMap<>());
      Map<Integer, Long> chunkIdToTimestamp = vehicleIdToChunkId.computeIfAbsent(vehicleId, k -> new HashMap<>());
      vehicleIdToChunkId.put(vehicleId, chunkIdToTimestamp);
      chunkIdToTimestamp.put(chunkId, timestamp);
    }
    LOGGER.info("[MergeEnd] frame: " + frameId + ", vehicle: " + vehicleId + ", chunk: " + chunkId + ", time: " + timestamp);
  }

  public void logFrameEndTime(int frameId, long timestamp) {
    timeFrameEnd.put(frameId, timestamp);
    LOGGER.info("[End] frame: " + frameId + ", time: " + timestamp);
  }

  public void logDataSavingTime(int frameId, long duration) {
    timeDataSaving.put(frameId, duration);
    LOGGER.info("[Save] frame: " + frameId + ", time: " + duration);
  }

  public void updateUploadingStatus(int frameId, int vehicleId, int chunkId) {
    synchronized (uploadingStatus) {
      Map<Integer, Integer> uploadingStatusCurrentFrame = uploadingStatus.computeIfAbsent(frameId, k -> new HashMap<>());
      uploadingStatusCurrentFrame.put(vehicleId, chunkId);
      LOGGER.debug("[ChunkInfo-v] frame: " + frameId + ", vehicle: " + vehicleId + ", chunk: " + chunkId);
    }
  }

  public void printUploadingStatus(int frameId) {
    LOGGER.info("[ChunkInfo] frame: " + frameId + ", status: " + uploadingStatus.get(frameId));
  }

  public void logReapTime() {
    //TODO
  }

  public void logUploadingSchedulingTime() {
    //TODO
  }

  public void printEdgeStats() {
    //TODO
  }

  public void saveEdgeStats() {
    //TODO
  }
}
