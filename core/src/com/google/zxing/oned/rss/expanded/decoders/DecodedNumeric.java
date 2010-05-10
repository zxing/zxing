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

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
final class DecodedNumeric extends DecodedObject {

  private final int firstDigit;
  private final int secondDigit;

  static final int FNC1 = 10;

  DecodedNumeric(int newPosition, int firstDigit, int secondDigit){
    super(newPosition);

    this.firstDigit  = firstDigit;
    this.secondDigit = secondDigit;

    if (this.firstDigit < 0 || this.firstDigit > 10) {
      throw new IllegalArgumentException("Invalid firstDigit: " + firstDigit);
    }

    if (this.secondDigit < 0 || this.secondDigit > 10) {
      throw new IllegalArgumentException("Invalid secondDigit: " + secondDigit);
    }
  }

  int getFirstDigit(){
    return this.firstDigit;
  }

  int getSecondDigit(){
    return this.secondDigit;
  }

  int getValue(){
    return this.firstDigit * 10 + this.secondDigit;
  }

  boolean isFirstDigitFNC1(){
    return this.firstDigit == FNC1;
  }

  boolean isSecondDigitFNC1(){
    return this.secondDigit == FNC1;
  }

  boolean isAnyFNC1(){
    return this.firstDigit == FNC1 || this.secondDigit == FNC1;
  }

}
