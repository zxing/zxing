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

import org.junit.Test;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 */
public final class AnyAIDecoderTest extends AbstractDecoderTest {

  private static final String header = ".....";

  @Test
  public void testAnyAIDecoder1() throws Exception {
    CharSequence data = header + numeric10 + numeric12 + numeric2alpha + alphaA + alpha2numeric + numeric12;
    String expected = "(10)12A12";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void testAnyAIDecoder2() throws Exception {
    CharSequence data = header + numeric10 + numeric12 + numeric2alpha + alphaA + alpha2isoiec646 + i646B;
    String expected = "(10)12AB";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void testAnyAIDecoder3() throws Exception {
    CharSequence data = header + numeric10 + numeric2alpha + alpha2isoiec646 + i646B + i646C + isoiec6462alpha + alphaA + alpha2numeric + numeric10;
    String expected = "(10)BCA10";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void testAnyAIDecodernumericFNC1secondDigit() throws Exception {
    CharSequence data = header + numeric10 + numeric1FNC1;
    String expected = "(10)1";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void testAnyAIDecoderalphaFNC1() throws Exception {
    CharSequence data = header + numeric10 + numeric2alpha + alphaA + alphaFNC1;
    String expected = "(10)A";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void testAnyAIDecoder646FNC1() throws Exception {
    CharSequence data = header + numeric10 + numeric2alpha + alphaA + isoiec6462alpha + i646B + i646FNC1;
    String expected = "(10)AB";

    assertCorrectBinaryString(data, expected);
  }
}
