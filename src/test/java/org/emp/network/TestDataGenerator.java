package org.emp.network;

import static com.google.common.truth.Truth.assertThat;

import java.nio.ByteBuffer;

/**
 * Generates continuous ASCII chars as the byte array.
 *
 * For example:
 * - "012346789:; ... MNOPQ"
 * - "012346789:; ... XYZ01234 ... RST"
 */
class TestDataGenerator {
  private static final char START_INCLUSIVE = '0';
  private static final char END_INCLUSIVE = 'Z';
  private final int id;

  public TestDataGenerator(int id) {
    this.id = id;
  }

  public byte[] generate(int numBytes) {
    byte[] data = new byte[numBytes];
    for (int i = 0; i < numBytes; i++) {
      data[i] = getByte(i);
    }
    return data;
  }

  public void verify(ByteBuffer byteBuffer, int length) {
    for (int i = 0; i < byteBuffer.position(); i++) {
      assertThat(byteBuffer.get(i)).isEqualTo(getByte(i, length));
    }
  }

  public void verify(ByteBuffer byteBuffer) {
    verify(byteBuffer, byteBuffer.position());
  }

  private byte getByte(int i) {
    return getByte(i, Integer.MAX_VALUE);
  }

  private byte getByte(int i, int length) {
    if (i % length == 0) {
      return (byte) id;
    }
    return (byte)(START_INCLUSIVE + (i % length) % (END_INCLUSIVE - START_INCLUSIVE + 1));
  }
}
