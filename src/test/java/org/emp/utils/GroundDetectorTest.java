package org.emp.utils;

import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class GroundDetectorTest extends EmpUnitTest {
    private static final Logger LOGGER = LogManager.getLogger(GroundDetectorTest.class);

    @Test
    public void testGroundDetector() throws IOException {
        LOGGER.info("Test GroundDetector ...");

        boolean outputPointClouds = true;
        String pointCloudPath = "src/test/resources/sample_data_for_merging/ego/velodyne/000003.bin";
        float[] pointsPrimary = TestUtils.readPointCloudFromFile(pointCloudPath);
        GroundDetector myGroundDetector = new GroundDetector();

        long tStart = System.currentTimeMillis();
        myGroundDetector.groundDetectorRANSAC(pointsPrimary, true, 1.57727f, true);
        long tEnd = System.currentTimeMillis();

        float[] myGround = myGroundDetector.getGroundPoints();
        float[] myObject = myGroundDetector.getObjectPoints();

        LOGGER.info(tEnd - tStart + "ms");
        LOGGER.info("Object Points: " + myObject.length / 4);
        LOGGER.info("Ground Points: " + myGround.length / 4);
        LOGGER.info("Total Points: " + pointsPrimary.length / 4);

        if(outputPointClouds){
            TestUtils.writePointCloudToFile(myObject, "src/test/resources/sample_data_for_merging/ego/ground_detect_results/object/000003.bin");
            TestUtils.writePointCloudToFile(myGround, "src/test/resources/sample_data_for_merging/ego/ground_detect_results/ground/000003.bin");
            TestUtils.writePointCloudToFile(pointsPrimary, "src/test/resources/sample_data_for_merging/ego/ground_detect_results/original/000003.bin");
        }
    }

    @Test
    public void testGroundDetectorHeightFiltered() throws IOException {
        LOGGER.info("Test GroundDetector with height filtering ...");

        boolean outputPointClouds = true;
        String pointCloudPath = "src/test/resources/sample_data_for_merging/ego/velodyne/000003.bin";
        float[] pointsPrimary = TestUtils.readPointCloudFromFile(pointCloudPath);
        GroundDetector myGroundDetector = new GroundDetector();

        long tStart = System.currentTimeMillis();
        myGroundDetector.groundDetectorRANSAC(pointsPrimary, false, 1.57727f, true);
        long tEnd  = System.currentTimeMillis();

        float[] myGround = myGroundDetector.getGroundPoints();
        float[] myObject = myGroundDetector.getObjectPoints();

        LOGGER.info(tEnd - tStart + "ms");
        LOGGER.info("Object Points: " + myObject.length / 4);
        LOGGER.info("Ground Points: " + myGround.length / 4);
        LOGGER.info("Total Points: " + pointsPrimary.length / 4);

        if(outputPointClouds){
            TestUtils.writePointCloudToFile(myObject, "src/test/resources/sample_data_for_merging/ego/ground_detect_results/object/000003_h.bin");
            TestUtils.writePointCloudToFile(myGround, "src/test/resources/sample_data_for_merging/ego/ground_detect_results/ground/000003_h.bin");
            TestUtils.writePointCloudToFile(pointsPrimary, "src/test/resources/sample_data_for_merging/ego/ground_detect_results/original/000003_h.bin");
        }
    }

    @Test
    public void benchmarkGroundDetector() throws IOException {

        boolean outputPointClouds = false;

        LOGGER.info("Benchmark GroundDetector ...");

        String data_path = "src/test/resources/gta_data/";
        String[] secondaryVehicle = {"0014850", "0023554", "0024066", "0025858", "0030466",
                "0032002", "0036610", "0113727", "0296223", "0413513"};
        String output_path = "src/test/resources/ground_detect_results/";

        GroundDetector myGroundDetector = new GroundDetector();

        List<Long> times = new ArrayList<>();
        List<Integer> groundPoints = new ArrayList<>();
        List<Integer> objectPoints = new ArrayList<>();
        List<Integer> totalPoints = new ArrayList<>();
        List<List<Long>> altTimes = new ArrayList<>(secondaryVehicle.length);
        List<List<Integer>> altGroundPoints = new ArrayList<>(secondaryVehicle.length);
        List<List<Integer>> altObjectPoints = new ArrayList<>(secondaryVehicle.length);
        List<List<Integer>> altTotalPoints = new ArrayList<>(secondaryVehicle.length);

        List<Long> detectingTimeMsList = new ArrayList<>();
        List<Float> objectSizeRatioList = new ArrayList<>();

        for(int i = 0; i < secondaryVehicle.length; i++){
            altTimes.add(new ArrayList<>());
            altGroundPoints.add(new ArrayList<>());
            altObjectPoints.add(new ArrayList<>());
            altTotalPoints.add(new ArrayList<>());
        }

        for(int i = 0; i < 220; i++){
            String frameID = String.format("%06d", i);

            float[] pointsPrimary = TestUtils.readPointCloudFromFile(data_path+"velodyne_2/"+frameID+".bin");
            float egoObjectHeight = TestUtils.loadEgoObjectHeight(data_path+"ego_object/"+frameID+".txt");

            // ground detect
            Long start = System.currentTimeMillis();
            boolean detected = myGroundDetector.groundDetectorRANSAC(pointsPrimary, true, egoObjectHeight, true);
            Long end = System.currentTimeMillis();
            if(detected){
                float[] myGround = myGroundDetector.getGroundPoints();
                float[] myObject = myGroundDetector.getObjectPoints();
                groundPoints.add(myGround.length / 4);
                objectPoints.add(myObject.length / 4);
                objectSizeRatioList.add((float) myObject.length / pointsPrimary.length);

                if(outputPointClouds){
                    TestUtils.writePointCloudToFile(myObject, output_path + "object/"+frameID+".bin");
                    TestUtils.writePointCloudToFile(myGround, output_path + "ground/"+frameID+".bin");
                    TestUtils.writePointCloudToFile(pointsPrimary, output_path + "original/"+frameID+".bin");
                }
            }
            else{
                groundPoints.add(0);
                objectPoints.add(0);
            }
            times.add(end - start);
            detectingTimeMsList.add(end - start);
            totalPoints.add(pointsPrimary.length / 4);

            String alt_path = data_path + "alt_perspective/";
            for(int j = 0; j < secondaryVehicle.length; j++){
                String ptclname = alt_path + secondaryVehicle[j] + "/velodyne_2/" + frameID + ".bin";
                File tmpFile = new File(ptclname);
                if(tmpFile.exists()){
                    pointsPrimary = TestUtils.readPointCloudFromFile(ptclname);
                    egoObjectHeight = TestUtils.loadEgoObjectHeight(alt_path + secondaryVehicle[j] + "/ego_object/" + frameID + ".txt");

                    // ground detect
                    start = System.currentTimeMillis();
                    detected = myGroundDetector.groundDetectorRANSAC(pointsPrimary, true, egoObjectHeight, true);
                    end = System.currentTimeMillis();

                    if(detected){
                        float[] myGround = myGroundDetector.getGroundPoints();
                        float[] myObject = myGroundDetector.getObjectPoints();
                        altGroundPoints.get(j).add(myGround.length / 4);
                        altObjectPoints.get(j).add(myObject.length / 4);
                        objectSizeRatioList.add((float) myObject.length / pointsPrimary.length);
                    }
                    else{
                        altGroundPoints.get(j).add(0);
                        altObjectPoints.get(j).add(0);
                    }
                    altTimes.get(j).add(end - start);
                    detectingTimeMsList.add(end - start);
                    altTotalPoints.get(j).add(pointsPrimary.length / 4);
                }
            }
        }

        LOGGER.info("Vehicle: Ego, Num of frames: " + times.size());
        LOGGER.info("Avg. ground detecting time: " + times.stream().mapToDouble(x -> x).average().orElse(0) + " ms, stddev: " +
                    new StandardDeviation().evaluate(times.stream().mapToDouble(x -> x).toArray()) + " ms");
        LOGGER.info("Avg. object points: " + objectPoints.stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                    new StandardDeviation().evaluate(objectPoints.stream().mapToDouble(x -> x).toArray()));
        LOGGER.info("Avg. ground points: " + groundPoints.stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                    new StandardDeviation().evaluate(groundPoints.stream().mapToDouble(x -> x).toArray()));
        LOGGER.info("Avg. total points: " + totalPoints.stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                    new StandardDeviation().evaluate(totalPoints.stream().mapToDouble(x -> x).toArray()));
        LOGGER.info("");

        for(int i = 0; i < secondaryVehicle.length; i++){
            LOGGER.info("Vehicle: " + secondaryVehicle[i] + ", Num of frames: " + altTimes.get(i).size());
            LOGGER.info("Avg. ground detecting time: " + altTimes.get(i).stream().mapToDouble(x -> x).average().orElse(0) + " ms, stddev: " +
                        new StandardDeviation().evaluate(altTimes.get(i).stream().mapToDouble(x -> x).toArray()) + " ms");
            LOGGER.info("Avg. object points: " + altObjectPoints.get(i).stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                        new StandardDeviation().evaluate(altObjectPoints.get(i).stream().mapToDouble(x -> x).toArray()));
            LOGGER.info("Avg. ground points: " + altGroundPoints.get(i).stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                        new StandardDeviation().evaluate(altGroundPoints.get(i).stream().mapToDouble(x -> x).toArray()));
            LOGGER.info("Avg. total points: " + altTotalPoints.get(i).stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                        new StandardDeviation().evaluate(altTotalPoints.get(i).stream().mapToDouble(x -> x).toArray()));
            LOGGER.info("");
        }

        LOGGER.info("Total num of frames: " + detectingTimeMsList.size());
        LOGGER.info("Detecting time: " + detectingTimeMsList.stream().mapToDouble(x -> x).average().orElse(0) +
                " ms, stddev: " + new StandardDeviation().evaluate(detectingTimeMsList.stream().mapToDouble(x -> x).toArray()));
        LOGGER.info("Object/Total ratio: " + objectSizeRatioList.stream().mapToDouble(x -> x).average().orElse(0) +
                ", stddev: " + new StandardDeviation().evaluate(objectSizeRatioList.stream().mapToDouble(x -> x).toArray()));
    }

    @Test
    public void benchmarkGroundDetectorHeightFiltered() throws IOException {

        boolean outputPointClouds = false;

        LOGGER.info("Benchmark GroundDetector with height filtering ...");

        String data_path = "src/test/resources/gta_data/";
        String[] secondaryVehicle = {"0014850", "0023554", "0024066", "0025858", "0030466",
                "0032002", "0036610", "0113727", "0296223", "0413513"};
        String output_path = "src/test/resources/ground_detect_results/";

        GroundDetector myGroundDetector = new GroundDetector();

        List<Long> times = new ArrayList<>();
        List<Integer> groundPoints = new ArrayList<>();
        List<Integer> objectPoints = new ArrayList<>();
        List<Integer> totalPoints = new ArrayList<>();
        List<List<Long>> altTimes = new ArrayList<>(secondaryVehicle.length);
        List<List<Integer>> altGroundPoints = new ArrayList<>(secondaryVehicle.length);
        List<List<Integer>> altObjectPoints = new ArrayList<>(secondaryVehicle.length);
        List<List<Integer>> altTotalPoints = new ArrayList<>(secondaryVehicle.length);

        List<Long> detectingTimeMsList = new ArrayList<>();
        List<Float> objectSizeRatioList = new ArrayList<>();

        for(int i = 0; i < secondaryVehicle.length; i++){
            altTimes.add(new ArrayList<>());
            altGroundPoints.add(new ArrayList<>());
            altObjectPoints.add(new ArrayList<>());
            altTotalPoints.add(new ArrayList<>());
        }

        for(int i = 0; i < 220; i++){
            String frameID = String.format("%06d", i);

            float[] pointsPrimary = TestUtils.readPointCloudFromFile(data_path+"velodyne_2/"+frameID+".bin");
            float egoObjectHeight = TestUtils.loadEgoObjectHeight(data_path+"ego_object/"+frameID+".txt");

            // ground detect
            Long start = System.currentTimeMillis();
            boolean detected = myGroundDetector.groundDetectorRANSAC(pointsPrimary, false, egoObjectHeight, true);
            Long end = System.currentTimeMillis();
            if(detected){
                float[] myGround = myGroundDetector.getGroundPoints();
                float[] myObject = myGroundDetector.getObjectPoints();
                groundPoints.add(myGround.length / 4);
                objectPoints.add(myObject.length / 4);
                objectSizeRatioList.add((float) myObject.length / pointsPrimary.length);

                if(outputPointClouds){
                    TestUtils.writePointCloudToFile(myObject, output_path + "object/"+frameID+"_h.bin");
                    TestUtils.writePointCloudToFile(myGround, output_path + "ground/"+frameID+"_h.bin");
                    TestUtils.writePointCloudToFile(pointsPrimary, output_path + "original/"+frameID+"_h.bin");
                }
            }
            else{
                groundPoints.add(0);
                objectPoints.add(0);
            }
            times.add(end - start);
            detectingTimeMsList.add(end - start);
            totalPoints.add(pointsPrimary.length / 4);

            String alt_path = data_path + "alt_perspective/";
            for(int j = 0; j < secondaryVehicle.length; j++){
                String ptclname = alt_path + secondaryVehicle[j] + "/velodyne_2/" + frameID + ".bin";
                File tmpFile = new File(ptclname);
                if(tmpFile.exists()){
                    pointsPrimary = TestUtils.readPointCloudFromFile(ptclname);
                    egoObjectHeight = TestUtils.loadEgoObjectHeight(alt_path + secondaryVehicle[j] + "/ego_object/" + frameID + ".txt");

                    // ground detect
                    start = System.currentTimeMillis();
                    detected = myGroundDetector.groundDetectorRANSAC(pointsPrimary, false, egoObjectHeight, true);
                    end = System.currentTimeMillis();

                    if(detected){
                        float[] myGround = myGroundDetector.getGroundPoints();
                        float[] myObject = myGroundDetector.getObjectPoints();
                        altGroundPoints.get(j).add(myGround.length / 4);
                        altObjectPoints.get(j).add(myObject.length / 4);
                        objectSizeRatioList.add((float) myObject.length / pointsPrimary.length);
                    }
                    else{
                        altGroundPoints.get(j).add(0);
                        altObjectPoints.get(j).add(0);
                    }
                    altTimes.get(j).add(end - start);
                    detectingTimeMsList.add(end - start);
                    altTotalPoints.get(j).add(pointsPrimary.length / 4);
                }
            }
        }

        LOGGER.info("Vehicle: Ego, Num of frames: " + times.size());
        LOGGER.info("Avg. ground detecting time: " + times.stream().mapToDouble(x -> x).average().orElse(0) + " ms, stddev: " +
                    new StandardDeviation().evaluate(times.stream().mapToDouble(x -> x).toArray()) + " ms");
        LOGGER.info("Avg. object points: " + objectPoints.stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                    new StandardDeviation().evaluate(objectPoints.stream().mapToDouble(x -> x).toArray()));
        LOGGER.info("Avg. ground points: " + groundPoints.stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                    new StandardDeviation().evaluate(groundPoints.stream().mapToDouble(x -> x).toArray()));
        LOGGER.info("Avg. total points: " + totalPoints.stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                    new StandardDeviation().evaluate(totalPoints.stream().mapToDouble(x -> x).toArray()));
        LOGGER.info("");

        for(int i = 0; i < secondaryVehicle.length; i++){
            LOGGER.info("Vehicle: " + secondaryVehicle[i] + ", Num of frames: " + altTimes.get(i).size());
            LOGGER.info("Avg. ground detecting time: " + altTimes.get(i).stream().mapToDouble(x -> x).average().orElse(0) + " ms, stddev: " +
                        new StandardDeviation().evaluate(altTimes.get(i).stream().mapToDouble(x -> x).toArray()) + " ms");
            LOGGER.info("Avg. object points: " + altObjectPoints.get(i).stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                        new StandardDeviation().evaluate(altObjectPoints.get(i).stream().mapToDouble(x -> x).toArray()));
            LOGGER.info("Avg. ground points: " + altGroundPoints.get(i).stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                        new StandardDeviation().evaluate(altGroundPoints.get(i).stream().mapToDouble(x -> x).toArray()));
            LOGGER.info("Avg. total points: " + altTotalPoints.get(i).stream().mapToDouble(x -> x).average().orElse(0) + ", stddev: " +
                        new StandardDeviation().evaluate(altTotalPoints.get(i).stream().mapToDouble(x -> x).toArray()));
            LOGGER.info("");
        }

        LOGGER.info("Total num of frames: " + detectingTimeMsList.size());
        LOGGER.info("Detecting time: " + detectingTimeMsList.stream().mapToDouble(x -> x).average().orElse(0) +
                " ms, stddev: " + new StandardDeviation().evaluate(detectingTimeMsList.stream().mapToDouble(x -> x).toArray()));
        LOGGER.info("Object/Total ratio: " + objectSizeRatioList.stream().mapToDouble(x -> x).average().orElse(0) +
                ", stddev: " + new StandardDeviation().evaluate(objectSizeRatioList.stream().mapToDouble(x -> x).toArray()));
    }

    private void writePointCloudToFileXYZ(float[] pointsToBeSaved, String filename) throws IOException {
        File output = new File(filename);
        output.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(output));
        for(int j = 0; j < pointsToBeSaved.length / 4; j++){
            bw.write(pointsToBeSaved[j*4] + " " + pointsToBeSaved[j*4+1] + " " + pointsToBeSaved[j*4+2] + "\n");
        }
        bw.flush();
        bw.close();
    }
}
