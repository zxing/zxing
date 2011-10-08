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
using WriterException = com.google.zxing.WriterException;
using EncodeHintType = com.google.zxing.EncodeHintType;
using ByteArray = com.google.zxing.common.ByteArray;
using ByteMatrix = com.google.zxing.common.ByteMatrix;
using CharacterSetECI = com.google.zxing.common.CharacterSetECI;
using GF256 = com.google.zxing.common.reedsolomon.GF256;
using ReedSolomonEncoder = com.google.zxing.common.reedsolomon.ReedSolomonEncoder;
using ErrorCorrectionLevel = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
using Mode = com.google.zxing.qrcode.decoder.Mode;
using Version = com.google.zxing.qrcode.decoder.Version;
namespace com.google.zxing.qrcode.encoder
{
	
	/// <author>  satorux@google.com (Satoru Takabayashi) - creator
	/// </author>
	/// <author>  dswitkin@google.com (Daniel Switkin) - ported from C++
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class Encoder
	{
		
		// The original table is defined in the table 5 of JISX0510:2004 (p.19).
		//UPGRADE_NOTE: Final was removed from the declaration of 'ALPHANUMERIC_TABLE'. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly int[] ALPHANUMERIC_TABLE = new int[]{- 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, - 1, 36, - 1, - 1, - 1, 37, 38, - 1, - 1, - 1, - 1, 39, 40, - 1, 41, 42, 43, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 44, - 1, - 1, - 1, - 1, - 1, - 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, - 1, - 1, - 1, - 1, - 1};
		
		internal const System.String DEFAULT_BYTE_MODE_ENCODING = "ISO-8859-1";
		
		private Encoder()
		{
		}
		
		// The mask penalty calculation is complicated.  See Table 21 of JISX0510:2004 (p.45) for details.
		// Basically it applies four rules and summate all penalties.
		private static int calculateMaskPenalty(ByteMatrix matrix)
		{
			int penalty = 0;
			penalty += MaskUtil.applyMaskPenaltyRule1(matrix);
			penalty += MaskUtil.applyMaskPenaltyRule2(matrix);
			penalty += MaskUtil.applyMaskPenaltyRule3(matrix);
			penalty += MaskUtil.applyMaskPenaltyRule4(matrix);
			return penalty;
		}
		
		/// <summary>  Encode "bytes" with the error correction level "ecLevel". The encoding mode will be chosen
		/// internally by chooseMode(). On success, store the result in "qrCode".
		/// 
		/// We recommend you to use QRCode.EC_LEVEL_L (the lowest level) for
		/// "getECLevel" since our primary use is to show QR code on desktop screens. We don't need very
		/// strong error correction for this purpose.
		/// 
		/// Note that there is no way to encode bytes in MODE_KANJI. We might want to add EncodeWithMode()
		/// with which clients can specify the encoding mode. For now, we don't need the functionality.
		/// </summary>
		public static void  encode(System.String content, ErrorCorrectionLevel ecLevel, QRCode qrCode)
		{
			encode(content, ecLevel, null, qrCode);
		}
		
		public static void  encode(System.String content, ErrorCorrectionLevel ecLevel, System.Collections.Hashtable hints, QRCode qrCode)
		{
			
			System.String encoding = hints == null?null:(System.String) hints[EncodeHintType.CHARACTER_SET];
			if (encoding == null)
			{
				encoding = DEFAULT_BYTE_MODE_ENCODING;
			}
			
			// Step 1: Choose the mode (encoding).
			Mode mode = chooseMode(content, encoding);
			
			// Step 2: Append "bytes" into "dataBits" in appropriate encoding.
			BitVector dataBits = new BitVector();
			appendBytes(content, mode, dataBits, encoding);
			// Step 3: Initialize QR code that can contain "dataBits".
			int numInputBytes = dataBits.sizeInBytes();
			initQRCode(numInputBytes, ecLevel, mode, qrCode);
			
			// Step 4: Build another bit vector that contains header and data.
			BitVector headerAndDataBits = new BitVector();
			
			// Step 4.5: Append ECI message if applicable
			if (mode == Mode.BYTE && !DEFAULT_BYTE_MODE_ENCODING.Equals(encoding))
			{
				CharacterSetECI eci = CharacterSetECI.getCharacterSetECIByName(encoding);
				if (eci != null)
				{
					appendECI(eci, headerAndDataBits);
				}
			}
			
			appendModeInfo(mode, headerAndDataBits);
			
			int numLetters = mode.Equals(Mode.BYTE)?dataBits.sizeInBytes():content.Length;
			appendLengthInfo(numLetters, qrCode.Version, mode, headerAndDataBits);
			headerAndDataBits.appendBitVector(dataBits);
			
			// Step 5: Terminate the bits properly.
			terminateBits(qrCode.NumDataBytes, headerAndDataBits);
			
			// Step 6: Interleave data bits with error correction code.
			BitVector finalBits = new BitVector();
			interleaveWithECBytes(headerAndDataBits, qrCode.NumTotalBytes, qrCode.NumDataBytes, qrCode.NumRSBlocks, finalBits);
			
			// Step 7: Choose the mask pattern and set to "qrCode".
			ByteMatrix matrix = new ByteMatrix(qrCode.MatrixWidth, qrCode.MatrixWidth);
			qrCode.MaskPattern = chooseMaskPattern(finalBits, qrCode.ECLevel, qrCode.Version, matrix);
			
			// Step 8.  Build the matrix and set it to "qrCode".
			MatrixUtil.buildMatrix(finalBits, qrCode.ECLevel, qrCode.Version, qrCode.MaskPattern, matrix);
			qrCode.Matrix = matrix;
			// Step 9.  Make sure we have a valid QR Code.
			if (!qrCode.Valid)
			{
				throw new WriterException("Invalid QR code: " + qrCode.ToString());
			}
		}
		
		/// <returns> the code point of the table used in alphanumeric mode or
		/// -1 if there is no corresponding code in the table.
		/// </returns>
		internal static int getAlphanumericCode(int code)
		{
			if (code < ALPHANUMERIC_TABLE.Length)
			{
				return ALPHANUMERIC_TABLE[code];
			}
			return - 1;
		}
		
		public static Mode chooseMode(System.String content)
		{
			return chooseMode(content, null);
		}
		
		/// <summary> Choose the best mode by examining the content. Note that 'encoding' is used as a hint;
		/// if it is Shift_JIS, and the input is only double-byte Kanji, then we return {@link Mode#KANJI}.
		/// </summary>
		public static Mode chooseMode(System.String content, System.String encoding)
		{
			if ("Shift_JIS".Equals(encoding))
			{
				// Choose Kanji mode if all input are double-byte characters
				return isOnlyDoubleByteKanji(content)?Mode.KANJI:Mode.BYTE;
			}
			bool hasNumeric = false;
			bool hasAlphanumeric = false;
			for (int i = 0; i < content.Length; ++i)
			{
				char c = content[i];
				if (c >= '0' && c <= '9')
				{
					hasNumeric = true;
				}
				else if (getAlphanumericCode(c) != - 1)
				{
					hasAlphanumeric = true;
				}
				else
				{
					return Mode.BYTE;
				}
			}
			if (hasAlphanumeric)
			{
				return Mode.ALPHANUMERIC;
			}
			else if (hasNumeric)
			{
				return Mode.NUMERIC;
			}
			return Mode.BYTE;
		}
		
		private static bool isOnlyDoubleByteKanji(System.String content)
		{
			sbyte[] bytes;
			try
			{
				//UPGRADE_TODO: Method 'java.lang.String.getBytes' was converted to 'System.Text.Encoding.GetEncoding(string).GetBytes(string)' which has a different behavior. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1073_javalangStringgetBytes_javalangString'"
				bytes = SupportClass.ToSByteArray(System.Text.Encoding.GetEncoding("Shift_JIS").GetBytes(content));
			}
			catch (System.IO.IOException)
			{
				return false;
			}
			int length = bytes.Length;
			if (length % 2 != 0)
			{
				return false;
			}
			for (int i = 0; i < length; i += 2)
			{
				int byte1 = bytes[i] & 0xFF;
				if ((byte1 < 0x81 || byte1 > 0x9F) && (byte1 < 0xE0 || byte1 > 0xEB))
				{
					return false;
				}
			}
			return true;
		}
		
		private static int chooseMaskPattern(BitVector bits, ErrorCorrectionLevel ecLevel, int version, ByteMatrix matrix)
		{
			
			int minPenalty = System.Int32.MaxValue; // Lower penalty is better.
			int bestMaskPattern = - 1;
			// We try all mask patterns to choose the best one.
			for (int maskPattern = 0; maskPattern < QRCode.NUM_MASK_PATTERNS; maskPattern++)
			{
				MatrixUtil.buildMatrix(bits, ecLevel, version, maskPattern, matrix);
				int penalty = calculateMaskPenalty(matrix);
				if (penalty < minPenalty)
				{
					minPenalty = penalty;
					bestMaskPattern = maskPattern;
				}
			}
			return bestMaskPattern;
		}
		
		/// <summary> Initialize "qrCode" according to "numInputBytes", "ecLevel", and "mode". On success,
		/// modify "qrCode".
		/// </summary>
		private static void  initQRCode(int numInputBytes, ErrorCorrectionLevel ecLevel, Mode mode, QRCode qrCode)
		{
			qrCode.ECLevel = ecLevel;
			qrCode.Mode = mode;
			
			// In the following comments, we use numbers of Version 7-H.
			for (int versionNum = 1; versionNum <= 40; versionNum++)
			{
				Version version = Version.getVersionForNumber(versionNum);
				// numBytes = 196
				int numBytes = version.TotalCodewords;
				// getNumECBytes = 130
				Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
				int numEcBytes = ecBlocks.TotalECCodewords;
				// getNumRSBlocks = 5
				int numRSBlocks = ecBlocks.NumBlocks;
				// getNumDataBytes = 196 - 130 = 66
				int numDataBytes = numBytes - numEcBytes;
				// We want to choose the smallest version which can contain data of "numInputBytes" + some
				// extra bits for the header (mode info and length info). The header can be three bytes
				// (precisely 4 + 16 bits) at most. Hence we do +3 here.
				if (numDataBytes >= numInputBytes + 3)
				{
					// Yay, we found the proper rs block info!
					qrCode.Version = versionNum;
					qrCode.NumTotalBytes = numBytes;
					qrCode.NumDataBytes = numDataBytes;
					qrCode.NumRSBlocks = numRSBlocks;
					// getNumECBytes = 196 - 66 = 130
					qrCode.NumECBytes = numEcBytes;
					// matrix width = 21 + 6 * 4 = 45
					qrCode.MatrixWidth = version.DimensionForVersion;
					return ;
				}
			}
			throw new WriterException("Cannot find proper rs block info (input data too big?)");
		}
		
		/// <summary> Terminate bits as described in 8.4.8 and 8.4.9 of JISX0510:2004 (p.24).</summary>
		internal static void  terminateBits(int numDataBytes, BitVector bits)
		{
			int capacity = numDataBytes << 3;
			if (bits.size() > capacity)
			{
				throw new WriterException("data bits cannot fit in the QR Code" + bits.size() + " > " + capacity);
			}
			// Append termination bits. See 8.4.8 of JISX0510:2004 (p.24) for details.
			// TODO: srowen says we can remove this for loop, since the 4 terminator bits are optional if
			// the last byte has less than 4 bits left. So it amounts to padding the last byte with zeroes
			// either way.
			for (int i = 0; i < 4 && bits.size() < capacity; ++i)
			{
				bits.appendBit(0);
			}
			int numBitsInLastByte = bits.size() % 8;
			// If the last byte isn't 8-bit aligned, we'll add padding bits.
			if (numBitsInLastByte > 0)
			{
				int numPaddingBits = 8 - numBitsInLastByte;
				for (int i = 0; i < numPaddingBits; ++i)
				{
					bits.appendBit(0);
				}
			}
			// Should be 8-bit aligned here.
			if (bits.size() % 8 != 0)
			{
				throw new WriterException("Number of bits is not a multiple of 8");
			}
			// If we have more space, we'll fill the space with padding patterns defined in 8.4.9 (p.24).
			int numPaddingBytes = numDataBytes - bits.sizeInBytes();
			for (int i = 0; i < numPaddingBytes; ++i)
			{
				if (i % 2 == 0)
				{
					bits.appendBits(0xec, 8);
				}
				else
				{
					bits.appendBits(0x11, 8);
				}
			}
			if (bits.size() != capacity)
			{
				throw new WriterException("Bits size does not equal capacity");
			}
		}
		
		/// <summary> Get number of data bytes and number of error correction bytes for block id "blockID". Store
		/// the result in "numDataBytesInBlock", and "numECBytesInBlock". See table 12 in 8.5.1 of
		/// JISX0510:2004 (p.30)
		/// </summary>
		internal static void  getNumDataBytesAndNumECBytesForBlockID(int numTotalBytes, int numDataBytes, int numRSBlocks, int blockID, int[] numDataBytesInBlock, int[] numECBytesInBlock)
		{
			if (blockID >= numRSBlocks)
			{
				throw new WriterException("Block ID too large");
			}
			// numRsBlocksInGroup2 = 196 % 5 = 1
			int numRsBlocksInGroup2 = numTotalBytes % numRSBlocks;
			// numRsBlocksInGroup1 = 5 - 1 = 4
			int numRsBlocksInGroup1 = numRSBlocks - numRsBlocksInGroup2;
			// numTotalBytesInGroup1 = 196 / 5 = 39
			int numTotalBytesInGroup1 = numTotalBytes / numRSBlocks;
			// numTotalBytesInGroup2 = 39 + 1 = 40
			int numTotalBytesInGroup2 = numTotalBytesInGroup1 + 1;
			// numDataBytesInGroup1 = 66 / 5 = 13
			int numDataBytesInGroup1 = numDataBytes / numRSBlocks;
			// numDataBytesInGroup2 = 13 + 1 = 14
			int numDataBytesInGroup2 = numDataBytesInGroup1 + 1;
			// numEcBytesInGroup1 = 39 - 13 = 26
			int numEcBytesInGroup1 = numTotalBytesInGroup1 - numDataBytesInGroup1;
			// numEcBytesInGroup2 = 40 - 14 = 26
			int numEcBytesInGroup2 = numTotalBytesInGroup2 - numDataBytesInGroup2;
			// Sanity checks.
			// 26 = 26
			if (numEcBytesInGroup1 != numEcBytesInGroup2)
			{
				throw new WriterException("EC bytes mismatch");
			}
			// 5 = 4 + 1.
			if (numRSBlocks != numRsBlocksInGroup1 + numRsBlocksInGroup2)
			{
				throw new WriterException("RS blocks mismatch");
			}
			// 196 = (13 + 26) * 4 + (14 + 26) * 1
			if (numTotalBytes != ((numDataBytesInGroup1 + numEcBytesInGroup1) * numRsBlocksInGroup1) + ((numDataBytesInGroup2 + numEcBytesInGroup2) * numRsBlocksInGroup2))
			{
				throw new WriterException("Total bytes mismatch");
			}
			
			if (blockID < numRsBlocksInGroup1)
			{
				numDataBytesInBlock[0] = numDataBytesInGroup1;
				numECBytesInBlock[0] = numEcBytesInGroup1;
			}
			else
			{
				numDataBytesInBlock[0] = numDataBytesInGroup2;
				numECBytesInBlock[0] = numEcBytesInGroup2;
			}
		}
		
		/// <summary> Interleave "bits" with corresponding error correction bytes. On success, store the result in
		/// "result". The interleave rule is complicated. See 8.6 of JISX0510:2004 (p.37) for details.
		/// </summary>
		internal static void  interleaveWithECBytes(BitVector bits, int numTotalBytes, int numDataBytes, int numRSBlocks, BitVector result)
		{
			
			// "bits" must have "getNumDataBytes" bytes of data.
			if (bits.sizeInBytes() != numDataBytes)
			{
				throw new WriterException("Number of bits and data bytes does not match");
			}
			
			// Step 1.  Divide data bytes into blocks and generate error correction bytes for them. We'll
			// store the divided data bytes blocks and error correction bytes blocks into "blocks".
			int dataBytesOffset = 0;
			int maxNumDataBytes = 0;
			int maxNumEcBytes = 0;
			
			// Since, we know the number of reedsolmon blocks, we can initialize the vector with the number.
			System.Collections.ArrayList blocks = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(numRSBlocks));
			
			for (int i = 0; i < numRSBlocks; ++i)
			{
				int[] numDataBytesInBlock = new int[1];
				int[] numEcBytesInBlock = new int[1];
				getNumDataBytesAndNumECBytesForBlockID(numTotalBytes, numDataBytes, numRSBlocks, i, numDataBytesInBlock, numEcBytesInBlock);
				
				ByteArray dataBytes = new ByteArray();
				dataBytes.set_Renamed(bits.Array, dataBytesOffset, numDataBytesInBlock[0]);
				ByteArray ecBytes = generateECBytes(dataBytes, numEcBytesInBlock[0]);
				blocks.Add(new BlockPair(dataBytes, ecBytes));
				
				maxNumDataBytes = System.Math.Max(maxNumDataBytes, dataBytes.size());
				maxNumEcBytes = System.Math.Max(maxNumEcBytes, ecBytes.size());
				dataBytesOffset += numDataBytesInBlock[0];
			}
			if (numDataBytes != dataBytesOffset)
			{
				throw new WriterException("Data bytes does not match offset");
			}
			
			// First, place data blocks.
			for (int i = 0; i < maxNumDataBytes; ++i)
			{
				for (int j = 0; j < blocks.Count; ++j)
				{
					ByteArray dataBytes = ((BlockPair) blocks[j]).DataBytes;
					if (i < dataBytes.size())
					{
						result.appendBits(dataBytes.at(i), 8);
					}
				}
			}
			// Then, place error correction blocks.
			for (int i = 0; i < maxNumEcBytes; ++i)
			{
				for (int j = 0; j < blocks.Count; ++j)
				{
					ByteArray ecBytes = ((BlockPair) blocks[j]).ErrorCorrectionBytes;
					if (i < ecBytes.size())
					{
						result.appendBits(ecBytes.at(i), 8);
					}
				}
			}
			if (numTotalBytes != result.sizeInBytes())
			{
				// Should be same.
				throw new WriterException("Interleaving error: " + numTotalBytes + " and " + result.sizeInBytes() + " differ.");
			}
		}
		
