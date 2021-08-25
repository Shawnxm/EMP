package org.emp.utils;

import org.junit.jupiter.api.BeforeAll;

public class EmpUnitTest {
  @BeforeAll
  public static void setup() {
    System.setProperty("log4j.configurationFile","log4j2-testConfig.xml");
  }
}
