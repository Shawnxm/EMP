package org.emp.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.emp.data.SensorDataChunk;
import org.emp.data.SensorDataHandler;
import org.emp.utils.EmpUnitTest;
import org.emp.utils.TestUtils;
import org.junit.jupiter.api.Test;

public class TaskSchedulerTest extends EmpUnitTest {
  private final ExecutorService executor = Executors.newFixedThreadPool(2);

  @Test
  public void testRunTaskScheduler() throws InterruptedException {
    SensorDataHandler sensorDataHandler = TestUtils.getNoopSensorDataHandler();
    TaskScheduler taskScheduler = TaskScheduler.getInstance(8, sensorDataHandler);

    Future future = executor.submit(taskScheduler);
    for (int i = 0; i < 1000; i++) {
      taskScheduler.submit(new DecodingTask(SensorDataChunk.builder().build(), sensorDataHandler));
    }
    taskScheduler.stopRunning();
    while (!future.isDone()) {
      Thread.sleep(10);
    }
  }
}
