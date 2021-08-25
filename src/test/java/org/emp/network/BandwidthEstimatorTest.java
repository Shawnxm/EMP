package org.emp.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.utils.EmpUnitTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BandwidthEstimatorTest extends EmpUnitTest {

    private static final Logger LOGGER = LogManager.getLogger(BandwidthEstimatorTest.class);

    public void receiveData(int clientID, int frameID, int receiveSize, int frameSize, BandwidthEstimator bandwidthEstimator) {
        Map<Integer, List<Integer>> frameSizeMap = new HashMap<>();
        List<Integer> sizeList = new ArrayList<>();
        // First read: frame 0 1000 out of 50000 bytes
        sizeList.add(receiveSize);
        sizeList.add(frameSize);
        frameSizeMap.put(frameID, sizeList);
        bandwidthEstimator.onReceiveData(clientID, frameSizeMap);
    }

    public void sleep(int ms) {
        try {
            Thread.sleep(ms);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printBW(int clientID, int frameID, BandwidthEstimator bandwidthEstimator) {
        int measuredBW = bandwidthEstimator.getMeasuredBW(clientID, frameID);
        LOGGER.info("Measured BW for client" + clientID + ", frame" + frameID + ": " + measuredBW);
        LOGGER.info("Predicted naive BW for client" + clientID + ": " + bandwidthEstimator.getEstimatedBW(clientID, "naive"));
        LOGGER.info("Predicted ewma BW for client" + clientID + ": " + bandwidthEstimator.getEstimatedBW(clientID, "ewma"));
    }

    @Test
    public void testBandwidthEstimator() {

        BandwidthEstimator bandwidthEstimator = new BandwidthEstimator();
        int clientID = 0;
        int frameID = 0;
        // First read: frame 0 1000 out of 50000 bytes
        receiveData(clientID, frameID, 1000, 50000, bandwidthEstimator);
        sleep(1000);
        // Second read: frame 0 1000 out of 50000 bytes
        receiveData(clientID, frameID, 50000, 50000, bandwidthEstimator);
        printBW(clientID, frameID, bandwidthEstimator);
//        printBW(clientID + 1, frameID, bandwidthEstimator);
        sleep(2000);
        // Third read: frame 1 2000 out of 80000 bytes
        receiveData(clientID, frameID + 1, 2000, 80000, bandwidthEstimator);
        sleep(3000);
        // Fourth read: frame 1 80000 out of 80000 bytes
        receiveData(clientID, frameID + 1, 80000, 80000, bandwidthEstimator);
        printBW(clientID, frameID + 1, bandwidthEstimator);
//        printBW(clientID + 1, frameID + 1, bandwidthEstimator);

    }

}
