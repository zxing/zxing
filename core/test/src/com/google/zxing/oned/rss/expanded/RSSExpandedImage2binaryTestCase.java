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
import java.util.Vector;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.GlobalHistogramBinarizer;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public final class RSSExpandedImage2binaryTestCase extends TestCase {

  public void testDecodeRow2binary_1() throws Exception{
    // (11)100224(17)110224(3102)000100
    String path = "test/data/blackbox/rssexpanded-1/1.jpg";
    String expected = " ...X...X .X....X. .XX...X. X..X...X ...XX.X. ..X.X... ..X.X..X ...X..X. X.X....X .X....X. .....X.. X...X...";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_2() throws Exception{
    // (01)90012345678908(3103)001750
    String path = "test/data/blackbox/rssexpanded-1/2.jpg";
    String expected = " ..X..... ......X. .XXX.X.X .X...XX. XXXXX.XX XX.X.... .XX.XX.X .XX.";

    assertCorrectImage2binary(path, expected);

  }

  public void testDecodeRow2binary_3() throws Exception{
    // (10)12A
    String path = "test/data/blackbox/rssexpanded-1/3.jpg";
    String expected = " .......X ..XX..X. X.X....X .......X ....";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_4() throws Exception{
    // (01)98898765432106(3202)012345(15)991231
    String path = "test/data/blackbox/rssexpanded-1/4.jpg";
    String expected = " ..XXXX.X XX.XXXX. .XXX.XX. XX..X... .XXXXX.. XX.X..X. ..XX..XX XX.X.XXX X..XX..X .X.XXXXX XXXX";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_5() throws Exception{
    // (01)90614141000015(3202)000150
    String path = "test/data/blackbox/rssexpanded-1/5.jpg";
    String expected = " ..X.X... .XXXX.X. XX..XXXX ....XX.. X....... ....X... ....X..X .XX.";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_10() throws Exception{
    // (01)98898765432106(15)991231(3103)001750(10)12A(422)123(21)123456(423)0123456789012
    String path = "test/data/blackbox/rssexpanded-1/10.png";
    String expected = " .X.XX..X XX.XXXX. .XXX.XX. XX..X... .XXXXX.. XX.X..X. ..XX...X XX.X.... X.X.X.X. X.X..X.X .X....X. XX...X.. ...XX.X. .XXXXXX. .X..XX.. X.X.X... .X...... XXXX.... XX.XX... XXXXX.X. ...XXXXX .....X.X ...X.... X.XXX..X X.X.X... XX.XX..X .X..X..X .X.X.X.X X.XX...X .XX.XXX. XXX.X.XX ..X.";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_11() throws Exception{
    // (01)98898765432106(15)991231(3103)001750(10)12A(422)123(21)123456
    String expected = " .X.XX..X XX.XXXX. .XXX.XX. XX..X... .XXXXX.. XX.X..X. ..XX...X XX.X.... X.X.X.X. X.X..X.X .X....X. XX...X.. ...XX.X. .XXXXXX. .X..XX.. X.X.X... .X...... XXXX.... XX.XX... XXXXX.X. ...XXXXX .....X.X ...X.... X.XXX..X X.X.X... ....";
    String path = "test/data/blackbox/rssexpanded-1/11.png";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_12() throws Exception{
    // (01)98898765432106(3103)001750

    String expected = " ..X..XX. XXXX..XX X.XX.XX. .X....XX XXX..XX. X..X.... .XX.XX.X .XX.";
    String path = "test/data/blackbox/rssexpanded-1/12.jpg";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_13() throws Exception{
    // (01)90012345678908(3922)795

    String expected = " ..XX..X. ........ .X..XXX. X.X.X... XX.XXXXX .XXXX.X. X.X.XXXX .X..X..X ......X.";
    String path = "test/data/blackbox/rssexpanded-1/13.png";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_14() throws Exception{
    // (01)90012345678908(3932)0401234

    String expected = " ..XX.X.. ........ .X..XXX. X.X.X... XX.XXXXX .XXXX.X. X.....X. X.....X. X.X.X.XX .X...... X...";
    String path = "test/data/blackbox/rssexpanded-1/14.png";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_15() throws Exception{
    // (01)90012345678908(3102)001750(11)100312

    String expected = " ..XXX... ........ .X..XXX. X.X.X... XX.XXXXX .XXXX.X. ..XX...X .X.....X .XX..... XXXX.X.. XX..";
    String path = "test/data/blackbox/rssexpanded-1/15.jpg";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_16() throws Exception{
    // (01)90012345678908(3202)001750(11)100312

    String expected = " ..XXX..X ........ .X..XXX. X.X.X... XX.XXXXX .XXXX.X. ..XX...X .X.....X .XX..... XXXX.X.. XX..";
    String path = "test/data/blackbox/rssexpanded-1/16.jpg";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_17() throws Exception{
    // (01)90012345678908(3102)001750(13)100312

    String expected = " ..XXX.X. ........ .X..XXX. X.X.X... XX.XXXXX .XXXX.X. ..XX...X .X.....X .XX..... XXXX.X.. XX..";
    String path = "test/data/blackbox/rssexpanded-1/17.jpg";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_18() throws Exception{
    // (01)90012345678908(3202)001750(13)100312

    String expected = " ..XXX.XX ........ .X..XXX. X.X.X... XX.XXXXX .XXXX.X. ..XX...X .X.....X .XX..... XXXX.X.. XX..";
    String path = "test/data/blackbox/rssexpanded-1/18.jpg";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_19() throws Exception{
    // (01)90012345678908(3102)001750(15)100312

    String expected = " ..XXXX.. ........ .X..XXX. X.X.X... XX.XXXXX .XXXX.X. ..XX...X .X.....X .XX..... XXXX.X.. XX..";
    String path = "test/data/blackbox/rssexpanded-1/19.jpg";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_20() throws Exception{
    // (01)90012345678908(3202)001750(15)100312

    String expected = " ..XXXX.X ........ .X..XXX. X.X.X... XX.XXXXX .XXXX.X. ..XX...X .X.....X .XX..... XXXX.X.. XX..";
    String path = "test/data/blackbox/rssexpanded-1/20.jpg";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_21() throws Exception{
    // (01)90012345678908(3102)001750(17)100312

    String expected = " ..XXXXX. ........ .X..XXX. X.X.X... XX.XXXXX .XXXX.X. ..XX...X .X.....X .XX..... XXXX.X.. XX..";
    String path = "test/data/blackbox/rssexpanded-1/21.jpg";

    assertCorrectImage2binary(path, expected);
  }

  public void testDecodeRow2binary_22() throws Exception{
    // (01)90012345678908(3202)001750(17)100312

    String expected = " ..XXXXXX ........ .X..XXX. X.X.X... XX.XXXXX .XXXX.X. ..XX...X .X.....X .XX..... XXXX.X.. XX..";
    String path = "test/data/blackbox/rssexpanded-1/22.jpg";

    assertCorrectImage2binary(path, expected);
  }

  private static void assertCorrectImage2binary(String path,
      String expected) throws IOException, NotFoundException {
    RSSExpandedReader rssExpandedReader = new RSSExpandedReader();

    BufferedImage image = ImageIO.read(new File(path));
    BinaryBitmap binaryMap = new BinaryBitmap(new GlobalHistogramBinarizer(new BufferedImageLuminanceSource(image)));
    int rowNumber = binaryMap.getHeight() / 2;
    BitArray row = binaryMap.getBlackRow(rowNumber, null);

    Vector pairs = rssExpandedReader.decodeRow2pairs(rowNumber, row);
    BitArray binary = BitArrayBuilder.buildBitArray(pairs);
    assertEquals(expected, binary.toString());
  }
}
