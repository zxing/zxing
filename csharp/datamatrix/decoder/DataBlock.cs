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
using System;
namespace com.google.zxing.datamatrix.decoder
{
	
	/// <summary> <p>Encapsulates a block of data within a Data Matrix Code. Data Matrix Codes may split their data into
	/// multiple blocks, each of which is a unit of data and error-correction codewords. Each
	/// is represented by an instance of this class.</p>
	/// 
	/// </summary>
	/// <author>  bbrown@google.com (Brian Brown)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class DataBlock
	{
		internal int NumDataCodewords
		{
			get
			{
				return numDataCodewords;
			}
			
		}
		internal sbyte[] Codewords
		{
			get
			{
				return codewords;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'numDataCodewords '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int numDataCodewords;
		//UPGRADE_NOTE: Final was removed from the declaration of 'codewords '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private sbyte[] codewords;
		
		private DataBlock(int numDataCodewords, sbyte[] codewords)
		{
			this.numDataCodewords = numDataCodewords;
			this.codewords = codewords;
		}
		
		/// <summary> <p>When Data Matrix Codes use multiple data blocks, they actually interleave the bytes of each of them.
		/// That is, the first byte of data block 1 to n is written, then the second bytes, and so on. This
		/// method will separate the data into original blocks.</p>
		/// 
		/// </summary>
		/// <param name="rawCodewords">bytes as read directly from the Data Matrix Code
		/// </param>
		/// <param name="version">version of the Data Matrix Code
		/// </param>
		/// <returns> {@link DataBlock}s containing original bytes, "de-interleaved" from representation in the
		/// Data Matrix Code
		/// </returns>
		internal static DataBlock[] getDataBlocks(sbyte[] rawCodewords, Version version)
		{
			// Figure out the number and size of data blocks used by this version
			Version.ECBlocks ecBlocks = version.getECBlocks();
			
			// First count the total number of data blocks
			int totalBlocks = 0;
			Version.ECB[] ecBlockArray = ecBlocks.getECBlocks();
			for (int i = 0; i < ecBlockArray.Length; i++)
			{
				totalBlocks += ecBlockArray[i].Count;
			}
			
			// Now establish DataBlocks of the appropriate size and number of data codewords
			DataBlock[] result = new DataBlock[totalBlocks];
			int numResultBlocks = 0;
			for (int j = 0; j < ecBlockArray.Length; j++)
			{
				Version.ECB ecBlock = ecBlockArray[j];
				for (int i = 0; i < ecBlock.Count; i++)
				{
					int numDataCodewords = ecBlock.DataCodewords;
					int numBlockCodewords = ecBlocks.ECCodewords + numDataCodewords;
					result[numResultBlocks++] = new DataBlock(numDataCodewords, new sbyte[numBlockCodewords]);
				}
			}
			
			// All blocks have the same amount of data, except that the last n
			// (where n may be 0) have 1 less byte. Figure out where these start.
			// TODO(bbrown): There is only one case where there is a difference for Data Matrix for size 144
			int longerBlocksTotalCodewords = result[0].codewords.Length;
			//int shorterBlocksTotalCodewords = longerBlocksTotalCodewords - 1;
			
			int longerBlocksNumDataCodewords = longerBlocksTotalCodewords - ecBlocks.ECCodewords;
			int shorterBlocksNumDataCodewords = longerBlocksNumDataCodewords - 1;
			// The last elements of result may be 1 element shorter for 144 matrix
			// first fill out as many elements as all of them have minus 1
			int rawCodewordsOffset = 0;
			for (int i = 0; i < shorterBlocksNumDataCodewords; i++)
			{
				for (int j = 0; j < numResultBlocks; j++)
				{
					result[j].codewords[i] = rawCodewords[rawCodewordsOffset++];
				}
			}
			
			// Fill out the last data block in the longer ones
			bool specialVersion = version.VersionNumber == 24;
			int numLongerBlocks = specialVersion?8:numResultBlocks;
			for (int j = 0; j < numLongerBlocks; j++)
			{
				result[j].codewords[longerBlocksNumDataCodewords - 1] = rawCodewords[rawCodewordsOffset++];
			}
			
			// Now add in error correction blocks
			int max = result[0].codewords.Length;
			for (int i = longerBlocksNumDataCodewords; i < max; i++)
			{
				for (int j = 0; j < numResultBlocks; j++)
				{
					int iOffset = (specialVersion && j > 7)?i - 1:i;
					result[j].codewords[iOffset] = rawCodewords[rawCodewordsOffset++];
				}
			}
			
			if (rawCodewordsOffset != rawCodewords.Length)
			{
				throw new System.ArgumentException();
			}
			
			return result;
		}
	}
}