		internal static ByteArray generateECBytes(ByteArray dataBytes, int numEcBytesInBlock)
		{
			int numDataBytes = dataBytes.size();
			int[] toEncode = new int[numDataBytes + numEcBytesInBlock];
			for (int i = 0; i < numDataBytes; i++)
			{
				toEncode[i] = dataBytes.at(i);
			}
			new ReedSolomonEncoder(GF256.QR_CODE_FIELD).encode(toEncode, numEcBytesInBlock);
			
			ByteArray ecBytes = new ByteArray(numEcBytesInBlock);
			for (int i = 0; i < numEcBytesInBlock; i++)
			{
				ecBytes.set_Renamed(i, toEncode[numDataBytes + i]);
			}
			return ecBytes;
		}
		
		/// <summary> Append mode info. On success, store the result in "bits".</summary>
		internal static void  appendModeInfo(Mode mode, BitVector bits)
		{
			bits.appendBits(mode.Bits, 4);
		}
		
		
		/// <summary> Append length info. On success, store the result in "bits".</summary>
		internal static void  appendLengthInfo(int numLetters, int version, Mode mode, BitVector bits)
		{
			int numBits = mode.getCharacterCountBits(Version.getVersionForNumber(version));
			if (numLetters > ((1 << numBits) - 1))
			{
				throw new WriterException(numLetters + "is bigger than" + ((1 << numBits) - 1));
			}
			bits.appendBits(numLetters, numBits);
		}
		
