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

public class DracoJNIBenchmark extends EmpUnitTest {
  private static final Logger LOGGER = LogManager.getLogger(DracoJNIBenchmark.class);

  @Test
  public void runEncodeAndDecodeWithValidation() throws IOException {
    // Validation only works for these parameters: cl=10, qb=14
    runEncodeAndDecodeTest(true, 10, 14);
  }

  @Test
  public void runEncodeAndDecodeWithoutValidation() throws IOException {
    runEncodeAndDecodeTest(false, 10, 12);
  }

  @Test
  public void runEncodeAndDecodeGTA() throws IOException {
    // Validation only works for these parameters: cl=10, qb=14
    runEncodeAndDecodeTestGTA(10, 14);
    runEncodeAndDecodeTestGTA(10, 12);
  }

  private List<String> constructBenchmarkDataPaths(
      String pathPrefix, int startIndexInclusive, int endIndexExclusive, String customPath) {
    return IntStream.range(startIndexInclusive, endIndexExclusive).mapToObj(
        i -> pathPrefix + customPath + String.format("%010d", i) + ".bin")
        .collect(Collectors.toList());
  }

  private void runEncodeAndDecodeTest(
      boolean validateResult, int cl, int qb) throws IOException {
    String pathPrefix = "src/test/resources/benchmark_data/2011_09_26/2011_09_26_drive_";
    List<String> originalDataFilenames = new ArrayList<>();
    List<String> expectedDataFilenames = new ArrayList<>();

    originalDataFilenames.addAll(
        constructBenchmarkDataPaths(pathPrefix, 0, 108,
            "0001_sync/velodyne_points/data/"));
    originalDataFilenames.addAll(
        constructBenchmarkDataPaths(pathPrefix, 0, 77,
            "0002_sync/velodyne_points/data/"));
    originalDataFilenames.addAll(
        constructBenchmarkDataPaths(pathPrefix, 0, 154,
            "0005_sync/velodyne_points/data/"));
    originalDataFilenames.addAll(
        constructBenchmarkDataPaths(pathPrefix, 0, 177,
            "0009_sync/velodyne_points/data/"));
    originalDataFilenames.addAll(
        constructBenchmarkDataPaths(pathPrefix, 181, 447,
            "0009_sync/velodyne_points/data/"));
    originalDataFilenames.addAll(
        constructBenchmarkDataPaths(pathPrefix, 0, 233,
            "0011_sync/velodyne_points/data/"));

    if (validateResult) {
      expectedDataFilenames.addAll(
          constructBenchmarkDataPaths(pathPrefix, 0, 108,
              "0001_sync/velodyne_points/processed/"));
      expectedDataFilenames.addAll(
          constructBenchmarkDataPaths(pathPrefix, 0, 77,
              "0002_sync/velodyne_points/processed/"));
      expectedDataFilenames.addAll(
          constructBenchmarkDataPaths(pathPrefix, 0, 154,
              "0005_sync/velodyne_points/processed/"));
      expectedDataFilenames.addAll(
          constructBenchmarkDataPaths(pathPrefix, 0, 177,
              "0009_sync/velodyne_points/processed/"));
      expectedDataFilenames.addAll(
          constructBenchmarkDataPaths(pathPrefix, 181, 447,
              "0009_sync/velodyne_points/processed/"));
      expectedDataFilenames.addAll(
          constructBenchmarkDataPaths(pathPrefix, 0, 233,
              "0011_sync/velodyne_points/processed/"));
    }

    List<Long> encodingTimeMsList = new ArrayList<>();
    List<Long> decodingTimeMsList = new ArrayList<>();
    List<Double> compressionRatioList = new ArrayList<>();

    for (int i = 0; i < originalDataFilenames.size(); i++) {
      LOGGER.debug("Input: " + originalDataFilenames.get(i));

      float[] points = TestUtils.readPointCloudFromFile(originalDataFilenames.get(i));

      DracoHelper helper = new DracoHelper();

      // Encode point cloud data
      Long start = System.currentTimeMillis();
      byte[] encodedData = helper.encode(points, cl, qb);
      Long end = System.currentTimeMillis();
      encodingTimeMsList.add(end - start);
      compressionRatioList.add(encodedData.length / (double)(points.length * 4));

      // Decode point cloud data
      start = System.currentTimeMillis();
      float[] decodedData = helper.decode(encodedData);
      end = System.currentTimeMillis();
      decodingTimeMsList.add(end - start);

      if (validateResult) {
        float[] expectedPoints = TestUtils.readPointCloudFromFile(expectedDataFilenames.get(i));
        assertThat(decodedData).isEqualTo(expectedPoints);
      }
    }

    LOGGER.info("Number of point cloud samples: " + originalDataFilenames.size());
    LOGGER.info("KD-tree avg. compression ratio: " +
            compressionRatioList.stream().mapToDouble(x -> x).average() + ", stddev: " +
            new StandardDeviation().evaluate(compressionRatioList.stream().mapToDouble(x -> x).toArray())
    );
    LOGGER.info("KD-tree avg. encoding time: " +
        encodingTimeMsList.stream().mapToDouble(x -> x).average() + " ms, stddev: " +
        new StandardDeviation().evaluate(encodingTimeMsList.stream().mapToDouble(x -> x).toArray())
        + " ms"
    );
    LOGGER.info("KD-tree avg. decoding time: " +
        decodingTimeMsList.stream().mapToDouble(x -> x).average() + " ms, stddev: " +
        new StandardDeviation().evaluate(decodingTimeMsList.stream().mapToDouble(x -> x).toArray())
        + " ms"
    );
  }

