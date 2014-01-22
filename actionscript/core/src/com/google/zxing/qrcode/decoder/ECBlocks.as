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
	      public class ECBlocks 
          {
          	
            protected var  ecCodewordsPerBlock:int;
            protected var  ecBlocks:Array;

            //public function ECBlocks(ecCodewordsPerBlock, ECB ecBlocks) 
            //{
            //  this.ecCodewordsPerBlock = ecCodewordsPerBlock;
            //  this.ecBlocks = new ECBArray([ecBlocks]);
            //}

            public function ECBlocks(ecCodewordsPerBlock:int, ecBlocks1:ECB, ecBlocks2:ECB=null)
            {
              this.ecCodewordsPerBlock = ecCodewordsPerBlock;
              if (ecBlocks2 != null)
              {
              	this.ecBlocks = [ecBlocks1, ecBlocks2];
              }
              else
              {
              	this.ecBlocks = [ecBlocks1];
              }
            }

            public function getECCodewordsPerBlock():int 
            {
              return ecCodewordsPerBlock;
            }

            public function getNumBlocks():int 
            {
              var total:int = 0;
              for (var i:int = 0; i < ecBlocks.length; i++) 
              {
                total += ecBlocks[i].getCount();
              }
              return total;
            }

            public function getTotalECCodewords():int 
            {
              return ecCodewordsPerBlock * getNumBlocks();
            }

            public function getECBlocks():Array 
            {
              return ecBlocks;
            }
          }

}