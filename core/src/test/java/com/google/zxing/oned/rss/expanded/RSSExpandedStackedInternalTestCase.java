/*
 * Copyright (C) 2012 ZXing authors
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

import java.util.List;

import com.google.zxing.oned.OneDReader;
import org.junit.Assert;
import org.junit.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.BitArray;

public final class RSSExpandedStackedInternalTestCase extends Assert {

  @Test
  public void testDecodingRowByRow() throws Exception {
    RSSExpandedReader rssExpandedReader = new RSSExpandedReader();

    BinaryBitmap binaryMap = TestCaseUtil.getBinaryBitmap("src/test/resources/blackbox/rssexpandedstacked-2/1000.png");

    int firstRowNumber = binaryMap.getHeight() / 3;
    BitArray firstRow = binaryMap.getBlackRow(firstRowNumber, null);
    try {
      rssExpandedReader.decodeRow2pairs(firstRowNumber, firstRow);
      fail(NotFoundException.class.getName() + " expected");
    } catch (NotFoundException nfe) {
      // ok
    }

    assertEquals(1, rssExpandedReader.getRows().size());
    ExpandedRow firstExpandedRow = rssExpandedReader.getRows().get(0);
    assertEquals(firstRowNumber, firstExpandedRow.getRowNumber());

    assertEquals(2, firstExpandedRow.getPairs().size());

    firstExpandedRow.getPairs().get(1).getFinderPattern().getStartEnd()[1] = 0;

    int secondRowNumber = 2 * binaryMap.getHeight() / 3;
    BitArray secondRow = binaryMap.getBlackRow(secondRowNumber, null);
    secondRow.reverse();

    List<ExpandedPair> totalPairs = rssExpandedReader.decodeRow2pairs(secondRowNumber, secondRow);

    Result result = RSSExpandedReader.constructResult(totalPairs);
    assertEquals("(01)98898765432106(3202)012345(15)991231", result.getText());
  }

  @Test
  public void testCompleteDecode() throws Exception {
    OneDReader rssExpandedReader = new RSSExpandedReader();

    BinaryBitmap binaryMap = TestCaseUtil.getBinaryBitmap("src/test/resources/blackbox/rssexpandedstacked-2/1000.png");

    Result result = rssExpandedReader.decode(binaryMap);
    assertEquals("(01)98898765432106(3202)012345(15)991231", result.getText());
  }


}
