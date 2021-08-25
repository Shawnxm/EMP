package org.emp.task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.EmpReapSensorDataHandler;
import org.emp.data.SensorDataHandler;
import org.emp.data.VehicleLocation;

/**
 * A task to upload vehicle locations.
 */
public class LocationUpdatingTask extends Task {
    private static final Logger LOGGER = LogManager.getLogger(LocationUpdatingTask.class);
    private final int vehicleId;
    private final int frameId;
    private final float[] oxts;
    private final SensorDataHandler sensorDataHandler;

    public LocationUpdatingTask(int vehicleId, int frameId, float[] oxts, SensorDataHandler sensorDataHandler) {
        super(TaskType.LOCATION_UPDATING);
        this.vehicleId = vehicleId;
        this.frameId = frameId;
        this.oxts = oxts;
        this.sensorDataHandler = sensorDataHandler;

    }

    @Override
    public TaskResult call() throws Exception {
        VehicleLocation location = VehicleLocation.builder().oxtsData(oxts).build();
        sensorDataHandler.updateVehicleLocation(vehicleId, frameId, location);
        LOGGER.info("vehicle: " + vehicleId + "; frame: " + frameId);
        return new LocationUpdatingTaskResult();
    }

    /**
     * TaskResult from a location_updating task
     */
    public class LocationUpdatingTaskResult extends TaskResult {

        public LocationUpdatingTaskResult() {
            super(TaskType.LOCATION_UPDATING);
        }

        @Override
        Object getResult() {
            return null;
        }
    }
}