package org.emp.edge;

import org.emp.utils.TestUtils;
import static com.google.common.truth.Truth.assertThat;

import java.nio.FloatBuffer;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

import org.emp.utils.EmpUnitTest;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.io.IOException;

public class PtClMergerTest extends EmpUnitTest{
    private static final int BUFFER_MAX_BYTES = 4 * 1024 * 1024;
    private static final Logger LOGGER = LogManager.getLogger(PtClMergerTest.class);

    @Test
    public void testPtClMergerNaive() throws IOException {
        LOGGER.info("Test PtClMergerNaive ...");

        // Test GTA data
        int nPointsPrimary;
        float[] pointsPrimary = new float[100000*4];
        float[] oxtsPrimary = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        List<Integer> nPointsSecondary = new ArrayList<Integer>();
        List<float[]> pointsSecondary = new ArrayList<float[]>();
        List<float[]> oxtsSecondary = new ArrayList<float[]>();

        LOGGER.info("Test PtClMergerNaive GTA Data ...");

        // pointsPrimary = loadPointCloud("/home/anlan/EMP/java/src/test/resources/sample_data_for_merging/ego/velodyne/000003.bin");
        String name1 = "src/test/resources/sample_data_for_merging/ego/velodyne/000003.bin";
        pointsPrimary = TestUtils.readPointCloudFromFile(name1);
        nPointsPrimary = pointsPrimary.length / 4;
        oxtsPrimary = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/ego/oxts/000003.txt");
        // for(int i = 0; i < nPointsPrimary; i++){
        //     System.out.printf("%f %f %f %f\n", pointsPrimary[i*4], pointsPrimary[i*4+1], pointsPrimary[i*4+2], pointsPrimary[i*4+3]);
        // }

        // float[] tempPoints = loadPointCloud("/home/anlan/EMP/java/src/test/resources/sample_data_for_merging/leftturn/velodyne/000003.bin");
        String name2 = "src/test/resources/sample_data_for_merging/leftturn/velodyne/000003.bin";
        float[] tempPoints = TestUtils.readPointCloudFromFile(name2);
        float[] tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/leftturn/oxts/000003.txt");
        nPointsSecondary.add(tempPoints.length / 4);
        pointsSecondary.add(tempPoints);
        oxtsSecondary.add(tempOxts);

        PtClMerger myMerger = new PtClMerger();

        Date start = new Date();

        float[] mergedResult = myMerger.naiveMerge(pointsPrimary, oxtsPrimary, pointsSecondary, oxtsSecondary);

        // LOGGER.info(Arrays.toString(mergedResult));

        Date end = new Date();

        LOGGER.info((end.getTime() - start.getTime()) + "ms");

        // use this for checking results
        // for(int i = 0; i < mergedResult.length / 4; i++){
        //     System.out.printf("%f %f %f\n", mergedResult[i*4], mergedResult[i*4+1], mergedResult[i*4+2]);
        // }

        // assert 
        float[] expected = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/mergeResultPy.bin");

        assertThat(mergedResult.length).isEqualTo(expected.length);

        int count = 0;
        for(int i = 0; i < mergedResult.length; i++){
            if((mergedResult[i] - expected[i] <= -0.001) || (mergedResult[i] - expected[i] >= 0.001)){
                System.out.printf("%d %f %f\n", i, mergedResult[i], expected[i]);
                count += 1;
            }
        }
        LOGGER.info("count = " + count);
        assertThat(count == 0).isTrue();
    }

    @Test
    public void testPtClMergerNaiveNoPrimary() throws IOException {
        LOGGER.info("Test PtClMergerNaiveNoPrimary ...");

        // Test GTA data
        float[] pointsPrimary = new float[100000*4];
        float[] oxtsPrimary = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        List<Integer> nPointsSecondary = new ArrayList<Integer>();
        List<float[]> pointsSecondary = new ArrayList<float[]>();
        List<float[]> oxtsSecondary = new ArrayList<float[]>();

        LOGGER.info("Test PtClMergerNaiveNoPrimary GTA Data ...");

        String name1 = "src/test/resources/sample_data_for_merging/ego/velodyne/000003.bin";
        pointsPrimary = TestUtils.readPointCloudFromFile(name1);
        oxtsPrimary = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/ego/oxts/000003.txt");

        String name2 = "src/test/resources/sample_data_for_merging/leftturn/velodyne/000003.bin";
        float[] tempPoints = TestUtils.readPointCloudFromFile(name2);
        float[] tempOxts = TestUtils.loadOxts("src/test/resources/sample_data_for_merging/leftturn/oxts/000003.txt");
        nPointsSecondary.add(tempPoints.length / 4);
        pointsSecondary.add(tempPoints);
        oxtsSecondary.add(tempOxts);

        // calculate the expected size
        int finalSize = pointsPrimary.length;
        for(int i = 0; i < pointsSecondary.size(); i++){
            finalSize += pointsSecondary.get(i).length;
        }

        // allocate float buffer
        FloatBuffer result = FloatBuffer.allocate(finalSize);
        for(int i = 0; i < pointsPrimary.length; i++){
            result.put(pointsPrimary[i]);
        }
        int offset = pointsPrimary.length;

        PtClMerger myMerger = new PtClMerger();

        Date start = new Date();
        offset = myMerger.fbMergeNoPrimary(oxtsPrimary, pointsSecondary, oxtsSecondary, result, offset);
        float[] mergedResult = result.array();
        Date end = new Date();

        LOGGER.info((end.getTime() - start.getTime()) + "ms");

        // assert
        float[] expected = TestUtils.readPointCloudFromFile("src/test/resources/sample_data_for_merging/mergeResultPy.bin");
        assertThat(mergedResult.length).isEqualTo(expected.length);

        int count = 0;
        for(int i = 0; i < mergedResult.length; i++){
            if((mergedResult[i] - expected[i] <= -0.001) || (mergedResult[i] - expected[i] >= 0.001)){
                System.out.printf("%d %f %f\n", i, mergedResult[i], expected[i]);
                count += 1;
            }
        }
        LOGGER.info("count = " + count);
        assertThat(count == 0).isTrue();
    }
}
