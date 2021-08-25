package org.emp.network;

import static java.lang.Math.min;

import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * A bandwidth estimation module that measures frame arrival time to predict future bandwidth
 */
public class BandwidthEstimator {

    private static final Logger LOGGER = LogManager.getLogger(BandwidthEstimator.class);
    // Measured bandwidth for each frame of each client in kbps
    private Map<Integer, Map<Integer, Integer>> measuredBW;
    // Timestamp and received size for starting receiving each frame of each client
    private Map<Integer, Map<Integer, Pair<Long, Integer>>> startTimestamp;
    // Timestamp for finishing receiving each frame of each client
    private Map<Integer, Map<Integer, Long>> finishTimestamp;
    // Expected size of each frame of each client
    private Map<Integer, Map<Integer, Integer>> clientToFrame;
    // The last frame received on each connection
    private Map<Integer, Integer> lastFrame;
    // The smoothed BW for each client
    private Map<Integer, Integer> ewmaBW;
    // Length of the moving averaging window
    private int MOVING_AVERAGE_LEN = 25;
    // Stores samples (BW, time) in moving average window
    private BlockingQueue<Long[]> BWsamples;
    // Time (in msec) adjustment reference for EWMA samples
    private int MAX_WINDOW_TIME_IN_MS = 30000;

    public BandwidthEstimator() {
        measuredBW = new HashMap<>();
        startTimestamp = new HashMap<>();
        finishTimestamp = new HashMap<>();
        clientToFrame = new HashMap<>();
        lastFrame = new HashMap<>();
        ewmaBW = new HashMap<>();
        BWsamples = new ArrayBlockingQueue<>(MOVING_AVERAGE_LEN);
        LOGGER.info("Init BandwidthEstimator");
    }

    /**
     * Called when a server reads data (partial or more than one frame) from socket buffers
     *
     * @param clientID the ID of the vehicle client
     * @param frameSizeMap maps each frame to a list which is the partially received and total expected frame size
     */
    public void onReceiveData(int clientID, Map<Integer, List<Integer>> frameSizeMap) {
        long currTime = System.currentTimeMillis();
        for (Integer frameID : frameSizeMap.keySet()) {
            int readSize = frameSizeMap.get(frameID).get(0);
            int frameSize = frameSizeMap.get(frameID).get(1);
            if (clientToFrame.containsKey(frameID)) {
                clientToFrame.get(clientID).put(frameID, frameSizeMap.get(frameID).get(1));
            } else {
                Map<Integer, Integer> frameIdToSize = new HashMap<>();
                frameIdToSize.put(frameID, frameSize);
                clientToFrame.put(clientID, frameIdToSize);
//                LOGGER.info(clientToFrame);
            }

            if (getStartTime(clientID, frameID) == -1) {
                // new frame
                setStartTime(clientID, frameID, currTime, readSize);
            }

            if (frameSizeMap.get(frameID).get(0).equals(frameSizeMap.get(frameID).get(1))) {
                // if the received size is equals the expected size, the frame is done
                if (frameID > getLastFrame(clientID)) {
                    lastFrame.put(clientID, frameID);
                }
                setFinishTime(clientID, frameID, currTime);
                measureBW(clientID, frameID);
            }
        }
    }

    /**
     * Bandwidth prediction (kbps) per measurement result
     *
     * @param clientID ID of the client
     * @param algo algorithm used to predict bandwidth
     */
    public int getEstimatedBW(int clientID, String algo) {
        int BW = -1;
        switch (algo) {
            case "naive":
                // Naive approach: Use latest past to predict future
                BW = getMeasuredBW(clientID, getLastFrame(clientID));
                break;
            case "ewma":
                // exponentially weighted moving average
                BW = ewmaBW.get(clientID);
                break;
            case "rls":
                // TODO: Recursive least square
                break;
            default:
                LOGGER.info("No such algo yet");
        }
        return BW;
    }

    /**
     * Bandwidth measurement (kbps) per frame timing
     *
     * @param clientID ID of the client
     * @param frameID ID of the frame
     */
    public void measureBW(int clientID, int frameID) {
        long startTime = getStartTime(clientID, frameID);
        long finishTime = getFinishTime(clientID, frameID);
        long duration = finishTime - startTime;
        int size = getFrameSize(clientID, frameID) - getStartSize(clientID, frameID);
//        LOGGER.info(size + " " + duration + " " + startTime + " " + finishTime);
        if (duration > 0 /*duration > 10 && size > 50*/) {
            // ignore small frames
            setMeasuredBW(clientID, frameID, (int) (size * 8 / duration));
            updateEwmaBW(clientID, getMeasuredBW(clientID, getLastFrame(clientID)));
        }
    }

