/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.pdf417.decoder;

/**
 * @author Guenther Grau
 */
final class BarcodeMetadata {

  private final int columnCount;
  private final int errorCorrectionLevel;
  private final int rowCountUpperPart;
  private final int rowCountLowerPart;
  private final int rowCount;

  BarcodeMetadata(int columnCount, int rowCountUpperPart, int rowCountLowerPart, int errorCorrectionLevel) {
    this.columnCount = columnCount;
    this.errorCorrectionLevel = errorCorrectionLevel;
    this.rowCountUpperPart = rowCountUpperPart;
    this.rowCountLowerPart = rowCountLowerPart;
    this.rowCount = rowCountUpperPart + rowCountLowerPart;
  }

  int getColumnCount() {
    return columnCount;
  }

  int getErrorCorrectionLevel() {
    return errorCorrectionLevel;
  }

  int getRowCount() {
    return rowCount;
  }

  int getRowCountUpperPart() {
    return rowCountUpperPart;
  }

  int getRowCountLowerPart() {
    return rowCountLowerPart;
  }

}