		/// <summary> Append "bytes" in "mode" mode (encoding) into "bits". On success, store the result in "bits".</summary>
		internal static void  appendBytes(System.String content, Mode mode, BitVector bits, System.String encoding)
		{
			if (mode.Equals(Mode.NUMERIC))
			{
				appendNumericBytes(content, bits);
			}
			else if (mode.Equals(Mode.ALPHANUMERIC))
			{
				appendAlphanumericBytes(content, bits);
			}
			else if (mode.Equals(Mode.BYTE))
			{
				append8BitBytes(content, bits, encoding);
			}
			else if (mode.Equals(Mode.KANJI))
			{
				appendKanjiBytes(content, bits);
			}
			else
			{
				throw new WriterException("Invalid mode: " + mode);
			}
		}
		
		internal static void  appendNumericBytes(System.String content, BitVector bits)
		{
			int length = content.Length;
			int i = 0;
			while (i < length)
			{
				int num1 = content[i] - '0';
				if (i + 2 < length)
				{
					// Encode three numeric letters in ten bits.
					int num2 = content[i + 1] - '0';
					int num3 = content[i + 2] - '0';
					bits.appendBits(num1 * 100 + num2 * 10 + num3, 10);
					i += 3;
				}
				else if (i + 1 < length)
				{
					// Encode two numeric letters in seven bits.
					int num2 = content[i + 1] - '0';
					bits.appendBits(num1 * 10 + num2, 7);
					i += 2;
				}
				else
				{
					// Encode one numeric letter in four bits.
					bits.appendBits(num1, 4);
					i++;
				}
			}
		}
		
