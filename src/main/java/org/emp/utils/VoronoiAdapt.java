package org.emp.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.ArrayList;

public class VoronoiAdapt {
    private static final Logger LOGGER = LogManager.getLogger(VoronoiAdapt.class);

    int dimension = 4;  // point cloud format: n*4 2D array

    public static class partitionDecision {
        public List<ArrayList<float[]>> pbSet = new ArrayList<ArrayList<float[]>>();
        public List<ArrayList<Integer>> neighbors = new ArrayList<ArrayList<Integer>>();
        List<Integer> indexSet = new ArrayList<Integer>();

        public partitionDecision(int oxtslength) {
            for (int i = 0; i < oxtslength; i++) {
                pbSet.add(new ArrayList<float[]>());
                neighbors.add(new ArrayList<Integer>());
            }
        }
    }

    public partitionDecision voronoiBasic(List<float[]> oxtsSet) {
        partitionDecision decision = new partitionDecision(oxtsSet.size());
        List<float[]> vehSet = new ArrayList<float[]>();

        for (int i = 0; i < oxtsSet.size(); i++) {
            decision.indexSet.add(i);
            vehSet.add(oxtsSet.get(i));
        }
        int numVehs = decision.indexSet.size();

        if (numVehs == 1)
            return decision;
        else if (numVehs == 2) {
            decision.neighbors.get(0).add(1);
            decision.neighbors.get(1).add(0);
        }
        else {
            // Apply Voronoi; Prepare Data
            double[] xValuesIn = new double[oxtsSet.size()];
            double[] yValuesIn = new double[oxtsSet.size()];
            for (int i = 0; i < oxtsSet.size(); i++) {
                float[] tempOxts = oxtsSet.get(i);
                xValuesIn[i] = (double)tempOxts[0];
                yValuesIn[i] = (double)tempOxts[1];
            }
            float[] minMaxOxtsSetXY = getMinMaxOxtsSetXY(oxtsSet);
            double minX = (double)minMaxOxtsSetXY[0] - 1.0;
            double maxX = (double)minMaxOxtsSetXY[1] + 1.0;
            double minY = (double)minMaxOxtsSetXY[2] - 1.0;
            double maxY = (double)minMaxOxtsSetXY[3] + 1.0;

            // get all neighbor pairs
            Voronoi myVoronoi = new Voronoi(0.0);
            myVoronoi.generateVoronoi(xValuesIn, yValuesIn, minX, maxX, minY, maxY);
            List<int[]> allNeighborPairs = myVoronoi.getAllNeighborPairs();
            for (int i = 0; i < allNeighborPairs.size(); i++) {
                int[] pair = allNeighborPairs.get(i);
                decision.neighbors.get(pair[0]).add(pair[1]);
                decision.neighbors.get(pair[1]).add(pair[0]);
            }
        }

        List<ArrayList<float[]>> vehs = new ArrayList<ArrayList<float[]>>();
        for (int i = 0; i < numVehs; i++) {
            vehs.add(new ArrayList<float[]>());
        }
        // LOGGER.info(decision.neighbors);
        for (int i = 0; i < numVehs; i++) {
            for (int tmp = 0; tmp < decision.neighbors.get(i).size(); tmp++) {
                int j = decision.neighbors.get(i).get(tmp);
                vehs.get(i).add(transformation(vehSet.get(i), vehSet.get(j)));
            }
            for (int x = 0; x < decision.neighbors.get(i).size(); x++)
                decision.pbSet.get(i).add(calculatePb(new float[]{0.0f, 0.0f}, vehs.get(i).get(x), true));
        }
        return decision;
    }

