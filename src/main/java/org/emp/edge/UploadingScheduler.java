package org.emp.edge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.StatHandler;
import org.emp.data.VehicleState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;


public class UploadingScheduler{
    private static final Logger LOGGER = LogManager.getLogger(UploadingScheduler.class);

    public static boolean check(Map<Integer, VehicleState> vehicleStateMap, int frameId, StatHandler statHandler) {
        LOGGER.debug("Start checking for frame " + frameId);

        // Initialize neighborInProgressIds for each vehicle by copying from neighborIds
        for (Integer vehicleId : vehicleStateMap.keySet()) {
            VehicleState vehicle = vehicleStateMap.get(vehicleId);
            // If the vehicle has no neighbors, check with "full frame" criteria
            if (vehicle.getNeighborIds() == null || vehicle.getNeighborIds().isEmpty()) {
                return checkForFullFrame(vehicleStateMap, frameId);
            }
            // Copy the current neighboring vehicle IDs to the neighborInProgressId set
            if (vehicle.getNeighborInProgressIds() == null || vehicle.getNeighborInProgressIds().isEmpty()) {
                vehicle.setNeighborInProgressIds(new HashSet<>(vehicle.getNeighborIds()));
            }
        }

        int numCheckedVehicles = 0;
        boolean isFull;
        for (Integer vehicleId : vehicleStateMap.keySet()) {
            // Get the state information of the vehicle being checked
            VehicleState vehicle = vehicleStateMap.get(vehicleId);
            LOGGER.debug("vehicle: " + vehicleId + "; neighbors: " + vehicle.getNeighborInProgressIds());

            // Compare the latest chunk ID of the current frame from this vehicle with that of all its neighbors
            int numCheckedNeighbors = 0;
            Set<Integer> toBeRemovedNeighborInProgressIds = new HashSet<>();
            for (int neighborId : vehicle.getNeighborInProgressIds()) {
                VehicleState neighborVehicle = vehicleStateMap.get(neighborId);

                // It is possible that the vehicle itself has not uploaded this frame
                int vehicleLatestChunkId = (vehicle.getFrames() == null ||
                        vehicle.getFrames().get(frameId) == null) ?
                        0 : vehicle.getFrames().get(frameId).getLatestChunkId();
                // It is possible that the neighbor vehicle has not uploaded this frame
                int neighborLatestChunkId = (neighborVehicle.getFrames() == null ||
                        neighborVehicle.getFrames().get(frameId) == null) ?
                        0 : neighborVehicle.getFrames().get(frameId).getLatestChunkId();

                // If vehicles are uploading full frames, simply check if each vehicle has uploaded this frame
                if (vehicleLatestChunkId == 5 || neighborLatestChunkId == 5) {
                    return checkForFullFrame(vehicleStateMap, frameId);
                }

                // Check "finish" conditions
                if (vehicleLatestChunkId + neighborLatestChunkId >= 4) {
                    toBeRemovedNeighborInProgressIds.add(neighborId);
                    neighborVehicle.getNeighborInProgressIds().remove(vehicleId);
                    numCheckedNeighbors ++;
                    LOGGER.debug("v" + vehicleId + ": " + vehicleLatestChunkId
                            + ", v" + neighborId + ": " + neighborLatestChunkId);
                    statHandler.updateUploadingStatus(frameId, vehicleId, vehicleLatestChunkId);
                    statHandler.updateUploadingStatus(frameId, neighborId, neighborLatestChunkId);
                }
                else {
                    LOGGER.debug("v" + vehicleId + ": " + vehicleLatestChunkId
                            + ", v" + neighborId + ": " + neighborLatestChunkId);
                    break;
                }
            }

            // If reach here due to finish condition dissatisfaction, stop checking immediately
            if (numCheckedNeighbors != vehicle.getNeighborInProgressIds().size()) {
                LOGGER.debug("vehicle " + vehicleId + " check interrupted");
                break;
            }

            // Remove the edge (vehicle - neighbor vehicle) that meet the conditions
            for (Integer toBeRemovedNeighbor : toBeRemovedNeighborInProgressIds) {
                vehicle.getNeighborInProgressIds().remove(toBeRemovedNeighbor);
            }
            numCheckedVehicles ++;
        }

        // If reach here due to finish condition dissatisfaction, stop checking immediately
        if (numCheckedVehicles != vehicleStateMap.keySet().size()) {
            return false;
        }
        else {
            // check if all the neighborInProgressIds are empty
            for (Integer vehicleId : vehicleStateMap.keySet()) {
                if (!vehicleStateMap.get(vehicleId).getNeighborInProgressIds().isEmpty()) {
                    LOGGER.debug(vehicleStateMap.get(vehicleId).getNeighborInProgressIds());
                    return false;
                }
            }
            statHandler.printUploadingStatus(frameId);
            return true;
        }
    }

    public static boolean checkForFullFrame(Map<Integer, VehicleState> vehicleStateMap, int frameId) {
        LOGGER.debug("Check for full frames ...");
        int numReceived = 0;
        LOGGER.debug("vehicle number: " + vehicleStateMap.keySet().size());
        for (Integer vehicleId : vehicleStateMap.keySet()) {
            int latestFrameId = vehicleStateMap.get(vehicleId).getLatestMergedFrameId();
            if (latestFrameId >= frameId) {
                numReceived++;
            }
            else {
                LOGGER.debug("vehicle " + vehicleId + " data not received/merged (latest frame: " + latestFrameId + ")");
                break;
            }
        }
        return numReceived == vehicleStateMap.size();
    }
}
