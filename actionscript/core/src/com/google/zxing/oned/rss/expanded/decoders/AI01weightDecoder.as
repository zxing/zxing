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

package com.google.zxing.oned.rss.expanded.decoders
{

import com.google.zxing.common.BitArray;
import com.google.zxing.common.flexdatatypes.StringBuilder;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 */
public class AI01weightDecoder extends AI01decoder {

  public function AI01weightDecoder(information:BitArray) {
    super(information);
  }

  protected function encodeCompressedWeight(buf:StringBuilder, currentPos:int, weightSize:int):void {
    var originalWeightNumeric:int = this.generalDecoder.extractNumericValueFromBitArray2(currentPos, weightSize);
    addWeightCode(buf, originalWeightNumeric);

    var weightNumeric:int = checkWeight(originalWeightNumeric);

    var currentDivisor:int = 100000;
    for(var i:int = 0; i < 5; ++i){
      if (int(weightNumeric / currentDivisor) == 0) {
        buf.Append('0');
      }
      currentDivisor = int(currentDivisor / 10);
    }
    buf.Append(weightNumeric);
  }

  protected function addWeightCode(buf:StringBuilder, weight:int):void{}
  protected function checkWeight(weight:int):int{ return 0;}
}
}