package org.emp.utils;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DracoCompressionLevelTest extends EmpUnitTest {
    private static final Logger LOGGER = LogManager.getLogger(DracoCompressionLevelTest.class);

    @Test
    public void runEncodeAndDecodeGTA() throws IOException {
        // Compare compression ratio and encode/decode time
        // Uncomment to run the tests (run all tests together may lead to the out of memory issue)

        // Different compression levels (qb = 12, 14)
//        runEncodeAndDecodeTestGTA(0, 14, 1);
//        runEncodeAndDecodeTestGTA(1, 14, 1);
//        runEncodeAndDecodeTestGTA(2, 14, 1);
//        runEncodeAndDecodeTestGTA(3, 14, 1);
//        runEncodeAndDecodeTestGTA(4, 14, 1);
//        runEncodeAndDecodeTestGTA(5, 14, 1);
//        runEncodeAndDecodeTestGTA(6, 14, 1);
        runEncodeAndDecodeTestGTA(7, 14, 1);
        runEncodeAndDecodeTestGTA(8, 14, 1);
        runEncodeAndDecodeTestGTA(9, 14, 1);
        runEncodeAndDecodeTestGTA(10, 14, 1);

        // Different ratios of data to be compressed
//        runEncodeAndDecodeTestGTA(10, 14, 0.1f);
//        runEncodeAndDecodeTestGTA(10, 14, 0.2f);
//        runEncodeAndDecodeTestGTA(10, 14, 0.3f);
//        runEncodeAndDecodeTestGTA(10, 14, 0.4f);
//        runEncodeAndDecodeTestGTA(10, 14, 0.5f);
//        runEncodeAndDecodeTestGTA(10, 14, 0.6f);
        runEncodeAndDecodeTestGTA(10, 14, 0.7f);
        runEncodeAndDecodeTestGTA(10, 14, 0.8f);
        runEncodeAndDecodeTestGTA(10, 14, 0.9f);
        runEncodeAndDecodeTestGTA(10, 14, 1);
    }

    private List<String> constructBenchmarkDataPathsGTA(
            String pathPrefix, int startIndexInclusive, int endIndexExclusive, String customPath) {
        return IntStream.range(startIndexInclusive, endIndexExclusive).mapToObj(
                i -> pathPrefix + customPath + String.format("%06d", i) + ".bin")
                .collect(Collectors.toList());
    }

    private List<String> generateFilenameListGTA(String pathPrefix) {
        List<String> originalDataFilenames = new ArrayList<>();
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 0, 220,
                        "velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 0, 18,
                        "alt_perspective/0014850/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 55, 173,
                        "alt_perspective/0023554/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 185, 206,
                        "alt_perspective/0023554/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 213, 219,
                        "alt_perspective/0023554/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 117, 185,
                        "alt_perspective/0024066/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 123, 141,
                        "alt_perspective/0025858/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 211, 220,
                        "alt_perspective/0030466/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 125, 200,
                        "alt_perspective/0032002/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 89, 171,
                        "alt_perspective/0036610/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 215, 220,
                        "alt_perspective/0113727/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 176, 194,
                        "alt_perspective/0296223/velodyne_2/"));
        originalDataFilenames.addAll(
                constructBenchmarkDataPathsGTA(pathPrefix, 150, 220,
                        "alt_perspective/0413513/velodyne_2/"));
        return originalDataFilenames;
    }

    /**
     *
     * @param cl  Draco compression level
     * @param qb  Draco quantization bits
     * @param ratioToBeCompressed  Ratio of size of data to be compressed to total size
     */
    private void runEncodeAndDecodeTestGTA(int cl, int qb, float ratioToBeCompressed) throws IOException {
        String pathPrefix = "src/test/resources/gta_data/";
        List<String> originalDataFilenames = generateFilenameListGTA(pathPrefix);

        List<Long> encodingTimeMsList = new ArrayList<>();
        List<Long> decodingTimeMsList = new ArrayList<>();
        List<Double> compressionRatioList = new ArrayList<>();

        DracoHelper helper = new DracoHelper();
        float[] points;
        float[] pointsToBeProcessed;
        byte[] encodedData;
        float[] decodedData;

        for (int i = 0; i < originalDataFilenames.size(); i++) {
//            LOGGER.debug("Input: " + originalDataFilenames.get(i));

            points = TestUtils.readPointCloudFromFile(originalDataFilenames.get(i));
            int numPoints = points.length / 4;
            int numPointsToBeProcessed = (int) (numPoints * ratioToBeCompressed);


            // Extract data to be encoded
            pointsToBeProcessed = Arrays.copyOfRange(points, 0, numPointsToBeProcessed * 4 );

            // Encode point cloud data
            long start = System.currentTimeMillis();
            encodedData = helper.encode(pointsToBeProcessed, cl, qb);
            long end = System.currentTimeMillis();
            encodingTimeMsList.add(end - start);
            compressionRatioList.add((encodedData.length + (numPoints - numPointsToBeProcessed) * 16 )
                    / (double)(points.length * 4));  // unit: byte

            // Decode point cloud data
            start = System.currentTimeMillis();
            decodedData = helper.decode(encodedData);
            end = System.currentTimeMillis();
            decodingTimeMsList.add(end - start);
        }

        LOGGER.info("Number of point cloud samples: " + originalDataFilenames.size());
        LOGGER.info("Settings: cl " + cl + ", qb " + qb + ", points processed / total points:" + ratioToBeCompressed);
        LOGGER.info("KD-tree avg. compression ratio: " +
                compressionRatioList.stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                new StandardDeviation().evaluate(compressionRatioList.stream().mapToDouble(x -> x).toArray())
        );
        LOGGER.info("KD-tree avg. encoding time: " +
                encodingTimeMsList.stream().mapToDouble(x -> x).average().orElse(0) + " ms, stddev: " +
                new StandardDeviation().evaluate(encodingTimeMsList.stream().mapToDouble(x -> x).toArray())
                + " ms"
        );
        LOGGER.info("KD-tree avg. decoding time: " +
                decodingTimeMsList.stream().mapToDouble(x -> x).average().orElse(0) + " ms, stddev: " +
                new StandardDeviation().evaluate(decodingTimeMsList.stream().mapToDouble(x -> x).toArray())
                + " ms"
        );
    }
}