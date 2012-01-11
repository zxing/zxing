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

package com.google.zxing.oned{

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.flexdatatypes.HashTable;
import com.google.zxing.common.flexdatatypes.IllegalArgumentException;

/**
 * <p>Encapsulates functionality and implementation that is common to UPC and EAN families
 * of one-dimensional barcodes.</p>
 *
 * @author aripollak@gmail.com (Ari Pollak)
 */
public class UPCEANWriter implements Writer 
{


  public function encode(contents:String , format:BarcodeFormat=null, width:int=0, height:int=0, hints:HashTable = null):Object 
  {
    if (contents == null || contents.length == 0) {
      throw new IllegalArgumentException("Found empty contents");
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Requested dimensions are too small: "
          + width + 'x' + height);
    }

    var code:Array = encode(contents) as Array;
    return renderResult(code, width, height);
  }

  /** @return a byte array of horizontal pixels (0 = white, 1 = black) */
  private static function renderResult(code:Array, width:int , height:int ):BitMatrix {
    var inputWidth:int = code.length;
    // Add quiet zone on both sides
    var fullWidth:int = inputWidth + (UPCEANReader.START_END_PATTERN.length << 1);
    var outputWidth:int = Math.max(width, fullWidth);
    var outputHeight:int = Math.max(1, height);

    var multiple:int = int(outputWidth / fullWidth);
    var leftPadding:int = int((outputWidth - (inputWidth * multiple)) / 2);

    var output:BitMatrix = new BitMatrix(outputWidth, outputHeight);
    var outputX:int = leftPadding;
    for (var inputX:int = 0; inputX < inputWidth; inputX++, outputX += multiple) {
      if (code[inputX] == 1) 
      {
        output.setRegion(outputX, 0, multiple, outputHeight);
      }
    }
    return output;
  }


  /**
   * Appends the given pattern to the target array starting at pos.
   *
   * @param startColor
   *          starting color - 0 for white, 1 for black
   * @return the number of elements added to target.
   */
   protected static function appendPattern(target:Array, pos:int, pattern:Array, startColor:int):int 
   {
    if (startColor != 0 && startColor != 1) 
    {
      throw new IllegalArgumentException(
          "startColor must be either 0 or 1, but got: " + startColor);
    }

    var color:int = startColor;
    var numAdded:int = 0;
    for (var i:int = 0; i < pattern.length; i++) {
      for (var j:int = 0; j < pattern[i]; j++) {
        target[pos] = color;
        pos += 1;
        numAdded += 1;
      }
      color ^= 1; // flip color after each segment
    }
    return numAdded;
  }

  
}
}
