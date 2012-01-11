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

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 */
public class CurrentParsingState {

  public var position:int;
  protected var encoding:int;

  public static var NUMERIC:int     = 1;
  public static var ALPHA:int       = 2;
  static public var ISO_IEC_646:int = 4;

  public function CurrentParsingState(){
    this.position = 0;
    this.encoding = NUMERIC;
  }

  public function  isAlpha():Boolean{
    return this.encoding == ALPHA;
  }

  public function  isNumeric():Boolean{
    return this.encoding == NUMERIC;
  }

  public function  isIsoIec646():Boolean{
    return this.encoding == ISO_IEC_646;
  }

  public function  setNumeric():void{
    this.encoding = NUMERIC;
  }

  public function  setAlpha():void{
    this.encoding = ALPHA;
  }

  public function  setIsoIec646():void{
    this.encoding = ISO_IEC_646;
  }
}
}