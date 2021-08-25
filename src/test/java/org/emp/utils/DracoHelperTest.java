package org.emp.utils;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Tests for DracoHelper
 */
public class DracoHelperTest extends EmpUnitTest {

  @Test
  public void testEncodeAndDecode() throws IOException {
    String originalDataFileName = "src/test/resources/point_cloud_sample_data_original.bin";
    String decodedDataFileName = "src/test/resources/point_cloud_sample_data_after_decoding.bin";
    float[] points = TestUtils.readPointCloudFromFile(originalDataFileName);
    float[] expectedPoints = TestUtils.readPointCloudFromFile(decodedDataFileName);

    DracoHelper helper = new DracoHelper();

    // Encode point cloud data
    byte[] encodedData = helper.encode(points, 10, 14);
    // Decode point cloud data
    float[] decodedData = helper.decode(encodedData);

    assertThat(decodedData).isEqualTo(expectedPoints);
  }
}
