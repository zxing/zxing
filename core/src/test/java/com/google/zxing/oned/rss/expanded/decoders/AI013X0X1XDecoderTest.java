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
public final class AI013X0X1XDecoderTest extends AbstractDecoderTest {

  private static final String header310x11 = "..XXX...";
  private static final String header320x11 = "..XXX..X";
  private static final String header310x13 = "..XXX.X.";
  private static final String header320x13 = "..XXX.XX";
  private static final String header310x15 = "..XXXX..";
  private static final String header320x15 = "..XXXX.X";
  private static final String header310x17 = "..XXXXX.";
  private static final String header320x17 = "..XXXXXX";

  @Test
  public void test01310X1XendDate() throws Exception {
    CharSequence data = header310x11 + compressedGtin900123456798908 + compressed20bitWeight1750 + compressedDateEnd;
    String expected = "(01)90012345678908(3100)001750";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void test01310X111() throws Exception {
    CharSequence data = header310x11 + compressedGtin900123456798908 + compressed20bitWeight1750 + compressedDateMarch12th2010;
    String expected = "(01)90012345678908(3100)001750(11)100312";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void test01320X111() throws Exception {
    CharSequence data = header320x11 + compressedGtin900123456798908 + compressed20bitWeight1750 + compressedDateMarch12th2010;
    String expected = "(01)90012345678908(3200)001750(11)100312";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void test01310X131() throws Exception {
    CharSequence data = header310x13 + compressedGtin900123456798908 + compressed20bitWeight1750 + compressedDateMarch12th2010;
    String expected = "(01)90012345678908(3100)001750(13)100312";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void test01320X131() throws Exception {
    CharSequence data = header320x13 + compressedGtin900123456798908 + compressed20bitWeight1750 + compressedDateMarch12th2010;
    String expected = "(01)90012345678908(3200)001750(13)100312";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void test01310X151() throws Exception {
    CharSequence data = header310x15 + compressedGtin900123456798908 + compressed20bitWeight1750 + compressedDateMarch12th2010;
    String expected = "(01)90012345678908(3100)001750(15)100312";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void test01320X151() throws Exception {
    CharSequence data = header320x15 + compressedGtin900123456798908 + compressed20bitWeight1750 + compressedDateMarch12th2010;
    String expected = "(01)90012345678908(3200)001750(15)100312";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void test01310X171() throws Exception {
    CharSequence data = header310x17 + compressedGtin900123456798908 + compressed20bitWeight1750 + compressedDateMarch12th2010;
    String expected = "(01)90012345678908(3100)001750(17)100312";

    assertCorrectBinaryString(data, expected);
  }

  @Test
  public void test01320X171() throws Exception {
    CharSequence data = header320x17 + compressedGtin900123456798908 + compressed20bitWeight1750 + compressedDateMarch12th2010;
    String expected = "(01)90012345678908(3200)001750(17)100312";

    assertCorrectBinaryString(data, expected);
  }

}
