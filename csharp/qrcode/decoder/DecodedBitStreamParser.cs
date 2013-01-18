using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

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

namespace com.google.zxing.qrcode.decoder
{

	using DecodeHintType = com.google.zxing.DecodeHintType;
	using FormatException = com.google.zxing.FormatException;
	using BitSource = com.google.zxing.common.BitSource;
	using CharacterSetECI = com.google.zxing.common.CharacterSetECI;
	using DecoderResult = com.google.zxing.common.DecoderResult;
	using StringUtils = com.google.zxing.common.StringUtils;
    using com.google.zxing.common;


	/// <summary>
	/// <p>QR Codes can encode text as bits in one of several modes, and can use multiple modes
	/// in one QR Code. This class decodes the bits back into text.</p>
	/// 
	/// <p>See ISO 18004:2006, 6.4.3 - 6.4.7</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	internal sealed class DecodedBitStreamParser
	{

	  /// <summary>
	  /// See ISO 18004:2006, 6.4.4 Table 5
	  /// </summary>
	  private static readonly char[] ALPHANUMERIC_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':'};
	  private const int GB2312_SUBSET = 1;

	  private DecodedBitStreamParser()
	  {
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: static com.google.zxing.common.DecoderResult decode(byte[] bytes, Version version, ErrorCorrectionLevel ecLevel, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.FormatException
      internal static DecoderResult decode(sbyte[] bytes, Version version, ErrorCorrectionLevel ecLevel, IDictionary<DecodeHintType, object> hints)
	  {
		BitSource bits = new BitSource(bytes);
		StringBuilder result = new StringBuilder(50);
		IList<sbyte[]> byteSegments = new List<sbyte[]>(1);
		try
		{
		  CharacterSetECI currentCharacterSetECI = null;
		  bool fc1InEffect = false;
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
			  mode = Mode.forBits(bits.readBits(4)); // mode is encoded by 4 bits
			}
			if (mode != Mode.TERMINATOR)
			{
			  if (mode == Mode.FNC1_FIRST_POSITION || mode == Mode.FNC1_SECOND_POSITION)
			  {
				// We do little with FNC1 except alter the parsed result a bit according to the spec
				fc1InEffect = true;
			  }
			  else if (mode == Mode.STRUCTURED_APPEND)
			  {
				if (bits.available() < 16)
				{
				  throw FormatException.FormatInstance;
				}
				// not really supported; all we do is ignore it
				// Read next 8 bits (symbol sequence #) and 8 bits (parity data), then continue
				bits.readBits(16);
			  }
			  else if (mode == Mode.ECI)
			  {
				// Count doesn't apply to ECI
				int value = parseECIValue(bits);
				currentCharacterSetECI = CharacterSetECI.getCharacterSetECIByValue(value);
				if (currentCharacterSetECI == null)
				{
				  throw FormatException.FormatInstance;
				}
			  }
			  else
			  {
				// First handle Hanzi mode which does not start with character count
				if (mode == Mode.HANZI)
				{
				  //chinese mode contains a sub set indicator right after mode indicator
				  int subset = bits.readBits(4);
				  int countHanzi = bits.readBits(mode.getCharacterCountBits(version));
				  if (subset == GB2312_SUBSET)
				  {
					decodeHanziSegment(bits, result, countHanzi);
				  }
				}
				else
				{
				  // "Normal" QR code modes:
				  // How many characters will follow, encoded in this mode?
				  int count = bits.readBits(mode.getCharacterCountBits(version));
				  if (mode == Mode.NUMERIC)
				  {
					decodeNumericSegment(bits, result, count);
				  }
				  else if (mode == Mode.ALPHANUMERIC)
				  {
					decodeAlphanumericSegment(bits, result, count, fc1InEffect);
				  }
				  else if (mode == Mode.BYTE)
				  {
					decodeByteSegment(bits, result, count, currentCharacterSetECI, byteSegments, hints);
				  }
				  else if (mode == Mode.KANJI)
				  {
					decodeKanjiSegment(bits, result, count);
				  }
				  else
				  {
					throw FormatException.FormatInstance;
				  }
				}
			  }
			}
		  } while (mode != Mode.TERMINATOR);
		}
		catch (System.ArgumentException iae)
		{
		  // from readBits() calls
		  throw FormatException.FormatInstance;
		}

		return new DecoderResult(bytes, result.ToString(), byteSegments.Count == 0 ? null : byteSegments, ecLevel == null ? null : ecLevel.ToString());
	  }

