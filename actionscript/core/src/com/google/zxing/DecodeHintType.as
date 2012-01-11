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
 * Encapsulates a type of hint that a caller may pass to a barcode reader to help it
 * more quickly or accurately decode it. It is up to implementations to decide what,
 * if anything, to do with the information that is supplied.
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 * @see Reader#decode(BinaryBitmap,java.util.Hashtable)
 */

	public class DecodeHintType
	{
          /**
           * Unspecified, application-specific hint. Maps to an unspecified {@link Object}.
           */
          public static var OTHER:DecodeHintType = new DecodeHintType("OTHER");

          /**
           * Image is a pure monochrome image of a barcode. Doesn't matter what it maps to;
           * use {@link Boolean#TRUE}.
           */
          public static var PURE_BARCODE:DecodeHintType = new DecodeHintType("PURE_BARCODE");

          /**
          * 
           * Image is known to be of one of a few possible formats.
           * Maps to a {@link java.util.Vector} of {@link BarcodeFormat}s.
           */
          public static var POSSIBLE_FORMATS:DecodeHintType = new DecodeHintType("POSSIBLE_FORMATS");

          /**
           * Spend more time to try to find a barcode; optimize for accuracy, not speed.
           * Doesn't matter what it maps to; use {@link Boolean#TRUE}.
           */
          public static var TRY_HARDER:DecodeHintType = new DecodeHintType("TRY_HARDER");
          
            /**
		   * Specifies what character encoding to use when decoding, where applicable (type String)
		   */
		  public static var CHARACTER_SET:DecodeHintType = new DecodeHintType("CHARACTER_SET");
		  
          /**
		   * Allowed lengths of encoded data -- reject anything else. Maps to an int[].
   			*/
  		  public static var ALLOWED_LENGTHS:DecodeHintType = new DecodeHintType("ALLOWED_LENGTHS");

		 /**
		   * Assume Code 39 codes employ a check digit. Maps to {@link Boolean}.
		   */
		  public static var  ASSUME_CODE_39_CHECK_DIGIT:DecodeHintType = new DecodeHintType("ASSUME_CODE_39_CHECK_DIGIT");
		
		  /**
		   * The caller needs to be notified via callback when a possible {@link ResultPoint}
		   * is found. Maps to a {@link ResultPointCallback}.
		   */
		  public static var NEED_RESULT_POINT_CALLBACK:DecodeHintType = new DecodeHintType("NEED_RESULT_POINT_CALLBACK");

		  public var  DHtype:String;
		  
		  public function DecodeHintType(aType:String)
		  {
		  	DHtype = aType;		    
		  }
	}
}