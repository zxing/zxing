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

package com.google.zxing;

/**
 * These are a set of hints that you may pass to Writers to specify their behavior.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public enum EncodeHintType {

  /**
   * Specifies what degree of error correction to use, for example in QR Codes.
   * Type depends on the encoder. For example for QR codes it's type
   * {@link com.google.zxing.qrcode.decoder.ErrorCorrectionLevel ErrorCorrectionLevel}.
   * For Aztec it is of type {@link Integer}, representing the minimal percentage of error correction words.
   * For PDF417 it is of type {@link Integer}, valid values being 0 to 8.
   * In all cases, it can also be a {@link String} representation of the desired value as well.
   * Note: an Aztec symbol should have a minimum of 25% EC words.
   */
  ERROR_CORRECTION,

  /**
   * Specifies what character encoding to use where applicable (type {@link String})
   */
  CHARACTER_SET,

  /**
   * Specifies the matrix shape for Data Matrix (type {@link com.google.zxing.datamatrix.encoder.SymbolShapeHint})
   */
  DATA_MATRIX_SHAPE,

  /**
   * Specifies whether to use compact mode for Data Matrix (type {@link Boolean}, or "true" or "false" 
   * {@link String } value).
   * The compact encoding mode also supports the encoding of characters that are not in the ISO-8859-1
   * character set via ECIs.
   * Please note that in that case, the most compact character encoding is chosen for characters in
   * the input that are not in the ISO-8859-1 character set. Based on experience, some scanners do not
   * support encodings like cp-1256 (Arabic). In such cases the encoding can be forced to UTF-8 by
   * means of the {@link #CHARACTER_SET} encoding hint.
   * Compact encoding also provides GS1-FNC1 support when {@link #GS1_FORMAT} is selected. In this case
   * group-separator character (ASCII 29 decimal) can be used to encode the positions of FNC1 codewords
   * for the purpose of delimiting AIs.
   * This option and {@link #FORCE_C40} are mutually exclusive.
   */
  DATA_MATRIX_COMPACT,

  /**
   * Specifies a minimum barcode size (type {@link Dimension}). Only applicable to Data Matrix now.
   *
   * @deprecated use width/height params in
   * {@link com.google.zxing.datamatrix.DataMatrixWriter#encode(String, BarcodeFormat, int, int)}
   */
  @Deprecated
  MIN_SIZE,

  /**
   * Specifies a maximum barcode size (type {@link Dimension}). Only applicable to Data Matrix now.
   *
   * @deprecated without replacement
   */
  @Deprecated
  MAX_SIZE,

  /**
   * Specifies margin, in pixels, to use when generating the barcode. The meaning can vary
   * by format; for example it controls margin before and after the barcode horizontally for
   * most 1D formats. (Type {@link Integer}, or {@link String} representation of the integer value).
   */
  MARGIN,

  /**
   * Specifies whether to use compact mode for PDF417 (type {@link Boolean}, or "true" or "false"
   * {@link String} value).
   */
  PDF417_COMPACT,

  /**
   * Specifies what compaction mode to use for PDF417 (type
   * {@link com.google.zxing.pdf417.encoder.Compaction Compaction} or {@link String} value of one of its
   * enum values).
   */
  PDF417_COMPACTION,

  /**
   * Specifies the minimum and maximum number of rows and columns for PDF417 (type
   * {@link com.google.zxing.pdf417.encoder.Dimensions Dimensions}).
   */
  PDF417_DIMENSIONS,

  /**
   * Specifies whether to automatically insert ECIs when encoding PDF417 (type {@link Boolean}, or "true" or "false"
   * {@link String} value). 
   * Please note that in that case, the most compact character encoding is chosen for characters in
   * the input that are not in the ISO-8859-1 character set. Based on experience, some scanners do not
   * support encodings like cp-1256 (Arabic). In such cases the encoding can be forced to UTF-8 by
   * means of the {@link #CHARACTER_SET} encoding hint.
   */
  PDF417_AUTO_ECI,

  /**
   * Specifies the required number of layers for an Aztec code.
   * A negative number (-1, -2, -3, -4) specifies a compact Aztec code.
   * 0 indicates to use the minimum number of layers (the default).
   * A positive number (1, 2, .. 32) specifies a normal (non-compact) Aztec code.
   * (Type {@link Integer}, or {@link String} representation of the integer value).
   */
   AZTEC_LAYERS,

   /**
    * Specifies the exact version of QR code to be encoded.
    * (Type {@link Integer}, or {@link String} representation of the integer value).
    */
   QR_VERSION,

  /**
   * Specifies the QR code mask pattern to be used. Allowed values are
   * 0..QRCode.NUM_MASK_PATTERNS-1. By default the code will automatically select
   * the optimal mask pattern.
   * * (Type {@link Integer}, or {@link String} representation of the integer value).
   */
  QR_MASK_PATTERN,


  /**
   * Specifies whether to use compact mode for QR code (type {@link Boolean}, or "true" or "false"
   * {@link String } value).
   * Please note that when compaction is performed, the most compact character encoding is chosen
   * for characters in the input that are not in the ISO-8859-1 character set. Based on experience,
   * some scanners do not support encodings like cp-1256 (Arabic). In such cases the encoding can
   * be forced to UTF-8 by means of the {@link #CHARACTER_SET} encoding hint.
   */
  QR_COMPACT,

  /**
   * Specifies whether the data should be encoded to the GS1 standard (type {@link Boolean}, or "true" or "false"
   * {@link String } value).
   */
  GS1_FORMAT,

  /**
   * Forces which encoding will be used. Currently only used for Code-128 code sets (Type {@link String}).
   * Valid values are "A", "B", "C".
   * This option and {@link #CODE128_COMPACT} are mutually exclusive.
   */
  FORCE_CODE_SET,

  /**
   * Forces C40 encoding for data-matrix (type {@link Boolean}, or "true" or "false") {@link String } value). This 
   * option and {@link #DATA_MATRIX_COMPACT} are mutually exclusive.
   */
  FORCE_C40,

  /**
   * Specifies whether to use compact mode for Code-128 code (type {@link Boolean}, or "true" or "false" 
   * {@link String } value).
   * This can yield slightly smaller bar codes. This option and {@link #FORCE_CODE_SET} are mutually
   * exclusive.
   */
  CODE128_COMPACT,

}
