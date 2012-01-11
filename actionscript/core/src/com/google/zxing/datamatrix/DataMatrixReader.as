/*
 * Copyright 2007 ZXing authors
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
package com.google.zxing.datamatrix
{
	import com.google.zxing.BarcodeFormat;
	import com.google.zxing.DecodeHintType;
	import com.google.zxing.BinaryBitmap;
	import com.google.zxing.Reader;
	import com.google.zxing.ReaderException;
	import com.google.zxing.Result;
	import com.google.zxing.ResultPoint;
	import com.google.zxing.ResultMetadataType;
	import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.DecoderResult;
	import com.google.zxing.common.DetectorResult;
	import com.google.zxing.datamatrix.decoder.Decoder;
	import com.google.zxing.datamatrix.detector.Detector;
	import com.google.zxing.common.flexdatatypes.HashTable;
	import com.google.zxing.NotFoundException;

	 

  
	 public class DataMatrixReader implements Reader
    {
    

  	public function reset():void {
    // do nothing
  }
    /**
 * This implementation can detect and decode Data Matrix codes in an image.
 *
 * @author bbrown@google.com (Brian Brown)
 */
	
    	public function DataMatrixReader()
    	{}
    	
          private static var  NO_POINTS:Array = new Array();
          private var decoder:Decoder = new Decoder();

          /**
           * Locates and decodes a Data Matrix code in an image.
           *
           * @return a String representing the content encoded by the Data Matrix code
           * @throws ReaderException if a Data Matrix code cannot be found, or cannot be decoded
           */
/*          public function decode( image:MonochromeBitmapSource):Result {
            return decode(image, null);
          }
*/

          public function decode( image:BinaryBitmap, hints:HashTable=null):Result
              {
            var decoderResult:DecoderResult ;
            var points:Array;
            if (hints != null && hints.ContainsKey(DecodeHintType.PURE_BARCODE)) 
            {
              var bits:BitMatrix = extractPureBits(image.getBlackMatrix());
              decoderResult = decoder.decode(bits);
              points = NO_POINTS;
            } 
            else 
            {
            	var bm:BitMatrix = image.getBlackMatrix();
                var detectorResult:DetectorResult = new Detector(bm).detect();
              decoderResult = decoder.decode(detectorResult.getBits());
              points = detectorResult.getPoints();
            }
            var result:Result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.DATAMATRIX);
            if (decoderResult.getByteSegments() != null) {
              result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, decoderResult.getByteSegments());
            }
            if (decoderResult.getECLevel() != null) 
            {
		      result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, decoderResult.getECLevel().toString());
    		}

            return result;
          }

          /**
           * This method detects a Data Matrix code in a "pure" image -- that is, pure monochrome image
           * which contains only an unrotated, unskewed, image of a Data Matrix code, with some white border
           * around it. This is a specialized method that works exceptionally fast in this special
           * case.
           */
           private static function extractPureBits(image:BitMatrix):BitMatrix
		   {
				var leftTopBlack:Array = image.getTopLeftOnBit();
				var rightBottomBlack:Array = image.getBottomRightOnBit();
				if (leftTopBlack == null || rightBottomBlack == null) 
				{
					throw NotFoundException.getNotFoundInstance();
				}
				
				var moduleSize:int = moduleSize(leftTopBlack, image);

				var top:int = leftTopBlack[1];
				var bottom:int = rightBottomBlack[1];
				var left:int = leftTopBlack[0];
				var right:int = rightBottomBlack[0];

				var matrixWidth:int = (right - left + 1) / moduleSize;
				var matrixHeight:int = (bottom - top + 1) / moduleSize;
				if (matrixWidth <= 0 || matrixHeight <= 0) 
				{
					throw NotFoundException.getNotFoundInstance();
				}

				// Push in the "border" by half the module width so that we start
				// sampling in the middle of the module. Just in case the image is a
				// little off, this will help recover.
				var nudge:int = moduleSize >> 1;
				top += nudge;
				left += nudge;

				// Now just read off the bits
				var bits:BitMatrix = new BitMatrix(matrixWidth, matrixHeight);
				for (var y:int = 0; y < matrixHeight; y++) 
				{
					var iOffset:int = top + y * moduleSize;
					for (var x:int = 0; x < matrixWidth; x++) 
					{
						if (image._get(left + x * moduleSize, iOffset)) 
						{
							bits._set(x, y);
						}
					}
				}
				
				return bits;
			}

		private static function moduleSize(leftTopBlack:Array, image:BitMatrix):int
		{
			var width:int = image.getWidth();
			var x:int = leftTopBlack[0];
			var y:int = leftTopBlack[1];
			while (x < width && image._get(x, y)) 
			{
				x++;
			}
			if (x == width) 
			{
				throw NotFoundException.getNotFoundInstance();
			}

			var moduleSize:int = x - leftTopBlack[0];
			if (moduleSize == 0) 
			{
				throw NotFoundException.getNotFoundInstance();
			}
			return moduleSize;
		}
    }

}