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

package com.google.zxing.oned.rss.expanded
{

import com.google.zxing.oned.rss.DataCharacter;
import com.google.zxing.oned.rss.FinderPattern;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 */
public class ExpandedPair {

  protected var _mayBeLast:Boolean;
  protected var leftChar:DataCharacter;
  protected var rightChar:DataCharacter;
  protected var finderPattern:FinderPattern;

  public function ExpandedPair(leftChar:DataCharacter, rightChar:DataCharacter, finderPattern:FinderPattern, mayBeLast:Boolean) 
  {
    this.leftChar      = leftChar;
    this.rightChar     = rightChar;
    this.finderPattern = finderPattern;
    this._mayBeLast     = mayBeLast;
  }

  public function mayBeLast():Boolean{
    return this._mayBeLast;
  }

  public function getLeftChar():DataCharacter {
    return this.leftChar;
  }

  public function getRightChar():DataCharacter {
    return this.rightChar;
  }

  public function getFinderPattern():FinderPattern {
    return this.finderPattern;
  }

  public function mustBeLast():Boolean {
    return this.rightChar == null;
  }
}
}