/*
 * Copyright 2008 ZXing authors
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
   	import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
   	import com.google.zxing.common.flexdatatypes.HashTable;
   	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
   	import com.google.zxing.common.ByteMatrix;
  	import com.google.zxing.qrcode.encoder.QRCode;
   	import com.google.zxing.qrcode.encoder.Encoder;
   	import com.google.zxing.BarcodeFormat;
	import com.google.zxing.EncodeHintType;
	import com.google.zxing.Writer;
	import com.google.zxing.WriterException;
	
    public class QRCodeWriter implements Writer
    { 

    	
          private static  var QUIET_ZONE_SIZE:int = 4;
          public function encode(contents:String, format:BarcodeFormat=null, width:int=0,height:int=0, hints:HashTable=null):Object
          {
			
            if (contents == null || contents.length == 0) {
              throw new IllegalArgumentException("Found empty contents");
            }

            if (format != BarcodeFormat.QR_CODE) {
              throw new IllegalArgumentException("Can only encode QR_CODE, but got " + format);
            }

            if (width < 0 || height < 0) {
              throw new IllegalArgumentException("Requested dimensions are too small: " + width + 'x' +
                  height);
            }

            var errorCorrectionLevel:ErrorCorrectionLevel = ErrorCorrectionLevel.L;
            if (hints != null) 
            {
              var requestedECLevel:ErrorCorrectionLevel = (hints.getValueByKey(EncodeHintType.ERROR_CORRECTION) as ErrorCorrectionLevel);
              if (requestedECLevel != null) 
              {
                errorCorrectionLevel = requestedECLevel;
              }
            }

            var code:QRCode = new QRCode();
            Encoder.encode(contents, errorCorrectionLevel, code);
            return renderResult(code, width, height);
          }

          // Note that the input matrix uses 0 == white, 1 == black, while the output matrix uses
          // 0 == black, 255 == white (i.e. an 8 bit greyscale bitmap).
          private static function renderResult( code:QRCode, width:int, height:int):ByteMatrix {
            var input:ByteMatrix  = code.getMatrix();
            var inputWidth:int = input.width();
            var inputHeight:int = input.height();
            var qrWidth:int = inputWidth + (QUIET_ZONE_SIZE << 1);
            var qrHeight:int = inputHeight + (QUIET_ZONE_SIZE << 1);
            var outputWidth:int = Math.max(width, qrWidth);
            var outputHeight:int = Math.max(height, qrHeight);

            var multiple:int = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
            // Padding includes both the quiet zone and the extra white pixels to accomodate the requested
            // dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
            // If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
            // handle all the padding from 100x100 (the actual QR) up to 200x160.
            var leftPadding:int = (outputWidth - (inputWidth * multiple)) / 2;
            var topPadding:int = (outputHeight - (inputHeight * multiple)) / 2;

            var output:ByteMatrix = new ByteMatrix(outputHeight, outputWidth);
            var outputArray:Array = output.getArray(); //sbyte[][]

            // We could be tricky and use the first row in each set of multiple as the temporary storage,
            // instead of allocating this separate array.
            var row:Array = new Array(outputWidth);

            // 1. Write the white lines at the top
            for (var y:int = 0; y < topPadding; y++) {
              setRowColor(outputArray[y], 255);
            }

            // 2. Expand the QR image to the multiple
            var inputArray:Array = input.getArray();
            for (var y2:int = 0; y2 < inputHeight; y2++) {
              // a. Write the white pixels at the left of each row
              for (var x2:int = 0; x2 < leftPadding; x2++) {
                row[x2] = 255;
              }

              // b. Write the contents of this row of the barcode
              var offset:int = leftPadding;
              for (var x3:int = 0; x3 < inputWidth; x3++) {
                var value:int = (inputArray[y2][x3] == 1) ? 0 : 255;
                for (var z:int = 0; z < multiple; z++) {
                  row[offset + z] = value;
                }
                offset += multiple;
              }

              // c. Write the white pixels at the right of each row
              offset = leftPadding + (inputWidth * multiple);
              for (var x4:int = offset; x4 < outputWidth; x4++) {
                row[x4] = 255;
              }

              // d. Write the completed row multiple times
              offset = topPadding + (y2 * multiple);
              for (var z2:int = 0; z2 < multiple; z2++) 
              {
              	//System.Array.Copy(row, 0, outputArray[offset + z], 0, outputWidth);
              	for (var ii:int=0;ii<outputWidth;ii++)
              	{
              		outputArray[offset + z2][ii] = row[ii];
              	}
                
              }
            }

            // 3. Write the white lines at the bottom
            var offset2:int = topPadding + (inputHeight * multiple);
            for (var y3:int = offset2; y3 < outputHeight; y3++)
            {
              setRowColor(outputArray[y3], 255);
            }
            return output;
          }

          private static function setRowColor(row:Array, value:int):void 
          {
            for (var x:int = 0; x < row.length; x++) 
            {
              row[x] = value;
            }
          }
    
    }
}