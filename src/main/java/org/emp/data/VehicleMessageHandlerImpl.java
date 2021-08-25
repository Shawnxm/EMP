package org.emp.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.network.BandwidthEstimator;
import org.emp.task.DecodingTask;
import org.emp.task.LocationUpdatingTask;
import org.emp.task.MergingTask;
import org.emp.task.TaskScheduler;

/**
 * An implementation of {@code VehicleDataHandler} that follows the
 * serialization protocol between the vehicle and the edge.
 */
public class VehicleMessageHandlerImpl implements VehicleMessageHandler {
  private static final Logger LOGGER = LogManager.getLogger(VehicleMessageHandlerImpl.class);

  public static final int HEADER_BYTES = 13;

  private final TaskScheduler taskScheduler;
  private final SensorDataHandler sensorDataHandler;
  private final BandwidthEstimator bandwidthEstimator;
  private final StatHandler statHandler;

  // Vehicle ID -> current ongoing sensor data chunk
  private final Map<Integer, SensorDataChunk> chunkMap = new HashMap<>();
  // Vehicle ID -> Last received chunk (frame ID, chunk ID, hashed ID)
  private final Map<Integer, List<Integer>> lastReceivedChunkMap = new HashMap<>();

  /**
   * Constructs {@code VehicleDataHandlerImpl} instance.
   * @param taskScheduler  The task scheduler.
   */
  public VehicleMessageHandlerImpl(TaskScheduler taskScheduler, SensorDataHandler sensorDataHandler,
                                BandwidthEstimator bandwidthEstimator) {
    this.taskScheduler = taskScheduler;
    this.sensorDataHandler = sensorDataHandler;
    this.bandwidthEstimator = bandwidthEstimator;
    this.statHandler = sensorDataHandler.statHandler;
  }

  /**
   * Parses the data received on the edge, sent by a vehicle.
   *
   * For each vehicle message,
   * the first four bytes indicate the payload size,
   * next four bytes indicate the vehicle ID,
   * next two bytes indicate the sensor data frame ID,
   * next two bytes indicate the chunk/piece ID (meaningless if the file type is oxts),
   * next one byte indicates the file type ('P', 'O')
   * and the rest of the data in the message is the payload which contains
   * the compressed point cloud data ('P') or oxts data ('O').
   *
   * Once a full message is received, the handler submits the data to the EMP
   * processing pipeline.
   *
   * @param byteBuffer  The data currently received on the edge.  It can be partial message.
   * @return vehicle ID.
   */
  @Override
  public int handle(ByteBuffer byteBuffer) {
    int returnVehicleId = -1;
    if (byteBuffer.position() >= HEADER_BYTES) {
      // Message header fully received
      int size = byteBuffer.getInt(0);
      int vehicleId = byteBuffer.getInt(4);
      returnVehicleId = vehicleId;
      int frameId = byteBuffer.getShort(8);
      int chunkId = byteBuffer.getShort(10);
      char fileType = (char)byteBuffer.get(12);
      int payloadLength = byteBuffer.position() - HEADER_BYTES;
//      LOGGER.info("frame: " + frameId + "; fileType: " + fileType
//              + "; payload/size: " + payloadLength + "/" + size);

      // Update bandwidth estimator when receiving point clouds
      if (fileType == 'P' || fileType == 'R') {
        List<Integer> lastReceivedChunk = lastReceivedChunkMap.computeIfAbsent(vehicleId,
                k -> Arrays.asList(frameId, chunkId, 0));
        int hashedId = lastReceivedChunk.get(2);
        if (lastReceivedChunk.get(0) != frameId || lastReceivedChunk.get(1) != chunkId) {
          hashedId += 1;
          lastReceivedChunk.set(0, frameId);
          lastReceivedChunk.set(1, chunkId);
          lastReceivedChunk.set(2, hashedId);
        }
//        LOGGER.info("vehicle: " + vehicleId + "; frame: " + frameId + "; chunk: " + chunkId + "; hashed: "
//                + hashedId + "; payload/size: " + payloadLength + "/" + size);

        Map<Integer, List<Integer>> frameSizeMap = new HashMap<>();
        List<Integer> sizeList = new ArrayList<>();
        if (payloadLength > size) sizeList.add(size+HEADER_BYTES);
        else sizeList.add(payloadLength+HEADER_BYTES);
        sizeList.add(size+HEADER_BYTES);
        frameSizeMap.put(hashedId, sizeList);
        bandwidthEstimator.onReceiveData(vehicleId, frameSizeMap);
      }

      if (payloadLength >= size) {
        // A complete message/chunk is received
        byte[] byteBufferArray = byteBuffer.array();
        if (fileType == 'P') { // point cloud
          byte[] bytes = Arrays.copyOfRange(byteBufferArray, HEADER_BYTES, HEADER_BYTES + size);
          statHandler.logReceiveTime(frameId, vehicleId, chunkId, System.currentTimeMillis());
          SensorDataChunk chunk = SensorDataChunk.builder().vehicleId(vehicleId).frameId(frameId).
                  chunkId(chunkId).compressedPointCloud(bytes).build();
          taskScheduler.submit(new DecodingTask(chunk, sensorDataHandler));
//          LOGGER.info("[estbw] " + bandwidthEstimator.getEstimatedBW(vehicleId, "ewma"));
//          LOGGER.info("[ptcl task] vehicle: " + vehicleId + "; frame: " + frameId);
        }
        else if (fileType == 'R') { // raw (uncompressed) point cloud
          int currentPosition = byteBuffer.position();
          byteBuffer.position(HEADER_BYTES);
          FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
          float[] floats = new float[size / 4];
          floatBuffer.get(floats, 0, size / 4);
          byteBuffer.position(currentPosition);
          statHandler.logReceiveTime(frameId, vehicleId, chunkId, System.currentTimeMillis());
          SensorDataChunk chunk = SensorDataChunk.builder().vehicleId(vehicleId).frameId(frameId).
                  chunkId(chunkId).decodedPointCloud(floats).build();
          taskScheduler.submit(new MergingTask(chunk, sensorDataHandler));
        }
        else if (fileType == 'O') {  // oxts
          byte[] bytes = Arrays.copyOfRange(byteBufferArray, HEADER_BYTES, HEADER_BYTES + size);
          String[] oxtsString = new String(bytes).split(" ");
          float[] oxts = new float[oxtsString.length];
          for (int i = 0; i < oxtsString.length; i++) {
            oxts[i] = Float.parseFloat(oxtsString[i]);
          }
          taskScheduler.submit(new LocationUpdatingTask(vehicleId, frameId, oxts, sensorDataHandler));
          if (vehicleId == 1) { taskScheduler.submit(new MergingTask(frameId, sensorDataHandler)); }
//          LOGGER.info("[oxts task] vehicle: " + vehicleId + "; frame: " + frameId);
        }
        else if (fileType == 'X') {  // empty message after connecting
        }
        else {
          LOGGER.error("Wrong data type received: " + fileType);
        }

        // TODO: make this more efficient in the future
        int oldPosition = byteBuffer.position();
        byteBuffer.rewind();
        int offset = HEADER_BYTES + size;
        byteBuffer.position(oldPosition - offset);
        for (int i = 0; i < oldPosition - offset; i++) {
          byteBufferArray[i] = byteBufferArray[i+offset];
        }
        handle(byteBuffer);
      }
    }
    return returnVehicleId;
  }

