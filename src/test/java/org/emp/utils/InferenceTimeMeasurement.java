package org.emp.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class InferenceTimeMeasurement extends EmpUnitTest {
    private static final Logger LOGGER = LogManager.getLogger(InferenceTimeMeasurement.class);

    @Test
    public void runInference() throws IOException {
        runInferenceTest("0014850");
        runInferenceTest("0023554");
    }

    private List<String> constructBenchmarkDataPaths(String dataPath, int startIndexInclusive, int endIndexExclusive) {
        return IntStream.range(startIndexInclusive, endIndexExclusive).mapToObj(
                i -> dataPath + String.format("%06d", i) + ".bin").collect(Collectors.toList());
    }

    private void runInferenceTest(String subPath) throws IOException {
        String dataPath = "src/test/resources/gta_data/alt_perspective/" + subPath + "/velodyne_2/";
        List<String> dataFilenames = new ArrayList<>(constructBenchmarkDataPaths(dataPath, 0, 80));
        List<Long> inferenceTimeMsList = new ArrayList<>();

        InferenceHelper helper = new InferenceHelper();

        LOGGER.info(dataPath);

        for (int i = 0; i < dataFilenames.size(); i++) {
            float[] points = TestUtils.readPointCloudFromFile(dataFilenames.get(i));

            Long start = System.currentTimeMillis();
            byte[] detections = helper.executeModel(points);
            Long end = System.currentTimeMillis();

            LOGGER.info(end - start);

            if (i == 0) {
                continue;
            }
            inferenceTimeMsList.add(end - start);
        }

        LOGGER.info("avg: " + inferenceTimeMsList.stream().mapToDouble(x -> x).average().orElse(0) + " ms, stddev: "
                + new StandardDeviation().evaluate(inferenceTimeMsList.stream().mapToDouble(x -> x).toArray()) + " ms");
    }
}

