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
package com.google.zxing
{
	public class MultiFormatWriter implements Writer
	{
		import com.google.zxing.common.ByteMatrix;
		import com.google.zxing.common.flexdatatypes.HashTable;
		import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
		import com.google.zxing.qrcode.QRCodeWriter
		import com.google.zxing.oned.EAN13Writer;
		import com.google.zxing.oned.EAN8Writer;

        public function  encode(contents:String,  format:BarcodeFormat=null,width:int=0,height:int=0, hints:HashTable=null):Object{
	
	    if (format == BarcodeFormat.EAN_8) {
	      return (new EAN8Writer()).encode(contents, format, width, height, hints);
	    } else if (format == BarcodeFormat.EAN_13) {
	      return (new EAN13Writer()).encode(contents, format, width, height, hints);
	    } else if (format == BarcodeFormat.QR_CODE) {
              return (new QRCodeWriter()).encode(contents, format, width, height, hints);
        } else {
          throw new IllegalArgumentException("No encoder available for format " + format);
        }
        }   
    }

}