/*
 * Copyright 2009 ZXing authors
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

namespace com.google.zxing.pdf417.decoder
{

	using ChecksumException = com.google.zxing.ChecksumException;
	using FormatException = com.google.zxing.FormatException;
	using BitMatrix = com.google.zxing.common.BitMatrix;
	using DecoderResult = com.google.zxing.common.DecoderResult;
	using ErrorCorrection = com.google.zxing.pdf417.decoder.ec.ErrorCorrection;

	/// <summary>
	/// <p>The main class which implements PDF417 Code decoding -- as
	/// opposed to locating and extracting the PDF417 Code from an image.</p>
	/// 
	/// @author SITA Lab (kevin.osullivan@sita.aero)
	/// </summary>
	public sealed class Decoder
	{

	  private const int MAX_ERRORS = 3;
	  private const int MAX_EC_CODEWORDS = 512;
	  private readonly ErrorCorrection errorCorrection;

	  public Decoder()
	  {
		errorCorrection = new ErrorCorrection();
	  }

	  /// <summary>
	  /// <p>Convenience method that can decode a PDF417 Code represented as a 2D array of booleans.
	  /// "true" is taken to mean a black module.</p>
	  /// </summary>
	  /// <param name="image"> booleans representing white/black PDF417 modules </param>
	  /// <returns> text and bytes encoded within the PDF417 Code </returns>
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
			if (image[j][i])
			{
			  bits.set(j, i);
			}
		  }
		}
		return decode(bits);
	  }

	  /// <summary>
	  /// <p>Decodes a PDF417 Code represented as a <seealso cref="BitMatrix"/>.
	  /// A 1 or "true" is taken to mean a black module.</p>
	  /// </summary>
	  /// <param name="bits"> booleans representing white/black PDF417 Code modules </param>
	  /// <returns> text and bytes encoded within the PDF417 Code </returns>
	  /// <exception cref="FormatException"> if the PDF417 Code cannot be decoded </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.DecoderResult decode(com.google.zxing.common.BitMatrix bits) throws com.google.zxing.FormatException, com.google.zxing.ChecksumException
	  public DecoderResult decode(BitMatrix bits)
	  {
		// Construct a parser to read the data codewords and error-correction level
		BitMatrixParser parser = new BitMatrixParser(bits);
		int[] codewords = parser.readCodewords();
		if (codewords.Length == 0)
		{
		  throw FormatException.FormatInstance;
		}

		int ecLevel = parser.ECLevel;
		int numECCodewords = 1 << (ecLevel + 1);
		int[] erasures = parser.Erasures;

		correctErrors(codewords, erasures, numECCodewords);
		verifyCodewordCount(codewords, numECCodewords);

		// Decode the codewords
		return DecodedBitStreamParser.decode(codewords);
	  }

	  /// <summary>
	  /// Verify that all is OK with the codeword array.
	  /// </summary>
	  /// <param name="codewords"> </param>
	  /// <returns> an index to the first data codeword. </returns>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void verifyCodewordCount(int[] codewords, int numECCodewords) throws com.google.zxing.FormatException
	  private static void verifyCodewordCount(int[] codewords, int numECCodewords)
	  {
		if (codewords.Length < 4)
		{
		  // Codeword array size should be at least 4 allowing for
		  // Count CW, At least one Data CW, Error Correction CW, Error Correction CW
		  throw FormatException.FormatInstance;
		}
		// The first codeword, the Symbol Length Descriptor, shall always encode the total number of data
		// codewords in the symbol, including the Symbol Length Descriptor itself, data codewords and pad
		// codewords, but excluding the number of error correction codewords.
		int numberOfCodewords = codewords[0];
		if (numberOfCodewords > codewords.Length)
		{
		  throw FormatException.FormatInstance;
		}
		if (numberOfCodewords == 0)
		{
		  // Reset to the length of the array - 8 (Allow for at least level 3 Error Correction (8 Error Codewords)
		  if (numECCodewords < codewords.Length)
		  {
			codewords[0] = codewords.Length - numECCodewords;
		  }
		  else
		  {
			throw FormatException.FormatInstance;
		  }
		}
	  }

	  /// <summary>
	  /// <p>Given data and error-correction codewords received, possibly corrupted by errors, attempts to
	  /// correct the errors in-place.</p>
	  /// </summary>
	  /// <param name="codewords">   data and error correction codewords </param>
	  /// <param name="erasures"> positions of any known erasures </param>
	  /// <param name="numECCodewords"> number of error correction codewards that were available in codewords </param>
	  /// <exception cref="ChecksumException"> if error correction fails </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private void correctErrors(int[] codewords, int[] erasures, int numECCodewords) throws com.google.zxing.ChecksumException
	  private void correctErrors(int[] codewords, int[] erasures, int numECCodewords)
	  {
		if (erasures.Length > numECCodewords / 2 + MAX_ERRORS || numECCodewords < 0 || numECCodewords > MAX_EC_CODEWORDS)
		{
		  // Too many errors or EC Codewords is corrupted
		  throw ChecksumException.ChecksumInstance;
		}
		errorCorrection.decode(codewords, numECCodewords, erasures);
	  }

	}

}