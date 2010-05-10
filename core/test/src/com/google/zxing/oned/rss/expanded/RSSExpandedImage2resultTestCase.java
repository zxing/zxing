/**
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
 *
 * This software consists of contributions made by many individuals,
 * listed below:
 *
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 *
 * These authors would like to acknowledge the Spanish Ministry of Industry,
 * Tourism and Trade, for the support in the project TSI020301-2008-2
 * "PIRAmIDE: Personalizable Interactions with Resources on AmI-enabled
 * Mobile Dynamic Environments", leaded by Treelogic
 * ( http://www.treelogic.com/ ):
 *
 *   http://www.piramidepse.com/
 *
 */

package com.google.zxing.oned.rss.expanded;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.result.ExpandedProductParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.GlobalHistogramBinarizer;

import junit.framework.TestCase;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public final class RSSExpandedImage2resultTestCase extends TestCase {

  public void testDecodeRow2result_2() throws Exception{
    // (01)90012345678908(3103)001750
    String path = "test/data/blackbox/rssexpanded-1/2.jpg";
    ExpandedProductParsedResult expected = new ExpandedProductParsedResult("90012345678908", "-", "-", "-", "-", "-", "-", "001750", ExpandedProductParsedResult.KILOGRAM, "3", "-", "-", "-", new Hashtable());

    assertCorrectImage2result(path, expected);
  }

  private static void assertCorrectImage2result(String path, ExpandedProductParsedResult expected) throws Exception {
    RSSExpandedReader rssExpandedReader = new RSSExpandedReader();

    BufferedImage image = ImageIO.read(new File(path));
    BinaryBitmap binaryMap = new BinaryBitmap(new GlobalHistogramBinarizer(new BufferedImageLuminanceSource(image)));
    int rowNumber = binaryMap.getHeight() / 2;
    BitArray row = binaryMap.getBlackRow(rowNumber, null);

    Result theResult = rssExpandedReader.decodeRow(rowNumber, row, new Hashtable());

    assertEquals(BarcodeFormat.RSS_EXPANDED, theResult.getBarcodeFormat());

    ParsedResult result = ResultParser.parseResult(theResult);

    assertEquals(expected, result);
  }

}
