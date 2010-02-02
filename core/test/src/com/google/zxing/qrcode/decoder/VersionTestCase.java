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

import junit.framework.TestCase;

/**
 * @author Sean Owen
 */
public final class VersionTestCase extends TestCase {

  public void testVersionForNumber() {
    try {
      Version.getVersionForNumber(0);
      fail("Should have thrown an exception");
    } catch (IllegalArgumentException iae) {
      // good
    }
    for (int i = 1; i <= 40; i++) {
      checkVersion(Version.getVersionForNumber(i), i, 4*i + 17);
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

  public void testGetProvisionalVersionForDimension() throws Exception {
    for (int i = 1; i <= 40; i++) {
      assertEquals(i, Version.getProvisionalVersionForDimension(4*i + 17).getVersionNumber());
    }
  }

  public void testDecodeVersionInformation() {
    // Spot check
    assertEquals(7, Version.decodeVersionInformation(0x07C94).getVersionNumber());
    assertEquals(12, Version.decodeVersionInformation(0x0C762).getVersionNumber());
    assertEquals(17, Version.decodeVersionInformation(0x1145D).getVersionNumber());
    assertEquals(22, Version.decodeVersionInformation(0x168C9).getVersionNumber());
    assertEquals(27, Version.decodeVersionInformation(0x1B08E).getVersionNumber());
    assertEquals(32, Version.decodeVersionInformation(0x209D5).getVersionNumber());    
  }

}