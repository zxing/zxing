/*
 * Copyright 2007 ZXing authors
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

/**
 * See ISO 18004:2006 and Annex1 Micro QR of JISX0510:2004
 */
public final class MQRVersion {

  private static final MQRVersion[] VERSIONS = buildVersions();

  private final int versionNumber;
  private final ECBlocks[] ecBlocks;

  private MQRVersion(int versionNumber,
                     ECBlocks... ecBlocks) {
    this.versionNumber = versionNumber;
    this.ecBlocks = ecBlocks;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public int getDimensionForVersion() {
    return 9 + 2 * versionNumber;
  }

  public ECBlocks getECBlocksForLevel(ErrorCorrectionLevel ecLevel) {
    int ps = ecLevel.ordinal();
    if (ecBlocks.length > ps) {
      ps = ecBlocks.length - 1;
    }
    return ecBlocks[ps];
  }

  public boolean isHalfBytesAtLast() {
    return ecBlocks[0].isHalfBytesAtLast();
  }

  public static MQRVersion getVersionForNumber(int versionNumber) {
    if (versionNumber < 1 || versionNumber > 4) {
      throw new IllegalArgumentException();
    }
    return VERSIONS[versionNumber - 1];
  }

  /**
   * Encapsulates a set of error-correction blocks in one symbol version.
   */
  public static final class ECBlocks {
    private final int ecCodewords;
    private final int dataCodewords;
    private final int dataBits;
    private final int typeInfo;

    ECBlocks(int typeInfo, int dataBits, int ecCodewords, int dataCodewords) {
      this.typeInfo = typeInfo;
      this.dataBits = dataBits;
      this.ecCodewords = ecCodewords;
      this.dataCodewords = dataCodewords;
    }

    public int getECCodewords() {
      return ecCodewords;
    }
    
    public int getDataCodewords() {
      return dataCodewords;
    }

    public int getDataBits() {
      return dataBits;
    }

    public int getTypeInfo() {
      return typeInfo;
    }

    public boolean isHalfBytesAtLast() {
      return dataBits % 8 == 4;
    }

  }

  @Override
  public String toString() {
    return String.valueOf(versionNumber);
  }

  /**
   * See Annex1 2.3.8 Table 8 of JISX0510:2004 (p.113)
   */
  private static MQRVersion[] buildVersions() {
    return new MQRVersion[]{
        new MQRVersion(1,
                     new ECBlocks(0,20, 2, 3)), // none
        new MQRVersion(2,
                     new ECBlocks(1,40, 5, 5), // L
                     new ECBlocks(2,32, 6, 4)),// M
        new MQRVersion(3,
                     new ECBlocks(3,84, 6, 11),// L
                     new ECBlocks(4,68, 8,  9)),// M
        new MQRVersion(4,
                     new ECBlocks(5,128,  8, 16),// L
                     new ECBlocks(6,112, 10, 14),// M
                     new ECBlocks(7,80, 14, 10))// Q
    };
  }

}