	  /// <summary>
	  /// See specification GBT 18284-2000
	  /// </summary>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void decodeHanziSegment(com.google.zxing.common.BitSource bits, StringBuilder result, int count) throws com.google.zxing.FormatException
	  private static void decodeHanziSegment(BitSource bits, StringBuilder result, int count)
	  {
		// Don't crash trying to read more bits than we have available.
		if (count * 13 > bits.available())
		{
		  throw FormatException.FormatInstance;
		}

		// Each character will require 2 bytes. Read the characters as 2-byte pairs
		// and decode as GB2312 afterwards
		sbyte[] buffer = new sbyte[2 * count];
		int offset = 0;
		while (count > 0)
		{
		  // Each 13 bits encodes a 2-byte character
		  int twoBytes = bits.readBits(13);
		  int assembledTwoBytes = ((twoBytes / 0x060) << 8) | (twoBytes % 0x060);
		  if (assembledTwoBytes < 0x003BF)
		  {
			// In the 0xA1A1 to 0xAAFE range
			assembledTwoBytes += 0x0A1A1;
		  }
		  else
		  {
			// In the 0xB0A1 to 0xFAFE range
			assembledTwoBytes += 0x0A6A1;
		  }
		  buffer[offset] = (sbyte)((assembledTwoBytes >> 8) & 0xFF);
		  buffer[offset + 1] = (sbyte)(assembledTwoBytes & 0xFF);
		  offset += 2;
		  count--;
		}

		try
		{
		    
          //result.Append(new string(buffer,  StringUtils.GB2312));
          result.Append(GetEncodedStringFromBuffer(buffer, StringUtils.GB2312));
		}
        catch (System.IO.IOException)
		{
		  throw FormatException.FormatInstance;
		}
	  }

