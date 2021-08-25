package org.emp.data;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.emp.network.BandwidthEstimator;
import org.emp.task.DecodingTask;
import org.emp.task.Task;
import org.emp.task.TaskScheduler;
import org.emp.utils.EmpUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class VehicleMessageHandlerImplTest extends EmpUnitTest {
  private static final int BYTE_BUFFER_CAPACITY = 128 * 1024; // 128KB
  SensorDataHandler sensorDataHandler = mock(EmpReapSensorDataHandler.class);
  TaskScheduler taskScheduler = mock(TaskScheduler.class);
  BandwidthEstimator bandwidthEstimator = mock(BandwidthEstimator.class);

  @Test
  public void testPartialMessageWithPartialHeader() {
    VehicleMessageHandlerImpl handler = prepareVehicleDataHandler(
        new int[]{10}, new int[]{-1}, new int[]{-1}, new int[]{-1}, new int[]{-1},
        new int[]{-1}
    );
    verify(taskScheduler, never()).submit(any());
    assertThat(handler.getChunkMap()).isEmpty();
  }

  @Test
  public void testPartialMessageWithCompleteHeader() {
    VehicleMessageHandlerImpl handler = prepareVehicleDataHandler(
        new int[]{100}, new int[]{0}, new int[]{1}, new int[]{2}, new int[]{80},
        new int[]{70}
    );
    verify(taskScheduler, never()).submit(any());
    SensorDataChunk expectedSensorDataChunk = SensorDataChunk.builder()
        .vehicleId(0).frameId(1).chunkId(2).build();
    assertThat(handler.getChunkMap())
        .isEqualTo(Collections.singletonMap(0, expectedSensorDataChunk));
  }

  @Test
  public void testCompleteMessage() {
    VehicleMessageHandlerImpl handler = prepareVehicleDataHandler(
        new int[]{100}, new int[]{0}, new int[]{1}, new int[]{2}, new int[]{80},
        new int[]{100}
    );
    ArgumentCaptor<Task> submittedTask = ArgumentCaptor.forClass(Task.class);
    verify(taskScheduler, times(1)).submit(submittedTask.capture());
    assertThat(submittedTask.getValue()).isInstanceOf(DecodingTask.class);
    assertThat(handler.getChunkMap()).isEmpty();
  }

  @Test
  public void testCompleteMessageAndPartialMessageWithPartialHeader() {
    VehicleMessageHandlerImpl handler = prepareVehicleDataHandler(
        new int[]{100, 200}, new int[]{0, 0}, new int[]{1, 2}, new int[]{2, -1},
        new int[]{80, 80}, new int[]{100, 120}
    );
    ArgumentCaptor<Task> submittedTask = ArgumentCaptor.forClass(Task.class);
    verify(taskScheduler, times(1)).submit(submittedTask.capture());
    assertThat(submittedTask.getValue()).isInstanceOf(DecodingTask.class);
    assertThat(handler.getChunkMap()).isEmpty();
  }

  @Test
  public void testCompleteMessageAndPartialMessageWithCompleteHeader() {
    VehicleMessageHandlerImpl handler = prepareVehicleDataHandler(
        new int[]{100, 200}, new int[]{0, 0}, new int[]{1, 2}, new int[]{2, 3},
        new int[]{80, 80}, new int[]{100, 120}
    );
    ArgumentCaptor<Task> submittedTask = ArgumentCaptor.forClass(Task.class);
    verify(taskScheduler, times(1)).submit(submittedTask.capture());
    assertThat(submittedTask.getValue()).isInstanceOf(DecodingTask.class);
    SensorDataChunk expectedSensorDataChunk = SensorDataChunk.builder()
        .vehicleId(0).frameId(2).chunkId(3).build();
    assertThat(handler.getChunkMap())
        .isEqualTo(Collections.singletonMap(0, expectedSensorDataChunk));
  }

  @Test
  public void testTwoCompleteMessagesAndPartialMessageWithCompleteHeader() {
    VehicleMessageHandlerImpl handler = prepareVehicleDataHandler(
        new int[]{100, 200, 300}, new int[]{0, 0, 0}, new int[]{1, 2, 3},
        new int[]{2, 3, 4}, new int[]{80, 80, 80}, new int[]{100, 200, 230}
    );
    ArgumentCaptor<Task> submittedTask = ArgumentCaptor.forClass(Task.class);
    verify(taskScheduler, times(2)).submit(submittedTask.capture());
    submittedTask.getAllValues().forEach(e -> assertThat(e).isInstanceOf(DecodingTask.class));
    SensorDataChunk expectedSensorDataChunk = SensorDataChunk.builder()
        .vehicleId(0).frameId(3).chunkId(4).build();
    assertThat(handler.getChunkMap())
        .isEqualTo(Collections.singletonMap(0, expectedSensorDataChunk));
  }

  @Test
  public void testTwoPartialMessagesFromDifferentVehicles() {
    VehicleMessageHandlerImpl handler = new VehicleMessageHandlerImpl(taskScheduler, sensorDataHandler, bandwidthEstimator);
    ByteBuffer buffer1 = ByteBuffer.allocate(BYTE_BUFFER_CAPACITY);
    ByteBuffer buffer2 = ByteBuffer.allocate(BYTE_BUFFER_CAPACITY);
    addTestData(buffer1, 100, 0, 1, 2, 80, 80);
    addTestData(buffer2, 200, 1, 2, 3, 80, 120);
    handler.handle(buffer1);
    handler.handle(buffer2);

    verify(taskScheduler, never()).submit(any());
    SensorDataChunk expectedSensorDataChunk1 = SensorDataChunk.builder()
        .vehicleId(0).frameId(1).chunkId(2).build();
    SensorDataChunk expectedSensorDataChunk2 = SensorDataChunk.builder()
        .vehicleId(1).frameId(2).chunkId(3).build();
    Map<Integer, SensorDataChunk> expectedMap = new HashMap<>();
    expectedMap.put(0, expectedSensorDataChunk1);
    expectedMap.put(1, expectedSensorDataChunk2);
    assertThat(handler.getChunkMap()).isEqualTo(expectedMap);
  }

  private VehicleMessageHandlerImpl prepareVehicleDataHandler(
      int[] sizes, int[] vehicleIds, int[] frameIds, int[] chunkIds, int[] types,
      int[] payloadSizes) {
    ByteBuffer buffer = ByteBuffer.allocate(BYTE_BUFFER_CAPACITY);
    for (int i = 0; i < sizes.length; i++) {
      addTestData(
          buffer, sizes[i], vehicleIds[i], frameIds[i], chunkIds[i],types[i], payloadSizes[i]);
    }
    VehicleMessageHandlerImpl handler = new VehicleMessageHandlerImpl(taskScheduler, sensorDataHandler, bandwidthEstimator);
    handler.handle(buffer);
    return handler;
  }

  private void addTestData(
      ByteBuffer byteBuffer, int size, int vehicleId, int frameId, int chunkId, int type,
      int payloadSize) {
    if (size < 0) {
      return;
    }
    byteBuffer.putInt(size);
    if (vehicleId < 0) {
      return;
    }
    byteBuffer.putShort((short) vehicleId);
    if (frameId < 0) {
      return;
    }
    byteBuffer.putShort((short) frameId);
    if (chunkId < 0) {
      return;
    }
    byteBuffer.putShort((short) chunkId);
    if (type < 0) {
      return;
    }
    byteBuffer.put((byte) type);
    byteBuffer.put(new byte[payloadSize]);
  }
}
