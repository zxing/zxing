/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing.multi.qrcode
{


import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.flexdatatypes.ArrayList;
import com.google.zxing.common.flexdatatypes.HashTable;

import com.google.zxing.multi.MultipleBarcodeReader;
import com.google.zxing.multi.qrcode.detector.MultiDetector;

import com.google.zxing.qrcode.QRCodeReader;

/**
 * This implementation can detect and decode multiple QR Codes in an image.
 *
 * @author Sean Owen
 * @author Hannes Erven
 */
public final class QRCodeMultiReader extends QRCodeReader implements MultipleBarcodeReader {

  private static var EMPTY_RESULT_ARRAY:Array = new Array(0);

  
  public function decodeMultiple(image:BinaryBitmap ,  hints:HashTable=null):Array {
    var results:ArrayList = new ArrayList();
    var detectorResult:Array = new MultiDetector(image.getBlackMatrix()).detectMulti(hints);
    for (var i:int = 0; i < detectorResult.length; i++) {
      try {
        var decoderResult:DecoderResult = getDecoder().decode(detectorResult[i].getBits());
        var points:Array = detectorResult[i].getPoints();
        var result:Result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points,
            BarcodeFormat.QR_CODE);
        if (decoderResult.getByteSegments() != null) {
          result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, decoderResult.getByteSegments());
        }
        if (decoderResult.getECLevel() != null) {
          result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, decoderResult.getECLevel().toString());
        }
        results.addElement(result);
      } catch (re:ReaderException) {
        // ignore and continue 
      }
    }
    if (results.isEmpty()) {
      return EMPTY_RESULT_ARRAY;
    } else {
      var resultArray:Array = new Array(results.size());
      for (var i2:int = 0; i2 < results.size(); i2++) {
        resultArray[i2] = (results.elementAt(i2) as Result);
      }
      return resultArray;
    }
  }


}
}