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
using System;
using ReaderException = com.google.zxing.ReaderException;
using BitMatrix = com.google.zxing.common.BitMatrix;
using DecoderResult = com.google.zxing.common.DecoderResult;
using GF256 = com.google.zxing.common.reedsolomon.GF256;
using ReedSolomonDecoder = com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
using ReedSolomonException = com.google.zxing.common.reedsolomon.ReedSolomonException;
namespace com.google.zxing.datamatrix.decoder
{
	
	/// <summary> <p>The main class which implements Data Matrix Code decoding -- as opposed to locating and extracting
	/// the Data Matrix Code from an image.</p>
	/// 
	/// </summary>
	/// <author>  bbrown@google.com (Brian Brown)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class Decoder
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'rsDecoder '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private ReedSolomonDecoder rsDecoder;
		
		public Decoder()
		{
			rsDecoder = new ReedSolomonDecoder(GF256.DATA_MATRIX_FIELD);
		}
		
		/// <summary> <p>Convenience method that can decode a Data Matrix Code represented as a 2D array of booleans.
		/// "true" is taken to mean a black module.</p>
		/// 
		/// </summary>
		/// <param name="image">booleans representing white/black Data Matrix Code modules
		/// </param>
		/// <returns> text and bytes encoded within the Data Matrix Code
		/// </returns>
		/// <throws>  ReaderException if the Data Matrix Code cannot be decoded </throws>
		public DecoderResult decode(bool[][] image)
		{
			int dimension = image.Length;
			BitMatrix bits = new BitMatrix(dimension);
			for (int i = 0; i < dimension; i++)
			{
				for (int j = 0; j < dimension; j++)
				{
					if (image[i][j])
					{
						bits.set_Renamed(j, i);
					}
				}
			}
			return decode(bits);
		}
		
		/// <summary> <p>Decodes a Data Matrix Code represented as a {@link BitMatrix}. A 1 or "true" is taken
		/// to mean a black module.</p>
		/// 
		/// </summary>
		/// <param name="bits">booleans representing white/black Data Matrix Code modules
		/// </param>
		/// <returns> text and bytes encoded within the Data Matrix Code
		/// </returns>
		/// <throws>  ReaderException if the Data Matrix Code cannot be decoded </throws>
		public DecoderResult decode(BitMatrix bits)
		{
			
			// Construct a parser and read version, error-correction level
			BitMatrixParser parser = new BitMatrixParser(bits);
			Version version = parser.readVersion(bits);
			
			// Read codewords
			sbyte[] codewords = parser.readCodewords();
			// Separate into data blocks
			DataBlock[] dataBlocks = DataBlock.getDataBlocks(codewords, version);
			
			// Count total number of data bytes
			int totalBytes = 0;
			for (int i = 0; i < dataBlocks.Length; i++)
			{
				totalBytes += dataBlocks[i].NumDataCodewords;
			}
			sbyte[] resultBytes = new sbyte[totalBytes];
			int resultOffset = 0;
			
			// Error-correct and copy data blocks together into a stream of bytes
			for (int j = 0; j < dataBlocks.Length; j++)
			{
				DataBlock dataBlock = dataBlocks[j];
				sbyte[] codewordBytes = dataBlock.Codewords;
				int numDataCodewords = dataBlock.NumDataCodewords;
				correctErrors(codewordBytes, numDataCodewords);
				for (int i = 0; i < numDataCodewords; i++)
				{
					resultBytes[resultOffset++] = codewordBytes[i];
				}
			}
			
			// Decode the contents of that stream of bytes
			return DecodedBitStreamParser.decode(resultBytes);
		}
		
		/// <summary> <p>Given data and error-correction codewords received, possibly corrupted by errors, attempts to
		/// correct the errors in-place using Reed-Solomon error correction.</p>
		/// 
		/// </summary>
		/// <param name="codewordBytes">data and error correction codewords
		/// </param>
		/// <param name="numDataCodewords">number of codewords that are data bytes
		/// </param>
		/// <throws>  ReaderException if error correction fails </throws>
		private void  correctErrors(sbyte[] codewordBytes, int numDataCodewords)
		{
			int numCodewords = codewordBytes.Length;
			// First read into an array of ints
			int[] codewordsInts = new int[numCodewords];
			for (int i = 0; i < numCodewords; i++)
			{
				codewordsInts[i] = codewordBytes[i] & 0xFF;
			}
			int numECCodewords = codewordBytes.Length - numDataCodewords;
			try
			{
				rsDecoder.decode(codewordsInts, numECCodewords);
			}
			catch (ReedSolomonException)
			{
				throw ReaderException.Instance;
			}
			// Copy back into array of bytes -- only need to worry about the bytes that were data
			// We don't care about errors in the error-correction codewords
			for (int i = 0; i < numDataCodewords; i++)
			{
				codewordBytes[i] = (sbyte) codewordsInts[i];
			}
		}
	}
}