    public partitionDecision voronoiBW(List<float[]> oxtsSet, List<Float> bwSet) {
        partitionDecision decision = new partitionDecision(oxtsSet.size());

        if (bwSet == null) {
            bwSet = new ArrayList<Float>();
            for (int i = 0; i < oxtsSet.size(); i++) {
                bwSet.add(1.0f);
            }
        }

        List<float[]> vehSet = new ArrayList<float[]>();
        for (int i = 0; i < oxtsSet.size(); i++) {
            decision.indexSet.add(i);
            vehSet.add(oxtsSet.get(i));
        }
        int numVehs = decision.indexSet.size();

        if (numVehs == 1)
            return decision;
        else if (numVehs == 2) {
            decision.neighbors.get(0).add(1);
            decision.neighbors.get(1).add(0);
        }
        else {
            // Apply Voronoi; Prepare Data
            double[] xValuesIn = new double[oxtsSet.size()];
            double[] yValuesIn = new double[oxtsSet.size()];
            for (int i = 0; i < oxtsSet.size(); i++) {
                float[] tempOxts = oxtsSet.get(i);
                xValuesIn[i] = (double)tempOxts[0];
                yValuesIn[i] = (double)tempOxts[1];
            }
            float[] minMaxOxtsSetXY = getMinMaxOxtsSetXY(oxtsSet);
            double minX = (double)minMaxOxtsSetXY[0] - 1.0;
            double maxX = (double)minMaxOxtsSetXY[1] + 1.0;
            double minY = (double)minMaxOxtsSetXY[2] - 1.0;
            double maxY = (double)minMaxOxtsSetXY[3] + 1.0;

            // get all neighbor pairs
            Voronoi myVoronoi = new Voronoi(0.0);
            myVoronoi.generateVoronoi(xValuesIn, yValuesIn, minX, maxX, minY, maxY);
            List<int[]> allNeighborPairs = myVoronoi.getAllNeighborPairs();
            for (int i = 0; i < allNeighborPairs.size(); i++) {
                int[] pair = allNeighborPairs.get(i);
                decision.neighbors.get(pair[0]).add(pair[1]);
                decision.neighbors.get(pair[1]).add(pair[0]);
            }
        }

        List<ArrayList<float[]>> vehs = new ArrayList<ArrayList<float[]>>();
        for (int i = 0; i < numVehs; i++) {
            vehs.add(new ArrayList<float[]>());
        }
        // LOGGER.info(decision.neighbors);
        // LOGGER.info(bwSet);
        for (int i = 0; i < numVehs; i++) {
            for (int tmp = 0; tmp < decision.neighbors.get(i).size(); tmp++) {
                int j = decision.neighbors.get(i).get(tmp);
                vehs.get(i).add(transformation(vehSet.get(i), vehSet.get(j)));
            }
            for (int x = 0; x < decision.neighbors.get(i).size(); x++) {
                int j = decision.neighbors.get(i).get(x);
                // LOGGER.info(String.format("[%d,%d]: %f, %f", i, j, bwSet.get(decision.indexSet.get(i)), bwSet.get(decision.indexSet.get(j))));
                decision.pbSet.get(i).add(calculatePerpendicularPower(new float[] {0.0f, 0.0f}, vehs.get(i).get(x),
                        true, bwSet.get(decision.indexSet.get(i))/2.0f, bwSet.get(decision.indexSet.get(j))/2.0f));
                // if(bwSet.get(decision.indexSet.get(i)) > 1000.0f || bwSet.get(decision.indexSet.get(j)) > 1000.0f){
                //     decision.pbSet.get(i).add(calculatePerpendicularPower(new float[] {0.0f, 0.0f}, vehs.get(i).get(x),
                //         true, bwSet.get(decision.indexSet.get(i))/100.0f, bwSet.get(decision.indexSet.get(j))/100.0f));
                // }
                // else if(bwSet.get(decision.indexSet.get(i)) > 100.0f || bwSet.get(decision.indexSet.get(j)) > 100.0f){
                //     decision.pbSet.get(i).add(calculatePerpendicularPower(new float[] {0.0f, 0.0f}, vehs.get(i).get(x),
                //         true, bwSet.get(decision.indexSet.get(i))/10.0f, bwSet.get(decision.indexSet.get(j))/10.0f));
                // }
                // else{
                //     decision.pbSet.get(i).add(calculatePerpendicularPower(new float[] {0.0f, 0.0f}, vehs.get(i).get(x),
                //         true, bwSet.get(decision.indexSet.get(i))/4.0f, bwSet.get(decision.indexSet.get(j))/4.0f));
                // }
            }
        }
        return decision;
    }

