package org.emp.utils;

import java.util.Arrays;
import java.util.Random;

public class GroundDetector {
    // Max number of iterations in one detection, should not be too small
    private static final int maxIterations = 10000;
    // Point dimension (x, y, z, intensity)
    private static final int dimension = 4;
    // Number of samples in each iteration
    private static final int numSample = 3;
    // Distance to the estimated plane
    private static final float distanceThreshold = 0.1f;
    // Probability in RANSAC
    private static final float P = 0.99f;
    // Upper limit of the filtering range
    private static final float filterRangeUp = 0.4f;
    // Lower limit of the filtering range
    private static final float filterRangeDown = -0.2f;
    // Offset of LiDAR to vehicle roof (height)
    private static final float lidarOffset = 0.1f;
    private static final float alphaThreshold = 0.03f;

    private float[] groundPoints;
    private float[] objectPoints;
    private float[] bestModelCoeffs;
    private float bestModelAlpha;

    private Random rand;

    public GroundDetector() {
        this.groundPoints = null;
        this.objectPoints = null;
        this.bestModelCoeffs = null;
        this.bestModelAlpha = 999.0f;
        this.rand = new Random(12345);
    }

    public boolean groundDetectorRANSAC(float[] data, boolean useAllSample, float egoObjectHeight, boolean filterGroundPoints) {
        this.cleanUp();

        // Number of total points
        int numPoints = data.length / dimension;
        // Indices of filtered points
        int[] filteredPointIds = new int[numPoints];
        // Number of filtered points
        int numFilteredPoints = 0;

        if (!useAllSample) {
            // Use height to filter
            float lidarHeight = -egoObjectHeight - lidarOffset + filterRangeUp;
            float lidarHeightDown = -egoObjectHeight - lidarOffset + filterRangeDown;
            for (int j = 0; j < numPoints; j++) {
                if (data[j * dimension + 2] < lidarHeight && data[j * dimension + 2] > lidarHeightDown) {
                    filteredPointIds[numFilteredPoints] = j;
                    numFilteredPoints += 1;
                }
            }
        } else {
            for (int j = 0; j < numPoints; j++) {
                filteredPointIds[numFilteredPoints] = j;
                numFilteredPoints += 1;
            }
        }

        if (numPoints < 1900 || numFilteredPoints < 180) {
            return false;
        }

        int maxNumGroundPoints = -999;
        float[] bestModel = null;
        int[] bestFilt = null;
        int bestFiltData = 0;
        int[] bestFiltObject = null;
        int bestFiltObjectData = 0;
        float alpha = 999.0f;
        int i = 0;
        float K = maxIterations;

        while (i < K) {
            // Randomly pick 3 points, return their index in filteredPointIds
            int[] sampleIds = getUniqueRandomInts(0, numFilteredPoints, numSample);
            float[] samples = new float[numSample * dimension];
            for (int j = 0; j < numSample; j++) {
                for (int t = 0; t < dimension; t++) {
                    samples[j * dimension + t] = data[filteredPointIds[sampleIds[j]] * dimension + t];
                }
            }

            // Calculate the coefficients of the plane
            float[] coeffs = estimatePlane(samples, false);
            if (coeffs == null) {
                continue;
            }

            // Norm of the normal vector
            float r = (float) Math.sqrt(coeffs[0] * coeffs[0] + coeffs[1] * coeffs[1] + coeffs[2] * coeffs[2]);
            // Angle of the normal factor and the z-axis
            float alphaZ = (float) Math.acos(Math.abs(coeffs[2]) / r);

            // Calculate distance of every point and the plane
            float[] d = matrixSplitMultAddAbsDiv(data, numPoints, dimension, 3, coeffs, 4, 3, 1, coeffs[3], r);
            // Filter the points according to the distance threshold
            int[] groundPointIds = new int[numPoints];
            int numGroundPoints = 0;
            int[] objectPointIds = new int[numPoints];
            int numObjectPoints = 0;
            for (int j = 0; j < numPoints; j++) {
                if (d[j] < distanceThreshold) {
                    groundPointIds[numGroundPoints] = j;
                    numGroundPoints += 1;
                } else {
                    objectPointIds[numObjectPoints] = j;
                    numObjectPoints += 1;
                }
            }

            if (numGroundPoints > maxNumGroundPoints && alphaZ < alphaThreshold) {
                maxNumGroundPoints = numGroundPoints;

                bestModel = coeffs;
                bestFilt = groundPointIds;
                bestFiltData = numGroundPoints;
                bestFiltObject = objectPointIds;
                bestFiltObjectData = numObjectPoints;
                alpha = alphaZ;

                float w = numGroundPoints / numPoints;
                float wn = w * w * w;
                float pNoOutliers = 1.0f - wn;
                K = (float) (Math.log(1.0f - P) / Math.log(pNoOutliers));
            }

            i += 1;

            if (i > maxIterations) {
                break;
            }
        }
        if (filterGroundPoints) {
            this.groundPoints = new float[bestFiltData * dimension];
            for (int j = 0; j < bestFiltData; j++) {
                for (int t = 0; t < dimension; t++) {
                    this.groundPoints[j * dimension + t] = data[bestFilt[j] * dimension + t];
                }
            }
        }
        this.objectPoints = new float[bestFiltObjectData * dimension];
        for (int j = 0; j < bestFiltObjectData; j++) {
            for (int t = 0; t < dimension; t++) {
                this.objectPoints[j * dimension + t] = data[bestFiltObject[j] * dimension + t];
            }
        }
        this.bestModelCoeffs = bestModel;
        this.bestModelAlpha = alpha;

        return true;
    }

