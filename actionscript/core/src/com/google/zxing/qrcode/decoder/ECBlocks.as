package com.google.zxing.qrcode.decoder
{
	      public class ECBlocks 
          {
          	
          	// bas : made public for debugging
            public var  ecCodewordsPerBlock:int;
            public var  ecBlocks:Array;

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