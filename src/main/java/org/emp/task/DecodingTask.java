package org.emp.task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.SensorDataChunk;
import org.emp.data.SensorDataHandler;
import org.emp.utils.DracoHelper;

/**
 * A task to decode compressed sensor data.
 */
public class DecodingTask extends Task {
  private static final Logger LOGGER = LogManager.getLogger(DecodingTask.class);
  private final DracoHelper dracoHelper = new DracoHelper();
  private final SensorDataChunk dataChunk;
  private final SensorDataHandler sensorDataHandler;

  public DecodingTask(SensorDataChunk dataChunk, SensorDataHandler sensorDataHandler) {
    super(TaskType.DECODING);
    this.dataChunk = dataChunk;
    this.sensorDataHandler = sensorDataHandler;
  }

  @Override
  public TaskResult call() throws Exception {
    byte[] compressedPointCloud = dataChunk.getCompressedPointCloud();
    if (compressedPointCloud != null) {
      int vehicleId = dataChunk.getVehicleId();
      int frameId = dataChunk.getFrameId();
      int chunkId = dataChunk.getChunkId();
      long tDracoDecode1 = System.currentTimeMillis();
      sensorDataHandler.statHandler.logStartDracoDecodingTime(frameId, vehicleId, chunkId, tDracoDecode1);

      dataChunk.setDecodedPointCloud((compressedPointCloud.length > 0) ?
              dracoHelper.decode(compressedPointCloud) : new float[0]);
      sensorDataHandler.saveDataChunk(dataChunk);

      long tDracoDecode2 = System.currentTimeMillis();
      sensorDataHandler.statHandler.logEndDracoDecodingTime(frameId, vehicleId, chunkId, tDracoDecode2);
      sensorDataHandler.statHandler.logDracoDecodingTime(frameId, vehicleId, chunkId, tDracoDecode2-tDracoDecode1);
      LOGGER.info("vehicle: " + vehicleId + "; frame: " + frameId + "; chunk: " + chunkId);
    }
    return new DecodingTaskResult(dataChunk);
  }

  /**
   * TaskResult from a decoding task, which contains a sensor data chunk
   */
  public class DecodingTaskResult extends TaskResult {
    private final SensorDataChunk dataChunk;

    public DecodingTaskResult(SensorDataChunk dataChunk) {
      super(TaskType.DECODING);
      this.dataChunk = dataChunk;
    }

    @Override
    Object getResult() {
      return dataChunk;
    }
  }
}
