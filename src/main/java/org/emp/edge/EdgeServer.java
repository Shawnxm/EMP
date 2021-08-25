package org.emp.edge;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.EmpReapSensorDataHandler;
import org.emp.data.SensorDataHandler;
import org.emp.data.VehicleMessageHandler;
import org.emp.data.VehicleMessageHandlerImpl;
import org.emp.network.BandwidthEstimator;
import org.emp.network.NonBlockingNetworkServer;
import org.emp.task.TaskScheduler;

/**
 * A class for running the edge processing
 *
 * To run the edge server:
 * java -Dlog4j.configurationFile=src/main/resources/log4j2-config.xml \
 *    -cp build/libs/emp-1.0.jar org.emp.edge.EdgeServer \
 *    -p [server port] -t [numThreads] -c [numClients] -a [algorithm index] (-s [save path])
 */
public class EdgeServer {
  private static final Logger LOGGER = LogManager.getLogger(EdgeServer.class);
  private final ExecutorService executor = Executors.newFixedThreadPool(2);
  private final NonBlockingNetworkServer networkServer;
  private final TaskScheduler scheduler;
  private final int port;
  private final int numThreads;
  private final int numClients;
  private final int algorithmId;
  private final String savePath;
  private Future taskSchedulerFuture;
  private Future networkServerFuture;

  public EdgeServer(int port, int numThreads, int numClients, int algorithmId, String savePath) throws UnknownHostException {
    this.port = port;
    this.numThreads = numThreads;
    this.numClients = numClients;
    this.algorithmId = algorithmId;
    this.savePath = savePath;
    BandwidthEstimator bandwidthEstimator = new BandwidthEstimator();  // TODO: use Singleton Pattern
    SensorDataHandler sensorDataHandler = new EmpReapSensorDataHandler(bandwidthEstimator, algorithmId, savePath);
    scheduler = TaskScheduler.getInstance(numThreads, sensorDataHandler);
    VehicleMessageHandler vehicleMessageHandler = new VehicleMessageHandlerImpl(scheduler, sensorDataHandler, bandwidthEstimator);
    networkServer = new NonBlockingNetworkServer(port, numClients, vehicleMessageHandler);
    sensorDataHandler.setNetworkServer(networkServer);
  }

  public static void main(String[] args) throws Exception {
    final Config config = new Config();
    JCommander.newBuilder().addObject(config).build().parse(args);
    int port = config.port;
    int numThread = config.numThread;
    int numClient = config.numClient;
    int algorithmId = config.algorithmId;
    String savePath = config.savePath;
    EdgeServer server = new EdgeServer(port, numThread, numClient, algorithmId, savePath);
    server.start();

    CountDownLatch terminateSignal = new CountDownLatch(1);
    Runtime.getRuntime().addShutdownHook(new Thread(terminateSignal::countDown));
    try {
      terminateSignal.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    server.stop();
  }

  public void start() {
    // Start task scheduler to schedule data processing tasks
    taskSchedulerFuture = executor.submit(scheduler);
    // Start network server to receive data from vehicles
    networkServerFuture = executor.submit(networkServer);
    LOGGER.info("Server running on port " + port + ", with " + numThreads + " threads ...");
  }

  public void stop() {
    LOGGER.info("Stopping server ...");
    networkServer.stopRunning();
    scheduler.stopRunning();
    while (!networkServerFuture.isDone() || !taskSchedulerFuture.isDone()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    LOGGER.info("Server stopped.");
  }

  static class Config {
    @Parameter(
        names = {"--port", "-p"},
        description = "Server port number to use"
    )
    public int port = 31904;

    @Parameter(
        names = {"--threads", "-t"},
        description = "Number of threads to use for running pipeline tasks"
    )
    public int numThread = 16;

    @Parameter(
            names = {"--algorithm", "-a"},
            description = "Index of partitioning algorithm to use"
    )
    public int algorithmId = 1;

    @Parameter(
            names = {"--clients", "-c"},
            description = "Number of clients to connect to the edge (test purpose)"
    )
    public int numClient = 1;

    @Parameter(
            names = {"--savepath", "-s"},
            description = "Path to save the merged point cloud for replay"
    )
    public String savePath = null;

    @Parameter(
        names = {"--help", "-h"},
        help = true
    )
    public Boolean help = false;
  }
}
