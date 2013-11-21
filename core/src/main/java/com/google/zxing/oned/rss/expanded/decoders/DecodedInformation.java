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
final class DecodedInformation extends DecodedObject {

  private final String newString;
  private final int remainingValue;
  private final boolean remaining;

  DecodedInformation(int newPosition, String newString){
    super(newPosition);
    this.newString = newString;
    this.remaining = false;
    this.remainingValue = 0;
  }

  DecodedInformation(int newPosition, String newString, int remainingValue){
    super(newPosition);
    this.remaining = true;
    this.remainingValue = remainingValue;
    this.newString = newString;
  }

  String getNewString(){
    return this.newString;
  }

  boolean isRemaining(){
    return this.remaining;
  }

  int getRemainingValue(){
    return this.remainingValue;
  }
}
