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
namespace com.google.zxing.qrcode.decoder
{
	
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class BitMatrixParser
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'bitMatrix '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private BitMatrix bitMatrix;
		private Version parsedVersion;
		private FormatInformation parsedFormatInfo;
		
		/// <param name="bitMatrix">{@link BitMatrix} to parse
		/// </param>
		/// <throws>  ReaderException if dimension is not >= 21 and 1 mod 4 </throws>
		internal BitMatrixParser(BitMatrix bitMatrix)
		{
			int dimension = bitMatrix.Dimension;
			if (dimension < 21 || (dimension & 0x03) != 1)
			{
				throw ReaderException.Instance;
			}
			this.bitMatrix = bitMatrix;
		}
		
		/// <summary> <p>Reads format information from one of its two locations within the QR Code.</p>
		/// 
		/// </summary>
		/// <returns> {@link FormatInformation} encapsulating the QR Code's format info
		/// </returns>
		/// <throws>  ReaderException if both format information locations cannot be parsed as </throws>
		/// <summary> the valid encoding of format information
		/// </summary>
		internal FormatInformation readFormatInformation()
		{
			
			if (parsedFormatInfo != null)
			{
				return parsedFormatInfo;
			}
			
			// Read top-left format info bits
			int formatInfoBits = 0;
			for (int i = 0; i < 6; i++)
			{
				formatInfoBits = copyBit(i, 8, formatInfoBits);
			}
			// .. and skip a bit in the timing pattern ...
			formatInfoBits = copyBit(7, 8, formatInfoBits);
			formatInfoBits = copyBit(8, 8, formatInfoBits);
			formatInfoBits = copyBit(8, 7, formatInfoBits);
			// .. and skip a bit in the timing pattern ...
			for (int j = 5; j >= 0; j--)
			{
				formatInfoBits = copyBit(8, j, formatInfoBits);
			}
			
			parsedFormatInfo = FormatInformation.decodeFormatInformation(formatInfoBits);
			if (parsedFormatInfo != null)
			{
				return parsedFormatInfo;
			}
			
			// Hmm, failed. Try the top-right/bottom-left pattern
			int dimension = bitMatrix.Dimension;
			formatInfoBits = 0;
			int iMin = dimension - 8;
			for (int i = dimension - 1; i >= iMin; i--)
			{
				formatInfoBits = copyBit(i, 8, formatInfoBits);
			}
			for (int j = dimension - 7; j < dimension; j++)
			{
				formatInfoBits = copyBit(8, j, formatInfoBits);
			}
			
			parsedFormatInfo = FormatInformation.decodeFormatInformation(formatInfoBits);
			if (parsedFormatInfo != null)
			{
				return parsedFormatInfo;
			}
			throw ReaderException.Instance;
		}
		
		/// <summary> <p>Reads version information from one of its two locations within the QR Code.</p>
		/// 
		/// </summary>
		/// <returns> {@link Version} encapsulating the QR Code's version
		/// </returns>
		/// <throws>  ReaderException if both version information locations cannot be parsed as </throws>
		/// <summary> the valid encoding of version information
		/// </summary>
		internal Version readVersion()
		{
			
			if (parsedVersion != null)
			{
				return parsedVersion;
			}
			
			int dimension = bitMatrix.Dimension;
			
			int provisionalVersion = (dimension - 17) >> 2;
			if (provisionalVersion <= 6)
			{
				return Version.getVersionForNumber(provisionalVersion);
			}
			
			// Read top-right version info: 3 wide by 6 tall
			int versionBits = 0;
			int ijMin = dimension - 11;
			for (int j = 5; j >= 0; j--)
			{
				for (int i = dimension - 9; i >= ijMin; i--)
				{
					versionBits = copyBit(i, j, versionBits);
				}
			}
			
			parsedVersion = Version.decodeVersionInformation(versionBits);
			if (parsedVersion != null && parsedVersion.DimensionForVersion == dimension)
			{
				return parsedVersion;
			}
			
			// Hmm, failed. Try bottom left: 6 wide by 3 tall
			versionBits = 0;
			for (int i = 5; i >= 0; i--)
			{
				for (int j = dimension - 9; j >= ijMin; j--)
				{
					versionBits = copyBit(i, j, versionBits);
				}
			}
			
			parsedVersion = Version.decodeVersionInformation(versionBits);
			if (parsedVersion != null && parsedVersion.DimensionForVersion == dimension)
			{
				return parsedVersion;
			}
			throw ReaderException.Instance;
		}
		
		private int copyBit(int i, int j, int versionBits)
		{
			return bitMatrix.get_Renamed(i, j)?(versionBits << 1) | 0x1:versionBits << 1;
		}
		
		/// <summary> <p>Reads the bits in the {@link BitMatrix} representing the finder pattern in the
		/// correct order in order to reconstitute the codewords bytes contained within the
		/// QR Code.</p>
		/// 
		/// </summary>
		/// <returns> bytes encoded within the QR Code
		/// </returns>
		/// <throws>  ReaderException if the exact number of bytes expected is not read </throws>
		internal sbyte[] readCodewords()
		{
			
			FormatInformation formatInfo = readFormatInformation();
			Version version = readVersion();
			
			// Get the data mask for the format used in this QR Code. This will exclude
			// some bits from reading as we wind through the bit matrix.
			DataMask dataMask = DataMask.forReference((int) formatInfo.DataMask);
			int dimension = bitMatrix.Dimension;
			dataMask.unmaskBitMatrix(bitMatrix, dimension);
			
			BitMatrix functionPattern = version.buildFunctionPattern();
			
			bool readingUp = true;
			sbyte[] result = new sbyte[version.TotalCodewords];
			int resultOffset = 0;
			int currentByte = 0;
			int bitsRead = 0;
			// Read columns in pairs, from right to left
			for (int j = dimension - 1; j > 0; j -= 2)
			{
				if (j == 6)
				{
					// Skip whole column with vertical alignment pattern;
					// saves time and makes the other code proceed more cleanly
					j--;
				}
				// Read alternatingly from bottom to top then top to bottom
				for (int count = 0; count < dimension; count++)
				{
					int i = readingUp?dimension - 1 - count:count;
					for (int col = 0; col < 2; col++)
					{
						// Ignore bits covered by the function pattern
						if (!functionPattern.get_Renamed(j - col, i))
						{
							// Read a bit
							bitsRead++;
							currentByte <<= 1;
							if (bitMatrix.get_Renamed(j - col, i))
							{
								currentByte |= 1;
							}
							// If we've made a whole byte, save it off
							if (bitsRead == 8)
							{
								result[resultOffset++] = (sbyte) currentByte;
								bitsRead = 0;
								currentByte = 0;
							}
						}
					}
				}
				readingUp ^= true; // readingUp = !readingUp; // switch directions
			}
			if (resultOffset != version.TotalCodewords)
			{
				throw ReaderException.Instance;
			}
			return result;
		}
	}
}