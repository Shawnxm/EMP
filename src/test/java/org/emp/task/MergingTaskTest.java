package org.emp.task;

import org.emp.data.EmpReapSensorDataHandler;
import org.emp.data.SensorDataChunk;
import org.emp.data.SensorDataHandler;
import org.emp.network.BandwidthEstimator;
import org.emp.utils.EmpUnitTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;

public class MergingTaskTest extends EmpUnitTest {
    /**
     * Remaining issues: If the sleep time after submitting a location updating task for vehicle 2
     * frame 1 is too short. The cleanup merging task later may not be able to get V2's location.
     * Potential reasons: (1) synchronization problems; (2) location updating task becomes slow here.
     *
     * Directly expanding LocationUpdatingTask here or increasing the sleep time solve the problem.
     */
    @Test
    public void testRunMergingTask() throws InterruptedException {
        BandwidthEstimator bandwidthEstimator = mock(BandwidthEstimator.class);
        SensorDataHandler sensorDataHandler = new EmpReapSensorDataHandler(bandwidthEstimator, 0, null);
        ExecutorService executor = Executors.newFixedThreadPool(8);

        // test case: v2f1c1 -> v2f1c2 -> oxts -> v1f1c5 -> v2f1c3
        float[] fakePointCloud_211 = new float[48000];
        float[] fakePointCloud_212 = new float[51000];
        float[] fakePointCloud_213 = new float[63000];
        float[] fakePointCloud_115 = new float[66000];
        Arrays.fill(fakePointCloud_211, 1);
        Arrays.fill(fakePointCloud_212, 1);
        Arrays.fill(fakePointCloud_213, 1);
        Arrays.fill(fakePointCloud_115, 1);
        float[] fakeOxts_1 = new float[]{240.484f, -1011.04f, 28.2961f, 0.00914782f, -0.000945845f, 1.19028f};
        float[] fakeOxts_2 = new float[]{252.49f, -1038.8f, 28.3415f, 0.00205729f, -0.00365747f, 2.98768f};
        SensorDataChunk chunk_211 = SensorDataChunk.builder().vehicleId(2).frameId(1).chunkId(1)
                .decodedPointCloud(fakePointCloud_211).build();
        SensorDataChunk chunk_212 = SensorDataChunk.builder().vehicleId(2).frameId(1).chunkId(2)
                .decodedPointCloud(fakePointCloud_212).build();
        SensorDataChunk chunk_213 = SensorDataChunk.builder().vehicleId(2).frameId(1).chunkId(3)
                .decodedPointCloud(fakePointCloud_213).build();
        SensorDataChunk chunk_115 = SensorDataChunk.builder().vehicleId(1).frameId(1).chunkId(5)
                .decodedPointCloud(fakePointCloud_115).build();

        // Receive vehicle 2 frame 1 oxts
        executor.submit(new LocationUpdatingTask(2, 1, fakeOxts_2, sensorDataHandler));
//        VehicleLocation location = VehicleLocation.builder().oxtsData(fakeOxts_2).build();
//        sensorDataHandler.updateVehicleLocation(2, 1, location);
        Thread.sleep(50);

        // Receive vehicle 2 frame 1 chunk 1 point cloud and finish decoding
        sensorDataHandler.saveDataChunk(chunk_211);
        // Submit a merging task for vehicle 2 frame 1 chunk 1
        executor.submit(new MergingTask(chunk_211, sensorDataHandler)); // immediate merging
        Thread.sleep(20);

        // Receive vehicle 2 frame 1 chunk 2 point cloud and finish decoding
        sensorDataHandler.saveDataChunk(chunk_212);
        // Submit a merging task for vehicle 2 frame 1 chunk 2
        executor.submit(new MergingTask(chunk_212, sensorDataHandler)); // immediate merging

        // Receive vehicle 1 frame 1 oxts
        executor.submit(new LocationUpdatingTask(1, 1, fakeOxts_1, sensorDataHandler));
        // Submit a cleanup merging task for frame 1
        executor.submit(new MergingTask(1, sensorDataHandler)); // cleanup merging
        Thread.sleep(30);

        // Receive vehicle 1 frame 1 chunk 5 point cloud and finish decoding
        sensorDataHandler.saveDataChunk(chunk_115);
        // Submit a merging task for vehicle 1 frame 1 chunk 5
        executor.submit(new MergingTask(chunk_115, sensorDataHandler)); // immediate merging
        Thread.sleep(10);

        // Receive vehicle 2 frame 1 chunk 3 point cloud and finish decoding
        sensorDataHandler.saveDataChunk(chunk_213);
        // Submit a merging task for vehicle 2 frame 1 chunk 3
        executor.submit(new MergingTask(chunk_213, sensorDataHandler)); // immediate merging


    }
}
