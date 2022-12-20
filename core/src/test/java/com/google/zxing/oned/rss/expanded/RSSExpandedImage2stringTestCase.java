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

package com.google.zxing.oned.rss.expanded;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.BufferedImageLuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.AbstractBlackBoxTestCase;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.GlobalHistogramBinarizer;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public final class RSSExpandedImage2stringTestCase extends Assert {

  @Test
  public void testDecodeRow2string1() throws Exception {
    assertCorrectImage2string("1.png", "(11)100224(17)110224(3102)000100");
  }

  @Test
  public void testDecodeRow2string2() throws Exception {
    assertCorrectImage2string("2.png", "(01)90012345678908(3103)001750");
  }

  @Test
  public void testDecodeRow2string3() throws Exception {
    assertCorrectImage2string("3.png", "(10)12A");
  }

  @Test
  public void testDecodeRow2string4() throws Exception {
    assertCorrectImage2string("4.png", "(01)98898765432106(3202)012345(15)991231");
  }

  @Test
  public void testDecodeRow2string5() throws Exception {
    assertCorrectImage2string("5.png", "(01)90614141000015(3202)000150");
  }

  @Test
  public void testDecodeRow2string7() throws Exception {
    assertCorrectImage2string("7.png", "(10)567(11)010101");
  }

  @Test
  public void testDecodeRow2string10() throws Exception {
    String expected = "(01)98898765432106(15)991231(3103)001750(10)12A(422)123(21)123456(423)012345678901";
    assertCorrectImage2string("10.png", expected);
  }

  @Test
  public void testDecodeRow2string11() throws Exception {
    assertCorrectImage2string("11.png", "(01)98898765432106(15)991231(3103)001750(10)12A(422)123(21)123456");
  }

  @Test
  public void testDecodeRow2string12() throws Exception {
    assertCorrectImage2string("12.png", "(01)98898765432106(3103)001750");
  }

  @Test
  public void testDecodeRow2string13() throws Exception {
    assertCorrectImage2string("13.png", "(01)90012345678908(3922)795");
  }

  @Test
  public void testDecodeRow2string14() throws Exception {
    assertCorrectImage2string("14.png", "(01)90012345678908(3932)0401234");
  }

  @Test
  public void testDecodeRow2string15() throws Exception {
    assertCorrectImage2string("15.png", "(01)90012345678908(3102)001750(11)100312");
  }

  @Test
  public void testDecodeRow2string16() throws Exception {
    assertCorrectImage2string("16.png", "(01)90012345678908(3202)001750(11)100312");
  }

  @Test
  public void testDecodeRow2string17() throws Exception {
    assertCorrectImage2string("17.png", "(01)90012345678908(3102)001750(13)100312");
  }

  @Test
  public void testDecodeRow2string18() throws Exception {
    assertCorrectImage2string("18.png", "(01)90012345678908(3202)001750(13)100312");
  }

  @Test
  public void testDecodeRow2string19() throws Exception {
    assertCorrectImage2string("19.png", "(01)90012345678908(3102)001750(15)100312");
  }

  @Test
  public void testDecodeRow2string20() throws Exception {
    assertCorrectImage2string("20.png", "(01)90012345678908(3202)001750(15)100312");
  }

  @Test
  public void testDecodeRow2string21() throws Exception {
    assertCorrectImage2string("21.png", "(01)90012345678908(3102)001750(17)100312");
  }

  @Test
  public void testDecodeRow2string22() throws Exception {
    assertCorrectImage2string("22.png", "(01)90012345678908(3202)001750(17)100312");
  }

  @Test
  public void testDecodeRow2string25() throws Exception {
    assertCorrectImage2string("25.png", "(10)123");
  }

  @Test
  public void testDecodeRow2string26() throws Exception {
    assertCorrectImage2string("26.png", "(10)5678(11)010101");
  }

  @Test
  public void testDecodeRow2string27() throws Exception {
    assertCorrectImage2string("27.png", "(10)1098-1234");
  }

  @Test
  public void testDecodeRow2string28() throws Exception {
    assertCorrectImage2string("28.png", "(10)1098/1234");
  }

  @Test
  public void testDecodeRow2string29() throws Exception {
    assertCorrectImage2string("29.png", "(10)1098.1234");
  }

  @Test
  public void testDecodeRow2string30() throws Exception {
    assertCorrectImage2string("30.png", "(10)1098*1234");
  }

  @Test
  public void testDecodeRow2string31() throws Exception {
    assertCorrectImage2string("31.png", "(10)1098,1234");
  }

  @Test
  public void testDecodeRow2string32() throws Exception {
    assertCorrectImage2string("32.png", "(15)991231(3103)001750(10)12A(422)123(21)123456(423)0123456789012");
  }

  private static void assertCorrectImage2string(String fileName, String expected)
      throws IOException, NotFoundException {
    Path path = AbstractBlackBoxTestCase.buildTestBase("src/test/resources/blackbox/rssexpanded-1/").resolve(fileName);

    BufferedImage image = ImageIO.read(path.toFile());
    BinaryBitmap binaryMap =
        new BinaryBitmap(new GlobalHistogramBinarizer(new BufferedImageLuminanceSource(image)));
    int rowNumber = binaryMap.getHeight() / 2;
    BitArray row = binaryMap.getBlackRow(rowNumber, null);

    Result result;
    try {
      RSSExpandedReader rssExpandedReader = new RSSExpandedReader();
      result = rssExpandedReader.decodeRow(rowNumber, row, null);
    } catch (ReaderException re) {
      fail(re.toString());
      return;
    }

    assertSame(BarcodeFormat.RSS_EXPANDED, result.getBarcodeFormat());
    assertEquals(expected, result.getText());
  }

}
