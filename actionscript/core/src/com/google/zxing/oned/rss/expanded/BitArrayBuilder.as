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

package com.google.zxing.oned.rss.expanded
{
	import com.google.zxing.common.BitArray;
	import com.google.zxing.common.flexdatatypes.ArrayList;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public class BitArrayBuilder {

  public function BitArrayBuilder() {
  }

  public static function buildBitArray(pairs:ArrayList):BitArray {
    var charNumber:int = (pairs.size() << 1) - 1;
    if ((pairs.lastElement() as ExpandedPair).getRightChar() == null) {
      charNumber -= 1;
    }

    var size:int = 12 * charNumber;

    var binary:BitArray = new BitArray(size);
    var accPos:int = 0;

    var firstPair:ExpandedPair =  pairs.elementAt(0) as ExpandedPair;
    var firstValue:int = firstPair.getRightChar().getValue();
    for(var i:int = 11; i >= 0; --i){
      if ((firstValue & (1 << i)) != 0) {
        binary._set(accPos);
      }
      accPos++;
    }

    for(i = 1; i < pairs.size(); ++i){
      var currentPair:ExpandedPair = pairs.elementAt(i) as ExpandedPair;

      var leftValue:int = currentPair.getLeftChar().getValue();
      for(var j:int = 11; j >= 0; --j){
        if ((leftValue & (1 << j)) != 0) {
          binary._set(accPos);
        }
        accPos++;
      }

      if(currentPair.getRightChar() != null){
        var rightValue:int = currentPair.getRightChar().getValue();
        for(j = 11; j >= 0; --j){
          if ((rightValue & (1 << j)) != 0) {
            binary._set(accPos);
          }
          accPos++;
        }
      }
    }
    return binary;
  }
}
}