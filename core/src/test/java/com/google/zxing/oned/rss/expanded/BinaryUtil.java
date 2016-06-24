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

import java.util.regex.Pattern;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 */
public final class BinaryUtil {

  private static final Pattern ONE = Pattern.compile("1");
  private static final Pattern ZERO = Pattern.compile("0");
  private static final Pattern SPACE = Pattern.compile(" ");

  private BinaryUtil() {
  }

  /*
  * Constructs a BitArray from a String like the one returned from BitArray.toString()
  */
  public static BitArray buildBitArrayFromString(CharSequence data) {
    CharSequence dotsAndXs = ZERO.matcher(ONE.matcher(data).replaceAll("X")).replaceAll(".");
    BitArray binary = new BitArray(SPACE.matcher(dotsAndXs).replaceAll("").length());
    int counter = 0;

    for (int i = 0; i < dotsAndXs.length(); ++i) {
      if (i % 9 == 0) { // spaces
        if (dotsAndXs.charAt(i) != ' ') {
          throw new IllegalStateException("space expected");
        }
        continue;
      }

      char currentChar = dotsAndXs.charAt(i);
      if (currentChar == 'X' || currentChar == 'x') {
        binary.set(counter);
      }
      counter++;
    }
    return binary;
  }

  public static BitArray buildBitArrayFromStringWithoutSpaces(CharSequence data) {
    StringBuilder sb = new StringBuilder();
    CharSequence dotsAndXs = ZERO.matcher(ONE.matcher(data).replaceAll("X")).replaceAll(".");
    int current = 0;
    while (current < dotsAndXs.length()) {
      sb.append(' ');
      for (int i = 0; i < 8 && current < dotsAndXs.length(); ++i) {
        sb.append(dotsAndXs.charAt(current));
        current++;
      }
    }
    return buildBitArrayFromString(sb.toString());
  }

}