		internal static void  appendAlphanumericBytes(System.String content, BitVector bits)
		{
			int length = content.Length;
			int i = 0;
			while (i < length)
			{
				int code1 = getAlphanumericCode(content[i]);
				if (code1 == - 1)
				{
					throw new WriterException();
				}
				if (i + 1 < length)
				{
					int code2 = getAlphanumericCode(content[i + 1]);
					if (code2 == - 1)
					{
						throw new WriterException();
					}
					// Encode two alphanumeric letters in 11 bits.
					bits.appendBits(code1 * 45 + code2, 11);
					i += 2;
				}
				else
				{
					// Encode one alphanumeric letter in six bits.
					bits.appendBits(code1, 6);
					i++;
				}
			}
		}
		
		internal static void  append8BitBytes(System.String content, BitVector bits, System.String encoding)
		{
			sbyte[] bytes;
			try
			{
				//UPGRADE_TODO: Method 'java.lang.String.getBytes' was converted to 'System.Text.Encoding.GetEncoding(string).GetBytes(string)' which has a different behavior. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1073_javalangStringgetBytes_javalangString'"
				bytes = SupportClass.ToSByteArray(System.Text.Encoding.GetEncoding(encoding).GetBytes(content));
			}
			catch (System.IO.IOException uee)
			{
				//UPGRADE_TODO: The equivalent in .NET for method 'java.lang.Throwable.toString' may return a different value. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1043'"
				throw new WriterException(uee.ToString());
			}
			for (int i = 0; i < bytes.Length; ++i)
			{
				bits.appendBits(bytes[i], 8);
			}
		}
		
