using System.Collections.Generic;

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

namespace com.google.zxing
{

	using BitMatrix = com.google.zxing.common.BitMatrix;


	/// <summary>
	/// The base class for all objects which encode/generate a barcode image.
	/// 
	/// @author dswitkin@google.com (Daniel Switkin)
	/// </summary>
	public interface Writer
	{

	  /// <summary>
	  /// Encode a barcode using the default settings.
	  /// </summary>
	  /// <param name="contents"> The contents to encode in the barcode </param>
	  /// <param name="format"> The barcode format to generate </param>
	  /// <param name="width"> The preferred width in pixels </param>
	  /// <param name="height"> The preferred height in pixels </param>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: com.google.zxing.common.BitMatrix encode(String contents, BarcodeFormat format, int width, int height) throws WriterException;
	  BitMatrix encode(string contents, BarcodeFormat format, int width, int height);

	  /// 
	  /// <param name="contents"> The contents to encode in the barcode </param>
	  /// <param name="format"> The barcode format to generate </param>
	  /// <param name="width"> The preferred width in pixels </param>
	  /// <param name="height"> The preferred height in pixels </param>
	  /// <param name="hints"> Additional parameters to supply to the encoder </param>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: com.google.zxing.common.BitMatrix encode(String contents, BarcodeFormat format, int width, int height, java.util.Map<EncodeHintType,?> hints) throws WriterException;
      BitMatrix encode(string contents, BarcodeFormat format, int width, int height, IDictionary<EncodeHintType, object> hints);

	}

}