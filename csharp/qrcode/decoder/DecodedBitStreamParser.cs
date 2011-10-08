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
using BitSource = com.google.zxing.common.BitSource;
using CharacterSetECI = com.google.zxing.common.CharacterSetECI;
using DecoderResult = com.google.zxing.common.DecoderResult;
namespace com.google.zxing.qrcode.decoder
{
	
	/// <summary> <p>QR Codes can encode text as bits in one of several modes, and can use multiple modes
	/// in one QR Code. This class decodes the bits back into text.</p>
	/// 
	/// <p>See ISO 18004:2006, 6.4.3 - 6.4.7</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class DecodedBitStreamParser
	{
		
		/// <summary> See ISO 18004:2006, 6.4.4 Table 5</summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'ALPHANUMERIC_CHARS'. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly char[] ALPHANUMERIC_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':'};
		private const System.String SHIFT_JIS = "SJIS";
		private const System.String EUC_JP = "EUC_JP";
		private static bool ASSUME_SHIFT_JIS;
		private const System.String UTF8 = "UTF-8";
        // Redivivus.in Java to c# Porting update
        // 30/01/2010 
        // Commented & Added        
        private const System.String ISO88591 = "ISO-8859-1";
		
		private DecodedBitStreamParser()
		{
		}
		
		internal static DecoderResult decode(sbyte[] bytes, Version version, ErrorCorrectionLevel ecLevel)
		{
			BitSource bits = new BitSource(bytes);
			System.Text.StringBuilder result = new System.Text.StringBuilder(50);
			CharacterSetECI currentCharacterSetECI = null;
			bool fc1InEffect = false;
			System.Collections.ArrayList byteSegments = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(1));
			Mode mode;
			do 
			{
				// While still another segment to read...
				if (bits.available() < 4)
				{
					// OK, assume we're done. Really, a TERMINATOR mode should have been recorded here
					mode = Mode.TERMINATOR;
				}
				else
				{
					try
					{
						mode = Mode.forBits(bits.readBits(4)); // mode is encoded by 4 bits
					}
					catch (System.ArgumentException)
					{
						throw ReaderException.Instance;
					}
				}
				if (!mode.Equals(Mode.TERMINATOR))
				{
					if (mode.Equals(Mode.FNC1_FIRST_POSITION) || mode.Equals(Mode.FNC1_SECOND_POSITION))
					{
						// We do little with FNC1 except alter the parsed result a bit according to the spec
						fc1InEffect = true;
					}
					else if (mode.Equals(Mode.STRUCTURED_APPEND))
					{
						// not really supported; all we do is ignore it
						// Read next 8 bits (symbol sequence #) and 8 bits (parity data), then continue
						bits.readBits(16);
					}
					else if (mode.Equals(Mode.ECI))
					{
						// Count doesn't apply to ECI
						int value_Renamed = parseECIValue(bits);
						currentCharacterSetECI = CharacterSetECI.getCharacterSetECIByValue(value_Renamed);
						if (currentCharacterSetECI == null)
						{
							throw ReaderException.Instance;
						}
					}
					else
					{
						// How many characters will follow, encoded in this mode?
						int count = bits.readBits(mode.getCharacterCountBits(version));
						if (mode.Equals(Mode.NUMERIC))
						{
							decodeNumericSegment(bits, result, count);
						}
						else if (mode.Equals(Mode.ALPHANUMERIC))
						{
							decodeAlphanumericSegment(bits, result, count, fc1InEffect);
						}
						else if (mode.Equals(Mode.BYTE))
						{
							decodeByteSegment(bits, result, count, currentCharacterSetECI, byteSegments);
						}
						else if (mode.Equals(Mode.KANJI))
						{
							decodeKanjiSegment(bits, result, count);
						}
						else
						{
							throw ReaderException.Instance;
						}
					}
				}
			}
			while (!mode.Equals(Mode.TERMINATOR));
			
			return new DecoderResult(bytes, result.ToString(), (byteSegments.Count == 0)?null:byteSegments, ecLevel);
		}
		
