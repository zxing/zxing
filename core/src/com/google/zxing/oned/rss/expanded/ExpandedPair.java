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

import com.google.zxing.oned.rss.DataCharacter;
import com.google.zxing.oned.rss.FinderPattern;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 */
final class ExpandedPair {

  private final boolean mayBeLast;
  private final DataCharacter leftChar;
  private final DataCharacter rightChar;
  private final FinderPattern finderPattern;

  ExpandedPair(DataCharacter leftChar,
               DataCharacter rightChar,
               FinderPattern finderPattern,
               boolean mayBeLast) {
    this.leftChar = leftChar;
    this.rightChar = rightChar;
    this.finderPattern = finderPattern;
    this.mayBeLast = mayBeLast;
  }

  boolean mayBeLast(){
    return this.mayBeLast;
  }

  DataCharacter getLeftChar() {
    return this.leftChar;
  }

  DataCharacter getRightChar() {
    return this.rightChar;
  }

  FinderPattern getFinderPattern() {
    return this.finderPattern;
  }

  public boolean mustBeLast() {
    return this.rightChar == null;
  }
  
  @Override
  public String toString() {
    return
        "[ " + leftChar + " , " + rightChar + " : " +
        (finderPattern == null ? "null" : finderPattern.getValue()) + " ]";
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ExpandedPair)) {
      return false;
    }
    ExpandedPair that = (ExpandedPair) o;
    return
        equalsOrNull(leftChar, that.leftChar) &&
        equalsOrNull(rightChar, that.rightChar) &&
        equalsOrNull(finderPattern, that.finderPattern);
  }

  private static boolean equalsOrNull(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

  @Override
  public int hashCode() {
    return hashNotNull(leftChar) ^ hashNotNull(rightChar) ^ hashNotNull(finderPattern);
  }

  private static int hashNotNull(Object o) {
    return o == null ? 0 : o.hashCode();
  }

}
