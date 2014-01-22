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

package com.google.zxing.datamatrix.decoder
{
          /**
           * <p>Encapsulates a set of error-correction blocks in one symbol version. Most versions will
           * use blocks of differing sizes within one version, so, this encapsulates the parameters for
           * each set of blocks. It also holds the number of error-correction codewords per block since it
           * will be the same across all blocks within one version.</p>
           */
          public  class ECBlocks 
          {
          	
            private  var ecCodewords:int;
            private  var ecBlocks:Array;

            public function ECBlocks(ecCodewords:int, ecBlocks:ECB, ecBlocks2:ECB=null) 
            {
           		this.ecCodewords = ecCodewords;

            	if (ecBlocks2 == null)
            	{
              		this.ecBlocks = [ecBlocks];
             	}
             	else
             	{
              		this.ecBlocks = [ecBlocks, ecBlocks2];
             	}
            }

            public function getECCodewords():int {
              return ecCodewords;
            }

            public function getECBlocks():Array {
              return ecBlocks;
            }
          }


}