		private static void  decodeKanjiSegment(BitSource bits, System.Text.StringBuilder result, int count)
		{
			// Each character will require 2 bytes. Read the characters as 2-byte pairs
			// and decode as Shift_JIS afterwards
			sbyte[] buffer = new sbyte[2 * count];
			int offset = 0;
			while (count > 0)
			{
				// Each 13 bits encodes a 2-byte character
				int twoBytes = bits.readBits(13);
				int assembledTwoBytes = ((twoBytes / 0x0C0) << 8) | (twoBytes % 0x0C0);
				if (assembledTwoBytes < 0x01F00)
				{
					// In the 0x8140 to 0x9FFC range
					assembledTwoBytes += 0x08140;
				}
				else
				{
					// In the 0xE040 to 0xEBBF range
					assembledTwoBytes += 0x0C140;
				}
				buffer[offset] = (sbyte) (assembledTwoBytes >> 8);
				buffer[offset + 1] = (sbyte) assembledTwoBytes;
				offset += 2;
				count--;
			}
			// Shift_JIS may not be supported in some environments:
			try
			{
				//UPGRADE_TODO: The differences in the Format  of parameters for constructor 'java.lang.String.String'  may cause compilation errors.  "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1092'"
				result.Append(System.Text.Encoding.GetEncoding(SHIFT_JIS).GetString(SupportClass.ToByteArray(buffer)));
			}
			catch (System.IO.IOException)
			{
				throw ReaderException.Instance;
			}
		}
		
		private static void  decodeByteSegment(BitSource bits, System.Text.StringBuilder result, int count, CharacterSetECI currentCharacterSetECI, System.Collections.ArrayList byteSegments)
		{
			sbyte[] readBytes = new sbyte[count];
			if (count << 3 > bits.available())
			{
				throw ReaderException.Instance;
			}
			for (int i = 0; i < count; i++)
			{
				readBytes[i] = (sbyte) bits.readBits(8);
			}
			System.String encoding;
			if (currentCharacterSetECI == null)
			{
				// The spec isn't clear on this mode; see
				// section 6.4.5: t does not say which encoding to assuming
				// upon decoding. I have seen ISO-8859-1 used as well as
				// Shift_JIS -- without anything like an ECI designator to
				// give a hint.
				encoding = guessEncoding(readBytes);
			}
			else
			{
				encoding = currentCharacterSetECI.EncodingName;
			}
			try
			{
				//UPGRADE_TODO: The differences in the Format  of parameters for constructor 'java.lang.String.String'  may cause compilation errors.  "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1092'"
				result.Append(System.Text.Encoding.GetEncoding(encoding).GetString(SupportClass.ToByteArray(readBytes)));
			}
			catch (System.IO.IOException)
			{
				throw ReaderException.Instance;
			}
			byteSegments.Add(SupportClass.ToByteArray(readBytes));
		}
		
		private static void  decodeAlphanumericSegment(BitSource bits, System.Text.StringBuilder result, int count, bool fc1InEffect)
		{
			// Read two characters at a time
			int start = result.Length;
			while (count > 1)
			{
				int nextTwoCharsBits = bits.readBits(11);
				result.Append(ALPHANUMERIC_CHARS[nextTwoCharsBits / 45]);
				result.Append(ALPHANUMERIC_CHARS[nextTwoCharsBits % 45]);
				count -= 2;
			}
			if (count == 1)
			{
				// special case: one character left
				result.Append(ALPHANUMERIC_CHARS[bits.readBits(6)]);
			}
			// See section 6.4.8.1, 6.4.8.2
			if (fc1InEffect)
			{
				// We need to massage the result a bit if in an FNC1 mode:
				for (int i = start; i < result.Length; i++)
				{
					if (result[i] == '%')
					{
						if (i < result.Length - 1 && result[i + 1] == '%')
						{
							// %% is rendered as %
							result.Remove(i + 1, 1);
						}
						else
						{
							// In alpha mode, % should be converted to FNC1 separator 0x1D
							result[i] = (char) 0x1D;
						}
					}
				}
			}
		}
		
