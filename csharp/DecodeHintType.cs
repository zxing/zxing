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
	/// Encapsulates a type of hint that a caller may pass to a barcode reader to help it
	/// more quickly or accurately decode it. It is up to implementations to decide what,
	/// if anything, to do with the information that is supplied.
	/// 
	/// @author Sean Owen
	/// @author dswitkin@google.com (Daniel Switkin) </summary>
	/// <seealso cref= Reader#decode(BinaryBitmap,java.util.Map) </seealso>
	public enum DecodeHintType
	{

	  /// <summary>
	  /// Unspecified, application-specific hint. Maps to an unspecified <seealso cref="Object"/>.
	  /// </summary>
	 OTHER,

	  /// <summary>
	  /// Image is a pure monochrome image of a barcode. Doesn't matter what it maps to;
	  /// use <seealso cref="Boolean#TRUE"/>.
	  /// </summary>
	  PURE_BARCODE,

	  /// <summary>
	  /// Image is known to be of one of a few possible formats.
	  /// Maps to a <seealso cref="java.util.List"/> of <seealso cref="BarcodeFormat"/>s.
	  /// </summary>
	  POSSIBLE_FORMATS,

	  /// <summary>
	  /// Spend more time to try to find a barcode; optimize for accuracy, not speed.
	  /// Doesn't matter what it maps to; use <seealso cref="Boolean#TRUE"/>.
	  /// </summary>
	  TRY_HARDER,

	  /// <summary>
	  /// Specifies what character encoding to use when decoding, where applicable (type String)
	  /// </summary>
	  CHARACTER_SET,

	  /// <summary>
	  /// Allowed lengths of encoded data -- reject anything else. Maps to an int[].
	  /// </summary>
	  ALLOWED_LENGTHS,

	  /// <summary>
	  /// Assume Code 39 codes employ a check digit. Maps to <seealso cref="Boolean"/>.
	  /// </summary>
	  ASSUME_CODE_39_CHECK_DIGIT,

	  /// <summary>
	  /// The caller needs to be notified via callback when a possible <seealso cref="ResultPoint"/>
	  /// is found. Maps to a <seealso cref="ResultPointCallback"/>.
	  /// </summary>
	  NEED_RESULT_POINT_CALLBACK,

	}

}