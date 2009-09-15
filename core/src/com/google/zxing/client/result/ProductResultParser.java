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

package com.google.zxing.client.result;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.oned.UPCEReader;

/**
 * Parses strings of digits that represent a UPC code.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class ProductResultParser extends ResultParser {

  private ProductResultParser() {
  }

  // Treat all UPC and EAN variants as UPCs, in the sense that they are all product barcodes.
  public static ProductParsedResult parse(Result result) {
    BarcodeFormat format = result.getBarcodeFormat();
    if (!(BarcodeFormat.UPC_A.equals(format) || BarcodeFormat.UPC_E.equals(format) ||
          BarcodeFormat.EAN_8.equals(format) || BarcodeFormat.EAN_13.equals(format))) {
      return null;
    }
    // Really neither of these should happen:
    String rawText = result.getText();
    if (rawText == null) {
      return null;
    }

    int length = rawText.length();
    for (int x = 0; x < length; x++) {
      char c = rawText.charAt(x);
      if (c < '0' || c > '9') {
        return null;
      }
    }
    // Not actually checking the checksum again here    

    String normalizedProductID;
    // Expand UPC-E for purposes of searching
    if (BarcodeFormat.UPC_E.equals(format)) {
      normalizedProductID = UPCEReader.convertUPCEtoUPCA(rawText);
    } else {
      normalizedProductID = rawText;
    }

    return new ProductParsedResult(rawText, normalizedProductID);
  }

}