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

package com.google.zxing.maxicode.decoder
{

	import com.google.zxing.ChecksumException;
	import com.google.zxing.FormatException;
	import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.DecoderResult;
	import com.google.zxing.common.reedsolomon.GenericGFPoly;
	import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
	import com.google.zxing.common.reedsolomon.ReedSolomonException;
	import com.google.zxing.common.flexdatatypes.HashTable;

	/**
	 * <p>The main class which implements MaxiCode decoding -- as opposed to locating and extracting
	 * the MaxiCode from an image.</p>
	 *
	 * @author Manuel Kasten
	 */
	public class Decoder {

	  private static var ALL:int = 0;
	  private static var EVEN:int = 1;
	  private static var ODD:int = 2;

	  private var rsDecoder:ReedSolomonDecoder;

	  public function Decoder() 
	  {
			//rsDecoder = new ReedSolomonDecoder(GF256.MAXICODE_FIELD_64);
	  }

	  public function decode(bits:BitMatrix, hints:HashTable=null):DecoderResult {
		var parser:BitMatrixParser = new BitMatrixParser(bits);
		var codewords:Array = parser.readCodewords();

		correctErrors(codewords, 0, 10, 10, ALL);
		var mode:int = codewords[0] & 0x0F;
		var datawords:Array;
		switch (mode) {
		  case 2:
		  case 3:
		  case 4:
			correctErrors(codewords, 20, 84, 40, EVEN);
			correctErrors(codewords, 20, 84, 40, ODD);
			datawords = new Array(94);
			break;
		  case 5:
			correctErrors(codewords, 20, 68, 56, EVEN);
			correctErrors(codewords, 20, 68, 56, ODD);
			datawords = new Array(78);
			break;
		  default:
			throw FormatException.getFormatInstance();
		}
		
		for (var i:int = 0; i < 10; i++) 
		{
		  datawords[i] = codewords[i];
		}
		for (i = 20; i < datawords.length + 10; i++) 
		{
		  datawords[i - 10] = codewords[i];
		}

		return DecodedBitStreamParser.decode(datawords, mode);
	  }
	  
	  private function correctErrors(codewordBytes:Array,
								 start:int,
								 dataCodewords:int,
								 ecCodewords:int,
								 mode:int):void
	  {
		var codewords:int = dataCodewords + ecCodewords;
		
		// in EVEN or ODD mode only half the codewords
		var divisor:int = mode == ALL ? 1 : 2;

		// First read into an array of ints
		var codewordsInts:Array = new Array(codewords / divisor);
		for (var i:int = 0; i < codewords; i++) 
		{
		  if ((mode == ALL) || (i % 2 == (mode - 1))) {
			codewordsInts[i / divisor] = codewordBytes[i + start] & 0xFF;
		  }
		}
		try 
		{
		  rsDecoder.decode(codewordsInts, ecCodewords / divisor);
		} 
		catch (rse:ReedSolomonException) 
		{
		  throw ChecksumException.getChecksumInstance();
		}
		// Copy back into array of bytes -- only need to worry about the bytes that were data
		// We don't care about errors in the error-correction codewords
		for (i = 0; i < dataCodewords; i++) 
		{
		  if ((mode == ALL) || (i % 2 == (mode - 1))) 
		  {
			codewordBytes[i + start] = (int(codewordsInts[i / divisor]) & 255);
		  }
		}
	  }

	}
}