  private List<String> constructBenchmarkDataPathsGTA(
          String pathPrefix, int startIndexInclusive, int endIndexExclusive, String customPath) {
    return IntStream.range(startIndexInclusive, endIndexExclusive).mapToObj(
            i -> pathPrefix + customPath + String.format("%06d", i) + ".bin")
            .collect(Collectors.toList());
  }

  private void runEncodeAndDecodeTestGTA(int cl, int qb) throws IOException {
    String pathPrefix = "src/test/resources/gta_data/";
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

    List<Long> encodingTimeMsList = new ArrayList<>();
    List<Long> decodingTimeMsList = new ArrayList<>();
    List<Double> compressionRatioList = new ArrayList<>();

    for (int i = 0; i < originalDataFilenames.size(); i++) {
      LOGGER.debug("Input: " + originalDataFilenames.get(i));

      float[] points = TestUtils.readPointCloudFromFile(originalDataFilenames.get(i));

      DracoHelper helper = new DracoHelper();

      // Encode point cloud data
      Long start = System.currentTimeMillis();
      byte[] encodedData = helper.encode(points, cl, qb);
      Long end = System.currentTimeMillis();
      encodingTimeMsList.add(end - start);
      compressionRatioList.add(encodedData.length / (double)(points.length * 4));

      // Decode point cloud data
      start = System.currentTimeMillis();
      float[] decodedData = helper.decode(encodedData);
      end = System.currentTimeMillis();
      decodingTimeMsList.add(end - start);
    }

    LOGGER.info("Number of point cloud samples: " + originalDataFilenames.size());
    LOGGER.info("Settings: cl " + cl + ", qb " + qb);
    LOGGER.info("KD-tree avg. compression ratio: " +
            compressionRatioList.stream().mapToDouble(x -> x).average() + ", stddev: " +
            new StandardDeviation().evaluate(compressionRatioList.stream().mapToDouble(x -> x).toArray())
    );
    LOGGER.info("KD-tree avg. encoding time: " +
            encodingTimeMsList.stream().mapToDouble(x -> x).average() + " ms, stddev: " +
            new StandardDeviation().evaluate(encodingTimeMsList.stream().mapToDouble(x -> x).toArray())
            + " ms"
    );
    LOGGER.info("KD-tree avg. decoding time: " +
            decodingTimeMsList.stream().mapToDouble(x -> x).average() + " ms, stddev: " +
            new StandardDeviation().evaluate(decodingTimeMsList.stream().mapToDouble(x -> x).toArray())
            + " ms"
    );
  }
}