    public partitionDecision voronoiAdapt(List<float[]> oxtsSet, List<Float> bwSet) {
        partitionDecision decision = new partitionDecision(oxtsSet.size());

        if (bwSet == null) {
            bwSet = new ArrayList<Float>();
            for (int i = 0; i < oxtsSet.size(); i++) {
                bwSet.add(1.0f);
            }
        }
        List<float[]> vehSet = new ArrayList<float[]>();
        for (int i = 0; i < oxtsSet.size(); i++) {
            decision.indexSet.add(i);
            vehSet.add(oxtsSet.get(i));
        }
        int numVehs = decision.indexSet.size();


        if (numVehs == 1)
            return decision;
        else if (numVehs == 2) {
            decision.neighbors.get(0).add(1);
            decision.neighbors.get(1).add(0);
        }
        else {
            // Apply Voronoi; Prepare Data
            double[] xValuesIn = new double[oxtsSet.size()];
            double[] yValuesIn = new double[oxtsSet.size()];
            for (int i = 0; i < oxtsSet.size(); i++) {
                float[] tempOxts = oxtsSet.get(i);
                xValuesIn[i] = (double)tempOxts[0];
                yValuesIn[i] = (double)tempOxts[1];
            }
            float[] minMaxOxtsSetXY = getMinMaxOxtsSetXY(oxtsSet);
            double minX = (double)minMaxOxtsSetXY[0] - 1.0;
            double maxX = (double)minMaxOxtsSetXY[1] + 1.0;
            double minY = (double)minMaxOxtsSetXY[2] - 1.0;
            double maxY = (double)minMaxOxtsSetXY[3] + 1.0;

            // get all neighbor pairs
            Voronoi myVoronoi = new Voronoi(0.0);
            myVoronoi.generateVoronoi(xValuesIn, yValuesIn, minX, maxX, minY, maxY);
            List<int[]> allNeighborPairs = myVoronoi.getAllNeighborPairs();
            for (int i = 0; i < allNeighborPairs.size(); i++) {
                int[] pair = allNeighborPairs.get(i);
                decision.neighbors.get(pair[0]).add(pair[1]);
                decision.neighbors.get(pair[1]).add(pair[0]);
            }
        }

        List<ArrayList<float[]>> vehs = new ArrayList<ArrayList<float[]>>();
        for (int i = 0; i < numVehs; i++) {
            vehs.add(new ArrayList<float[]>());
        }

        for (int i = 0; i < numVehs; i++) {
            for (int tmp = 0; tmp < decision.neighbors.get(i).size(); tmp++) {
                int j = decision.neighbors.get(i).get(tmp);
                vehs.get(i).add(transformation(vehSet.get(i), vehSet.get(j)));
            }
            for (int x = 0; x < decision.neighbors.get(i).size(); x++) {
                int j = decision.neighbors.get(i).get(x);
                decision.pbSet.get(i).add(calculatePerpendicularPower2(new float[] {0.0f, 0.0f}, vehs.get(i).get(x),
                        true, bwSet.get(decision.indexSet.get(i))/2.0f, bwSet.get(decision.indexSet.get(j))/2.0f, 0.3f, 0.3f));
                // LOGGER.info(String.format("[%d,%d]: %f, %f", i, j, bwSet.get(decision.indexSet.get(i)), bwSet.get(decision.indexSet.get(j))));
                // if(bwSet.get(decision.indexSet.get(i)) > 1000.0f || bwSet.get(decision.indexSet.get(j)) > 1000.0f){
                //     decision.pbSet.get(i).add(calculatePerpendicularPower2(new float[] {0.0f, 0.0f}, vehs.get(i).get(x),
                //         true, bwSet.get(decision.indexSet.get(i))/100.0f, bwSet.get(decision.indexSet.get(j))/100.0f, 0.3f, 0.3f));
                // }
                // else if(bwSet.get(decision.indexSet.get(i)) > 100.0f || bwSet.get(decision.indexSet.get(j)) > 100.0f){
                //     decision.pbSet.get(i).add(calculatePerpendicularPower2(new float[] {0.0f, 0.0f}, vehs.get(i).get(x),
                //         true, bwSet.get(decision.indexSet.get(i))/10.0f, bwSet.get(decision.indexSet.get(j))/10.0f, 0.3f, 0.3f));
                // }
                // else{
                //     decision.pbSet.get(i).add(calculatePerpendicularPower2(new float[] {0.0f, 0.0f}, vehs.get(i).get(x),
                //         true, bwSet.get(decision.indexSet.get(i))/4.0f, bwSet.get(decision.indexSet.get(j))/4.0f, 0.3f, 0.3f));
                // }
            }
        }
        return decision;
    }

