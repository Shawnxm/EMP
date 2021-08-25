package org.emp.task;

/**
 * A task to carry out object detection for received sensor data.
 */
public class ObjectDetectionTask extends Task {

  public ObjectDetectionTask() {
    super(TaskType.OBJECT_DETECTION);
  }

  @Override
  public TaskResult call() throws Exception {
    return new ObjectDetectionTaskResult();
  }

  /**
   * TaskResult from a object detection task
   */
  public class ObjectDetectionTaskResult extends TaskResult {

    public ObjectDetectionTaskResult() {
      super(TaskType.OBJECT_DETECTION);
    }

    @Override
    Object getResult() {
      return null;
    }
  }
}
