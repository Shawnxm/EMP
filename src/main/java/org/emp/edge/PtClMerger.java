package org.emp.edge;

import java.nio.FloatBuffer;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class PtClMerger {

    private static final String mergeModelName = "torch-merge-model.pt1";

    // transformation matrix - rotation (to the perspective of oxts1)
    private float[] rotate(float[] oxts1, float[] oxts2, boolean transpose) {
        double dYaw = (double)oxts2[5] - (double)oxts1[5];
        double dPitch = (double)oxts2[4] - (double)oxts1[4];
        double dRoll = (double)oxts2[3] - (double)oxts1[3];

        float[] rZ = {
            (float)Math.cos(dYaw), -(float)Math.sin(dYaw), 0.0f, 0.0f,
            (float)Math.sin(dYaw), (float)Math.cos(dYaw), 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
        float[] rY = {
            (float)Math.cos(dPitch), 0.0f, (float)Math.sin(dPitch), 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            -(float)Math.sin(dPitch), 0.0f, (float)Math.cos(dPitch), 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
        float[] rX = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, (float)Math.cos(dRoll), -(float)Math.sin(dRoll), 0.0f,
            0.0f, (float)Math.sin(dRoll), (float)Math.cos(dRoll), 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };

        float[] rotation2 = matrixMultiplication(rZ, 4, 4, rY, 4, 4);
        float[] rotation3 = matrixMultiplication(rotation2, 4, 4, rX, 4, 4);
        // System.out.println("Rotation3:");
        // for(int i = 0; i < 4; i++){
        //     for(int j = 0; j < 4; j++){
        //         System.out.print(rotation3[i*4+j] + " ");
        //     }
        //     System.out.println("");
        // }

        // transpose
        float[] rotation = new float[16];

        if(transpose){
            for(int i = 0; i < 4; i++){
                for(int j = 0; j < 4; j++){
                    rotation[j*4+i] = rotation3[i*4+j];
                }
            }
        }
        else{
            rotation = rotation3;
        }

        return rotation;
    }

    // transformation matrix - translation (to the perspective of oxts1)
    private float[] translate(float[] oxts1, float[] oxts2) {
        float da = oxts2[0] - oxts1[0];  // south --> north
        float db = oxts2[1] - oxts1[1];  // east --> west
        float dx = da * (float)Math.cos(oxts1[5]) + db * (float)Math.sin(oxts1[5]);
        float dy = da * (-(float)Math.sin(oxts1[5])) + db * (float)Math.cos(oxts1[5]);
        float dz = oxts2[2] - oxts1[2];
        float[] translation = {dx, dy, dz, 0.0f};

        // System.out.println("Translation: " + translation[0] + " " + translation[1] + " " + translation[2] + " " + translation[3]);

        return translation;
    }

    public float[] naiveMerge(float[] pointsPrimary, float[] oxtsPrimary,
        List<float[]> pointsSecondary, List<float[]> oxtsSecondary) {

        int totalSize = pointsPrimary.length / 4;
        for(int i = 0; i < pointsSecondary.size(); i++){
            totalSize += pointsSecondary.get(i).length / 4;
        }

        float[] pcl = new float[totalSize*4];

        float[] concated = pointsPrimary;

        for(int i = 0; i < pointsSecondary.size(); i++){
            float[] rotationT = rotate(oxtsPrimary, oxtsSecondary.get(i), true);
            float[] translation = translate(oxtsPrimary, oxtsSecondary.get(i));
            float[] result = matrixMulAdd(pointsSecondary.get(i), pointsSecondary.get(i).length/4, 4, rotationT, 4, 4, translation);
            concated = ArrayUtils.addAll(concated, result);
        }

        return concated;

    }

    public float[] naiveMergeNoPrimary(float[] oxtsPrimary, List<float[]> pointsSecondary, List<float[]> oxtsSecondary) {
        float[] concated = new float[0];

        for(int i = 0; i < pointsSecondary.size(); i++) {
            float[] rotationT = rotate(oxtsPrimary, oxtsSecondary.get(i), true);
            float[] translation = translate(oxtsPrimary, oxtsSecondary.get(i));
            float[] result = matrixMulAdd(pointsSecondary.get(i), pointsSecondary.get(i).length / 4, 4, rotationT, 4, 4, translation);
            concated = ArrayUtils.addAll(concated, result);
        }

        return concated;

    }

    // Merging using FloatBuffer to save memory copy time
    public float[] fbMerge(float[] pointsPrimary, float[] oxtsPrimary, List<float[]> pointsSecondary, List<float[]> oxtsSecondary) {

        int totalSize = pointsPrimary.length / 4;
        for (float[] points : pointsSecondary) {
            totalSize += points.length / 4;
        }

        FloatBuffer concated = FloatBuffer.allocate(totalSize*4);
        int offset = 0;

        for (float point : pointsPrimary) {
            concated.put(point);
        }
        offset += pointsPrimary.length;

        for(int i = 0; i < pointsSecondary.size(); i++){
            float[] rotationT = rotate(oxtsPrimary, oxtsSecondary.get(i), true);
            float[] translation = translate(oxtsPrimary, oxtsSecondary.get(i));
            matrixMulAddFB(pointsSecondary.get(i), pointsSecondary.get(i).length/4, 4, rotationT, 4, 4, translation, concated, offset);
            offset += pointsSecondary.get(i).length;
        }

        return concated.array();
    }

    public int fbMergeNoPrimary(float[] oxtsPrimary, List<float[]> pointsSecondary, List<float[]> oxtsSecondary, FloatBuffer result, int offset) {

        for(int i = 0; i < pointsSecondary.size(); i++){
            // System.out.println("offset: "+ offset);
            float[] rotationT = rotate(oxtsPrimary, oxtsSecondary.get(i), true);
            float[] translation = translate(oxtsPrimary, oxtsSecondary.get(i));
            matrixMulAddFB(pointsSecondary.get(i), pointsSecondary.get(i).length/4, 4, rotationT, 4, 4, translation, result, offset);
            offset += pointsSecondary.get(i).length;
        }

        return offset;
    }

    private float[] matrixMultiplication(float[] matA, int rowA, int colA, float[] matB, int rowB, int colB){
        float[] result = new float[rowA*colB];

        for(int i = 0; i < rowA; i++){
            for(int j = 0; j < colB; j++){
                float temp = 0.0f;
                for(int k = 0; k < colA; k++){
                    temp += matA[i*colA+k] * matB[k*colB+j];
                }
                result[i*colB+j] = temp;
            }
        }

        return result;
    }

    private float[] matrixMulAdd(float[] matA, int rowA, int colA, float[] matB, int rowB, int colB, float[] matC){
        float[] result = new float[rowA*colB];

        for(int i = 0; i < rowA; i++){
            for(int j = 0; j < colB; j++){
                float temp = 0.0f;
                for(int k = 0; k < colA; k++){
                    temp += matA[i*colA+k] * matB[k*colB+j];
                }
                result[i*colB+j] = temp + matC[j];
            }
        }

        return result;
    }

    private void matrixMulAddFB(float[] matA, int rowA, int colA, float[] matB, int rowB, int colB, float[] matC, FloatBuffer fb, int offset){
        for(int i = 0; i < rowA; i++){
            for(int j = 0; j < colB; j++){
                float temp = 0.0f;
                for(int k = 0; k < colA; k++){
                    temp += matA[i*colA+k] * matB[k*colB+j];
                }
                // result[i*colB+j] = temp + matC[j];
                fb.put(offset+i*colB+j, temp + matC[j]);
            }
        }
    }
}