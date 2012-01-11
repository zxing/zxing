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
 * Enumerates barcode formats known to this package.
 *
 * @author Sean Owen
 */
 import com.google.zxing.common.flexdatatypes.HashTable;
 import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
	
public class BarcodeFormat
{
﻿        // No, we can't use an enum here. J2ME doesn't support it.

	private static var VALUES:HashTable = new HashTable();

  /** Aztec 2D barcode format. */
  		public static var AZTEC:BarcodeFormat = new BarcodeFormat("AZTEC");
  /** CODABAR 1D format. */
        public static var CODABAR:BarcodeFormat = new BarcodeFormat("CODABAR");
  /** QR Code 2D barcode format. */
        public static var QR_CODE:BarcodeFormat = new BarcodeFormat("QR_CODE");
  /** DataMatrix 2D barcode format. */
        public static var DATAMATRIX:BarcodeFormat = new BarcodeFormat("DATAMATRIX");
  /** UPC-E 1D format. */
        public static var UPC_E:BarcodeFormat = new BarcodeFormat("UPC_E");
  /** UPC-A 1D format. */
        public static var UPC_A:BarcodeFormat = new BarcodeFormat("UPC_A");
  /** UPC/EAN extension format. Not a stand-alone format. */
  		public static var UPC_EAN_EXTENSION:BarcodeFormat = new BarcodeFormat("UPC_EAN_EXTENSION");
  /** EAN-8 1D format. */
        public static var EAN_8:BarcodeFormat = new BarcodeFormat("EAN_8");
  /** EAN-13 1D format. */
        public static var EAN_13:BarcodeFormat = new BarcodeFormat("EAN_13");
  /** Code 128 1D format. */
        public static var CODE_128:BarcodeFormat = new BarcodeFormat("CODE_128");
  /** Code 93 1D format. */
        public static var CODE_93:BarcodeFormat = new BarcodeFormat("CODE_93");
  /** Code 39 1D format. */
        public static var CODE_39:BarcodeFormat = new BarcodeFormat("CODE_39");
  /** ITF (Interleaved Two of Five) 1D format. */
        public static var ITF:BarcodeFormat = new BarcodeFormat("ITF");
  /** PDF417 format. */
        public static var PDF417:BarcodeFormat = new BarcodeFormat("PDF417");
  /** RSS 14 */
  		public static var RSS_14:BarcodeFormat = new BarcodeFormat("RSS_14");
  /** RSS EXPANDED */
  		public static var RSS_EXPANDED:BarcodeFormat = new BarcodeFormat("RSS_EXPANDED");
  /** MAXICODE */
  		public static var MAXICODE:BarcodeFormat = new BarcodeFormat("MAXICODE");
		
        private var _name:String;

        public function BarcodeFormat(name:String)
        {
            this._name = name;
			VALUES._put(name, this);
        }

        public  function toString():String 
        {
            return this._name;
        }
        
        
        public function get name():String
        {
        	return this._name;		
        }
        
        public static function valueOf(name:String):BarcodeFormat 
		{
			if (name == null || name.length == 0) {
			  throw new IllegalArgumentException();
			}
			var format:BarcodeFormat  = (VALUES._get(name) as BarcodeFormat);
			if (format == null) 
			{
			  throw new IllegalArgumentException();
			}
			return format;
		}
		
    }
}