    public float[] getGroundPoints() {
        return this.groundPoints;
    }

    public float[] getObjectPoints() {
        return this.objectPoints;
    }

    public float[] getBestModelCoeffs() {
        return this.bestModelCoeffs;
    }

    public float getBestModelAlpha() {
        return this.bestModelAlpha;
    }

    private float[] estimatePlane(float[] xyz, boolean normalize) {
        float[] result = {-65536.0f, -65536.0f, -65536.0f, -65536.0f};

        float[] vector1 = new float[3];
        float[] vector2 = new float[3];
        vector1[0] = xyz[1*dimension+0] - xyz[0*dimension+0];
        vector1[1] = xyz[1*dimension+1] - xyz[0*dimension+1];
        vector1[2] = xyz[1*dimension+2] - xyz[0*dimension+2];
        vector2[0] = xyz[2*dimension+0] - xyz[0*dimension+0];
        vector2[1] = xyz[2*dimension+1] - xyz[0*dimension+1];
        vector2[2] = xyz[2*dimension+2] - xyz[0*dimension+2];

        // Check if will be divided by zero
        if(vector1[0]*vector1[1]*vector1[2] == 0.0f) {
            return null;
        }

        float[] dy1dy2 = new float[3];
        for(int i = 0; i < 3; i++) {
            dy1dy2[i] = vector2[i] / vector1[i];
        }
        // Three points in line
        if(!(dy1dy2[0] != dy1dy2[1] || dy1dy2[2] != dy1dy2[1])) {
            return null;
        }

        float a = (vector1[1]*vector2[2]) - (vector1[2]*vector2[1]);
        float b = (vector1[2]*vector2[0]) - (vector1[0]*vector2[2]);
        float c = (vector1[0]*vector2[1]) - (vector1[1]*vector2[0]);

        // Normalize
        if(normalize) {
            float r = (float)Math.sqrt((double)(a*a + b*b + c*c));
            a = a / r;
            b = b / r;
            c = c / r;
        }
        float d = -(a*xyz[0*dimension+0] + b*xyz[0*dimension+1] + c*xyz[0*dimension+2]);

        result[0] = a;
        result[1] = b;
        result[2] = c;
        result[3] = d;

        return result;
    }

    private int[] getUniqueRandomInts(int min, int max, int n) {
        int[] ints = new int[n];
        int range = max - min;
        boolean[] isUsed = new boolean[range];
        int sample;
        for (int i = 0; i < n; i++) {
            do {
                sample = rand.nextInt(range);
            } while (isUsed[sample]);
            isUsed[sample] = true;
            ints[i] = sample + min;
        }
        return ints;
    }

    private float[] matrixSplitMultAddAbsDiv(float[] matA, int rowA, int colA, int splitColA, float[] matB, int rowB, int splitRowB, int colB, float addItem, float divItem) {
        float[] result = new float[rowA*colB];

        for(int i = 0; i < rowA; i++) {
            for(int j = 0; j < colB; j++) {
                float temp = 0.0f;
                for(int k = 0; k < splitColA; k++) {
                    temp += matA[i*colA+k] * matB[k*colB+j];
                }
                result[i*colB+j] = Math.abs(temp + addItem) / divItem;
            }
        }
        return result;
    }

    private void cleanUp() {
        this.groundPoints = null;
        this.objectPoints = null;
        this.bestModelCoeffs = null;
        this.bestModelAlpha = 999.0f;
    }
}