  /**
   * @return  The mapping of vehicle ID to current ongoing sensor data chunk.
   */
  public Map<Integer, SensorDataChunk> getChunkMap() {
    return chunkMap;
  }

  /**
   * Encode the data to be sent to a vehicle.
   *
   * For each edge message,
   * the first four bytes indicate the payload size,
   * next two bytes indicate the frame ID if needed (-1 if unused),
   * next two bytes indicate the chunk/piece ID (meaningless if the file type is oxts),
   * next one byte indicates the file type ('I', 'M', 'S', 'D')
   * and the rest of the data in the message is the payload which contains
   * the object detection results ('I') or partitioning decision ('M') or nothing ('S', 'D').
   *
   * Once a full message is received, the handler submits the data to the EMP
   * processing pipeline.
   *
   * @param payload  The data to be sent.
   * @param frameID  Frame index.
   * @param msgType  Message type.
   * @return array of bytes to be sent.
   */
  public static byte[] encodeMessage(byte[] payload, int frameID, char msgType) {
    /*
    header = payload size (4) + frameID (2) + file type (1)
    payload = content
    payload size = len(payload)
    fileType = I (inference results) or M (partitioning decision) or S (start signal) or D (finish signal)
     */

    byte[] result = new byte[payload.length + 4 + 2 + 1];
    byte[] size = ByteBuffer.allocate(4).putInt(payload.length).array();
    byte[] fId = ByteBuffer.allocate(2).putShort((short)frameID).array();
    byte[] type = ByteBuffer.allocate(1).put((byte)msgType).array();
    System.arraycopy(size, 0, result, 0, 4);
    System.arraycopy(fId, 0, result, 4, 2);
    System.arraycopy(type, 0, result, 4 + 2, 1);
    System.arraycopy(payload, 0, result, 4 + 2 + 1, payload.length);
    return result;
  }

  public static byte[] floatArrayListToByteArray(Object listOfArray) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = null;
    byte[] outputByteArray;
    try {
      out = new ObjectOutputStream(bos);
      out.writeObject(listOfArray);
      out.flush();
      outputByteArray = bos.toByteArray();
    } finally {
      try {
        bos.close();
      } catch (IOException ex) {}
    }
    return outputByteArray;
  }
}
