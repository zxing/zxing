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

import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
final class AI013x0x1xDecoder extends AI01weightDecoder {

  private static final int headerSize = 7 + 1;
  private static final int weightSize = 20;
  private static final int dateSize   = 16;

  private final String dateCode;
  private final String firstAIdigits;

  AI013x0x1xDecoder(BitArray information, String firstAIdigits, String dateCode) {
    super(information);
    this.dateCode      = dateCode;
    this.firstAIdigits = firstAIdigits;
  }

  public String parseInformation() throws NotFoundException {
    if (this.information.size != headerSize + gtinSize + weightSize + dateSize) {
      throw NotFoundException.getNotFoundInstance();
    }

    StringBuffer buf = new StringBuffer();

    encodeCompressedGtin(buf, headerSize);
    encodeCompressedWeight(buf, headerSize + gtinSize, weightSize);
    encodeCompressedDate(buf, headerSize + gtinSize + weightSize);

    return buf.toString();
  }

  private void encodeCompressedDate(StringBuffer buf, int currentPos) {
    int numericDate = this.generalDecoder.extractNumericValueFromBitArray(currentPos, dateSize);
    if(numericDate == 38400) {
      return;
    }

    buf.append('(');
    buf.append(this.dateCode);
    buf.append(')');

    int day   = numericDate % 32;
    numericDate /= 32;
    int month = numericDate % 12 + 1;
    numericDate /= 12;
    int year  = numericDate;

    if (year / 10 == 0) {
      buf.append('0');
    }
    buf.append(year);
    if (month / 10 == 0) {
      buf.append('0');
    }
    buf.append(month);
    if (day / 10 == 0) {
      buf.append('0');
    }
    buf.append(day);
  }

  protected void addWeightCode(StringBuffer buf, int weight) {
    int lastAI = weight / 100000;
    buf.append('(');
    buf.append(this.firstAIdigits);
    buf.append(lastAI);
    buf.append(')');
  }

  protected int checkWeight(int weight) {
    return weight % 100000;
  }
}