    /**
     * Update the ewmaBW when getting a new BW sample
     *
     * @param clientID ID of the client
     * @param newBW the new BW sample
     */
    public void updateEwmaBW(int clientID, int newBW) {
        double ewma = 0.0;
        try {
            if (BWsamples.size() == MOVING_AVERAGE_LEN) {
                BWsamples.take();
            }
            Long[] bwAndTime = {(long) newBW, System.currentTimeMillis()};
            BWsamples.put(bwAndTime);
            Iterator iter = BWsamples.iterator();
            int cnt = 0;
            long lastTime = 0;
            while (iter.hasNext()) {
                Object obj = iter.next();
//                LOGGER.info("OBJ: " + obj);
                Long[] bwNTime = (Long[]) obj;
                if (cnt == 0) {
                    ewma = (double) bwNTime[0];
                    lastTime = bwNTime[1];
                }
                else {
                    long currTime = bwNTime[1];
                    // Give less weight to stale BW samples
                    double alpha = 0.75 + 0.25 * (currTime - lastTime) / MAX_WINDOW_TIME_IN_MS;
                    alpha = min(alpha, 1);
                    ewma = (1 - alpha) * ewma + alpha * (double) bwNTime[0];
//                    LOGGER.info("alpha=" + alpha + " old=" + ewma + " new=" + (double) bwNTime[0]);
                    lastTime = currTime;
                }
                cnt++;
            }
        }
        catch(Exception e) {
            System.out.println(e);
        }
//        LOGGER.info("EWMA: " + ewma);
        ewmaBW.put(clientID, (int) ewma);
    }

    /**
     * Get the startTimestamp of a frame of a client
     *
     * @param clientID ID of the client
     * @param frameID ID of the frame
     * @return startTimestamp
     */
    public long getStartTime(int clientID, int frameID) {
        if (!startTimestamp.containsKey(clientID)) {
            return -1;
        }
        else if (!startTimestamp.get(clientID).containsKey(frameID)) {
            return -1;
        }
        else {
            return startTimestamp.get(clientID).get(frameID).getKey();
        }
    }

    /**
     * Get the first piece of a frame of a client
     *
     * @param clientID ID of the client
     * @param frameID ID of the frame
     * @return startTimestamp
     */
    public int getStartSize(int clientID, int frameID) {
        if (!startTimestamp.containsKey(clientID)) {
            return -1;
        }
        else if (!startTimestamp.get(clientID).containsKey(frameID)) {
            return -1;
        }
        else {
            return startTimestamp.get(clientID).get(frameID).getValue();
        }
    }

    /**
     * Get the finishTimestamp of a frame of a client
     *
     * @param clientID ID of the client
     * @param frameID ID of the frame
     * @return finishTimestamp the timestamp the server finishes receiving the frame
     */
    public long getFinishTime(int clientID, int frameID) {
        if (!finishTimestamp.containsKey(clientID)) {
            return -1;
        }
        else if (!finishTimestamp.get(clientID).containsKey(frameID)) {
            return -1;
        }
        else {
            return finishTimestamp.get(clientID).get(frameID);
        }
    }

    /**
     * Set the startTimestamp of a frame of a client
     *
     * @param clientID ID of the client
     * @param frameID ID of the frame
     */
    public void setStartTime(int clientID, int frameID, long timestamp, int readSize) {
        if (startTimestamp.containsKey(clientID)) {
            startTimestamp.get(clientID).put(frameID, new Pair(timestamp, readSize));
        }
        else {
            Map<Integer, Pair<Long, Integer>> frameIdToTimestamp = new HashMap<>();
            frameIdToTimestamp.put(frameID, new Pair(timestamp, readSize));
            startTimestamp.put(clientID, frameIdToTimestamp);
        }
    }

    /**
     * Set the finishTimestamp of a frame of a client
     *
     * @param clientID ID of the client
     * @param frameID ID of the frame
     */
    public void setFinishTime(int clientID, int frameID, long timestamp) {
        if (finishTimestamp.containsKey(clientID)) {
            finishTimestamp.get(clientID).put(frameID, timestamp);
        }
        else {
            Map<Integer, Long> frameIdToTimestamp = new HashMap<>();
            frameIdToTimestamp.put(frameID, timestamp);
            finishTimestamp.put(clientID, frameIdToTimestamp);
        }
    }

    /**
     * Get the full size of a frame
     *
     * @param clientID ID of the client
     * @param frameID ID of the frame
     * @return the frame size
     */
    public int getFrameSize(int clientID, int frameID) {
//        LOGGER.info(clientID + " " + frameID);
        int frameSize = clientToFrame.get(clientID).get(frameID);
//        LOGGER.info("frameSize=" + frameSize);
        return frameSize;
    }

    /**
     * Set the measuredBW
     *
     * @param clientID ID of the client
     * @param frameID ID of the frame
     * @param BW the bandwidth to set
     */
    public void setMeasuredBW(int clientID, int frameID, int BW) {
        if (measuredBW.containsKey(clientID)) {
            measuredBW.get(clientID).put(frameID, BW);
        }
        else {
            Map<Integer, Integer> frameIdToBW = new HashMap<>();
            frameIdToBW.put(frameID, BW);
            measuredBW.put(clientID, frameIdToBW);
        }
    }

    /**
     * Get the measuredBW
     *
     * @param clientID ID of the client
     * @param frameID ID of the frame
     * @return the bandwidth measured
     */
    public int getMeasuredBW(int clientID, int frameID) {
        if (!measuredBW.containsKey(clientID)) {
            return -1;
        }
        else if (!measuredBW.get(clientID).containsKey(frameID)) {
            return -1;
        }
        else {
            return measuredBW.get(clientID).get(frameID);
        }
    }

    /**
     * Get the last received frame
     *
     * @param clientID ID of the client
     * @return the frameID of the latest frame
     */
    public int getLastFrame(int clientID) {
        if (!lastFrame.containsKey(clientID)) {
            return -1;
        }
        else {
            return lastFrame.get(clientID);
        }
    }

}
