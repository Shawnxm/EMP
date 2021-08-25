package org.emp.task;

import java.util.concurrent.Callable;
import org.emp.data.SensorDataChunk;

/**
 * A task that processes sensor data in different part of the pipeline
 */
public abstract class Task implements Callable<TaskResult> {

  private TaskType taskType;

  Task(TaskType taskType) {
    this.taskType = taskType;
  }

  public enum TaskType {
    // Update the current location of a vehicle
    LOCATION_UPDATING,
    // Decode the compressed point cloud from the vehicle
    DECODING,
    // Merge the received point cloud to a central view
    MERGING,
    // Object detection using the point cloud
    OBJECT_DETECTION
  }
}
