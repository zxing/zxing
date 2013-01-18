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

namespace com.google.zxing.datamatrix.decoder
{

	using ChecksumException = com.google.zxing.ChecksumException;
	using FormatException = com.google.zxing.FormatException;
	using BitMatrix = com.google.zxing.common.BitMatrix;
	using DecoderResult = com.google.zxing.common.DecoderResult;
	using GenericGF = com.google.zxing.common.reedsolomon.GenericGF;
	using ReedSolomonDecoder = com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
	using ReedSolomonException = com.google.zxing.common.reedsolomon.ReedSolomonException;

	/// <summary>
	/// <p>The main class which implements Data Matrix Code decoding -- as opposed to locating and extracting
	/// the Data Matrix Code from an image.</p>
	/// 
	/// @author bbrown@google.com (Brian Brown)
	/// </summary>
	public sealed class Decoder
	{

	  private readonly ReedSolomonDecoder rsDecoder;

	  public Decoder()
	  {
		rsDecoder = new ReedSolomonDecoder(GenericGF.DATA_MATRIX_FIELD_256);
	  }

	  /// <summary>
	  /// <p>Convenience method that can decode a Data Matrix Code represented as a 2D array of booleans.
	  /// "true" is taken to mean a black module.</p>
	  /// </summary>
	  /// <param name="image"> booleans representing white/black Data Matrix Code modules </param>
	  /// <returns> text and bytes encoded within the Data Matrix Code </returns>
	  /// <exception cref="FormatException"> if the Data Matrix Code cannot be decoded </exception>
	  /// <exception cref="ChecksumException"> if error correction fails </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.DecoderResult decode(boolean[][] image) throws com.google.zxing.FormatException, com.google.zxing.ChecksumException
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
			  bits.set(j, i);
			}
		  }
		}
		return decode(bits);
	  }

	  /// <summary>
	  /// <p>Decodes a Data Matrix Code represented as a <seealso cref="BitMatrix"/>. A 1 or "true" is taken
	  /// to mean a black module.</p>
	  /// </summary>
	  /// <param name="bits"> booleans representing white/black Data Matrix Code modules </param>
	  /// <returns> text and bytes encoded within the Data Matrix Code </returns>
	  /// <exception cref="FormatException"> if the Data Matrix Code cannot be decoded </exception>
	  /// <exception cref="ChecksumException"> if error correction fails </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.DecoderResult decode(com.google.zxing.common.BitMatrix bits) throws com.google.zxing.FormatException, com.google.zxing.ChecksumException
	  public DecoderResult decode(BitMatrix bits)
	  {

		// Construct a parser and read version, error-correction level
		BitMatrixParser parser = new BitMatrixParser(bits);
		Version version = parser.Version;

		// Read codewords
		sbyte[] codewords = parser.readCodewords();
		// Separate into data blocks
		DataBlock[] dataBlocks = DataBlock.getDataBlocks(codewords, version);

		int dataBlocksCount = dataBlocks.Length;

		// Count total number of data bytes
		int totalBytes = 0;
		foreach (DataBlock db in dataBlocks)
		{
		  totalBytes += db.NumDataCodewords;
		}
		sbyte[] resultBytes = new sbyte[totalBytes];

		// Error-correct and copy data blocks together into a stream of bytes
		for (int j = 0; j < dataBlocksCount; j++)
		{
		  DataBlock dataBlock = dataBlocks[j];
		  sbyte[] codewordBytes = dataBlock.Codewords;
		  int numDataCodewords = dataBlock.NumDataCodewords;
		  correctErrors(codewordBytes, numDataCodewords);
		  for (int i = 0; i < numDataCodewords; i++)
		  {
			// De-interlace data blocks.
			resultBytes[i * dataBlocksCount + j] = codewordBytes[i];
		  }
		}

		// Decode the contents of that stream of bytes
		return DecodedBitStreamParser.decode(resultBytes);
	  }

	  /// <summary>
	  /// <p>Given data and error-correction codewords received, possibly corrupted by errors, attempts to
	  /// correct the errors in-place using Reed-Solomon error correction.</p>
	  /// </summary>
	  /// <param name="codewordBytes"> data and error correction codewords </param>
	  /// <param name="numDataCodewords"> number of codewords that are data bytes </param>
	  /// <exception cref="ChecksumException"> if error correction fails </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private void correctErrors(byte[] codewordBytes, int numDataCodewords) throws com.google.zxing.ChecksumException
	  private void correctErrors(sbyte[] codewordBytes, int numDataCodewords)
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
		catch (ReedSolomonException rse)
		{
		  throw ChecksumException.ChecksumInstance;
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