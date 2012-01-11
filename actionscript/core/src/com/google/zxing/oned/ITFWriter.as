/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.oned
{

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.flexdatatypes.HashTable;
import com.google.zxing.common.flexdatatypes.IllegalArgumentException;

/**
 * This object renders a ITF code as a {@link BitMatrix}.
 * 
 * @author erik.barbara@gmail.com (Erik Barbara)
 */
public class ITFWriter extends UPCEANWriter 
{

  public override function encode(contents:String ,
                          format:BarcodeFormat=null ,
                          width:int=0,
                          height:int=0,
                          hints:HashTable=null ) : Object 
  {
  	
    if (format != null)
    {
    if (format != BarcodeFormat.ITF) {
      throw new IllegalArgumentException("Can only encode ITF, but got " + format);
    }

    return super.encode(contents, format, width, height, hints);
    }

  //public byte[] encode(String contents) {
    var length:int = contents.length;
    if (length > 80) {
      throw new IllegalArgumentException(
          "Requested contents should be less than 80 digits long, but got " + length);
    }
    var result:Array = new Array(9 + 9 * length);
    var start:Array = [1, 1, 1, 1];
    var pos:int = appendPattern(result, 0, start, 1);
    for (var i:int = 0; i < length; i += 2) {
      var one:int = contents.charCodeAt(i) - 48 ;
      var two:int = contents.charCodeAt(i+1) - 48;
      var encoding:Array = new Array(18);
      for (var j:int = 0; j < 5; j++) {
        encoding[(j << 1)] = ITFReader.PATTERNS[one][j];
        encoding[(j << 1) + 1] = ITFReader.PATTERNS[two][j];
      }
      pos += appendPattern(result, pos, encoding, 1);
    }
    var end:Array = [3, 1, 1];
    pos += appendPattern(result, pos, end, 1);

    return result;
  }
}
}