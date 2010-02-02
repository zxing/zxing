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

package com.google.zxing.datamatrix.decoder;

import junit.framework.TestCase;

/**
 * @author bbrown@google.com (Brian Brown)
 */
public final class DecodedBitStreamParserTestCase extends TestCase{

  public void testAsciiStandardDecode() throws Exception {
    // ASCII characters 0-127 are encoded as the value + 1
    byte[] bytes = {(byte) ('a' + 1), (byte) ('b' + 1), (byte) ('c' + 1),
                    (byte) ('A' + 1), (byte) ('B' + 1), (byte) ('C' + 1)};
    String decodedString = DecodedBitStreamParser.decode(bytes).getText();
    assertEquals("abcABC", decodedString);
  }
  
  public void testAsciiDoubleDigitDecode() throws Exception{
    // ASCII double digit (00 - 99) Numeric Value + 130
    byte[] bytes = {(byte)       130 , (byte) ( 1 + 130),
                    (byte) (98 + 130), (byte) (99 + 130)};
    String decodedString = DecodedBitStreamParser.decode(bytes).getText();
    assertEquals("00019899", decodedString);
  }
  
  // TODO(bbrown): Add test cases for each encoding type
  // TODO(bbrown): Add test cases for switching encoding types
}