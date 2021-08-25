package org.emp.edge;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.edge.PtClMerger;
import org.emp.utils.EmpUnitTest;
import org.emp.utils.TestUtils;
import org.emp.utils.VoronoiAdapt;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

public class PartitionAndMergeTest extends EmpUnitTest {
    private static final Logger LOGGER = LogManager.getLogger(PtClMergerTest.class);

    // Dataset: object-0401, all vehicles, 220 unique frames, 728 frames

    String[] secondaryIds = {"0014850", "0023554", "0024066", "0025858", "0030466", "0032002", "0036610", "0113727", "0296223", "0413513"};
    String primaryPath = "src/test/resources/gta_data/";
    String secondaryPathPrefix = "src/test/resources/gta_data/alt_perspective/";
    VoronoiAdapt partitioner = new VoronoiAdapt();
    PtClMerger merger = new PtClMerger();

    @Test
    public void testPartitionAndNaiveMerge() throws IOException {
        List<Long> mergeTimeList = new ArrayList<>();
        Map<Integer, List<Long>> mergeTimeListByNumVehicle = new HashMap<>();
        int count = 0;

        Set<String> frameIdsWithExt= Stream.of(new File(primaryPath +"oxts").listFiles()).map(File::getName).collect(Collectors.toSet());

        for (String frameId : frameIdsWithExt) {
            // Go through all frame indices of the primary vehicle, load point clouds and oxts data
            frameId = frameId.substring(0, frameId.length()-4);
            float[] primaryPointCloud = TestUtils.readPointCloudFromFile(primaryPath + "velodyne_2/" + frameId + ".bin");
            float[] primaryOxts = TestUtils.loadOxts(primaryPath +"oxts/" + frameId + ".txt");

            // Load point clouds and oxts data for secondary vehicles if exists
            Map<String, float[]> pointCloudMap = new HashMap<String, float[]>() {{put("0000001", primaryPointCloud);}};
            Map<String, float[]> oxtsMap = new HashMap<String, float[]>() {{put("0000001", primaryOxts);}};
            for (String secondaryId : secondaryIds) {
                String pointCloudPath = secondaryPathPrefix + secondaryId + "/velodyne_2/" + frameId + ".bin";
                String oxtsPath = secondaryPathPrefix + secondaryId + "/oxts/" + frameId + ".txt";
                File tmpFile = new File(pointCloudPath);
                if (tmpFile.exists()) {
                    pointCloudMap.put(secondaryId, TestUtils.readPointCloudFromFile(pointCloudPath));
                    oxtsMap.put(secondaryId, TestUtils.loadOxts(oxtsPath));
                }
            }

            // Map vehicle ID (element) to ordered index (index), prepare fake bandwidth list
            List<String> orderedIds = new ArrayList<>(pointCloudMap.keySet());
            List<float[]> oxtsList = new ArrayList<>();
            for (String vehicleId : orderedIds) {
                oxtsList.add(oxtsMap.get(vehicleId));
            }
            List<Float> bwList = Collections.nCopies(pointCloudMap.size()+1, 10f);

            // Partition point clouds based on vehicle locations
            VoronoiAdapt.partitionDecision decision = partitioner.voronoiAdapt(oxtsList, bwList);
            Map<String, List<float[]>> partitionedPointCloudMap = new HashMap<>();
            for (int i = 0; i < orderedIds.size(); i++) {
                String vehicleId = orderedIds.get(i);
                partitionedPointCloudMap.put(vehicleId, partitioner.voronoiMaskAdapt(pointCloudMap.get(vehicleId), decision.pbSet.get(i)));
            }

            // Prepare for merging
            Map<String, List<float[]>> secondaryPointCloudMap = new HashMap<>(partitionedPointCloudMap);
            Map<String, float[]> secondaryOxtsMap = new HashMap<>(oxtsMap);
            secondaryPointCloudMap.remove("0000001");
            secondaryOxtsMap.remove("0000001");
            List<String> orderedSecondaryIds = new ArrayList<>(secondaryPointCloudMap.keySet());

            // Create a float array
            long t_start = System.currentTimeMillis();
            float[] ptcl = primaryPointCloud.clone();

            // Merge (compare NaiveMerge and NaiveMergeFloatButter)
            List<Long> mergeTimeListByFrame = new ArrayList<>();
            for (String vehicleId : orderedSecondaryIds) {
                long frameMergeTime = 0L;

                // Merge the first two chunks
                for (int j = 0; j < 2; j++) {
                    List<float[]> pointsSecondary = Collections.singletonList(secondaryPointCloudMap.get(vehicleId).get(j));
                    List<float[]> oxtsSecondary = Collections.singletonList(secondaryOxtsMap.get(vehicleId));
                    long t1 = System.currentTimeMillis();
                    float[] result = merger.naiveMergeNoPrimary(primaryOxts, pointsSecondary, oxtsSecondary);
                    long t2 = System.currentTimeMillis();
                    frameMergeTime += t2 - t1;
                    assertThat(result.length == pointsSecondary.get(0).length).isTrue();

                    ptcl = ArrayUtils.addAll(ptcl, result);
                }
                mergeTimeListByFrame.add(frameMergeTime);
                mergeTimeList.add(frameMergeTime);
                count ++;
            }
            long t_end = System.currentTimeMillis();
            List<Long> mergeTimeListTmp = mergeTimeListByNumVehicle.computeIfAbsent(orderedSecondaryIds.size()+1, k -> new ArrayList<>());
            mergeTimeListTmp.add(t_end - t_start);

            LOGGER.info("frame: " + frameId + " (" + orderedSecondaryIds.size() + ")"
                    + ", merge average: " + mergeTimeListByFrame.stream().mapToDouble(x -> x).average().orElse(0)
                    + ", stddev: " + new StandardDeviation().evaluate(mergeTimeListByFrame.stream().mapToDouble(x -> x).toArray()));
        }

        LOGGER.info("[Naive] " + count + " frames."
                + ", avg. naiveMergeNoPrimary time: " + mergeTimeList.stream().mapToDouble(x -> x).average().orElse(0)
                + ", stddev: " + new StandardDeviation().evaluate(mergeTimeList.stream().mapToDouble(x -> x).toArray()));

        for (Integer numVehicle : mergeTimeListByNumVehicle.keySet()) {
            LOGGER.info("number of vehicles: " + numVehicle + " (" + mergeTimeListByNumVehicle.get(numVehicle).size() + ")"
                    + ", avg. total merge time: " + mergeTimeListByNumVehicle.get(numVehicle).stream().mapToDouble(x -> x).average().orElse(0)
                    + ", stddev: " + new StandardDeviation().evaluate(mergeTimeListByNumVehicle.get(numVehicle).stream().mapToDouble(x -> x).toArray()));
        }
    }

