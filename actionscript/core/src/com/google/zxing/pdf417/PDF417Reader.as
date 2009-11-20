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

  public function decode(image:BinaryBitmap ,  hints:HashTable=null):Result 
    {
    var decoderResult:DecoderResult ;
    var points:Array ;
    if (hints != null && hints.ContainsKey(DecodeHintType.PURE_BARCODE)) {
      var bits:BitMatrix  = extractPureBits(image);
      decoderResult = decoder.decode(bits);
      points = NO_POINTS;
    } else {
      var detectorResult:DetectorResult = new Detector(image).detect();
      decoderResult = decoder.decode(detectorResult.getBits());
      points = detectorResult.getPoints();
    }
    return new Result(decoderResult.getText(), decoderResult.getRawBytes(), points,
        BarcodeFormat.PDF417);
  }

  /**
   * This method detects a barcode in a "pure" image -- that is, pure monochrome image
   * which contains only an unrotated, unskewed, image of a barcode, with some white border
   * around it. This is a specialized method that works exceptionally fast in this special
   * case.
   */
  private static function extractPureBits(image:BinaryBitmap ):BitMatrix {
    // Now need to determine module size in pixels
    var matrix:BitMatrix  = image.getBlackMatrix();
    var height:int = matrix.getHeight();
    var width:int = matrix.getWidth();
    var minDimension:int = Math.min(height, width);

    // First, skip white border by tracking diagonally from the top left down and to the right:
    var borderWidth:int = 0;
    while (borderWidth < minDimension && !matrix._get(borderWidth, borderWidth)) {
      borderWidth++;
    }
    if (borderWidth == minDimension) {
      throw new ReaderException("PDF417Reader : extractPureBits");
    }

    // And then keep tracking across the top-left black module to determine module size
    var moduleEnd:int = borderWidth;
    while (moduleEnd < minDimension && matrix._get(moduleEnd, moduleEnd)) {
      moduleEnd++;
    }
    if (moduleEnd == minDimension) {
      throw new ReaderException("PDF417Reader : extractPureBits");
    }

    var moduleSize:int = moduleEnd - borderWidth;

    // And now find where the rightmost black module on the first row ends
    var rowEndOfSymbol:int = width - 1;
    while (rowEndOfSymbol >= 0 && !matrix._get(rowEndOfSymbol, borderWidth)) {
      rowEndOfSymbol--;
    }
    if (rowEndOfSymbol < 0) {
      throw new ReaderException("PDF417Reader : extractPureBits");
    }
    rowEndOfSymbol++;

    // Make sure width of barcode is a multiple of module size
    if ((rowEndOfSymbol - borderWidth) % moduleSize != 0) {
      throw new ReaderException("PDF417Reader : extractPureBits");
    }
    var dimension:int = (rowEndOfSymbol - borderWidth) / moduleSize;

    // Push in the "border" by half the module width so that we start
    // sampling in the middle of the module. Just in case the image is a
    // little off, this will help recover.
    borderWidth += moduleSize >> 1;

    var sampleDimension:int = borderWidth + (dimension - 1) * moduleSize;
    if (sampleDimension >= width || sampleDimension >= height) {
      throw new ReaderException("PDF417Reader : extractPureBits");
    }

    // Now just read off the bits
    var bits:BitMatrix = new BitMatrix(dimension);
    for (var y:int = 0; y < dimension; y++) {
      var iOffset:int = borderWidth + y * moduleSize;
      for (var x:int = 0; x < dimension; x++) {
        if (matrix._get(borderWidth + x * moduleSize, iOffset)) {
          bits._set(x, y);
        }
      }
    }
    return bits;
  }
}
}