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

package com.google.zxing;

/**
 * Encapsulates a type of hint that a caller may pass to a barcode reader to help it
 * more quickly or accurately decode it. It is up to implementations to decide what,
 * if anything, to do with the information that is supplied.
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 * @see Reader#decode(BinaryBitmap,java.util.Hashtable)
 */
public final class DecodeHintType {

  // No, we can't use an enum here. J2ME doesn't support it.

  /**
   * Unspecified, application-specific hint. Maps to an unspecified {@link Object}.
   */
  public static final DecodeHintType OTHER = new DecodeHintType();

  /**
   * Image is a pure monochrome image of a barcode. Doesn't matter what it maps to;
   * use {@link Boolean#TRUE}.
   */
  public static final DecodeHintType PURE_BARCODE = new DecodeHintType();

  /**
   * Image is known to be of one of a few possible formats.
   * Maps to a {@link java.util.Vector} of {@link BarcodeFormat}s.
   */
  public static final DecodeHintType POSSIBLE_FORMATS = new DecodeHintType();

  /**
   * Spend more time to try to find a barcode; optimize for accuracy, not speed.
   * Doesn't matter what it maps to; use {@link Boolean#TRUE}.
   */
  public static final DecodeHintType TRY_HARDER = new DecodeHintType();

  /**
   * Specifies what character encoding to use when decoding, where applicable (type String)
   */
  public static final DecodeHintType CHARACTER_SET = new DecodeHintType();

  /**
   * Allowed lengths of encoded data -- reject anything else. Maps to an int[].
   */
  public static final DecodeHintType ALLOWED_LENGTHS = new DecodeHintType();

  /**
   * Assume Code 39 codes employ a check digit. Maps to {@link Boolean}.
   */
  public static final DecodeHintType ASSUME_CODE_39_CHECK_DIGIT = new DecodeHintType();

  /**
   * The caller needs to be notified via callback when a possible {@link ResultPoint}
   * is found. Maps to a {@link ResultPointCallback}.
   */
  public static final DecodeHintType NEED_RESULT_POINT_CALLBACK = new DecodeHintType();

  private DecodeHintType() {
  }

}
