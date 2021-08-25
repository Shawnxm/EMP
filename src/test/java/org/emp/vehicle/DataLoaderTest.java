package org.emp.vehicle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.utils.EmpUnitTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


/**
 * Tests for DataLoader
 */
public class DataLoaderTest extends EmpUnitTest {
    private static final Logger LOGGER = LogManager.getLogger(DataLoaderTest.class);

    @Test
    public void testDataLoader() throws IOException, InterruptedException {
        String path = "src/test/resources/object-0227-1/";
        String order_path = path + "order/";
        String vehicleID;
        String ptcl_path;
        String oxts_path;
        String ego_path;
        int[] frameIDs;

        String inputID = "22786";  // "1"

        if (inputID.equals("1") || inputID.isEmpty()) {
            inputID = "1";
            vehicleID = String.format("%07d", Integer.parseInt(inputID));
            ptcl_path = path + "velodyne_2/";
            oxts_path = path + "oxts/";
            ego_path = path + "ego_object/";
        } else {
            vehicleID = String.format("%07d", Integer.parseInt(inputID));
            ptcl_path = path + "alt_perspective/" + vehicleID + "/velodyne_2/";
            oxts_path = path + "alt_perspective/" + vehicleID + "/oxts/";
            ego_path = path + "alt_perspective/" + vehicleID + "/ego_object/";
        }
        BufferedReader br = new BufferedReader(new FileReader(order_path + vehicleID + ".txt"));
        try {
            String line;
            List<Integer> frameIDList = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                frameIDList.add(line.isEmpty() ? 0 : Integer.parseInt(line));
            }
            frameIDs = frameIDList.stream().mapToInt(i -> i).toArray();
        } finally {
            br.close();
        }

        LOGGER.info("Before DataLoader ...");
        DataLoader loader = new DataLoader(frameIDs, ptcl_path, oxts_path, ego_path);
        loader.prepareDataFromFile();
        loader.run();
        LOGGER.info("After DataLoader ...");
    }
}