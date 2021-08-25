package org.emp.utils;

/**
 * Helper class to encode and decode point cloud data using Draco library
 */
public class DracoHelper {
  static {
    System.loadLibrary("EmpNative");
  }

  /**
   * Encodes point cloud data using Draco.
   *
   * @param data  A float array of size 4 * n, with n as the number of points.  Each point has
   *              four float: the coordinate (x, y, z) and the intensity.
   * @param cl  cl value
   * @param qb  qb value
   * @return  Encoded data in byte array.
   */
  public native byte[] encode(float[] data, int cl, int qb);

  /**
   * Decodes point cloud data using Draco.
   *
   * @param data  Encoded data in byte array.
   * @return  A float array of size 4 * n, with n as the number of points.  Each point has
   * four float: the coordinate (x, y, z) and the intensity.
   */
  public native float[] decode(byte[] data);
}
