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
package com.google.zxing.pdf417
{



/**
 * This implementation can detect and decode PDF417 codes in an image.
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 */
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.pdf417.decoder.Decoder;
import com.google.zxing.pdf417.detector.Detector;
import com.google.zxing.common.flexdatatypes.HashTable;
import com.google.zxing.NotFoundException;

public final class PDF417Reader implements Reader {


  private static var NO_POINTS:Array = new Array();

  private var decoder:Decoder  = new Decoder();

  /**
   * Locates and decodes a PDF417 code in an image.
   *
   * @return a String representing the content encoded by the PDF417 code
   * @throws ReaderException if a PDF417 code cannot be found, or cannot be decoded
   */
   /*
  public function decode(image:BinaryBitmap ):Result {
    return decode(image, null);
  }
*/

  public function  reset():void {
    // do nothing
  }
  
  public function decode(image:BinaryBitmap ,  hints:HashTable=null):Result 
    {
    var decoderResult:DecoderResult ;
    var points:Array ;
    if (hints != null && hints.ContainsKey(DecodeHintType.PURE_BARCODE)) {
      var bits:BitMatrix  = extractPureBits(image.getBlackMatrix());
      decoderResult = decoder.decode(bits);
      points = NO_POINTS;
    } else {
      var detectorResult:DetectorResult = new Detector(image).detect();
      decoderResult = decoder.decode(detectorResult.getBits());
      points = detectorResult.getPoints();
    }
    return new Result(decoderResult.getText(), decoderResult.getRawBytes(), points,BarcodeFormat.PDF417,0);
  }
  
                        			

  /**
   * This method detects a barcode in a "pure" image -- that is, pure monochrome image
   * which contains only an unrotated, unskewed, image of a barcode, with some white border
   * around it. This is a specialized method that works exceptionally fast in this special
   * case.
   */
  private static function extractPureBits(image:BitMatrix):BitMatrix {

    var leftTopBlack:Array = image.getTopLeftOnBit();
    var rightBottomBlack:Array = image.getBottomRightOnBit();
    if (leftTopBlack == null || rightBottomBlack == null) {
      throw NotFoundException.getNotFoundInstance();
    }

    var moduleSize:int = moduleSize(leftTopBlack, image);

    var top:int = leftTopBlack[1];
    var bottom:int = rightBottomBlack[1];
    var left:int = findPatternStart(leftTopBlack[0], top, image);
    var right:int = findPatternEnd(leftTopBlack[0], top, image);

    var matrixWidth:int = (right - left + 1) / moduleSize;
    var matrixHeight:int = (bottom - top + 1) / moduleSize;
    if (matrixWidth <= 0 || matrixHeight <= 0) {
      throw NotFoundException.getNotFoundInstance();
    }

    // Push in the "border" by half the module width so that we start
    // sampling in the middle of the module. Just in case the image is a
    // little off, this will help recover.
    var nudge:int = moduleSize >> 1;
    top += nudge;
    left += nudge;

    // Now just read off the bits
    var bits:BitMatrix  = new BitMatrix(matrixWidth, matrixHeight);
    for (var y:int = 0; y < matrixHeight; y++) {
      var iOffset:int = top + y * moduleSize;
      for (var x:int = 0; x < matrixWidth; x++) {
        if (image._get(left + x * moduleSize, iOffset)) {
          bits._set(x, y);
        }
      }
    }
    return bits;
  }
  
  private static function moduleSize(leftTopBlack:Array , image:BitMatrix ):int 
  {
    var x:int = leftTopBlack[0];
    var y:int = leftTopBlack[1];
    var width:int = image.getWidth();
    while (x < width && image._get(x, y)) {
      x++;
    }
    if (x == width) {
      throw NotFoundException.getNotFoundInstance();
    }

    var moduleSize:int = (x - leftTopBlack[0]) >>> 3; // We've crossed left first bar, which is 8x
    if (moduleSize == 0) {
      throw NotFoundException.getNotFoundInstance();
    }

    return moduleSize;
  }

  private static function findPatternStart(x:int, y:int, image:BitMatrix):int {
    var width:int = image.getWidth();
    var start:int = x;
    // start should be on black
    var transitions:int = 0;
    var black:Boolean = true;
    while (start < width - 1 && transitions < 8) {
      start++;
      var newBlack:Boolean = image._get(start, y);
      if (black != newBlack) {
        transitions++;
      }
      black = newBlack;
    }
    if (start == width - 1) {
      throw NotFoundException.getNotFoundInstance();
    }
    return start;
  }

  private static function findPatternEnd(x:int, y:int, image:BitMatrix):int {
    var width:int = image.getWidth();
    var end:int = width - 1;
    // end should be on black
    while (end > x && !image._get(end, y)) {
      end--;
    }
    var transitions:int = 0;
    var black:Boolean = true;
    while (end > x && transitions < 9) {
      end--;
      var newBlack:Boolean = image._get(end, y);
      if (black != newBlack) {
        transitions++;
      }
      black = newBlack;
    }
    if (end == x) {
      throw NotFoundException.getNotFoundInstance();
    }
    return end;
  }


  
  
}
}