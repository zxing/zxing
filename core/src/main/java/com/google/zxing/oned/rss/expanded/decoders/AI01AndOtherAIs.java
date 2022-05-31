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

import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
final class AI01AndOtherAIs extends AI01decoder {

  private static final int HEADER_SIZE = 1 + 1 + 2; //first bit encodes the linkage flag,
                          //the second one is the encodation method, and the other two are for the variable length
  AI01AndOtherAIs(BitArray information) {
    super(information);
  }

  @Override
  public String parseInformation() throws NotFoundException, FormatException {
    StringBuilder buff = new StringBuilder();

    buff.append("(01)");
    int initialGtinPosition = buff.length();
    int firstGtinDigit = this.getGeneralDecoder().extractNumericValueFromBitArray(HEADER_SIZE, 4);
    buff.append(firstGtinDigit);

    this.encodeCompressedGtinWithoutAI(buff, HEADER_SIZE + 4, initialGtinPosition);

    return this.getGeneralDecoder().decodeAllCodes(buff, HEADER_SIZE + 44);
  }
}