    public float[] voronoiMask(float[] pcl, List<float[]> pbSet) {
        if(pbSet.size() == 0){
            return pcl;
        }
        float[] zeros = {0.0f, 0.0f};

        // decide side
        for(int i = 0; i < pbSet.size(); i++){
            float[] pb_k = pbSet.get(i);
            float[] pb = {pb_k[0], pb_k[1]};
            float[] temp = matrixMultiplication(zeros, 1, 2, pb, 2, 1);
            if(temp[0] + pb_k[2] < 0){
                for(int j = 0; j < pb_k.length; j++){
                    pb_k[j] = -pb_k[j];
                }
            }
        }

        int nPoints = (int)(pcl.length / 4);
        int[] signs = new int[nPoints];
        for(int i = 0; i < nPoints; i++){
            signs[i] = 0;
        }
        for(int i = 0; i < pbSet.size(); i++){
            float[] pb_k = pbSet.get(i);
            float[] pb = {pb_k[0], pb_k[1]};
            float[] temp = splitMatrixMultiplication(pcl, nPoints, dimension, 2, pb, 2, 1);
            for(int j = 0; j < nPoints; j++){
                signs[j] += (temp[j] >= -pb_k[2] ? 1 : 0);
            }
        }
        int nPointsNew = 0;
        for(int i = 0; i < nPoints; i++){
            signs[i] = (signs[i] == pbSet.size() ? 1 : 0);
            nPointsNew += signs[i];
        }

        float[] pclNew = new float[nPointsNew*dimension];

        int idx = 0;
        for(int i = 0; i < nPoints; i++){
            if(signs[i] == 1){
                for(int j = 0; j < dimension; j++){
                    pclNew[idx*dimension+j] = pcl[i*dimension+j];
                }
                idx += 1;
            }
        }

        return pclNew;
    }

    public float[] voronoiMaskBW(float[] pcl, List<float[]> pbSet) {
        if(pbSet.size() == 0){
            return pcl;
        }

        int nPoints = (int)(pcl.length / 4);
        int[] signs = new int[nPoints];
        for(int i = 0; i < nPoints; i++){
            signs[i] = 0;
        }
        for(int i = 0; i < pbSet.size(); i++){
            float[] pb_k = pbSet.get(i);
            float[] pb = {pb_k[0], pb_k[1]};
            float[] temp = splitMatrixMultiplication(pcl, nPoints, dimension, 2, pb, 2, 1);
            for(int j = 0; j < nPoints; j++){
                signs[j] += (temp[j] >= -pb_k[2] ? 1 : 0);
            }
        }
        int nPointsNew = 0;
        for(int i = 0; i < nPoints; i++){
            signs[i] = (signs[i] == pbSet.size() ? 1 : 0);
            nPointsNew += signs[i];
        }

        float[] pclNew = new float[nPointsNew*dimension];

        int idx = 0;
        for(int i = 0; i < nPoints; i++){
            if(signs[i] == 1){
                for(int j = 0; j < dimension; j++){
                    pclNew[idx*dimension+j] = pcl[i*dimension+j];
                }
                idx += 1;
            }
        }

        return pclNew;
    }

