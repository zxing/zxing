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
		import com.google.zxing.oned.Code128Writer;
		import com.google.zxing.oned.Code39Writer;
		import com.google.zxing.oned.EAN13Writer;
		import com.google.zxing.oned.EAN8Writer;
		import com.google.zxing.oned.ITFWriter;
		import com.google.zxing.oned.UPCAWriter;
		import com.google.zxing.pdf417.encoder.PDF417Writer;

        public function  encode(contents:String,  format:BarcodeFormat=null,width:int=0,height:int=0, hints:HashTable=null):Object{
	
		var writer:Writer;
	    if (format == BarcodeFormat.EAN_8) {
	      writer = new EAN8Writer();
	    } else if (format == BarcodeFormat.EAN_13) {
	      writer = new EAN13Writer();
	    } else if (format == BarcodeFormat.UPC_A) {
	      writer = new UPCAWriter();
	    } else if (format == BarcodeFormat.CODE_39) {
	      writer = new Code39Writer();
	    } else if (format == BarcodeFormat.CODE_128) {
	      writer = new Code128Writer();
	    } else if (format == BarcodeFormat.ITF) {
	      writer = new ITFWriter();
	    } else if (format == BarcodeFormat.PDF417) {
	      writer = new PDF417Writer();
	    } else if (format == BarcodeFormat.QR_CODE) {
              writer = new QRCodeWriter();
        } else {
          throw new IllegalArgumentException("No encoder available for format " + format);
        }
        return writer.encode(contents, format, width, height, hints);
        }   
    }

}