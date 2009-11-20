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

package com.google.zxing.multi
{
	import com.google.zxing.common.flexdatatypes.ArrayList;
	import com.google.zxing.common.flexdatatypes.HashTable;
	

/**
 * <p>Attempts to locate multiple barcodes in an image by repeatedly decoding portion of the image.
 * After one barcode is found, the areas left, above, right and below the barcode's
 * {@link com.google.zxing.ResultPoint}s are scanned, recursively.</p>
 *
 * <p>A caller may want to also employ {@link ByQuadrantReader} when attempting to find multiple
 * 2D barcodes, like QR Codes, in an image, where the presence of multiple barcodes might prevent
 * detecting any one of them.</p>
 *
 * <p>That is, instead of passing a {@link Reader} a caller might pass
 * <code>new ByQuadrantReader(reader)</code>.</p>
 *
 * @author Sean Owen
 */
public final class GenericMultipleBarcodeReader implements MultipleBarcodeReader {

import Reader;
import Result;
import BinaryBitmap;
import ReaderException;
import ResultPoint;
import common.flexdatatypes.HashTable;
import common.flexdatatypes.ArrayList;



  private static var  MIN_DIMENSION_TO_RECUR:int = 30;

  private var  delegate:Reader;

  public function GenericMultipleBarcodeReader(delegate:Reader ) {
    this.delegate = delegate;
  }


  public function decodeMultiple(image:BinaryBitmap , hints:HashTable=null):Array
  {
    var results:ArrayList  = new ArrayList();
    doDecodeMultiple(image, hints, results, 0, 0);
    if (results.isEmpty()) {
      throw new ReaderException("multi : GenericMultipleBarcodeReader : decodeMultiple");
    }
    var numResults:int = results.size();
    var resultArray:Array = new Array(numResults);
    for (var i:int = 0; i < numResults; i++) {
      resultArray[i] = (results.elementAt(i) as Result);
    }
    return resultArray;
  }

  private function doDecodeMultiple(image:BinaryBitmap,
                                		hints:HashTable ,
                                		results:ArrayList,
                                		xOffset:int,
                                		yOffset:int ):void {
    var result:Result;
    try {
      result = delegate.decode(image, hints);
    } catch (re:ReaderException) {
      return;
    }
    var alreadyFound:Boolean = false;
    for (var i:int = 0; i < results.size(); i++) {
      var existingResult:Result  = (results.elementAt(i) as Result);
      if (existingResult.getText() == result.getText()) {
        alreadyFound = true;
        break;
      }
    }
    if (alreadyFound) {
      return;
    }
    results.addElement(translateResultPoints(result, xOffset, yOffset));
    var  resultPoints:Array = result.getResultPoints();
    if (resultPoints == null || resultPoints.length == 0) {
      return;
    }
    var width:int = image.getWidth();
    var height:int = image.getHeight();
    var minX:Number = width;
    var minY:Number = height;
    var maxX:Number = 0.0;
    var maxY:Number = 0.0;
    for (var i2:int = 0; i2 < resultPoints.length; i2++) {
      var point:ResultPoint = resultPoints[i2];
      var x:Number = point.getX();
      var y:Number = point.getY();
      if (x < minX) {
        minX = x;
      }
      if (y < minY) {
        minY = y;
      }
      if (x > maxX) {
        maxX = x;
      }
      if (y > maxY) {
        maxY = y;
      }
    }

    if (minX > MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, 0, int(minX), height), hints, results, 0, 0);
    }
    if (minY > MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, 0, width, int(minY)), hints, results, 0, 0);
    }
    if (maxX < width - MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(int(maxX), 0, width, height), hints, results, int(maxX), 0);
    }
    if (maxY < height - MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, int(maxY), width, height), hints, results, 0, int(maxY));
    }
  }

  private static function translateResultPoints(result:Result , xOffset:int , yOffset:int ):Result {
    var oldResultPoints:Array  = result.getResultPoints();
    var newResultPoints:Array = new Array(oldResultPoints.length);
    for (var i:int = 0; i < oldResultPoints.length; i++) {
      var oldPoint:ResultPoint = oldResultPoints[i];
      newResultPoints[i] = new ResultPoint(oldPoint.getX() + xOffset, oldPoint.getY() + yOffset);
    }
    return new Result(result.getText(), result.getRawBytes(), newResultPoints,
        result.getBarcodeFormat());
	}
}
}