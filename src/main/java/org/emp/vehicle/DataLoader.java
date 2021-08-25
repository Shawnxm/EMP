package org.emp.vehicle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.utils.DataUtils;

import java.io.*;
import java.util.LinkedList;
import java.util.Queue;

public class DataLoader implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(DataLoader.class);

    private static final long FIXED_WAIT = 2000;
    private long frameRate = 100;
    public Queue<dataElement> _prepareSet = new LinkedList<>();
    public Queue<dataElement> _dataSet = new LinkedList<>();
    private final int[] frameIDs;
    private final String pointCloudPath;
    private final String oxtsPath;
    private final String egoObjectPath;

    public float vehicleHeight;

    public DataLoader(int[] IDs, String pPath, String oPath, String egoPath) {
        frameIDs = IDs;
        pointCloudPath = pPath;
        oxtsPath = oPath;
        this.egoObjectPath = egoPath;
    }

    public DataLoader(int[] IDs, String pPath, String oPath, String egoPath, long frameRate) {
        frameIDs = IDs;
        pointCloudPath = pPath;
        oxtsPath = oPath;
        this.frameRate = frameRate;
        this.egoObjectPath = egoPath;
    }

    public void run() {
        LOGGER.debug("Number of frames: " + frameIDs.length);
        for(int i=0; i < frameIDs.length; i++) {
            long t_r1 = System.currentTimeMillis();
            if(frameIDs[i] == 1) {
                dataElement popedData = _prepareSet.poll();
                popedData.timestamp = System.currentTimeMillis();
                LOGGER.debug("Frame " + popedData.idx + " loaded");
                _dataSet.offer(popedData);
                long t_r2 = System.currentTimeMillis();
                if(i == 0 || i == 1){
                    sleep(FIXED_WAIT - (t_r2 - t_r1));
                    continue;
                }
                sleep(frameRate - (t_r2 - t_r1));
            }
            else{
                long t_r2 = System.currentTimeMillis();
                if(i == 0 || i == 1){
                    sleep(FIXED_WAIT - (t_r2 - t_r1));
                    continue;
                }
                sleep(frameRate - (t_r2 - t_r1));
            }
        }
        sleep(2000);
    }

    private void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void prepareDataFromFile() throws IOException {
        for(int i=0; i < frameIDs.length; i++) {
            if(frameIDs[i] == 1) {
                dataElement currentData = new dataElement();
                // Get frame index
                currentData.idx = String.format("%06d", i);
                // Read point cloud
                currentData.points = DataUtils.readPointCloudFromFile(pointCloudPath + currentData.idx + ".bin");
                // Read oxts
                File oxtsFile = new File(oxtsPath + currentData.idx + ".txt");
                InputStreamReader reader = new InputStreamReader(new FileInputStream(oxtsFile));
                BufferedReader br = new BufferedReader(reader);
                String line = "";
                currentData.oxts = br.readLine();;
                reader.close();

                _prepareSet.offer(currentData);
            }
        }

        vehicleHeight = DataUtils.readVehicleHeight(egoObjectPath);
    }

    public static class dataElement {
        public float[] points;
        public String oxts;
        public String idx;
        public long timestamp = System.currentTimeMillis();
    }

}
