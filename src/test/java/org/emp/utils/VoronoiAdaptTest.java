package org.emp.utils;

import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.io.IOException;

public class VoronoiAdaptTest extends EmpUnitTest {

    private static final Logger LOGGER = LogManager.getLogger(VoronoiAdaptTest.class);
    private static final int OXTS_DIMENSION = 6;

    @Test
    public void testVoronoiBasic() throws IOException {
        LOGGER.info("Test VoronoiBasic ...");

        List<float[]> oxtsSet = new ArrayList<float[]>();

        float[] tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/ego/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/leftturn/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/straight/oxts/000003.txt");
        oxtsSet.add(tempOxts);

        VoronoiAdapt myVoronoiAdapt = new VoronoiAdapt();
        VoronoiAdapt.partitionDecision decision = myVoronoiAdapt.voronoiBasic(oxtsSet);

        for (int i = 0; i < decision.pbSet.size(); i++) {
            for (int j = 0; j < decision.pbSet.get(i).size(); j++) {
                float[] tempPb = decision.pbSet.get(i).get(j);
                LOGGER.info(String.format("pbSet[%d,%d]: %f %f %f", i, j, tempPb[0], tempPb[1], tempPb[2]));
            }
        }
        LOGGER.info("neighbors: " + decision.neighbors);
        LOGGER.info("indexSet: " + decision.indexSet);
    }

    @Test
    public void testVoronoiBW() throws IOException {
        LOGGER.info("Test VoronoiBW...");

        List<float[]> oxtsSet = new ArrayList<float[]>();

        float[] tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/ego/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/leftturn/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/straight/oxts/000003.txt");
        oxtsSet.add(tempOxts);

        VoronoiAdapt myVoronoiAdapt = new VoronoiAdapt();
        List<Float> bwSet = Arrays.asList(4.0f, 6.0f, 8.0f);
        VoronoiAdapt.partitionDecision decision = myVoronoiAdapt.voronoiBW(oxtsSet, bwSet);  // bwSet: null

        for (int i = 0; i < decision.pbSet.size(); i++) {
            for (int j = 0; j < decision.pbSet.get(i).size(); j++) {
                float[] tempPb = decision.pbSet.get(i).get(j);
                LOGGER.info(String.format("pbSet[%d,%d]: %f %f %f", i, j, tempPb[0], tempPb[1], tempPb[2]));
            }
        }
        LOGGER.info("neighbors: " + decision.neighbors);
        LOGGER.info("indexSet: " + decision.indexSet);
    }

    @Test
    public void testVoronoiAdapt() throws IOException {
        LOGGER.info("Test VoronoiAdapt ...");

        List<float[]> oxtsSet = new ArrayList<float[]>();

        float[] tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/ego/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/leftturn/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/straight/oxts/000003.txt");
        oxtsSet.add(tempOxts);

        VoronoiAdapt myVoronoiAdapt = new VoronoiAdapt();
        List<Float> bwSet = Arrays.asList(4.0f, 6.0f, 8.0f);
        VoronoiAdapt.partitionDecision decision = myVoronoiAdapt.voronoiAdapt(oxtsSet, bwSet);  // bwSet: null

        for (int i = 0; i < decision.pbSet.size(); i++) {
            for (int j = 0; j < decision.pbSet.get(i).size(); j++) {
                float[] tempPb = decision.pbSet.get(i).get(j);
                LOGGER.info(String.format("pbSet[%d,%d]: %f %f %f %f %f", i, j, tempPb[0], tempPb[1], tempPb[2], tempPb[3], tempPb[4]));
            }
        }
        LOGGER.info("neighbors: " + decision.neighbors);
        LOGGER.info("indexSet: " + decision.indexSet);
    }

