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

	 public class DataMatrixReader implements Reader
    {
    
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
          private static function extractPureBits(image:BitMatrix ):BitMatrix {
            // Now need to determine module size in pixels

            var height:int = image.getHeight();
            var width:int = image.getWidth();
            var minDimension:int = Math.min(height, width);

            // First, skip white border by tracking diagonally from the top left down and to the right:
            var borderWidth:int = 0;
            while (borderWidth < minDimension && !image._get(borderWidth, borderWidth)) {
              borderWidth++;
            }
            if (borderWidth == minDimension) {
              throw new ReaderException("DataMatrixReader : extractPureBits : borderWidth == minDimension");
            }

            // And then keep tracking across the top-left black module to determine module size
            var moduleEnd:int = borderWidth + 1;
            while (moduleEnd < width && image._get(moduleEnd, borderWidth)) {
              moduleEnd++;
            }
            if (moduleEnd == width) {
              throw new ReaderException("DataMatrixReader : extractPureBits : moduleEnd == width");
            }

            var moduleSize:int = moduleEnd - borderWidth;

            // And now find where the bottommost black module on the first column ends
            var columnEndOfSymbol:int = height - 1;
            while (columnEndOfSymbol >= 0 && !image._get(borderWidth, columnEndOfSymbol)) {
    	        columnEndOfSymbol--;
            }
            if (columnEndOfSymbol < 0) {
              throw new ReaderException("DataMatrixReader : extractPureBits : columnEndOfSymbol < 0");
            }
            columnEndOfSymbol++;

            // Make sure width of barcode is a multiple of module size
            if ((columnEndOfSymbol - borderWidth) % moduleSize != 0) {
              throw new ReaderException("DataMatrixReader : extractPureBits : barcode width is not a multiple of module size");
            }
            var dimension:int = (columnEndOfSymbol - borderWidth) / moduleSize;

            // Push in the "border" by half the module width so that we start
            // sampling in the middle of the module. Just in case the image is a
            // little off, this will help recover.
            borderWidth += moduleSize >> 1;

            var sampleDimension:int = borderWidth + (dimension - 1) * moduleSize;
            if (sampleDimension >= width || sampleDimension >= height) {
              throw new ReaderException("DataMatrixReader : extractPureBits : sampleDimension ("+sampleDimension+") is large than width ("+width+") or height ("+height+")");
            }

            // Now just read off the bits
            var bits:BitMatrix = new BitMatrix(dimension);
            for (var i:int = 0; i < dimension; i++) {
              var iOffset:int = borderWidth + i * moduleSize;
              for (var j:int = 0; j < dimension; j++) {
                if (image._get(borderWidth + j * moduleSize, iOffset)) {
                  bits._set(j, i);
                }
              }
            }
            return bits;
          }
    }

}