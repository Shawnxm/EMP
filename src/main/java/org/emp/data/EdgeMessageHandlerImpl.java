package org.emp.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.network.BandwidthEstimator;
import org.emp.task.DecodingTask;
import org.emp.task.LocationUpdatingTask;
import org.emp.task.MergingTask;
import org.emp.task.TaskScheduler;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * An implementation of {@code EdgeMessageHandler} that follows the
 * serialization protocol between the vehicle and the edge.
 */
public class EdgeMessageHandlerImpl implements EdgeMessageHandler {
  private static final Logger LOGGER = LogManager.getLogger(EdgeMessageHandlerImpl.class);

  public static final int HEADER_BYTES = 7;
  private List<float[]> mask;
  private boolean ready;
  private int frameToSent = 0;

  public EdgeMessageHandlerImpl() {
  }

  @Override
  public void handle(ByteBuffer byteBuffer) throws IOException, ClassNotFoundException {
    if (byteBuffer.position() >= HEADER_BYTES) {
      // Message header fully received
      int size = byteBuffer.getInt(0);
      int frameId = byteBuffer.getShort(4);
      char fileType = (char)byteBuffer.get(6);
      int payloadLength = byteBuffer.position() - HEADER_BYTES;
//      LOGGER.info("frame: " + frameId + "; fileType: " + fileType
//              + "; payload/size: " + payloadLength + "/" + size);

      if (payloadLength >= size) {
        // A complete message/chunk is received
        byte[] byteBufferArray = byteBuffer.array();
        byte[] bytes = Arrays.copyOfRange(byteBufferArray, HEADER_BYTES, HEADER_BYTES + size);
        if (fileType == 'I') { // inference results
          // TODO: to be finished
        }
        else if (fileType == 'M') {  // "mask", partitioning decisions
          mask = byteArrayToFloatArrayList(bytes);
          for (float[] fArray : mask) {
            LOGGER.info("[Mask] frame: " + frameId + " " + Arrays.toString(fArray));
          }
        }
        else if (fileType == 'S') {  // "start" signal, start the entire workflow
          ready = true;
          LOGGER.info("[Begin]");
        }
        else if (fileType == 'D') {  // "finish" signal, stop uploading the current frame
          frameToSent = frameId + 1;
          LOGGER.info("[Finish] frame: " + frameId);
        }
        else {
          LOGGER.error("Wrong data type received: " + fileType);
        }

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
  }

  public static List<float[]> byteArrayToFloatArrayList(byte[] inputByteArray) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bis = new ByteArrayInputStream(inputByteArray);
    ObjectInput in = null;
    Object floatArrayList;
    try {
      in = new ObjectInputStream(bis);
      floatArrayList = in.readObject();
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException ex) {}
    }
    return (List<float[]>)floatArrayList;
  }

  public static byte[] encodeMessage(byte[] payload, int vehicleID, int frameID, int pieceID, char fileType) {
    /*
    header = payload size (4) + vehicleID (4) + frameID (2) + pieceID (2) + file type (1)
    payload = content
    payload size = len(payload)
    fileType = P (ptcl) or O (oxts)
    pieceID: 1 or 2 or 3 or 4 or 5 (no partition)
     */

    byte[] result = new byte[payload.length + 4 + 4 + 2 + 2 + 1];
    byte[] size = ByteBuffer.allocate(4).putInt(payload.length).array();
    byte[] vId = ByteBuffer.allocate(4).putInt(vehicleID).array();
    byte[] fId = ByteBuffer.allocate(2).putShort((short)frameID).array();
    byte[] pId = ByteBuffer.allocate(2).putShort((short)pieceID).array();
    byte[] type = ByteBuffer.allocate(1).put((byte)fileType).array();
    System.arraycopy(size, 0, result, 0, 4);
    System.arraycopy(vId, 0, result, 4, 4);
    System.arraycopy(fId, 0, result, 4 + 4, 2);
    System.arraycopy(pId, 0, result, 4 + 4 + 2, 2);
    System.arraycopy(type, 0, result, 4 + 4 + 2 + 2, 1);
    System.arraycopy(payload, 0, result, 4 + 4 + 2 + 2 + 1, payload.length);

    return result;
  }

  public List<float[]> getMask() {
    return mask;
  }

  public boolean isReady() {
    return ready;
  }

  public int getFrameToSent() {
    return frameToSent;
  }
}
