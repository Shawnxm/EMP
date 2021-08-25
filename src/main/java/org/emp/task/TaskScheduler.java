package org.emp.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.data.SensorDataFrame;
import org.emp.data.SensorDataHandler;

/**
 * A task scheduler to chain different tasks together into a processing pipeline
 */
public class TaskScheduler implements Runnable {
  private static final Logger LOGGER = LogManager.getLogger(TaskScheduler.class);

  // Singleton TaskScheduler instance
  private static TaskScheduler instance;
  // ExecutorService instance to run different Task
  private final ExecutorService executor;
  private final List<Future<TaskResult>> listFutures;
  private final SensorDataHandler sensorDataHandler;
  // Should the task scheduler run.  When false, the scheduler stops and exits.
  private boolean shouldRun;

  /**
   * Constructs {@code TaskScheduler} instance.
   * @param numThreads  Number of threads to run concurrently.
   * @param sensorDataHandler  Sensor data handler.
   */
  private TaskScheduler(int numThreads, SensorDataHandler sensorDataHandler) {
    executor = Executors.newFixedThreadPool(numThreads);
    listFutures = new LinkedList<>();
    shouldRun = true;
    this.sensorDataHandler = sensorDataHandler;
  }

  /**
   * @param numThreads  Number of threads to run concurrently for tasks.
   * @return  The singleton {@code TaskScheduler} instance.
   */
  public static TaskScheduler getInstance(
      int numThreads, SensorDataHandler sensorDataHandler) {
    if (instance == null) {
      instance = new TaskScheduler(numThreads, sensorDataHandler);
    }
    return instance;
  }

  /**
   * Submits a task for execution.
   * @param task  The {@code Task} instance to execute.
   */
  public void submit(Task task) {
    Future<TaskResult> future = executor.submit(task);
    synchronized (listFutures) {
      listFutures.add(future);
    }
  }

  @Override
  public void run() {
    while (shouldRun || listFutures.size() > 0) {
      List<Task> newTasks = new ArrayList<>();

      synchronized (listFutures) {
        ListIterator<Future<TaskResult>> iterator = listFutures.listIterator();
        while (iterator.hasNext()) {
          Future<TaskResult> future = iterator.next();
          if (future.isDone()) {
            try {
              TaskResult taskResult = future.get();
              if (taskResult != null) {
                LOGGER.debug("Finished task type: " + taskResult.getTaskType());
                switch (taskResult.getTaskType()) {
                  case DECODING:
                    newTasks.add(new MergingTask(taskResult.getResult(), sensorDataHandler));
                    break;
                  case MERGING:
                    if (sensorDataHandler.shouldRunObjectDetection()) {
                      newTasks.add(new ObjectDetectionTask());
                    }
                    break;
                  case OBJECT_DETECTION:
                    break;
                  default:
                }
              }
            } catch (InterruptedException | ExecutionException | IOException e) {
              e.printStackTrace();
            }

            // Remove the {@code TaskResult} instance for a finished task
            iterator.remove();
            break;
          }
        }
      }

      for (Task newTask : newTasks) {
        submit(newTask);
      }
    }
  }

  public void stopRunning() {
    shouldRun = false;
  }
}
