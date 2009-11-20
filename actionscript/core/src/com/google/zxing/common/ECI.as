/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.common
{
/**
 * Superclass of classes encapsulating types ECIs, according to "Extended Channel Interpretations"
 * 5.3 of ISO 18004.
 *
 * @author Sean Owen
 */
    public  class ECI
    { 
    	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    	
          private var value:int;
        
          public function ECI( value:int) {
            this.value = value;
          }

          public function getValue():int {
            return value;
          }

           /**
   * @param value ECI value
   * @return {@link ECI} representing ECI of given value, or null if it is legal but unsupported
   * @throws IllegalArgumentException if ECI value is invalid
   */

          public static function getECIByValue(value:int ):ECI {
            if (value < 0 || value > 999999) {
              throw new IllegalArgumentException("Bad ECI value: " + value);
            }
            if (value < 900) { // Character set ECIs use 000000 - 000899
              return CharacterSetECI.getCharacterSetECIByValue(value);
            }
            return null;
          }

    }
}