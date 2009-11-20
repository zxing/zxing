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