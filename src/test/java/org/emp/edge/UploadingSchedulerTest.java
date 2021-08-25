package org.emp.edge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.*;
import org.emp.utils.EmpUnitTest;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.mockito.Mockito.mock;

public class UploadingSchedulerTest extends EmpUnitTest {
    private static final Logger LOGGER = LogManager.getLogger(UploadingSchedulerTest.class);
    StatHandler statHandler = mock(StatHandler.class);

    /**
     * Test the correctness of uploading scheduling algorithm.
     * The test scenario comes of the example in the paper.
     */
    @Test
    public void testUploadingScheduler() throws InterruptedException {
        // test case 1: (2,1,0,3,2), partitioned frames, unfinished
        // test case 2: (4,4,0,3,4), partitioned frames, finished
        // test case 3: (5,0,5,5,5), full frames, unfinished
        // test case 4: (5,5,5,5,5), full frames, finished
        Map<Integer, VehicleState> vehicleStateMap = buildVehicleStateMap(2,1,0,3,2);

        LOGGER.info(UploadingScheduler.check(vehicleStateMap, 1, statHandler));
    }

    private Map<Integer, VehicleState> buildVehicleStateMap (int p1, int p2, int p3, int p4, int p5) {
        VehicleState vehicle_1 = buildVehicleStateWithOneFrame(1, 1, p1, Arrays.asList(2, 3, 4));
        VehicleState vehicle_2 = buildVehicleStateWithOneFrame(2, 1, p2, Arrays.asList(1, 3, 4, 5));
        VehicleState vehicle_3 = buildVehicleStateWithOneFrame(3, 1, p3, Arrays.asList(1, 2, 5));
        VehicleState vehicle_4 = buildVehicleStateWithOneFrame(4, 1, p4, Arrays.asList(1, 2, 5));
        VehicleState vehicle_5 = buildVehicleStateWithOneFrame(5, 1, p5, Arrays.asList(2, 3, 4));

        Map<Integer, VehicleState> vehicleStateMap = new HashMap<>();
        vehicleStateMap.put(1, vehicle_1);
        vehicleStateMap.put(2, vehicle_2);
        vehicleStateMap.put(3, vehicle_3);
        vehicleStateMap.put(4, vehicle_4);
        vehicleStateMap.put(5, vehicle_5);

        return vehicleStateMap;
    }

    private VehicleState buildVehicleStateWithOneFrame (int vehicleId, int frameId, int maxChunkId, List<Integer> neighbors) {
        if (maxChunkId > 0) {
            Map<Integer, SensorDataChunk> chunkMap_xy = new HashMap<>();
            for (int z = 0; z < maxChunkId; z++) {
                SensorDataChunk chunk_xyz = SensorDataChunk.builder().vehicleId(vehicleId).frameId(frameId).chunkId(z+1).build();
                chunkMap_xy.put(z+1, chunk_xyz);
            }
            SensorDataFrame frame_xy = SensorDataFrame.builder().vehicleId(vehicleId).frameId(frameId).chunks(chunkMap_xy).isMerged(true).build();
            Map<Integer, SensorDataFrame> frameMap_x = new HashMap<>();
            frameMap_x.put(frameId, frame_xy);
            Set<Integer> neighbors_x = new HashSet<>(neighbors);
            return VehicleState.builder().vehicleId(1).frames(frameMap_x).neighborIds(neighbors_x).build();
        }
        else {
            Set<Integer> neighbors_x = new HashSet<>(neighbors);
            return VehicleState.builder().vehicleId(1).neighborIds(neighbors_x).build();
        }
    }
}
