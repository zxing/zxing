/*
 * Copyright (C) 2010 ZXing authors
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

/*
 * These authors would like to acknowledge the Spanish Ministry of Industry,
 * Tourism and Trade, for the support in the project TSI020301-2008-2
 * "PIRAmIDE: Personalizable Interactions with Resources on AmI-enabled
 * Mobile Dynamic Environments", led by Treelogic
 * ( http://www.treelogic.com/ ):
 *
 *   http://www.piramidepse.com/
 */

package com.google.zxing.oned.rss.expanded.decoders;

import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;
import com.google.zxing.oned.rss.expanded.BinaryUtil;

import org.junit.Assert;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 */
abstract class AbstractDecoderTest extends Assert {

  static final String numeric10                     = "..X..XX";
  static final String numeric12                     = "..X.X.X";
  static final String numeric1FNC1                  = "..XXX.X";
  // static final String numericFNC11                  = "XXX.XXX";

  static final String numeric2alpha                  = "....";

  static final String alphaA                        = "X.....";
  static final String alphaFNC1                      = ".XXXX";
  static final String alpha2numeric                  = "...";
  static final String alpha2isoiec646                = "..X..";

  static final String i646B                         = "X.....X";
  static final String i646C                         = "X....X.";
  static final String i646FNC1                      = ".XXXX";
  static final String isoiec6462alpha               = "..X..";

  static final String compressedGtin900123456798908 = ".........X..XXX.X.X.X...XX.XXXXX.XXXX.X.";
  static final String compressedGtin900000000000008 = "........................................";

  static final String compressed15bitWeight1750     = "....XX.XX.X.XX.";
  static final String compressed15bitWeight11750    = ".X.XX.XXXX..XX.";
  static final String compressed15bitWeight0        = "...............";

  static final String compressed20bitWeight1750     = ".........XX.XX.X.XX.";

  static final String compressedDateMarch12th2010 = "....XXXX.X..XX..";
  static final String compressedDateEnd             = "X..X.XX.........";

  static void assertCorrectBinaryString(CharSequence binaryString,
                                        String expectedNumber) throws NotFoundException, FormatException {
    BitArray binary = BinaryUtil.buildBitArrayFromStringWithoutSpaces(binaryString);
    AbstractExpandedDecoder decoder = AbstractExpandedDecoder.createDecoder(binary);
    String result = decoder.parseInformation();
    assertEquals(expectedNumber, result);
  }
}
