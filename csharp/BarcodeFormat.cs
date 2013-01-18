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

namespace com.google.zxing
{

	/// <summary>
	/// Enumerates barcode formats known to this package. Please keep alphabetized.
	/// 
	/// @author Sean Owen
	/// </summary>
	public enum BarcodeFormat
	{

	  /// <summary>
	  /// Aztec 2D barcode format. </summary>
	  AZTEC,

	  /// <summary>
	  /// CODABAR 1D format. </summary>
	  CODABAR,

	  /// <summary>
	  /// Code 39 1D format. </summary>
	  CODE_39,

	  /// <summary>
	  /// Code 93 1D format. </summary>
	  CODE_93,

	  /// <summary>
	  /// Code 128 1D format. </summary>
	  CODE_128,

	  /// <summary>
	  /// Data Matrix 2D barcode format. </summary>
	  DATA_MATRIX,

	  /// <summary>
	  /// EAN-8 1D format. </summary>
	  EAN_8,

	  /// <summary>
	  /// EAN-13 1D format. </summary>
	  EAN_13,

	  /// <summary>
	  /// ITF (Interleaved Two of Five) 1D format. </summary>
	  ITF,

	  /// <summary>
	  /// MaxiCode 2D barcode format. </summary>
	  MAXICODE,

	  /// <summary>
	  /// PDF417 format. </summary>
	  PDF_417,

	  /// <summary>
	  /// QR Code 2D barcode format. </summary>
	  QR_CODE,

	  /// <summary>
	  /// RSS 14 </summary>
	  RSS_14,

	  /// <summary>
	  /// RSS EXPANDED </summary>
	  RSS_EXPANDED,

	  /// <summary>
	  /// UPC-A 1D format. </summary>
	  UPC_A,

	  /// <summary>
	  /// UPC-E 1D format. </summary>
	  UPC_E,

	  /// <summary>
	  /// UPC/EAN extension format. Not a stand-alone format. </summary>
	  UPC_EAN_EXTENSION

	}

}