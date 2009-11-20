package com.google.zxing.client.result
{
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


import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.oned.UPCEReader;

/**
 * Parses strings of digits that repesent a UPC code.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ProductResultParser extends ResultParser {

  public function ProductResultParser() {
  }

  // Treat all UPC and EAN variants as UPCs, in the sense that they are all product barcodes.
  public static function  parse(result:Result):ProductParsedResult {
    var format:BarcodeFormat  = result.getBarcodeFormat();
    if (!((BarcodeFormat.UPC_A == format) || (BarcodeFormat.UPC_E == format) ||
          (BarcodeFormat.EAN_8 == format) || (BarcodeFormat.EAN_13 == format))) {
      return null;
    }
    // Really neither of these should happen:
    var rawText:String = result.getText();
    if (rawText == null) {
      return null;
    }

    var length:int = rawText.length;
    for (var x:int = 0; x < length; x++) {
      var c:int = rawText.charCodeAt(x);//.charAt(x);
      if (c < ('0').charCodeAt(0) || c > ('9').charCodeAt(0)) {
        return null;
      }
    }
    // Not actually checking the checksum again here    

    var normalizedProductID:String;
    // Expand UPC-E for purposes of searching
    if (BarcodeFormat.UPC_E == format) {
      normalizedProductID = UPCEReader.convertUPCEtoUPCA(rawText);
    } else {
      normalizedProductID = rawText;
    }

    return new ProductParsedResult(rawText, normalizedProductID);
  }

}}