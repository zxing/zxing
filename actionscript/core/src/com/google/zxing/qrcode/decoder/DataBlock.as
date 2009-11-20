/*
 * Copyright 2007 ZXing authors
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
	public class DataBlock
	{
		import com.google.zxing.common.zxingByteArray;
		
		//debug
		public function get NumDataCodewords():int
        {
            return numDataCodewords;
        }
        
        public function get Codewords():Array
        {
            return codewords;
        }

        //UPGRADE_NOTE: Final was removed from the declaration of 'numDataCodewords '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
        private var numDataCodewords:int;
        //UPGRADE_NOTE: Final was removed from the declaration of 'codewords '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
        private var codewords:Array;

        public function DataBlock(numDataCodewords:int, codewords:Array)
        {
            this.numDataCodewords = numDataCodewords;
            this.codewords = codewords;
        }

        /// <summary> <p>When QR Codes use multiple data blocks, they are actually interleave the bytes of each of them.
        /// That is, the first byte of data block 1 to n is written, then the second bytes, and so on. This
        /// method will separate the data into original blocks.</p>
        /// 
        /// </summary>
        /// <param name="rawCodewords">bytes as read directly from the QR Code
        /// </param>
        /// <param name="version">version of the QR Code
        /// </param>
        /// <param name="ecLevel">error-correction level of the QR Code
        /// </param>
        /// <returns> {@link DataBlock}s containing original bytes, "de-interleaved" from representation in the
        /// QR Code
        /// </returns>
        public static function getDataBlocks( rawCodewords:zxingByteArray,  version:Version,  ecLevel:ErrorCorrectionLevel):Array
        {
            // Figure out the number and size of data blocks used by this version and
            // error correction level
            var  ecBlocks:ECBlocks = version.getECBlocksForLevel(ecLevel);

            // First count the total number of data blocks
            var totalBlocks:int = 0;
            var ecBlockArray:Array = ecBlocks.getECBlocks();
            for (var i:int = 0; i < ecBlockArray.length; i++)
            {
                totalBlocks += ecBlockArray[i].getCount();
            }

            // Now establish DataBlocks of the appropriate size and number of data codewords
            var result:Array = new Array(totalBlocks);
            var numResultBlocks:int = 0;
            for (var j:int = 0; j < ecBlockArray.length; j++)
            {
                var ecBlock:ECB = ecBlockArray[j];
                for (var i3:int = 0; i3 < ecBlock.getCount(); i3++)
                {
                    var numDataCodewords:int = ecBlock.getDataCodewords();
                    //var numBlockCodewords:int = ecBlocks.getTotalECCodewords() + numDataCodewords;
                    var numBlockCodewords:int = ecBlocks.getECCodewordsPerBlock() + numDataCodewords;
                    result[numResultBlocks] = new DataBlock(numDataCodewords, new Array(numBlockCodewords));
                    numResultBlocks++;
                }
            }

            // All blocks have the same amount of data, except that the last n
            // (where n may be 0) have 1 more byte. Figure out where these start.
            var shorterBlocksTotalCodewords:int = result[0].codewords.length;
            var longerBlocksStartAt:int = result.length - 1;
            while (longerBlocksStartAt >= 0)
            {
                var numCodewords:int = result[longerBlocksStartAt].codewords.length;
                if (numCodewords == shorterBlocksTotalCodewords)
                {
                    break;
                }
                if (numCodewords != shorterBlocksTotalCodewords + 1)
                {
                    throw new Error("Data block sizes differ by more than 1");
                }
                longerBlocksStartAt--;
            }
            longerBlocksStartAt++;
            
            var shorterBlocksNumDataCodewords:int = shorterBlocksTotalCodewords - ecBlocks.getECCodewordsPerBlock();
            // The last elements of result may be 1 element longer;
            // first fill out as many elements as all of them have
            var rawCodewordsOffset:int = 0;
            for (var i2:int = 0; i2 < shorterBlocksNumDataCodewords; i2++)
            {
                for (var j2:int = 0; j2 < numResultBlocks; j2++)
                {
                    result[j2].codewords[i2] = rawCodewords.getByte(rawCodewordsOffset++);
                }
            }
            // Fill out the last data block in the longer ones
            for (var j3:int = longerBlocksStartAt; j3 < numResultBlocks; j3++)
            {
                result[j3].codewords[shorterBlocksNumDataCodewords] = rawCodewords.getByte(rawCodewordsOffset++);
            }
            // Now add in error correction blocks
            var max:int = result[0].codewords.length;
            for (var i4 :int= shorterBlocksNumDataCodewords; i4 < max; i4++)
            {
                for (var j4:int = 0; j4 < numResultBlocks; j4++)
                {
                    var iOffset:int = j4 < longerBlocksStartAt ? i4 : i4 + 1;
                    result[j4].codewords[iOffset] = rawCodewords.getByte(rawCodewordsOffset++);
                }
            }

            return result;
        }

	}
}