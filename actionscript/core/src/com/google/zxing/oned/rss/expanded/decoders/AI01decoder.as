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
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public class AI01decoder extends AbstractExpandedDecoder {

  protected static var GTIN_SIZE:int = 40;

  public function AI01decoder(information:BitArray) {
    super(information);
  }

  protected function encodeCompressedGtin(buf:StringBuilder, currentPos:int):void {
    buf.Append("(01)");
    var initialPosition:int = buf.length;
    buf.Append('9');

    encodeCompressedGtinWithoutAI(buf, currentPos, initialPosition);
  }

  protected function encodeCompressedGtinWithoutAI(buf:StringBuilder , currentPos:int, initialBufferPosition:int):void {
    for(var i:int = 0; i < 4; ++i){
      var currentBlock:int = this.generalDecoder.extractNumericValueFromBitArray2(currentPos + 10 * i, 10);
      if (int(currentBlock / 100) == 0) {
        buf.Append('0');
      }
      if (int(currentBlock / 10) == 0) {
        buf.Append('0');
      }
      buf.Append(currentBlock);
    }

      appendCheckDigit(buf, initialBufferPosition);
  }

  private static function appendCheckDigit(buf:StringBuilder, currentPos:int):void{
    var checkDigit:int = 0;
    for (var i:int = 0; i < 13; i++) {
      var digit:int = (buf.charAt(i + currentPos)).charCodeAt(0) - ('0' as String).charCodeAt(0);
      checkDigit += (i & 0x01) == 0 ? 3 * digit : digit;
    }

    checkDigit = 10 - (checkDigit % 10);
    if (checkDigit == 10) {
      checkDigit = 0;
    }

    buf.Append(checkDigit);
  }

}
}