    @Test
    public void testPartitionAndFbMerge() throws IOException {
        List<Long> mergeTimeList = new ArrayList<>();
        Map<Integer, List<Long>> mergeTimeListByNumVehicle = new HashMap<>();
        int count = 0;

        Set<String> frameIdsWithExt= Stream.of(new File(primaryPath +"oxts").listFiles()).map(File::getName).collect(Collectors.toSet());

        for (String frameId : frameIdsWithExt) {
            // Go through all frame indices of the primary vehicle, load point clouds and oxts data
            frameId = frameId.substring(0, frameId.length()-4);
            float[] primaryPointCloud = TestUtils.readPointCloudFromFile(primaryPath + "velodyne_2/" + frameId + ".bin");
            float[] primaryOxts = TestUtils.loadOxts(primaryPath +"oxts/" + frameId + ".txt");

            // Load point clouds and oxts data for secondary vehicles if exists
            Map<String, float[]> pointCloudMap = new HashMap<String, float[]>() {{put("0000001", primaryPointCloud);}};
            Map<String, float[]> oxtsMap = new HashMap<String, float[]>() {{put("0000001", primaryOxts);}};
            for (String secondaryId : secondaryIds) {
                String pointCloudPath = secondaryPathPrefix + secondaryId + "/velodyne_2/" + frameId + ".bin";
                String oxtsPath = secondaryPathPrefix + secondaryId + "/oxts/" + frameId + ".txt";
                File tmpFile = new File(pointCloudPath);
                if (tmpFile.exists()) {
                    pointCloudMap.put(secondaryId, TestUtils.readPointCloudFromFile(pointCloudPath));
                    oxtsMap.put(secondaryId, TestUtils.loadOxts(oxtsPath));
                }
            }

            // Map vehicle ID (element) to ordered index (index), prepare fake bandwidth list
            List<String> orderedIds = new ArrayList<>(pointCloudMap.keySet());
            List<float[]> oxtsList = new ArrayList<>();
            for (String vehicleId : orderedIds) {
                oxtsList.add(oxtsMap.get(vehicleId));
            }
            List<Float> bwList = Collections.nCopies(pointCloudMap.size()+1, 10f);

            // Partition point clouds based on vehicle locations
            VoronoiAdapt.partitionDecision decision = partitioner.voronoiAdapt(oxtsList, bwList);
            Map<String, List<float[]>> partitionedPointCloudMap = new HashMap<>();
            for (int i = 0; i < orderedIds.size(); i++) {
                String vehicleId = orderedIds.get(i);
                partitionedPointCloudMap.put(vehicleId, partitioner.voronoiMaskAdapt(pointCloudMap.get(vehicleId), decision.pbSet.get(i)));
            }

            // Prepare for merging
            Map<String, List<float[]>> secondaryPointCloudMap = new HashMap<>(partitionedPointCloudMap);
            Map<String, float[]> secondaryOxtsMap = new HashMap<>(oxtsMap);
            secondaryPointCloudMap.remove("0000001");
            secondaryOxtsMap.remove("0000001");
            List<String> orderedSecondaryIds = new ArrayList<>(secondaryPointCloudMap.keySet());

            // Create a FloatBuffer
            long t_start = System.currentTimeMillis();
            int maxSize = 0;
            for (float[] pointCloud : pointCloudMap.values()) {
                maxSize += pointCloud.length;
            }
            FloatBuffer result = FloatBuffer.allocate(maxSize);
            for (float point : primaryPointCloud) {
                result.put(point);
            }
            int offset = primaryPointCloud.length;

            // Merge (compare NaiveMerge and NaiveMergeFloatButter)
            List<Long> mergeTimeListByFrame = new ArrayList<>();
            for (String vehicleId : orderedSecondaryIds) {
                long frameMergeTime = 0L;

                // Merge the first two chunks
                for (int j = 0; j < 2; j++) {
                    List<float[]> pointsSecondary = Collections.singletonList(secondaryPointCloudMap.get(vehicleId).get(j));
                    List<float[]> oxtsSecondary = Collections.singletonList(secondaryOxtsMap.get(vehicleId));
                    long t1 = System.currentTimeMillis();
                    offset = merger.fbMergeNoPrimary(primaryOxts, pointsSecondary, oxtsSecondary, result, offset);
                    long t2 = System.currentTimeMillis();
                    frameMergeTime += t2 - t1;
                }
                mergeTimeListByFrame.add(frameMergeTime);
                mergeTimeList.add(frameMergeTime);
                count ++;
            }
            long t_end = System.currentTimeMillis();
            List<Long> mergeTimeListTmp = mergeTimeListByNumVehicle.computeIfAbsent(orderedSecondaryIds.size()+1, k -> new ArrayList<>());
            mergeTimeListTmp.add(t_end - t_start);


            LOGGER.info("frame: " + frameId + " (" + orderedSecondaryIds.size() + ")"
                    + ", merge average: " + mergeTimeListByFrame.stream().mapToDouble(x -> x).average().orElse(0)
                    + ", stddev: " + new StandardDeviation().evaluate(mergeTimeListByFrame.stream().mapToDouble(x -> x).toArray()));
        }

        LOGGER.info("[Fb] " + count + " frames."
                + ", avg. fbMergeNoPrimary time: " + mergeTimeList.stream().mapToDouble(x -> x).average().orElse(0)
                + ", stddev: " + new StandardDeviation().evaluate(mergeTimeList.stream().mapToDouble(x -> x).toArray()));

        for (Integer numVehicle : mergeTimeListByNumVehicle.keySet()) {
            LOGGER.info("number of vehicles: " + numVehicle + " (" + mergeTimeListByNumVehicle.get(numVehicle).size() + ")"
                    + ", avg. total merge time: " + mergeTimeListByNumVehicle.get(numVehicle).stream().mapToDouble(x -> x).average().orElse(0)
                    + ", stddev: " + new StandardDeviation().evaluate(mergeTimeListByNumVehicle.get(numVehicle).stream().mapToDouble(x -> x).toArray()));
        }
    }
}
