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

package com.google.zxing.common
{
	import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
	import mx.controls.List;
	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
	import com.google.zxing.common.flexdatatypes.ArrayList;
	

/**
 * <p>Encapsulates the result of decoding a matrix of bits. This typically
 * applies to 2D barcode formats. For now it contains the raw bytes obtained,
 * as well as a String interpretation of those bytes, if applicable.</p>
 *
 * @author Sean Owen
 */    public class DecoderResult
    {
    	
          private var rawBytes:Array;
          private var text:String ;
          private var byteSegments:ArrayList;
          private var ecLevel:String;

          public function DecoderResult(rawBytes:Array, text:String,  byteSegments:ArrayList, ecLevel:String)
          {
            if (rawBytes == null && text == null) 
            {
              throw new IllegalArgumentException("common : DecoderResult : Constructor : rawBytes array contains no data and text == null");
            }
            this.rawBytes = rawBytes;
            this.text = text;
            this.byteSegments = byteSegments;
            this.ecLevel = ecLevel
          }

          public function getRawBytes():Array {
            return this.rawBytes;
          }

          public function getText():String {
            return text;
          }

          public function getByteSegments():ArrayList
          {
            return byteSegments;
          }
	
	      public function getECLevel():String 
	      {
    		return ecLevel;
    	  }
    }
 }