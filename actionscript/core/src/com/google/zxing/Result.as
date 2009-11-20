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
package com.google.zxing
{
	/**
 * <p>Encapsulates the result of decoding a barcode within an image.</p>
 *
 * @author Sean Owen
 */
	public class Result
	{
		import com.google.zxing.common.flexdatatypes.HashTable;
		import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
		
		//BAS : made public for debugging
		  private var text:String;
          private var rawBytes:Array;
          private var resultPoints:Array;
          private var format:BarcodeFormat;
          private var resultMetadata:HashTable;

          public function Result( text:String,
                         		  	rawBytes:Array,
                        			resultPoints:Array,
                        			format:BarcodeFormat) 
          {
            if (text == null && rawBytes == null) {
              throw new IllegalArgumentException("Result : Text and bytes are both null");
            }
            this.text = text;
            this.rawBytes = rawBytes;
            this.resultPoints = resultPoints;
            this.format = format;
            this.resultMetadata = null;
          }

          /**
           * @return raw text encoded by the barcode, if applicable, otherwise <code>null</code>
           */
          public function getText():String {
            return text;
          }

          /**
           * @return raw bytes encoded by the barcode, if applicable, otherwise <code>null</code>
           */
          public function getRawBytes():Array {
            return rawBytes;
          }

          /**
           * @return points related to the barcode in the image. These are typically points
           *         identifying finder patterns or the corners of the barcode. The exact meaning is
           *         specific to the type of barcode that was decoded.
           */
          public function getResultPoints():Array {
            return resultPoints;
          }

          /**
           * @return {@link BarcodeFormat} representing the format of the barcode that was recognized and decoded
           */
          public function getBarcodeFormat():BarcodeFormat {
            return format;
          }

          /**
           * @return {@link HashTable} mapping {@link ResultMetadataType} keys to values. May be <code>null</code>.
           *  This contains optional metadata about what was detected about the barcode, like orientation.
           */
          public function getResultMetadata():HashTable {
            return resultMetadata;
          }

          public function putMetadata(type:ResultMetadataType, value:Object ):void {
            if (resultMetadata == null) {
              resultMetadata = new HashTable(3);
            }
            resultMetadata.Add(type, value);
          }

          public function toString():String {
            if (text == null) {
              return "[" + rawBytes.length + " bytes]";
            } else {
              return text;
            }
          }
    
 
	}
}