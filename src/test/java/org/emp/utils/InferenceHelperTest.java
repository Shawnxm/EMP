package org.emp.utils;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

/**
 * Tests for InferenceHelper
 */
public class InferenceHelperTest extends EmpUnitTest {
    private static final Logger LOGGER = LogManager.getLogger(InferenceHelperTest.class);

    @Test
    public void testInference() throws IOException {
        String originalDataFileName = "src/test/resources/point_cloud_sample_data_original.bin";
        float[] points = TestUtils.readPointCloudFromFile(originalDataFileName);
        LOGGER.info("test-ckpt1");
        InferenceHelper helper = new InferenceHelper();

        // Do inference on point cloud data
        LOGGER.info("test-ckpt2");
        byte[] detections = helper.executeModel(points);
        System.out.println(new String(detections));  // System.out.println(Arrays.toString(encodedData));
        LOGGER.info("test-ckpt3");

    }
}
