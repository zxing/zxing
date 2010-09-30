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
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
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
  public void testDecodeRow2string_1() throws Exception {
    String path = "test/data/blackbox/rssexpanded-1/1.jpg";
    String expected = "(11)100224(17)110224(3102)000100";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_2() throws Exception {
    String path = "test/data/blackbox/rssexpanded-1/2.jpg";
    String expected = "(01)90012345678908(3103)001750";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_3() throws Exception {
    String path = "test/data/blackbox/rssexpanded-1/3.jpg";
    String expected = "(10)12A";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_4() throws Exception {
    String path = "test/data/blackbox/rssexpanded-1/4.jpg";
    String expected = "(01)98898765432106(3202)012345(15)991231";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_5() throws Exception {
    String path = "test/data/blackbox/rssexpanded-1/5.jpg";
    String expected = "(01)90614141000015(3202)000150";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_7() throws Exception {
    String path = "test/data/blackbox/rssexpanded-1/7.png";
    String expected = "(10)567(11)010101";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_10() throws Exception {
    String path = "test/data/blackbox/rssexpanded-1/10.png";
    String expected = "(01)98898765432106(15)991231(3103)001750(10)12A(422)123(21)123456(423)012345678901";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_11() throws Exception {
    String expected = "(01)98898765432106(15)991231(3103)001750(10)12A(422)123(21)123456";
    String path = "test/data/blackbox/rssexpanded-1/11.png";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_12() throws Exception {
    String expected = "(01)98898765432106(3103)001750";
    String path = "test/data/blackbox/rssexpanded-1/12.jpg";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_13() throws Exception {
    String expected = "(01)90012345678908(3922)795";
    String path = "test/data/blackbox/rssexpanded-1/13.png";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_14() throws Exception {
    String expected = "(01)90012345678908(3932)0401234";
    String path = "test/data/blackbox/rssexpanded-1/14.png";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_15() throws Exception {
    String expected = "(01)90012345678908(3102)001750(11)100312";
    String path = "test/data/blackbox/rssexpanded-1/15.jpg";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_16() throws Exception {
    String expected = "(01)90012345678908(3202)001750(11)100312";
    String path = "test/data/blackbox/rssexpanded-1/16.jpg";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_17() throws Exception {
    String expected = "(01)90012345678908(3102)001750(13)100312";
    String path = "test/data/blackbox/rssexpanded-1/17.jpg";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_18() throws Exception {
    String expected = "(01)90012345678908(3202)001750(13)100312";
    String path = "test/data/blackbox/rssexpanded-1/18.jpg";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_19() throws Exception {
    String expected = "(01)90012345678908(3102)001750(15)100312";
    String path = "test/data/blackbox/rssexpanded-1/19.jpg";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_20() throws Exception {
    String expected = "(01)90012345678908(3202)001750(15)100312";
    String path = "test/data/blackbox/rssexpanded-1/20.jpg";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_21() throws Exception {
    String expected = "(01)90012345678908(3102)001750(17)100312";
    String path = "test/data/blackbox/rssexpanded-1/21.jpg";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_22() throws Exception {
    String expected = "(01)90012345678908(3202)001750(17)100312";
    String path = "test/data/blackbox/rssexpanded-1/22.jpg";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_25() throws Exception {
    String expected = "(10)123";
    String path = "test/data/blackbox/rssexpanded-1/25.png";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_26() throws Exception {
    String expected = "(10)5678(11)010101";
    String path = "test/data/blackbox/rssexpanded-1/26.png";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_27() throws Exception {
    String expected = "(10)1098-1234";
    String path = "test/data/blackbox/rssexpanded-1/27.png";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_28() throws Exception {
    String expected = "(10)1098/1234";
    String path = "test/data/blackbox/rssexpanded-1/28.png";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_29() throws Exception {
    String expected = "(10)1098.1234";
    String path = "test/data/blackbox/rssexpanded-1/29.png";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_30() throws Exception {
    String expected = "(10)1098*1234";
    String path = "test/data/blackbox/rssexpanded-1/30.png";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_31() throws Exception {
    String expected = "(10)1098,1234";
    String path = "test/data/blackbox/rssexpanded-1/31.png";

    assertCorrectImage2string(path, expected);
  }

  @Test
  public void testDecodeRow2string_32() throws Exception {
    String expected = "(15)991231(3103)001750(10)12A(422)123(21)123456(423)0123456789012";
    String path = "test/data/blackbox/rssexpanded-1/32.png";

    assertCorrectImage2string(path, expected);
  }

  private static void assertCorrectImage2string(String path, String expected) throws IOException, NotFoundException {
    RSSExpandedReader rssExpandedReader = new RSSExpandedReader();

    BufferedImage image = ImageIO.read(new File(path));
    BinaryBitmap binaryMap = new BinaryBitmap(new GlobalHistogramBinarizer(new BufferedImageLuminanceSource(image)));
    int rowNumber = binaryMap.getHeight() / 2;
    BitArray row = binaryMap.getBlackRow(rowNumber, null);

    Result result = rssExpandedReader.decodeRow(rowNumber, row, new Hashtable());

    assertSame(BarcodeFormat.RSS_EXPANDED, result.getBarcodeFormat());
    assertEquals(expected, result.getText());
  }

}
