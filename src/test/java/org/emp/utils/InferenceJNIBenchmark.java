package org.emp.utils;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class InferenceJNIBenchmark extends EmpUnitTest {
    private static final Logger LOGGER = LogManager.getLogger(InferenceJNIBenchmark.class);

    @Test
    public void runInference() throws IOException {
        runInferenceTest();
    }

    private List<String> constructBenchmarkDataPaths(
            String pathPrefix, int startIndexInclusive, int endIndexExclusive, String customPath) {
        return IntStream.range(startIndexInclusive, endIndexExclusive).mapToObj(
                i -> pathPrefix + customPath + String.format("%010d", i) + ".bin")
                .collect(Collectors.toList());
    }

    private void runInferenceTest() throws IOException {
        String pathPrefix = "src/test/resources/benchmark_data/2011_09_26/2011_09_26_drive_";
        List<String> dataFilenames = new ArrayList<>();

        dataFilenames.addAll(
                constructBenchmarkDataPaths(pathPrefix, 0, 108,
                        "0001_sync/velodyne_points/data/"));
        dataFilenames.addAll(
                constructBenchmarkDataPaths(pathPrefix, 0, 77,
                        "0002_sync/velodyne_points/data/"));
        dataFilenames.addAll(
                constructBenchmarkDataPaths(pathPrefix, 0, 154,
                        "0005_sync/velodyne_points/data/"));
        dataFilenames.addAll(
                constructBenchmarkDataPaths(pathPrefix, 0, 177,
                        "0009_sync/velodyne_points/data/"));
        dataFilenames.addAll(
                constructBenchmarkDataPaths(pathPrefix, 181, 447,
                        "0009_sync/velodyne_points/data/"));
        dataFilenames.addAll(
                constructBenchmarkDataPaths(pathPrefix, 0, 233,
                        "0011_sync/velodyne_points/data/"));

        List<Long> inferenceTimeMsList = new ArrayList<>();

        InferenceHelper helper = new InferenceHelper();

        for (int i = 0; i < dataFilenames.size(); i++) {
            LOGGER.debug("Input: " + dataFilenames.get(i));

            float[] points = TestUtils.readPointCloudFromFile(dataFilenames.get(i));

            // Do inference on point cloud data
            Long start = System.currentTimeMillis();
            byte[] detections = helper.executeModel(points);
            Long end = System.currentTimeMillis();
            // System.out.println(new String(detections));
            if (i == 0) {
                continue;
            }
            inferenceTimeMsList.add(end - start);
        }

        LOGGER.info("Number of point cloud samples: " + (dataFilenames.size()-1));
        LOGGER.info("Avg. inference time: " +
                inferenceTimeMsList.stream().mapToDouble(x -> x).average() + " ms, stddev: " +
                new StandardDeviation().evaluate(inferenceTimeMsList.stream().mapToDouble(x -> x).toArray())
                + " ms"
        );
    }
}

