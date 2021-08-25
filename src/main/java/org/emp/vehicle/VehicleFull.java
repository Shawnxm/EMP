package org.emp.vehicle;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.EdgeMessageHandlerImpl;
import org.emp.data.StatHandler;
import org.emp.network.NonBlockingNetworkClient;
import org.emp.utils.DracoHelper;
import org.emp.utils.GroundDetector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.emp.data.EdgeMessageHandlerImpl.encodeMessage;

/**
 * A class for running the vehicle processing
 *
 * To run the vehicle client:
 * java -Dlog4j.configurationFile=src/main/resources/log4j2-config.xml \
 *    -cp build/libs/emp-1.0.jar org.emp.vehicle.VehicleFull \
 *    -i [server ip address] -p [server port number] -c [client port number] \
 *    -v [vehicle ID] -d [data path] -r [frame rate]
 */
public class VehicleFull {
    private static final Logger LOGGER = LogManager.getLogger(VehicleFull.class);
    private static String ptclPath;
    private static String oxtsPath;
    private static String egoPath;
    private static int[] frameIDs;
    private static String dataPath;
    private static String orderPath;
    private static String vehicleID;
    private static String inputID;

    public static void main(String[] args) throws Exception {
        final VehicleFull.Config config = new VehicleFull.Config();
        JCommander.newBuilder().addObject(config).build().parse(args);
        String serverIp = config.serverIp;
        int serverPort = config.serverPort;
        int clientPort = config.clientPort;
        inputID = config.vehicleID;  // 1, 22786, 37122, 191023, 399881, 735239
        int frameRate = config.frameRate;

        // Configure paths
        dataPath = config.dataPath;
        orderPath = dataPath + "order/";
        configureFrames();

        GroundDetector groundDetector = new GroundDetector();
        DracoHelper dracoHelper = new DracoHelper();
        StatHandler statHandler = new StatHandler();

        // Load data
        DataLoader dataLoader = new DataLoader(frameIDs, ptclPath, oxtsPath, egoPath, frameRate);
        dataLoader.prepareDataFromFile();
        float height = dataLoader.vehicleHeight;
        Thread loaderThread = new Thread(dataLoader);

        // Start network client
        EdgeMessageHandlerImpl edgeMessageHandler = new EdgeMessageHandlerImpl();
        NonBlockingNetworkClient nonBlockingNetworkClient = new NonBlockingNetworkClient(serverIp, serverPort, clientPort, edgeMessageHandler);
        Thread networkThread = new Thread(nonBlockingNetworkClient);
        networkThread.start();
        nonBlockingNetworkClient.putDataToSendByteBufferQueue(encodeMessage(new byte[0], Integer.parseInt(vehicleID), -1, 5, 'X'));
        LOGGER.info("Client running ...");

        // Wait for the "start" signal
        while (!edgeMessageHandler.isReady()) {
            Thread.sleep(1);
        }
        loaderThread.start();

        while (true) {
            Queue dataSetQueue = dataLoader._dataSet;
            if (dataSetQueue.size() > 0) {
                DataLoader.dataElement myDataElement = (DataLoader.dataElement) dataSetQueue.remove();
                int frameId = Integer.parseInt(myDataElement.idx);
                float[] ptcl = myDataElement.points;
                String oxts = myDataElement.oxts;

                LOGGER.info("Waiting on frame: " + frameId);
                while (edgeMessageHandler.getFrameToSent() != frameId) {
                    Thread.sleep(1);
                }
                LOGGER.info("Preparing for frame: " + frameId);
                statHandler.logFrameStartTime(frameId, System.currentTimeMillis());

                // Send oxts
                byte[] oxtsBytes = oxts.getBytes();
                byte[] oxtsWithHeader = encodeMessage(oxtsBytes, Integer.parseInt(vehicleID), frameId, 5, 'O');
                nonBlockingNetworkClient.putDataToSendByteBufferQueue(oxtsWithHeader);

                // Remove ground
                Long tGround1 = System.currentTimeMillis();
                groundDetector.groundDetectorRANSAC(ptcl, false, height, false);
                ptcl = groundDetector.getObjectPoints();
                Long tGround2 = System.currentTimeMillis();
                statHandler.logGroundRemovalTime(frameId, tGround2-tGround1);

                // Compress and send Ptcl
                Long tDracoEncode1 = System.currentTimeMillis();
                byte[] encodedPtcl = dracoHelper.encode(ptcl, 10, 12);
                Long tDracoEncode2 = System.currentTimeMillis();
                statHandler.logDracoEncodingTime(frameId, 5, tDracoEncode2-tDracoEncode1);

                byte[] encodedPtclWithHeader = encodeMessage(encodedPtcl, Integer.parseInt(vehicleID), frameId, 5, 'P');

                statHandler.logSendTime(frameId, 5, System.currentTimeMillis());
                nonBlockingNetworkClient.putDataToSendByteBufferQueue(encodedPtclWithHeader);
            }
            Thread.sleep(1); // otherwise won't be looping
        }
    }

    public static void configureFrames() throws Exception {

        if (inputID.equals("1") || inputID.isEmpty()) {
            inputID = "1";
            vehicleID = String.format("%07d", Integer.parseInt(inputID));
            ptclPath = dataPath + "velodyne_2/";
            oxtsPath = dataPath + "oxts/";
            egoPath = dataPath + "ego_object/";
        }
        else {
            vehicleID = String.format("%07d", Integer.parseInt(inputID));
            ptclPath = dataPath + "alt_perspective/" + vehicleID + "/velodyne_2/";
            oxtsPath = dataPath + "alt_perspective/" + vehicleID + "/oxts/";
            egoPath = dataPath + "alt_perspective/" + vehicleID + "/ego_object/";
        }
        LOGGER.info("Point cloud path: " + ptclPath);

        BufferedReader br = new BufferedReader(new FileReader(orderPath + vehicleID + ".txt"));
        try {
            String line;
            List<Integer> frameIDList = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                frameIDList.add(line.isEmpty() ? 0 : Integer.parseInt(line));
            }
            frameIDs = frameIDList.stream().mapToInt(i->i).toArray();
        } finally {
            br.close();
        }

    }

    static class Config {
        @Parameter(
                names = {"--serverip", "-i"},
                description = "Server ip address to use"
        )
        public String serverIp = null;

        @Parameter(
                names = {"--serverport", "-p"},
                description = "Server port number to use"
        )
        public int serverPort = 31904;

        @Parameter(
                names = {"--clientport", "-c"},
                description = "Client port number to use"
        )
        public int clientPort = 55854;

        @Parameter(
                names = {"--vehicle", "-v"},
                description = "ID of the vehicle instance to be started"
        )
        public String vehicleID = "1";

        @Parameter(
                names = {"--datapath", "-d"},
                description = "Path of the vehicle dataset"
        )
        public String dataPath = null;

        @Parameter(
                names = {"--framerate", "-r"},
                description = "Frame loading rate of the data loader"
        )
        public int frameRate = 100;
    }
}
