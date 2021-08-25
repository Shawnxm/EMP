package org.emp.utils;

/**
 * Class to load EMP native library
 */
final class EmpNativeLoader {
  static {
    System.loadLibrary("EmpNative");
  }

  private EmpNativeLoader() {
    throw new UnsupportedOperationException("Instantiation of EmpNativeLoader is not allowed");
  }
}
