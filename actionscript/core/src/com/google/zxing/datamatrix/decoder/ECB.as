package com.google.zxing.datamatrix.decoder
{
          /**
           * <p>Encapsualtes the parameters for one error-correction block in one symbol version.
           * This includes the number of data codewords, and the number of times a block with these
           * parameters is used consecutively in the Data Matrix code version's format.</p>
           */
          public class ECB {
            private  var count:int;
            private  var dataCodewords:int;

            public function ECB(count:int,  dataCodewords:int) {
              this.count = count;
              this.dataCodewords = dataCodewords;
            }

            public function getCount():int {
              return count;
            }

            public function getDataCodewords():int {
              return dataCodewords;
            }
          }


}