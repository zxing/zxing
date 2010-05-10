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
 */
final class CurrentParsingState {

  int position;
  private int encoding;

  private static final int NUMERIC     = 1;
  private static final int ALPHA       = 2;
  private static final int ISO_IEC_646 = 4;

  CurrentParsingState(){
    this.position = 0;
    this.encoding = NUMERIC;
  }

  boolean isAlpha(){
    return this.encoding == ALPHA;
  }

  boolean isNumeric(){
    return this.encoding == NUMERIC;
  }

  boolean isIsoIec646(){
    return this.encoding == ISO_IEC_646;
  }

  void setNumeric(){
    this.encoding = NUMERIC;
  }

  void setAlpha(){
    this.encoding = ALPHA;
  }

  void setIsoIec646(){
    this.encoding = ISO_IEC_646;
  }
}
