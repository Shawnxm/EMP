package org.emp.task;

import org.emp.task.Task.TaskType;

/**
 * The result from executing a {@code Task}, which is used in following tasks.
 */
public abstract class TaskResult {
  TaskType taskType;

  TaskResult(TaskType taskType) {
    this.taskType = taskType;
  }

  TaskType getTaskType() {
    return taskType;
  }

  /**
   * @return  Result data after task finishes.
   */
  abstract Object getResult();
}