    public List<float[]> voronoiMaskAdapt(float[] pcl, List<float[]> pbSet) {
        if(pbSet.size() == 0){
            List<float[]> pclNewList = new ArrayList<float[]>();
            pclNewList.add(pcl);
            pclNewList.add(null);
            pclNewList.add(null);
            pclNewList.add(null);
            return pclNewList;
        }

        // perpendicular - self
        int nPoints = (int)(pcl.length / 4);
        int[] signs1 = new int[nPoints];
        int[] signs2 = new int[nPoints];
        int[] signs3 = new int[nPoints];
        int[] signs4 = new int[nPoints];
        for(int i = 0; i < nPoints; i++){
            signs1[i] = 0;
            signs2[i] = 0;
            signs3[i] = 0;
            signs4[i] = 0;
        }
        for(int i = 0; i < pbSet.size(); i++){
            float[] pb_k = pbSet.get(i);
            float[] pb = {pb_k[0], pb_k[1]};
            float[] temp = splitMatrixMultiplication(pcl, nPoints, dimension, 2, pb, 2, 1);
            for(int j = 0; j < nPoints; j++){
                if(temp[j] >= -pb_k[3]){
                    // perpendicular - self
                    signs1[j] += 1;
                }
                if(temp[j] >= -pb_k[2]){
                    // perpendicular - bisector
                    signs2[j] += 1;
                }
                if(temp[j] >= -pb_k[4]){
                    // perpendicular - others
                    signs3[j] += 1;
                }
                // else{
                //     // perpendicular - others
                //     signs4[j] += 1;
                // }
            }
        }
        int nPointsNew1 = 0;
        int nPointsNew2 = 0;
        int nPointsNew3 = 0;
        int nPointsNew4 = 0;
        for(int i = 0; i < nPoints; i++){
            signs1[i] = (signs1[i] == pbSet.size() ? 1 : 0);
            nPointsNew1 += signs1[i];
            signs2[i] = (signs2[i] == pbSet.size() ? 1 : 0);
            if(signs2[i] == 1 && signs1[i] == 1){
                signs2[i] = 0;
            }
            nPointsNew2 += signs2[i];
            signs3[i] = (signs3[i] == pbSet.size() ? 1 : 0);
            if(signs3[i] == 1 && (signs2[i] == 1 || signs1[i] == 1)){
                signs3[i] = 0;
            }
            nPointsNew3 += signs3[i];
            // signs4[i] = (signs4[i] == pbSet.size() ? 1 : 0);
            if(signs3[i] == 0 && signs2[i] == 0 && signs1[i] == 0){
                signs4[i] = 1;
            }
            nPointsNew4 += signs4[i];
        }

        float[] pclNew1 = new float[nPointsNew1*dimension];
        float[] pclNew2 = new float[nPointsNew2*dimension];
        float[] pclNew3 = new float[nPointsNew3*dimension];
        float[] pclNew4 = new float[nPointsNew4*dimension];

        int idx1 = 0;
        int idx2 = 0;
        int idx3 = 0;
        int idx4 = 0;
        for(int i = 0; i < nPoints; i++){
            if(signs1[i] == 1){
                for(int j = 0; j < dimension; j++){
                    pclNew1[idx1*dimension+j] = pcl[i*dimension+j];
                }
                idx1 += 1;
            }
            else if(signs2[i] == 1){
                for(int j = 0; j < dimension; j++){
                    pclNew2[idx2*dimension+j] = pcl[i*dimension+j];
                }
                idx2 += 1;
            }
            else if(signs3[i] == 1){
                for(int j = 0; j < dimension; j++){
                    pclNew3[idx3*dimension+j] = pcl[i*dimension+j];
                }
                idx3 += 1;
            }
            else if(signs4[i] == 1){
                for(int j = 0; j < dimension; j++){
                    pclNew4[idx4*dimension+j] = pcl[i*dimension+j];
                }
                idx4 += 1;
            }
        }

        List<float[]> pclNewList = new ArrayList<float[]>();
        pclNewList.add(pclNew1);
        pclNewList.add(pclNew2);
        pclNewList.add(pclNew3);
        pclNewList.add(pclNew4);

        return pclNewList;
    }

