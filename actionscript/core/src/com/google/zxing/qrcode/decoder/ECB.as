/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.qrcode.decoder
{
	          /**
           * <p>Encapsualtes the parameters for one error-correction block in one symbol version.
           * This includes the number of data codewords, and the number of times a block with these
           * parameters is used consecutively in the QR code version's format.</p>
           */
          public class ECB 
          {
            protected var count:int;
            protected var dataCodewords:int;

            public function ECB(count:int, dataCodewords:int) 
            {
              this.count = count;
              this.dataCodewords = dataCodewords;
            }

            public function getCount():int 
            {
              return count;
            }

            public function getDataCodewords():int 
            {
              return dataCodewords;
            }
          }

}