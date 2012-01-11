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

package com.google.zxing.oned
{
	import com.google.zxing.common.BitMatrix;
	
/**
 * This object renders an EAN13 code as a ByteMatrix 2D array of greyscale
 * values.
 * 
 * @author aripollak@gmail.com (Ari Pollak)
 */
public final class EAN13Writer extends UPCEANWriter {

	import com.google.zxing.common.flexdatatypes.HashTable;
	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
	import com.google.zxing.BarcodeFormat;
	import com.google.zxing.WriterException;
	import com.google.zxing.common.ByteMatrix;

  private static var codeWidth:int = 3 + // start guard
      (7 * 6) + // left bars
      5 + // middle guard
      (7 * 6) + // right bars
      3; // end guard


	public override function encode(contents:String, format:BarcodeFormat=null, width:int=0, height:int=0,  hints:HashTable=null):Object
	{
		if (format == null)
		{
			return (this.encode_simple(contents) as Array);
		}
		else
		{
			return (this.encode_extended(contents,format,width,height,hints) as BitMatrix);
		}
	}
  
  public function encode_extended(contents:String, format:BarcodeFormat, width:int, height:int,  hints:HashTable):Object
  {
    if (format != BarcodeFormat.EAN_13) {
      throw new IllegalArgumentException("oned : EAN13Writer : encode_extended : Can only encode EAN_13, but got " + format);
    }
    
    return super.encode(contents, format, width, height, hints);
  }

  public function encode_simple(contents:String):Array 
  {
    if (contents.length != 13) {
      throw new IllegalArgumentException("oned : EAN13Writer : encode_simple : Requested contents should be 13 digits long, but got " + contents.length);
    }

    var firstDigit:int = parseInt(contents.substring(0, 1));
    var parities:int = EAN13Reader.FIRST_DIGIT_ENCODINGS[firstDigit];
    var result:Array = new Array(codeWidth);
    var pos:int = 0;

    pos += appendPattern(result, pos,UPCEANReader.START_END_PATTERN, 1);

    // See {@link #EAN13Reader} for a description of how the first digit & left bars are encoded
    for (var i:int = 1; i <= 6; i++) {
      var digit:int = parseInt(contents.substring(i, i + 1));
      if ((parities >> (6 - i) & 1) == 1) {
        digit += 10;
      }
      pos += appendPattern(result, pos, UPCEANReader.L_AND_G_PATTERNS[digit], 0);
    }

    pos += appendPattern(result, pos, UPCEANReader.MIDDLE_PATTERN, 0);

    for (var i2:int = 7; i2 <= 12; i2++) {
      var digit2:int = parseInt(contents.substring(i2, i2 + 1));
      pos += appendPattern(result, pos, UPCEANReader.L_PATTERNS[digit2], 1);
    }
    pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, 1);

    return result;
  }

}

}