		private static void  decodeNumericSegment(BitSource bits, System.Text.StringBuilder result, int count)
		{
			// Read three digits at a time
			while (count >= 3)
			{
				// Each 10 bits encodes three digits
				int threeDigitsBits = bits.readBits(10);
				if (threeDigitsBits >= 1000)
				{
					throw ReaderException.Instance;
				}
				result.Append(ALPHANUMERIC_CHARS[threeDigitsBits / 100]);
				result.Append(ALPHANUMERIC_CHARS[(threeDigitsBits / 10) % 10]);
				result.Append(ALPHANUMERIC_CHARS[threeDigitsBits % 10]);
				count -= 3;
			}
			if (count == 2)
			{
				// Two digits left over to read, encoded in 7 bits
				int twoDigitsBits = bits.readBits(7);
				if (twoDigitsBits >= 100)
				{
					throw ReaderException.Instance;
				}
				result.Append(ALPHANUMERIC_CHARS[twoDigitsBits / 10]);
				result.Append(ALPHANUMERIC_CHARS[twoDigitsBits % 10]);
			}
			else if (count == 1)
			{
				// One digit left over to read
				int digitBits = bits.readBits(4);
				if (digitBits >= 10)
				{
					throw ReaderException.Instance;
				}
				result.Append(ALPHANUMERIC_CHARS[digitBits]);
			}
		}
		
