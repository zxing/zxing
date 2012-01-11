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

import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.flexdatatypes.StringBuilder;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public class AI01AndOtherAIs extends AI01decoder {

  private static var HEADER_SIZE:int = 1 + 1 + 2; //first bit encodes the linkage flag,
                          //the second one is the encodation method, and the other two are for the variable length
  public function AI01AndOtherAIs(information:BitArray) {
    super(information);
  }

  public override function parseInformation():String {
    var buff:StringBuilder  = new StringBuilder();

    buff.Append("(01)");
    var initialGtinPosition:int = buff.length;
    var firstGtinDigit:int = this.generalDecoder.extractNumericValueFromBitArray2(HEADER_SIZE, 4);
    buff.Append(firstGtinDigit);

    this.encodeCompressedGtinWithoutAI(buff, HEADER_SIZE + 4, initialGtinPosition);

    return this.generalDecoder.decodeAllCodes(buff, HEADER_SIZE + 44);
  }
}
}