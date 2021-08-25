package org.emp.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Set;
import org.emp.data.SensorDataChunk;
import org.emp.data.SensorDataHandler;
import org.emp.data.VehicleLocation;
import org.emp.network.NonBlockingNetworkServer;

public final class TestUtils {
  private static final int BUFFER_MAX_BYTES = 4 * 1024 * 1024;

  /**
   * Reads uncompressed point cloud data from a binary file.
   * Each point has four float: the coordinate (x, y, z) and the intensity.
   *
   * @param filePath  The binary file containing the point cloud data.
   * @return  A float array of size 4 * n, with n as the number of points.
   * @throws IOException  upon I/O error.
   */
  public static float[] readPointCloudFromFile(String filePath) throws IOException {
    RandomAccessFile file = new RandomAccessFile(filePath, "r");
    FileChannel channel = file.getChannel();
    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_MAX_BYTES);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.clear();

    channel.read(buffer);

    float[] points = new float[buffer.position() / Float.BYTES];
    buffer.rewind();
    buffer.asFloatBuffer().get(points);

    channel.close();
    return points;
  }

  public static void writePointCloudToFile(float[] pointCloud, String filePath) throws IOException {
    RandomAccessFile file = new RandomAccessFile(filePath, "rw");
    FileChannel channel = file.getChannel();
    ByteBuffer byteBuffer = ByteBuffer.allocate(4 * pointCloud.length);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
    floatBuffer.put(pointCloud);
    floatBuffer.flip();

    channel.write(byteBuffer);

    file.close();
    channel.close();
  }

  public static float[] loadOxts(String fileName) throws IOException {
    float[] oxts = new float[6];
    File file = new File(fileName);
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String tempString;
    while ((tempString = reader.readLine()) != null) {
      String[] oxtsStrings = tempString.split(" ");
      for (int i = 0; i < oxtsStrings.length; i++) {
        oxts[i] = Float.parseFloat(oxtsStrings[i]);
      }
    }
    reader.close();

    return oxts;
  }

  public static float loadEgoObjectHeight(String fileName) throws IOException {
    float height = 0.0f;
    File file = new File(fileName);
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String tempString;
    while ((tempString = reader.readLine()) != null) {
      String[] egoObjectStrings = tempString.split(" ");
      height = Float.parseFloat(egoObjectStrings[8]);
    }
    reader.close();

    return height;
  }

  /**
   * @return A no-op {@link SensorDataHandler} for testing.
   */
  public static SensorDataHandler getNoopSensorDataHandler() {
    return new SensorDataHandler() {

      @Override
      public void saveDataChunk(SensorDataChunk dataChunk) {

      }

      @Override
      public void saveMergedPointCloud(int frameId, Set<Integer> vehicleIds) {

      }

      @Override
      public void updateVehicleLocation(int vehicleId, int frameId, VehicleLocation location) {

      }

      @Override
      public void pushUnmergedData(int vehicleId, int frameId, int chunkId) {

      }

      @Override
      public Map<Integer, Set<Integer>> popUnmergedData(int frameId) {
        return null;
      }

      @Override
      public float[] getDataChunk(int vehicleId, int frameId, int chunkId) {
        return new float[0];
      }

      @Override
      public VehicleLocation getVehicleLocation(int vehicleId, int frameId) {
        return null;
      }

      @Override
      public VehicleLocation getPrimaryLocation(int frameId) {
        return null;
      }

      @Override
      public FloatBuffer getMergedPointCloud (int frameId) {
        return null;
      }

      @Override
      public int updateMergedPointCloudOffset (int frameId, int dataSize) {
        return 0;
      }

      @Override
      public void updatePartitioningDecisions(int frameId) {

      }

      @Override
      public boolean shouldRunMerging(int frameId) {
        return false;
      }

      @Override
      public boolean shouldRunObjectDetection() {
        return false;
      }

      @Override
      public void setNetworkServer(NonBlockingNetworkServer networkServer) {

      }
    };
  }
}
