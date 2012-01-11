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
public class AI013x0x1xDecoder extends AI01weightDecoder {

  private static var HEADER_SIZE:int = 7 + 1;
  private static var WEIGHT_SIZE:int = 20;
  private static var DATE_SIZE:int = 16;

  private var dateCode:String;
  private var firstAIdigits:String;

  public function AI013x0x1xDecoder(information:BitArray, firstAIdigits:String, dateCode:String) {
    super(information);
    this.dateCode      = dateCode;
    this.firstAIdigits = firstAIdigits;
  }

  public override function parseInformation():String {
    if (this.information.Size != HEADER_SIZE + GTIN_SIZE + WEIGHT_SIZE + DATE_SIZE) {
      throw NotFoundException.getNotFoundInstance();
    }

    var buf:StringBuilder = new StringBuilder();

    encodeCompressedGtin(buf, HEADER_SIZE);
    encodeCompressedWeight(buf, HEADER_SIZE + GTIN_SIZE, WEIGHT_SIZE);
    encodeCompressedDate(buf, HEADER_SIZE + GTIN_SIZE + WEIGHT_SIZE);

    return buf.toString();
  }

  private function encodeCompressedDate(buf:StringBuilder, currentPos:int):void {
    var numericDate:int = this.generalDecoder.extractNumericValueFromBitArray2(currentPos, DATE_SIZE);
    if(numericDate == 38400) {
      return;
    }

    buf.Append('(');
    buf.Append(this.dateCode);
    buf.Append(')');

    var day:int   = numericDate % 32;
    numericDate = int(numericDate / 32);
    var month:int = numericDate % 12 + 1;
    numericDate = int(numericDate / 12);
    var year:int  = numericDate;

    if (int(year / 10) == 0) {
      buf.Append('0');
    }
    buf.Append(year);
    if (int(month / 10) == 0) {
      buf.Append('0');
    }
    buf.Append(month);
    if (day / 10 == 0) {
      buf.Append('0');
    }
    buf.Append(day);
  }

  protected override function addWeightCode(buf:StringBuilder, weight:int):void {
    var lastAI:int = int(weight / 100000);
    buf.Append('(');
    buf.Append(this.firstAIdigits);
    buf.Append(lastAI);
    buf.Append(')');
  }

  protected override function checkWeight(weight:int):int {
    return weight % 100000;
  }
}
}