      private static string GetEncodedStringFromBuffer(sbyte[] buffer, string encoding)
      {
          byte[] bytes = buffer.ToBytes();
          Encoding en = Encoding.GetEncoding(encoding);
          return en.GetString(bytes);
      }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void decodeKanjiSegment(com.google.zxing.common.BitSource bits, StringBuilder result, int count) throws com.google.zxing.FormatException
	  private static void decodeKanjiSegment(BitSource bits, StringBuilder result, int count)
	  {
		// Don't crash trying to read more bits than we have available.
		if (count * 13 > bits.available())
		{
		  throw FormatException.FormatInstance;
		}

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
		  buffer[offset] = (sbyte)(assembledTwoBytes >> 8);
		  buffer[offset + 1] = (sbyte) assembledTwoBytes;
		  offset += 2;
		  count--;
		}
		// Shift_JIS may not be supported in some environments:
		try
		{
          //result.Append(new string(buffer, StringUtils.SHIFT_JIS));
            result.Append(GetEncodedStringFromBuffer(buffer, StringUtils.SHIFT_JIS));
		}
        catch (System.IO.IOException)
		{
		  throw FormatException.FormatInstance;
		}
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void decodeByteSegment(com.google.zxing.common.BitSource bits, StringBuilder result, int count, com.google.zxing.common.CharacterSetECI currentCharacterSetECI, java.util.Collection<byte[]> byteSegments, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.FormatException
      private static void decodeByteSegment(BitSource bits, StringBuilder result, int count, CharacterSetECI currentCharacterSetECI, ICollection<sbyte[]> byteSegments, IDictionary<DecodeHintType, object> hints)
	  {
		// Don't crash trying to read more bits than we have available.
		if (count << 3 > bits.available())
		{
		  throw FormatException.FormatInstance;
		}

		sbyte[] readBytes = new sbyte[count];
		for (int i = 0; i < count; i++)
		{
		  readBytes[i] = (sbyte) bits.readBits(8);
		}
		string encoding;
		if (currentCharacterSetECI == null)
		{
		  // The spec isn't clear on this mode; see
		  // section 6.4.5: t does not say which encoding to assuming
		  // upon decoding. I have seen ISO-8859-1 used as well as
		  // Shift_JIS -- without anything like an ECI designator to
		  // give a hint.
		  encoding = StringUtils.guessEncoding(readBytes, hints);
		}
		else
		{
		  encoding = currentCharacterSetECI.name();
		}
		try
		{
          //result.Append(new string(readBytes, encoding));
            result.Append(GetEncodedStringFromBuffer(readBytes, encoding));
		}
        catch (System.IO.IOException)
		{
		  throw FormatException.FormatInstance;
		}
		byteSegments.Add(readBytes);
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static char toAlphaNumericChar(int value) throws com.google.zxing.FormatException
	  private static char toAlphaNumericChar(int value)
	  {
		if (value >= ALPHANUMERIC_CHARS.Length)
		{
		  throw FormatException.FormatInstance;
		}
		return ALPHANUMERIC_CHARS[value];
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void decodeAlphanumericSegment(com.google.zxing.common.BitSource bits, StringBuilder result, int count, boolean fc1InEffect) throws com.google.zxing.FormatException
	  private static void decodeAlphanumericSegment(BitSource bits, StringBuilder result, int count, bool fc1InEffect)
	  {
		// Read two characters at a time
		int start = result.Length;
		while (count > 1)
		{
		  if (bits.available() < 11)
		  {
			throw FormatException.FormatInstance;
		  }
		  int nextTwoCharsBits = bits.readBits(11);
		  result.Append(toAlphaNumericChar(nextTwoCharsBits / 45));
		  result.Append(toAlphaNumericChar(nextTwoCharsBits % 45));
		  count -= 2;
		}
		if (count == 1)
		{
		  // special case: one character left
		  if (bits.available() < 6)
		  {
			throw FormatException.FormatInstance;
		  }
		  result.Append(toAlphaNumericChar(bits.readBits(6)));
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

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void decodeNumericSegment(com.google.zxing.common.BitSource bits, StringBuilder result, int count) throws com.google.zxing.FormatException
	  private static void decodeNumericSegment(BitSource bits, StringBuilder result, int count)
	  {
		// Read three digits at a time
		while (count >= 3)
		{
		  // Each 10 bits encodes three digits
		  if (bits.available() < 10)
		  {
			throw FormatException.FormatInstance;
		  }
		  int threeDigitsBits = bits.readBits(10);
		  if (threeDigitsBits >= 1000)
		  {
			throw FormatException.FormatInstance;
		  }
		  result.Append(toAlphaNumericChar(threeDigitsBits / 100));
		  result.Append(toAlphaNumericChar((threeDigitsBits / 10) % 10));
		  result.Append(toAlphaNumericChar(threeDigitsBits % 10));
		  count -= 3;
		}
		if (count == 2)
		{
		  // Two digits left over to read, encoded in 7 bits
		  if (bits.available() < 7)
		  {
			throw FormatException.FormatInstance;
		  }
		  int twoDigitsBits = bits.readBits(7);
		  if (twoDigitsBits >= 100)
		  {
			throw FormatException.FormatInstance;
		  }
		  result.Append(toAlphaNumericChar(twoDigitsBits / 10));
		  result.Append(toAlphaNumericChar(twoDigitsBits % 10));
		}
		else if (count == 1)
		{
		  // One digit left over to read
		  if (bits.available() < 4)
		  {
			throw FormatException.FormatInstance;
		  }
		  int digitBits = bits.readBits(4);
		  if (digitBits >= 10)
		  {
			throw FormatException.FormatInstance;
		  }
		  result.Append(toAlphaNumericChar(digitBits));
		}
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static int parseECIValue(com.google.zxing.common.BitSource bits) throws com.google.zxing.FormatException
	  private static int parseECIValue(BitSource bits)
	  {
		int firstByte = bits.readBits(8);
		if ((firstByte & 0x80) == 0)
		{
		  // just one byte
		  return firstByte & 0x7F;
		}
		if ((firstByte & 0xC0) == 0x80)
		{
		  // two bytes
		  int secondByte = bits.readBits(8);
		  return ((firstByte & 0x3F) << 8) | secondByte;
		}
		if ((firstByte & 0xE0) == 0xC0)
		{
		  // three bytes
		  int secondThirdBytes = bits.readBits(16);
		  return ((firstByte & 0x1F) << 16) | secondThirdBytes;
		}
		throw FormatException.FormatInstance;
	  }

	}

}