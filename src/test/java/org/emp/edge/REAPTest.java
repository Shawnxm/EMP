package org.emp.edge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.EmpReapSensorDataHandler;
import org.emp.data.SensorDataHandler;
import org.emp.data.VehicleLocation;
import org.emp.network.BandwidthEstimator;
import org.emp.utils.EmpUnitTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;

public class REAPTest extends EmpUnitTest {
    private static final Logger LOGGER = LogManager.getLogger(REAPTest.class);
    BandwidthEstimator bandwidthEstimator = mock(BandwidthEstimator.class);
    SensorDataHandler sensorDataHandler = new EmpReapSensorDataHandler(bandwidthEstimator, 1, null);

    @Test
    public void testREAP() throws IOException {
        float[] oxts_1 = new float[]{10f, 0f, 28.2961f, 0.00914782f, -0.000945845f, 1.19028f};
        float[] oxts_2 = new float[]{5f, 25f, 28.3415f, 0.00205729f, -0.00365747f, 2.98768f};
        float[] oxts_3 = new float[]{0f, 30f, 28.2961f, 0.00914782f, -0.000945845f, 1.19028f};
        float[] oxts_4 = new float[]{30f, 45f, 28.3415f, 0.00205729f, -0.00365747f, 2.98768f};
        float[] oxts_5 = new float[]{10f, 50f, 28.2961f, 0.00914782f, -0.000945845f, 1.19028f};

        VehicleLocation location_1 = VehicleLocation.builder().oxtsData(oxts_1).build();
        VehicleLocation location_2 = VehicleLocation.builder().oxtsData(oxts_2).build();
        VehicleLocation location_3 = VehicleLocation.builder().oxtsData(oxts_3).build();
        VehicleLocation location_4 = VehicleLocation.builder().oxtsData(oxts_4).build();
        VehicleLocation location_5 = VehicleLocation.builder().oxtsData(oxts_5).build();

        sensorDataHandler.updateVehicleLocation(1, 1, location_1);
        sensorDataHandler.updateVehicleLocation(22, 1, location_2);
        sensorDataHandler.updateVehicleLocation(38, 1, location_3);
        sensorDataHandler.updateVehicleLocation(405, 1, location_4);
        sensorDataHandler.updateVehicleLocation(5111, 1, location_5);

        sensorDataHandler.updatePartitioningDecisions(1);
    }
}
