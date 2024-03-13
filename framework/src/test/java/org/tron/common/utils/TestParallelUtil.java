package org.tron.common.utils;

import org.apache.commons.lang3.StringUtils;

public class TestParallelUtil {

  public static int getWorkerId() {
    String workerid = System.getProperty("org.gradle.test.worker");
    if(StringUtils.isNotEmpty(workerid)) {
      return Integer.parseInt(workerid);
    }
    return 0;
  }
}
