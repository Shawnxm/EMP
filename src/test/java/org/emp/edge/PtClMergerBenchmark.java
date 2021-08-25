package org.emp.edge;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.emp.utils.DracoHelper;
import org.emp.utils.TestUtils;
import org.emp.utils.EmpUnitTest;
import static com.google.common.truth.Truth.assertThat;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PtClMergerBenchmark extends EmpUnitTest{
    private static final Logger LOGGER = LogManager.getLogger(PtClMergerTest.class);

    private static final int POINT_DIMENSION = 4;
    private static final int OXTS_DIMENSION = 6;

    @Test
    public void testPtClMergerNaive() throws IOException {
        LOGGER.info("Test PtClMergerNaive ...");

        // Test GTA data
        int nPointsPrimary = 100000;
        float[] pointsPrimary = new float[100000*4];
        float[] oxtsPrimary = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        List<Integer> nPointsSecondary = new ArrayList<Integer>();
        List<float[]> pointsSecondary = new ArrayList<float[]>();
        List<float[]> oxtsSecondary = new ArrayList<float[]>();

        LOGGER.info("Test PtClMergerNaive GTA Data ...");

        String data_path = "src/test/resources/gta_data/";
        String[] secondaryVehicle = {"0014850", "0023554", "0024066", "0025858", "0030466",
                "0032002", "0036610", "0113727", "0296223", "0413513"};

        List<List<Long>> times = new ArrayList<List<Long>>(7);
        for (int i = 0; i < 7; i++) {
            ArrayList<Long> time =new ArrayList<>();
            times.add(time);
        }
        PtClMerger merger = new PtClMerger();

        for (int i = 0; i < 220; i++) {
            nPointsSecondary.clear();
            pointsSecondary.clear();
            oxtsSecondary.clear();

            String frameID = String.format("%06d", i);
            // LOGGER.debug("Input: " + frameID);

            pointsPrimary = TestUtils.readPointCloudFromFile(data_path+"velodyne_2/"+frameID+".bin");
            nPointsPrimary = pointsPrimary.length / 4;
            oxtsPrimary = TestUtils.loadOxts(data_path+"oxts/"+frameID+".txt");

            String alt_path = data_path+"alt_perspective/";
            for (int j = 0; j < secondaryVehicle.length; j++) {
                String ptclname = alt_path + secondaryVehicle[j] + "/velodyne_2/" + frameID + ".bin";
                File tmpFile = new File(ptclname);
                if (tmpFile.exists()) {
                    float[] tempPoints = TestUtils.readPointCloudFromFile(ptclname);
                    float[] tempOxts = TestUtils.loadOxts(alt_path + secondaryVehicle[j] + "/oxts/" + frameID + ".txt");
                    nPointsSecondary.add(tempPoints.length / 4);
                    pointsSecondary.add(tempPoints);
                    oxtsSecondary.add(tempOxts);
                }
            }
            int num_vehs = pointsSecondary.size()+1;

            // Merge point cloud data
            Long start = System.currentTimeMillis();
            float[] mergedResult = merger.naiveMerge(pointsPrimary, oxtsPrimary, pointsSecondary, oxtsSecondary);
            Long end = System.currentTimeMillis();

            times.get(num_vehs-1).add(end - start);
        }

        LOGGER.info("Avg. merging time: ");
        for (int i = 0; i < 7; i++) {
            LOGGER.info(String.format("%d", i+1) + ": " +
                    times.get(i).stream().mapToDouble(x -> x).average() + " ms, stddev: " +
                    new StandardDeviation().evaluate(times.get(i).stream().mapToDouble(x -> x).toArray()) + " ms, Num of frames: " +
                    times.get(i).size());
        }
    }

}