    private static float[] getMinMaxOxtsSetXY(List<float[]> oxtsSet) {
        float[] result = {0.0f, 0.0f, 0.0f, 0.0f};

        if (oxtsSet.size() < 1) {
            return result;
        }

        float[] oxts0 = oxtsSet.get(0);
        float minX = oxts0[0];
        float maxX = oxts0[0];
        float minY = oxts0[1];
        float maxY = oxts0[1];

        for (int i = 1; i < oxtsSet.size(); i++) {
            float[] tmpOxts = oxtsSet.get(i);
            minX = (tmpOxts[0] < minX) ? tmpOxts[0] : minX;
            maxX = (tmpOxts[0] > maxX) ? tmpOxts[0] : maxX;
            minY = (tmpOxts[1] < minY) ? tmpOxts[1] : minY;
            maxY = (tmpOxts[1] > maxY) ? tmpOxts[1] : maxY;
        }

        result[0] = minX;
        result[1] = maxX;
        result[2] = minY;
        result[3] = maxY;

        return result;

    }

    private static float[] calculatePb(float[] p1, float[] p2, boolean normalize){
        float x1 = p1[0];
        float y1 = p1[1];
        float x2 = p2[0];
        float y2 = p2[1];

        float[] result = {-65536.0f, -65536.0f, -65536.0f};
        float a = -65536.0f;
        float b = -65536.0f;
        float c = -65536.0f;

        if (x1 == x2 && y1 == y2) {
            LOGGER.error("calculatePb: Two same coordinates!" + String.format("(%f,%f) (%f,%f)", x1, y1, x2, y2));
            return result;
        }
        else if (x1 == x2) {
            a = 0.0f;
            b = 1.0f;
        }
        else if (y1 == y2) {
            a = 1.0f;
            b = 0.0f;
        }
        else {
            a = x2 - x1;
            b = y2 - y1;
        }

        if (normalize == true) {
            float r = (float)Math.sqrt((double)a * (double)a + (double)b * (double)b);
            a = a / r;
            b = b / r;
        }
        c = -(x1+x2)/2*a - (y1+y2)/2*b;

        if (x1*a + y1*b + c < 0.0f) {
            a = -a;
            b = -b;
            c = -c;
        }

        result[0] = a;
        result[1] = b;
        result[2] = c;
        return result;
    }

    private static float[] calculatePerpendicularPower(float[] p1, float[] p2, boolean normalize, float r1, float r2) {
        float x1 = p1[0];
        float y1 = p1[1];
        float x2 = p2[0];
        float y2 = p2[1];

        float[] result = {-65536.0f, -65536.0f, -65536.0f};
        float a = -65536.0f;
        float b = -65536.0f;
        float c = -65536.0f;

        if (x1 == x2 && y1 == y2) {
            LOGGER.error("calculatePerpendicularPower: Two same coordinates!" + String.format("(%f,%f) (%f,%f)", x1, y1, x2, y2));
            return result;
        }
        else {
            a = x2 - x1;
            b = y2 - y1;
        }

        float r = (float)Math.sqrt((double)a * (double)a + (double)b * (double)b);
        if (normalize == true) {
            a = a / r;
            b = b / r;
        }

        c = ((x1*x1 + y1*y1 - r1*r1) - (x2*x2 + y2*y2 - r2*r2)) / (2*r);

        if ((x1*a + y1*b + c) * (x2*a + y2*b + c) < 0.0f) {
            if ((x1*a + y1*b + c) < 0.0f) {
                a = -a;
                b = -b;
                c = -c;
            }
        }
        else {
            if ((x1*a + y1*b + c) > 0.0f && r1 < r2) {
                a = -a;
                b = -b;
                c = -c;
            }
            if ((x1*a + y1*b + c) < 0.0f && r1 > r2) {
                a = -a;
                b = -b;
                c = -c;
            }
        }

        result[0] = a;
        result[1] = b;
        result[2] = c;
        return result;
    }

