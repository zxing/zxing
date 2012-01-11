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

package com.google.zxing.maxicode
{

	import com.google.zxing.BarcodeFormat;
	import com.google.zxing.BinaryBitmap;
	import com.google.zxing.ChecksumException;
	import com.google.zxing.DecodeHintType;
	import com.google.zxing.FormatException;
	import com.google.zxing.NotFoundException;
	import com.google.zxing.Reader;
	import com.google.zxing.Result;
	import com.google.zxing.ResultMetadataType;
	import com.google.zxing.ResultPoint;
	import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.DecoderResult;
	import com.google.zxing.maxicode.decoder.Decoder;

	import com.google.zxing.common.flexdatatypes.HashTable;
	import com.google.zxing.NotFoundException;

	/**
	 * This implementation can detect and decode a MaxiCode in an image.
	 */
	public class MaxiCodeReader implements Reader 
	{

	  private static var NO_POINTS:Array = new Array(0);
	  private static var MATRIX_WIDTH:int = 30;
	  private static var MATRIX_HEIGHT:int = 33;

	  private var decoder:Decoder = new Decoder();

	  public function getDecoder():Decoder 
	  {
		return decoder;
	  }

	  /**
	   * Locates and decodes a MaxiCode in an image.
	   *
	   * @return a String representing the content encoded by the MaxiCode
	   * @throws NotFoundException if a MaxiCode cannot be found
	   * @throws FormatException if a MaxiCode cannot be decoded
	   * @throws ChecksumException if error correction fails
	   */
	  public function decode(image:BinaryBitmap, hints:HashTable=null):Result
	  {
		var decoderResult:DecoderResult ;
		if (hints != null && hints.containsKey(DecodeHintType.PURE_BARCODE)) 
		{
		  var bits:BitMatrix = extractPureBits(image.getBlackMatrix());
		  decoderResult = decoder.decode(bits, hints);
		} 
		else 
		{
		  throw NotFoundException.getNotFoundInstance();
		}

		var points:Array = NO_POINTS;
		var result:Result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.MAXICODE);

		if (decoderResult.getECLevel() != null) 
		{
		  result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, decoderResult.getECLevel());
		}
		return result;
	  }

	  public function reset():void 
	  {
		// do nothing
	  }

	  /**
	   * This method detects a code in a "pure" image -- that is, pure monochrome image
	   * which contains only an unrotated, unskewed, image of a code, with some white border
	   * around it. This is a specialized method that works exceptionally fast in this special
	   * case.
	   *
	   * @see com.google.zxing.pdf417.PDF417Reader#extractPureBits(BitMatrix)
	   * @see com.google.zxing.datamatrix.DataMatrixReader#extractPureBits(BitMatrix)
	   * @see com.google.zxing.qrcode.QRCodeReader#extractPureBits(BitMatrix)
	   */
	  private static function extractPureBits(image:BitMatrix):BitMatrix 
	  {
		
		var enclosingRectangle:Array = image.getEnclosingRectangle();
		if (enclosingRectangle == null) 
		{
		  throw NotFoundException.getNotFoundInstance();
		}
		
		var left:int = enclosingRectangle[0];
		var top:int = enclosingRectangle[1];
		var width:int = enclosingRectangle[2];
		var height:int = enclosingRectangle[3];

		// Now just read off the bits
		var bits:BitMatrix = new BitMatrix(MATRIX_WIDTH, MATRIX_HEIGHT);
		for (var y:int = 0; y < MATRIX_HEIGHT; y++) {
		  var iy:int = top + (y * height + height / 2) / MATRIX_HEIGHT;
		  for (var x:int = 0; x < MATRIX_WIDTH; x++) {
			var ix:int = left + (x * width + width / 2 + (y & 0x01) *  width / 2) / MATRIX_WIDTH;
			if (image._get(ix, iy)) {
			  bits._set(x, y);
			}
		  }
		}
		return bits;
	  }

	}
}