		internal static void  appendKanjiBytes(System.String content, BitVector bits)
		{
			sbyte[] bytes;
			try
			{
				//UPGRADE_TODO: Method 'java.lang.String.getBytes' was converted to 'System.Text.Encoding.GetEncoding(string).GetBytes(string)' which has a different behavior. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1073_javalangStringgetBytes_javalangString'"
				bytes = SupportClass.ToSByteArray(System.Text.Encoding.GetEncoding("Shift_JIS").GetBytes(content));
			}
			catch (System.IO.IOException uee)
			{
				//UPGRADE_TODO: The equivalent in .NET for method 'java.lang.Throwable.toString' may return a different value. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1043'"
				throw new WriterException(uee.ToString());
			}
			int length = bytes.Length;
			for (int i = 0; i < length; i += 2)
			{
				int byte1 = bytes[i] & 0xFF;
				int byte2 = bytes[i + 1] & 0xFF;
				int code = (byte1 << 8) | byte2;
				int subtracted = - 1;
				if (code >= 0x8140 && code <= 0x9ffc)
				{
					subtracted = code - 0x8140;
				}
				else if (code >= 0xe040 && code <= 0xebbf)
				{
					subtracted = code - 0xc140;
				}
				if (subtracted == - 1)
				{
					throw new WriterException("Invalid byte sequence");
				}
				int encoded = ((subtracted >> 8) * 0xc0) + (subtracted & 0xff);
				bits.appendBits(encoded, 13);
			}
		}
		
		private static void  appendECI(CharacterSetECI eci, BitVector bits)
		{
			bits.appendBits(Mode.ECI.Bits, 4);
			// This is correct for values up to 127, which is all we need now.
			bits.appendBits(eci.Value, 8);
		}
	}
}
