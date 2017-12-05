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

package com.google.zxing.qrcode.encoder;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.MQRVersion;

/**
 * A representation class for Micro QR
 */
public final class MQRCode {

  public static final int NUM_MASK_PATTERNS = 4;

  private Mode mode;
  private ErrorCorrectionLevel ecLevel;
  private MQRVersion version;
  private int maskPattern;
  private ByteMatrix matrix;

  public MQRCode() {
    maskPattern = -1;
  }

  public Mode getMode() {
    return mode;
  }

  public ErrorCorrectionLevel getECLevel() {
    return ecLevel;
  }

  public MQRVersion getVersion() {
    return version;
  }

  public int getMaskPattern() {
    return maskPattern;
  }

  public ByteMatrix getMatrix() {
    return matrix;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(200);
    result.append("<<\n");
    result.append(" mode: ");
    result.append(mode);
    result.append("\n ecLevel: ");
    result.append(ecLevel);
    result.append("\n version: ");
    result.append(version);
    result.append("\n maskPattern: ");
    result.append(maskPattern);
    if (matrix == null) {
      result.append("\n matrix: null\n");
    } else {
      result.append("\n matrix:\n");
      result.append(matrix);
    }
    result.append(">>\n");
    return result.toString();
  }

  public void setMode(Mode value) {
    mode = value;
  }

  public void setECLevel(ErrorCorrectionLevel value) {
    ecLevel = value;
  }

  public void setVersion(MQRVersion version) {
    this.version = version;
  }

  public void setMaskPattern(int value) {
    maskPattern = value;
  }

  public void setMatrix(ByteMatrix value) {
    matrix = value;
  }

  // Check if "mask_pattern" is valid.
  public static boolean isValidMaskPattern(int maskPattern) {
    return maskPattern >= 0 && maskPattern < NUM_MASK_PATTERNS;
  }

}
