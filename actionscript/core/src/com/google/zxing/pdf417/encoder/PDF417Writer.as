/*
 * Copyright 2011 ZXing authors
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

package com.google.zxing.pdf417.encoder{

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.flexdatatypes.HashTable;
import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
/**
 * @author Jacob Haynes
 */
public class PDF417Writer implements Writer 
{

 public function encode(contents:String,  format:BarcodeFormat=null, width:int=0, height:int=0, hints:HashTable=null):Object
 {
    if (format != BarcodeFormat.PDF417) {
      throw new IllegalArgumentException("Can only encode PDF_417, but got " + format);
    }

    var encoder:PDF417  = new PDF417();

    //No error correction at the moment
    var errorCorrectionLevel:int = 3;
    encoder.generateBarcodeLogic(contents, errorCorrectionLevel);

    // Give it data to be encoded
    //encoderExt.setData(content.getBytes());
    // Find the Error correction level automatically

    //encoderExt.encode();
    //encoderExt.createArray();
    var lineThickness:int = 3;
    var aspectRatio:int = 8;
    var originalScale:Array = encoder.getBarcodeMatrix().getScaledMatrix(lineThickness, aspectRatio * lineThickness);
  
    var rotated:Boolean = false;
    var val1:Boolean = (height > width)?true:false;
    var val2:Boolean = (originalScale[0].length < originalScale.length)?true:false;
    if ((val1 && !val2) || (!val1 && val2)) 
    {
      originalScale = rotateArray(originalScale);
      rotated = true;
    }

    var scaleX:int = width / originalScale[0].length;
    var scaleY:int = height / originalScale.length;

    var scale:int;
    if (scaleX < scaleY) {
      scale = scaleX;
    } else {
      scale = scaleY;
    }

    if (scale > 1) {
      var scaledMatrix:Array =
          encoder.getBarcodeMatrix().getScaledMatrix(scale * lineThickness, scale * aspectRatio * lineThickness);
      if (rotated) {
        scaledMatrix = rotateArray(scaledMatrix);
      }
      return bitMatrixFrombitArray(scaledMatrix);
    }
    return bitMatrixFrombitArray(originalScale);
  }

  /**
   * This takes an array holding the values of the PDF 417
   *
   * @param input a byte array of information with 0 is black, and 1 is white
   * @return BitMatrix of the input
   */
  private static function bitMatrixFrombitArray(input:Array):BitMatrix {
    //Creates a small whitespace boarder around the barcode
    var whiteSpace:int = 30;

    //Creates the bitmatrix with extra space for whitespace
    var output:BitMatrix  = new BitMatrix(input.length + 2 * whiteSpace, input[0].length + 2 * whiteSpace);
    output.clear();
    for (var ii:int = 0; ii < input.length; ii++) {
      for (var jj:int = 0; jj < input[0].length; jj++) {
        // Zero is white in the bytematrix
        if (input[ii][jj] == 1) {
          output._set(ii + whiteSpace, jj + whiteSpace);
        }
      }
    }
    var result:String = output.toString2();
    return output;
  }

  /**
   * Takes and rotates the it 90 degrees
   */
  private static function rotateArray(bitarray:Array):Array {
    var temp:Array = new Array();//byte[bitarray[0].length][bitarray.length];
    for (var ii:int = 0; ii < bitarray.length; ii++) {
      // This makes the direction consistent on screen when rotating the
      // screen;
      var inverseii:int = bitarray.length - ii - 1;
      for (var jj:int = 0; jj < bitarray[0].length; jj++) {
        temp[jj][inverseii] = bitarray[ii][jj];
      }
    }
    return temp;
  }
}
}