    @Test
    public void testVoronoiMask() throws IOException {
        LOGGER.info("Test VoronoiMask ...");

        List<float[]> oxtsSet = new ArrayList<float[]>();

        float[] tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/ego/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/leftturn/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/straight/oxts/000003.txt");
        oxtsSet.add(tempOxts);

        VoronoiAdapt myVoronoiAdapt = new VoronoiAdapt();
        VoronoiAdapt.partitionDecision decision = myVoronoiAdapt.voronoiBasic(oxtsSet);

        float[] pcl = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/ego/velodyne/000003.bin");
        LOGGER.info("Full: " + pcl.length/4);
        float[] pclNew = myVoronoiAdapt.voronoiMask(pcl, decision.pbSet.get(0));
        LOGGER.info("Part: " + pclNew.length/4);

        pcl = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/leftturn/velodyne/000003.bin");
        LOGGER.info("Full: " + pcl.length/4);
        pclNew = myVoronoiAdapt.voronoiMask(pcl, decision.pbSet.get(1));
        LOGGER.info("Part: " + pclNew.length/4);

        pcl = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/straight/velodyne/000003.bin");
        LOGGER.info("Full: " + pcl.length/4);
        pclNew = myVoronoiAdapt.voronoiMask(pcl, decision.pbSet.get(2));
        LOGGER.info("Part: " + pclNew.length/4);

    }

    @Test
    public void testVoronoiMaskBW() throws IOException {
        LOGGER.info("Test VoronoiMaskBW ...");

        List<float[]> oxtsSet = new ArrayList<float[]>();

        float[] tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/ego/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/leftturn/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/straight/oxts/000003.txt");
        oxtsSet.add(tempOxts);

        VoronoiAdapt myVoronoiAdapt = new VoronoiAdapt();
        List<Float> bwSet = Arrays.asList(4.0f, 6.0f, 8.0f);
        VoronoiAdapt.partitionDecision decision = myVoronoiAdapt.voronoiBW(oxtsSet, bwSet);

        float[] pcl = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/ego/velodyne/000003.bin");
        LOGGER.info("Full: " + pcl.length/4);
        float[] pclNew = myVoronoiAdapt.voronoiMaskBW(pcl, decision.pbSet.get(0));
        LOGGER.info("Part: " + pclNew.length/4);

        pcl = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/leftturn/velodyne/000003.bin");
        LOGGER.info("Full: " + pcl.length/4);
        pclNew = myVoronoiAdapt.voronoiMaskBW(pcl, decision.pbSet.get(1));
        LOGGER.info("Part: " + pclNew.length/4);

        pcl = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/straight/velodyne/000003.bin");
        LOGGER.info("Full: " + pcl.length/4);
        pclNew = myVoronoiAdapt.voronoiMaskBW(pcl, decision.pbSet.get(2));
        LOGGER.info("Part: " + pclNew.length/4);

    }

    @Test
    public void testVoronoiMaskAdapt() throws IOException {
        LOGGER.info("Test VoronoiMaskAdapt ...");

        List<float[]> oxtsSet = new ArrayList<float[]>();

        float[] tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/ego/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/leftturn/oxts/000003.txt");
        oxtsSet.add(tempOxts);
        tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/straight/oxts/000003.txt");
        oxtsSet.add(tempOxts);

        VoronoiAdapt myVoronoiAdapt = new VoronoiAdapt();
        List<Float> bwSet = Arrays.asList(4.0f, 6.0f, 8.0f);
        VoronoiAdapt.partitionDecision decision = myVoronoiAdapt.voronoiAdapt(oxtsSet, bwSet);

        float[] pcl = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/ego/velodyne/000003.bin");
        LOGGER.info("Full: " + pcl.length/4);
        List<float[]> pclNewList = myVoronoiAdapt.voronoiMaskAdapt(pcl, decision.pbSet.get(0));
        for(int i = 0; i < pclNewList.size(); i++){
            LOGGER.info(String.format("Part %d: %d", i+1, pclNewList.get(i).length/4));
        }

        pcl = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/leftturn/velodyne/000003.bin");
        LOGGER.info("Full: " + pcl.length/4);
        pclNewList = myVoronoiAdapt.voronoiMaskAdapt(pcl, decision.pbSet.get(1));
        for(int i = 0; i < pclNewList.size(); i++){
            LOGGER.info(String.format("Part %d: %d", i+1, pclNewList.get(i).length/4));
        }

        pcl = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/straight/velodyne/000003.bin");
        LOGGER.info("Full: " + pcl.length/4);
        pclNewList = myVoronoiAdapt.voronoiMaskAdapt(pcl, decision.pbSet.get(2));
        for(int i = 0; i < pclNewList.size(); i++){
            LOGGER.info(String.format("Part %d: %d", i+1, pclNewList.get(i).length/4));
        }

    }
}