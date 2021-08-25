package org.emp.utils;

/**
 * Helper class to call PointPillars 3D object detection using Inference library
 */
public class InferenceHelper {
    static {
        System.loadLibrary("EmpNative");
    }

    private long nativeInference;

    private native long createNativeObject();

    public InferenceHelper() {
        nativeInference = createNativeObject();
    }

    /**
     * Do inference on point cloud data using PointPillars.
     *
     * @param data  A float array of size 4 * n, with n as the number of points.  Each point has
     *              four float: the coordinate (x, y, z) and the intensity.
     * @return  Detected object (2D/3D bounding box dimensions, location, rotation_z, score).
     */
    public native byte[] executeModel(float[] data);
}
