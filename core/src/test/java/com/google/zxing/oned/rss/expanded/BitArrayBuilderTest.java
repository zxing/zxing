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

import com.google.zxing.common.BitArray;
import com.google.zxing.oned.rss.DataCharacter;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public final class BitArrayBuilderTest extends Assert {

  @Test
  public void testBuildBitArray1() {
    int[][] pairValues = {{19}, {673, 16}};

    String expected = " .......X ..XX..X. X.X....X .......X ....";

    checkBinary(pairValues, expected);
  }

  private static void checkBinary(int[][] pairValues, String expected) {
    BitArray binary = buildBitArray(pairValues);
    assertEquals(expected, binary.toString());
  }

  private static BitArray buildBitArray(int[][] pairValues) {
    List<ExpandedPair> pairs = new ArrayList<>();
    for (int i = 0; i < pairValues.length; ++i) {
      int [] pair = pairValues[i];

      DataCharacter leftChar;
      if (i == 0) {
        leftChar = null;
      } else {
        leftChar = new DataCharacter(pair[0], 0);
      }

      DataCharacter rightChar;
      if (i == 0) {
        rightChar = new DataCharacter(pair[0], 0);
      } else if (pair.length == 2) {
        rightChar = new DataCharacter(pair[1], 0);
      } else {
        rightChar = null;
      }

      ExpandedPair expandedPair = new ExpandedPair(leftChar, rightChar, null);
      pairs.add(expandedPair);
    }

    return BitArrayBuilder.buildBitArray(pairs);
  }
}
