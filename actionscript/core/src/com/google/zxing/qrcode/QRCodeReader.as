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
package com.google.zxing.qrcode
{
	import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.flexdatatypes.HashTable;
	import com.google.zxing.DecodeHintType;
	import com.google.zxing.Reader;
	import com.google.zxing.ReaderException;
	import com.google.zxing.Result;
	import com.google.zxing.BarcodeFormat;
	import com.google.zxing.ResultPoint;
	import com.google.zxing.ResultMetadataType;
	import com.google.zxing.BinaryBitmap;
	import com.google.zxing.common.DecoderResult;
	import com.google.zxing.common.DetectorResult;
	import com.google.zxing.qrcode.decoder.Decoder;
	import com.google.zxing.qrcode.detector.Detector;

    public class QRCodeReader implements Reader
    { 

          private static var  NO_POINTS:Array = new Array(0);
          private var decoder:Decoder = new Decoder();

		  protected function getDecoder():Decoder 
		  {
		    return decoder;
		  }

          /**
           * Locates and decodes a QR code in an image.
           *
           * @return a String representing the content encoded by the QR code
           * @throws ReaderException if a QR code cannot be found, or cannot be decoded
           */
           
          //public function decode(image:MonochromeBitmapSource):Result 
          //{
          //    try
          //    {
          //      return decode(image, null);
          //    }
          //    catch(e:Exception)
          //    {
          //      throw new ReaderException(e.message);
          //    }
          //  
          //}

          public function decode(image:BinaryBitmap,  hints:HashTable=null):Result{  
                var decoderResult:DecoderResult;
                var points:Array;
                if (hints != null && hints.ContainsKey(DecodeHintType.PURE_BARCODE)) 
                {
                  var bits:BitMatrix = extractPureBits(image.getBlackMatrix());
                  decoderResult = decoder.decode(bits);
                  points = NO_POINTS;
                } 
                else 
                {
                  var detectorResult:DetectorResult = new Detector(image.getBlackMatrix()).detect(hints);
                  decoderResult = decoder.decode(detectorResult.getBits());
                  points = detectorResult.getPoints();
                }

                var result:Result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.QR_CODE);
                if (decoderResult.getByteSegments() != null) {
                  result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, decoderResult.getByteSegments());
                }
                return result;
          }

          /**
           * This method detects a barcode in a "pure" image -- that is, pure monochrome image
           * which contains only an unrotated, unskewed, image of a barcode, with some white border
           * around it. This is a specialized method that works exceptionally fast in this special
           * case.
           */
          private static function extractPureBits(image:BitMatrix):BitMatrix{
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
              throw new ReaderException("QRCodeReader : extractPureBits : borderWidth == minDimension");
            }

            // And then keep tracking across the top-left black module to determine module size
            var moduleEnd:int = borderWidth;
            while (moduleEnd < minDimension && image._get(moduleEnd, moduleEnd)) {
              moduleEnd++;
            }
            if (moduleEnd == minDimension) {
              throw new ReaderException("QRCodeReader : extractPureBits : moduleEnd == minDimension");
            }

            var moduleSize:int = moduleEnd - borderWidth;

            // And now find where the rightmost black module on the first row ends
            var rowEndOfSymbol:int = width - 1;
            while (rowEndOfSymbol >= 0 && !image._get(rowEndOfSymbol, borderWidth)) {
              rowEndOfSymbol--;
            }
            if (rowEndOfSymbol < 0) {
              throw new ReaderException("QRCodeReader : extractPureBits : rowEndOfSymbol < 0");
            }
            rowEndOfSymbol++;

            // Make sure width of barcode is a multiple of module size
            if ((rowEndOfSymbol - borderWidth) % moduleSize != 0) {
              throw new ReaderException("QRCodeReader : extractPureBits : width of barcode is NOT a multiple of module size");
            }
            var dimension:int = (rowEndOfSymbol - borderWidth) / moduleSize;

            // Push in the "border" by half the module width so that we start
            // sampling in the middle of the module. Just in case the image is a
            // little off, this will help recover.
            borderWidth += moduleSize >> 1;

            var sampleDimension:int = borderWidth + (dimension - 1) * moduleSize;
            if (sampleDimension >= width || sampleDimension >= height) {
              throw new ReaderException("QRCodeReader : extractPureBits : sampleDimension("+sampleDimension+") larger than width ("+width+") or heigth ("+height+")");
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