    private static float[] calculatePerpendicularPower2(float[] p1, float[] p2, boolean normalize, float r1, float r2, float add, float minu) {
        float x1 = p1[0];
        float y1 = p1[1];
        float x2 = p2[0];
        float y2 = p2[1];

        float[] result = {-65536.0f, -65536.0f, -65536.0f, -65536.0f, -65536.0f};
        float a = -65536.0f;
        float b = -65536.0f;
        float c = -65536.0f;
        float c1 = -65536.0f;
        float c2 = -65536.0f;

        if (x1 == x2 && y1 == y2) {
            LOGGER.error("calculatePerpendicularPower2: Two same coordinates!" + String.format("(%f,%f) (%f,%f)", x1, y1, x2, y2));
            return result;
        }
        else {
            a = x2 - x1;
            b = y2 - y1;
        }

        float r = (float)Math.sqrt((double)a * (double)a + (double)b * (double)b);
        if (normalize == true) {
            a = a / r;
            b = b / r;
        }

        c = ((x1*x1 + y1*y1 - r1*r1) - (x2*x2 + y2*y2 - r2*r2)) / (2*r);
        c1 = ((x1*x1 + y1*y1 - (r1*(1.0f - minu))*(r1*(1.0f - minu))) - (x2*x2 + y2*y2 - (r2*(1.0f + add))*(r2*(1.0f + add)))) / (2*r);
        c2 = ((x1*x1 + y1*y1 - (r1*(1.0f + add))*(r1*(1.0f + add))) - (x2*x2 + y2*y2 - (r2*(1.0f - minu))*(r2*(1.0f - minu)))) / (2*r);

        if ((x1*a + y1*b + c) * (x2*a + y2*b + c) < 0.0f) {
            if ((x1*a + y1*b + c) < 0.0f) {
                a = -a;
                b = -b;
                c = -c;
                c1 = -c1;
                c2 = -c2;
            }
        }
        else {
            if ((x1*a + y1*b + c) > 0.0f && r1 < r2) {
                a = -a;
                b = -b;
                c = -c;
                c1 = -c1;
                c2 = -c2;
            }
            if ((x1*a + y1*b + c) < 0.0f && r1 > r2) {
                a = -a;
                b = -b;
                c = -c;
                c1 = -c1;
                c2 = -c2;
            }
        }

        result[0] = a;
        result[1] = b;
        result[2] = c;
        result[3] = c1;
        result[4] = c2;
        return result;
    }

    private static float[] transformation(float[] oxts1, float[] oxts2) {
        // transformation matrix - translation (to the perspective of oxts1)
        float da = oxts2[0] - oxts1[0];  // south --> north
        float db = oxts2[1] - oxts1[1];  // east --> west
        float dx = da * (float)Math.cos(oxts1[5]) + db * (float)Math.sin(oxts1[5]);
        float dy = da * (-(float)Math.sin(oxts1[5])) + db * (float)Math.cos(oxts1[5]);
        // float dz = oxts2[2] - oxts1[2];
        // float[] translation = {dx, dy, dz, 0.0f};
        float[] translation = {dx, dy};

        // LOGGER.info("Translation: " + translation[0] + " " + translation[1]);
        return translation;
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

    private float[] splitMatrixMultiplication(float[] matA, int rowA, int colA, int splitColA, float[] matB, int rowB, int colB){
        float[] result = new float[rowA*colB];

        for(int i = 0; i < rowA; i++){
            for(int j = 0; j < colB; j++){
                float temp = 0.0f;
                for(int k = 0; k < splitColA; k++){
                    temp += matA[i*colA+k] * matB[k*colB+j];
                }
                result[i*colB+j] = temp;
            }
        }

        return result;
    }
}