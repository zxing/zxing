/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.qrcode.decoder;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Sean Owen
 */
public final class VersionTestCase extends Assert {

  @Test(expected = IllegalArgumentException.class)
  public void testBadVersion() {
    Version.getVersionForNumber(0);
  }

  @Test
  public void testVersionForNumber() {
    for (int i = 1; i <= 40; i++) {
      checkVersion(Version.getVersionForNumber(i), i, 4 * i + 17);
    }
  }

  private static void checkVersion(Version version, int number, int dimension) {
    assertNotNull(version);
    assertEquals(number, version.getVersionNumber());
    assertNotNull(version.getAlignmentPatternCenters());
    if (number > 1) {
      assertTrue(version.getAlignmentPatternCenters().length > 0);
    }
    assertEquals(dimension, version.getDimensionForVersion());
    assertNotNull(version.getECBlocksForLevel(ErrorCorrectionLevel.H));
    assertNotNull(version.getECBlocksForLevel(ErrorCorrectionLevel.L));
    assertNotNull(version.getECBlocksForLevel(ErrorCorrectionLevel.M));
    assertNotNull(version.getECBlocksForLevel(ErrorCorrectionLevel.Q));
    assertNotNull(version.buildFunctionPattern());
  }

  @Test
  public void testGetProvisionalVersionForDimension() throws Exception {
    for (int i = 1; i <= 40; i++) {
      assertEquals(i, Version.getProvisionalVersionForDimension(4 * i + 17).getVersionNumber());
    }
  }

  @Test
  public void testDecodeVersionInformation() {
    // Spot check
    doTestVersion(7, 0x07C94);
    doTestVersion(12, 0x0C762);
    doTestVersion(17, 0x1145D);
    doTestVersion(22, 0x168C9);
    doTestVersion(27, 0x1B08E);
    doTestVersion(32, 0x209D5);
  }
  
  private static void doTestVersion(int expectedVersion, int mask) {
    Version version = Version.decodeVersionInformation(mask);
    assertNotNull(version);
    assertEquals(expectedVersion, version.getVersionNumber());
  }

}