		private static System.String guessEncoding(sbyte[] bytes)
		{
			if (ASSUME_SHIFT_JIS)
			{
				return SHIFT_JIS;
			}
			// Does it start with the UTF-8 byte order mark? then guess it's UTF-8
			if (bytes.Length > 3 && bytes[0] == (sbyte) SupportClass.Identity(0xEF) && bytes[1] == (sbyte) SupportClass.Identity(0xBB) && bytes[2] == (sbyte) SupportClass.Identity(0xBF))
			{
				return UTF8;
			}
			// For now, merely tries to distinguish ISO-8859-1, UTF-8 and Shift_JIS,
			// which should be by far the most common encodings. ISO-8859-1
			// should not have bytes in the 0x80 - 0x9F range, while Shift_JIS
			// uses this as a first byte of a two-byte character. If we see this
			// followed by a valid second byte in Shift_JIS, assume it is Shift_JIS.
			// If we see something else in that second byte, we'll make the risky guess
			// that it's UTF-8.
			int length = bytes.Length;
			bool canBeISO88591 = true;
			bool canBeShiftJIS = true;
			int maybeDoubleByteCount = 0;
			int maybeSingleByteKatakanaCount = 0;
			bool sawLatin1Supplement = false;
			bool lastWasPossibleDoubleByteStart = false;
			for (int i = 0; i < length && (canBeISO88591 || canBeShiftJIS); i++)
			{
				int value_Renamed = bytes[i] & 0xFF;
				if ((value_Renamed == 0xC2 || value_Renamed == 0xC3) && i < length - 1)
				{
					// This is really a poor hack. The slightly more exotic characters people might want to put in
					// a QR Code, by which I mean the Latin-1 supplement characters (e.g. u-umlaut) have encodings
					// that start with 0xC2 followed by [0xA0,0xBF], or start with 0xC3 followed by [0x80,0xBF].
					int nextValue = bytes[i + 1] & 0xFF;
					if (nextValue <= 0xBF && ((value_Renamed == 0xC2 && nextValue >= 0xA0) || (value_Renamed == 0xC3 && nextValue >= 0x80)))
					{
						sawLatin1Supplement = true;
					}
				}
				if (value_Renamed >= 0x7F && value_Renamed <= 0x9F)
				{
					canBeISO88591 = false;
				}
				if (value_Renamed >= 0xA1 && value_Renamed <= 0xDF)
				{
					// count the number of characters that might be a Shift_JIS single-byte Katakana character
					if (!lastWasPossibleDoubleByteStart)
					{
						maybeSingleByteKatakanaCount++;
					}
				}
				if (!lastWasPossibleDoubleByteStart && ((value_Renamed >= 0xF0 && value_Renamed <= 0xFF) || value_Renamed == 0x80 || value_Renamed == 0xA0))
				{
					canBeShiftJIS = false;
				}
				if (((value_Renamed >= 0x81 && value_Renamed <= 0x9F) || (value_Renamed >= 0xE0 && value_Renamed <= 0xEF)))
				{
					// These start double-byte characters in Shift_JIS. Let's see if it's followed by a valid
					// second byte.
					if (lastWasPossibleDoubleByteStart)
					{
						// If we just checked this and the last byte for being a valid double-byte
						// char, don't check starting on this byte. If this and the last byte
						// formed a valid pair, then this shouldn't be checked to see if it starts
						// a double byte pair of course.
						lastWasPossibleDoubleByteStart = false;
					}
					else
					{
						// ... otherwise do check to see if this plus the next byte form a valid
						// double byte pair encoding a character.
						lastWasPossibleDoubleByteStart = true;
						if (i >= bytes.Length - 1)
						{
							canBeShiftJIS = false;
						}
						else
						{
							int nextValue = bytes[i + 1] & 0xFF;
							if (nextValue < 0x40 || nextValue > 0xFC)
							{
								canBeShiftJIS = false;
							}
							else
							{
								maybeDoubleByteCount++;
							}
							// There is some conflicting information out there about which bytes can follow which in
							// double-byte Shift_JIS characters. The rule above seems to be the one that matches practice.
						}
					}
				}
				else
				{
					lastWasPossibleDoubleByteStart = false;
				}
			}
			// Distinguishing Shift_JIS and ISO-8859-1 can be a little tough. The crude heuristic is:
			// - If we saw
			//   - at least three byte that starts a double-byte value (bytes that are rare in ISO-8859-1), or
			//   - over 5% of bytes that could be single-byte Katakana (also rare in ISO-8859-1),
			// - and, saw no sequences that are invalid in Shift_JIS, then we conclude Shift_JIS
			if (canBeShiftJIS && (maybeDoubleByteCount >= 3 || 20 * maybeSingleByteKatakanaCount > length))
			{
				return SHIFT_JIS;
			}
			// Otherwise, we default to ISO-8859-1 unless we know it can't be
			if (!sawLatin1Supplement && canBeISO88591)
			{
				return ISO88591;
			}
			// Otherwise, we take a wild guess with UTF-8
			return UTF8;
		}
		
		private static int parseECIValue(BitSource bits)
		{
			int firstByte = bits.readBits(8);
			if ((firstByte & 0x80) == 0)
			{
				// just one byte
				return firstByte & 0x7F;
			}
			else if ((firstByte & 0xC0) == 0x80)
			{
				// two bytes
				int secondByte = bits.readBits(8);
				return ((firstByte & 0x3F) << 8) | secondByte;
			}
			else if ((firstByte & 0xE0) == 0xC0)
			{
				// three bytes
				int secondThirdBytes = bits.readBits(16);
				return ((firstByte & 0x1F) << 16) | secondThirdBytes;
			}
			throw new System.ArgumentException("Bad ECI bits starting with byte " + firstByte);
		}
		static DecodedBitStreamParser()
		{
			{
                // Redivivus.in Java to c# Porting update
                // 30/01/2010 
                // Commented & Added
				//System.String platformDefault = System_Renamed.getProperty("file.encoding");
				//ASSUME_SHIFT_JIS = SHIFT_JIS.ToUpper().Equals(platformDefault.ToUpper()) || EUC_JP.ToUpper().Equals(platformDefault.ToUpper());
                ASSUME_SHIFT_JIS = false;
			}
		}
	}
}
