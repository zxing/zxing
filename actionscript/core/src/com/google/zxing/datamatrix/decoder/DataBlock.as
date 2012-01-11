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
package com.google.zxing.datamatrix.decoder
{
	    /**
     * <p>Encapsulates a block of data within a Data Matrix Code. Data Matrix Codes may split their data into
     * multiple blocks, each of which is a unit of data and error-correction codewords. Each
     * is represented by an instance of this class.</p>
     *
     * @author bbrown@google.com (Brian Brown)
     */
    public class DataBlock
    {
    	import com.google.zxing.datamatrix.decoder.ECB;
    	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    	
          private  var numDataCodewords:int;
          private  var codewords:Array;

          public function DataBlock(numDataCodewords:int, codewords:Array) {
            this.numDataCodewords = numDataCodewords;
            this.codewords = codewords;
          }

          /**
           * <p>When Data Matrix Codes use multiple data blocks, they actually interleave the bytes of each of them.
           * That is, the first byte of data block 1 to n is written, then the second bytes, and so on. This
           * method will separate the data into original blocks.</p>
           *
           * @param rawCodewords bytes as read directly from the Data Matrix Code
           * @param version version of the Data Matrix Code
           * @return {@link DataBlock}s containing original bytes, "de-interleaved" from representation in the
           *         Data Matrix Code
           */
          public static function getDataBlocks(rawCodewords:Array,
                                           version:Version):Array {
            // Figure out the number and size of data blocks used by this version
            var ecBlocks:ECBlocks  = version.getECBlocks();

            // First count the total number of data blocks
            var totalBlocks:int = 0;
            var ecBlockArray:Array = ecBlocks.getECBlocks();
            for (var i:int = 0; i < ecBlockArray.length; i++) {
              totalBlocks += ecBlockArray[i].getCount();
            }
            // Now establish DataBlocks of the appropriate size and number of data codewords
            var result:Array = new Array(totalBlocks);
            var numResultBlocks:int = 0;
            for (var j:int = 0; j < ecBlockArray.length; j++) {
              var ecBlock:ECB = ecBlockArray[j];
              for (var i3:int = 0; i3 < ecBlock.getCount(); i3++) {
                var numDataCodewords:int = ecBlock.getDataCodewords();
                var numBlockCodewords:int = ecBlocks.getECCodewords() + numDataCodewords;
                result[numResultBlocks++] = new DataBlock(numDataCodewords, new Array(numBlockCodewords));
              }
            }

            // All blocks have the same amount of data, except that the last n
            // (where n may be 0) have 1 less byte. Figure out where these start.
            // TODO(bbrown): There is only one case where there is a difference for Data Matrix for size 144
            var longerBlocksTotalCodewords:int = result[0].codewords.length;
            //int shorterBlocksTotalCodewords = longerBlocksTotalCodewords - 1;

            var longerBlocksNumDataCodewords:int = longerBlocksTotalCodewords - ecBlocks.getECCodewords();
            var shorterBlocksNumDataCodewords:int = longerBlocksNumDataCodewords - 1;
            // The last elements of result may be 1 element shorter for 144 matrix
            // first fill out as many elements as all of them have minus 1
            var rawCodewordsOffset:int = 0;
            for (var i2:int = 0; i2 < shorterBlocksNumDataCodewords; i2++) {
              for (var j2:int = 0; j2 < numResultBlocks; j2++) 
              {
                result[j2].codewords[i2] = rawCodewords[rawCodewordsOffset++];
              }
            }
            
            // Fill out the last data block in the longer ones
            var specialVersion:Boolean = version.getVersionNumber() == 24;
            var numLongerBlocks:int = specialVersion ? 8 : numResultBlocks;
            for (var j3:int = 0; j3 < numLongerBlocks; j3++) {
              result[j3].codewords[longerBlocksNumDataCodewords - 1] = rawCodewords[rawCodewordsOffset++];
            }
            
            // Now add in error correction blocks
            var max:int = result[0].codewords.length;
            for (var i4:int = longerBlocksNumDataCodewords; i4 < max; i4++) {
              for (var j4:int = 0; j4 < numResultBlocks; j4++) {              	
                var iOffset:int = (specialVersion && j4 > 7) ? i4 - 1 : i4;
                result[j4].codewords[iOffset] = rawCodewords[rawCodewordsOffset++];
              }
            }

            if (rawCodewordsOffset != rawCodewords.length) {
              throw new IllegalArgumentException("DataBlock : getDataBlocks : rawCodewordsOffset != rawCodewords.length : "+rawCodewordsOffset +" - "+rawCodewords.length);
            }

            return result;
          }

          public function getNumDataCodewords():int {
            return numDataCodewords;
          }

          public function getCodewords():Array {
            return codewords;
          }
    }

}