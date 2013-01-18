using System;
using System.Collections.Generic;
using System.Text;
using com.google.zxing.common;

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

namespace com.google.zxing.datamatrix.decoder
{

	using FormatException = com.google.zxing.FormatException;
	using BitSource = com.google.zxing.common.BitSource;
	using DecoderResult = com.google.zxing.common.DecoderResult;


	/// <summary>
	/// <p>Data Matrix Codes can encode text as bits in one of several modes, and can use multiple modes
	/// in one Data Matrix Code. This class decodes the bits back into text.</p>
	/// 
	/// <p>See ISO 16022:2006, 5.2.1 - 5.2.9.2</p>
	/// 
	/// @author bbrown@google.com (Brian Brown)
	/// @author Sean Owen
	/// </summary>
	internal sealed class DecodedBitStreamParser
	{

	  private enum Mode
	  {
		PAD_ENCODE, // Not really a mode
		ASCII_ENCODE,
		C40_ENCODE,
		TEXT_ENCODE,
		ANSIX12_ENCODE,
		EDIFACT_ENCODE,
		BASE256_ENCODE
	  }

	  /// <summary>
	  /// See ISO 16022:2006, Annex C Table C.1
	  /// The C40 Basic Character Set (*'s used for placeholders for the shift values)
	  /// </summary>
	  private static readonly char[] C40_BASIC_SET_CHARS = {'*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

	  private static readonly char[] C40_SHIFT2_SET_CHARS = {'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_'};

	  /// <summary>
	  /// See ISO 16022:2006, Annex C Table C.2
	  /// The Text Basic Character Set (*'s used for placeholders for the shift values)
	  /// </summary>
	  private static readonly char[] TEXT_BASIC_SET_CHARS = {'*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

	  private static readonly char[] TEXT_SHIFT3_SET_CHARS = {'\'', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '{', '|', '}', '~', (char) 127};

	  private DecodedBitStreamParser()
	  {
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: static com.google.zxing.common.DecoderResult decode(byte[] bytes) throws com.google.zxing.FormatException
	  internal static DecoderResult decode(sbyte[] bytes)
	  {
		BitSource bits = new BitSource(bytes);
		StringBuilder result = new StringBuilder(100);
		StringBuilder resultTrailer = new StringBuilder(0);
		IList<sbyte[]> byteSegments = new List<sbyte[]>(1);
		Mode mode = Mode.ASCII_ENCODE;
		do
		{
		  if (mode == Mode.ASCII_ENCODE)
		  {
			mode = decodeAsciiSegment(bits, result, resultTrailer);
		  }
		  else
		  {
			switch (mode)
			{
			  case com.google.zxing.datamatrix.decoder.DecodedBitStreamParser.Mode.C40_ENCODE:
				decodeC40Segment(bits, result);
				break;
			  case com.google.zxing.datamatrix.decoder.DecodedBitStreamParser.Mode.TEXT_ENCODE:
				decodeTextSegment(bits, result);
				break;
			  case com.google.zxing.datamatrix.decoder.DecodedBitStreamParser.Mode.ANSIX12_ENCODE:
				decodeAnsiX12Segment(bits, result);
				break;
			  case com.google.zxing.datamatrix.decoder.DecodedBitStreamParser.Mode.EDIFACT_ENCODE:
				decodeEdifactSegment(bits, result);
				break;
			  case com.google.zxing.datamatrix.decoder.DecodedBitStreamParser.Mode.BASE256_ENCODE:
				decodeBase256Segment(bits, result, byteSegments);
				break;
			  default:
				throw FormatException.FormatInstance;
			}
			mode = Mode.ASCII_ENCODE;
		  }
		} while (mode != Mode.PAD_ENCODE && bits.available() > 0);
		if (resultTrailer.Length > 0)
		{
		  result.Append(resultTrailer.ToString());
		}
		return new DecoderResult(bytes, result.ToString(), byteSegments.Count == 0 ? null : byteSegments, null);
	  }

	  /// <summary>
	  /// See ISO 16022:2006, 5.2.3 and Annex C, Table C.2
	  /// </summary>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static Mode decodeAsciiSegment(com.google.zxing.common.BitSource bits, StringBuilder result, StringBuilder resultTrailer) throws com.google.zxing.FormatException
	  private static Mode decodeAsciiSegment(BitSource bits, StringBuilder result, StringBuilder resultTrailer)
	  {
		bool upperShift = false;
		do
		{
		  int oneByte = bits.readBits(8);
		  if (oneByte == 0)
		  {
			throw FormatException.FormatInstance;
		  } // ASCII data (ASCII value + 1)
		  else if (oneByte <= 128)
		  {
			if (upperShift)
			{
			  oneByte += 128;
			  //upperShift = false;
			}
			result.Append((char)(oneByte - 1));
			return Mode.ASCII_ENCODE;
		  } // Pad
		  else if (oneByte == 129)
		  {
			return Mode.PAD_ENCODE;
		  } // 2-digit data 00-99 (Numeric Value + 130)
		  else if (oneByte <= 229)
		  {
			int value = oneByte - 130;
			if (value < 10) // padd with '0' for single digit values
			{
			  result.Append('0');
			}
			result.Append(value);
		  } // Latch to C40 encodation
		  else if (oneByte == 230)
		  {
			return Mode.C40_ENCODE;
		  } // Latch to Base 256 encodation
		  else if (oneByte == 231)
		  {
			return Mode.BASE256_ENCODE;
		  }
		  else if (oneByte == 232)
		  {
			// FNC1
			result.Append((char) 29); // translate as ASCII 29
		  }
		  else if (oneByte == 233 || oneByte == 234)
		  {
			// Structured Append, Reader Programming
			// Ignore these symbols for now
			//throw ReaderException.getInstance();
		  } // Upper Shift (shift to Extended ASCII)
		  else if (oneByte == 235)
		  {
			upperShift = true;
		  } // 05 Macro
		  else if (oneByte == 236)
		  {
			result.Append("[)>\u001E05\u001D");
			resultTrailer.Insert(0, "\u001E\u0004");
		  } // 06 Macro
		  else if (oneByte == 237)
		  {
			result.Append("[)>\u001E06\u001D");
			resultTrailer.Insert(0, "\u001E\u0004");
		  } // Latch to ANSI X12 encodation
		  else if (oneByte == 238)
		  {
			return Mode.ANSIX12_ENCODE;
		  } // Latch to Text encodation
		  else if (oneByte == 239)
		  {
			return Mode.TEXT_ENCODE;
		  } // Latch to EDIFACT encodation
		  else if (oneByte == 240)
		  {
			return Mode.EDIFACT_ENCODE;
		  } // ECI Character
		  else if (oneByte == 241)
		  {
			// TODO(bbrown): I think we need to support ECI
			//throw ReaderException.getInstance();
			// Ignore this symbol for now
		  } // Not to be used in ASCII encodation
		  else if (oneByte >= 242)
		  {
			// ... but work around encoders that end with 254, latch back to ASCII
			if (oneByte != 254 || bits.available() != 0)
			{
			  throw FormatException.FormatInstance;
			}
		  }
		} while (bits.available() > 0);
		return Mode.ASCII_ENCODE;
	  }

	  /// <summary>
	  /// See ISO 16022:2006, 5.2.5 and Annex C, Table C.1
	  /// </summary>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void decodeC40Segment(com.google.zxing.common.BitSource bits, StringBuilder result) throws com.google.zxing.FormatException
	  private static void decodeC40Segment(BitSource bits, StringBuilder result)
	  {
		// Three C40 values are encoded in a 16-bit value as
		// (1600 * C1) + (40 * C2) + C3 + 1
		// TODO(bbrown): The Upper Shift with C40 doesn't work in the 4 value scenario all the time
		bool upperShift = false;

		int[] cValues = new int[3];
		int shift = 0;

		do
		{
		  // If there is only one byte left then it will be encoded as ASCII
		  if (bits.available() == 8)
		  {
			return;
		  }
		  int firstByte = bits.readBits(8);
		  if (firstByte == 254) // Unlatch codeword
		  {
			return;
		  }

		  parseTwoBytes(firstByte, bits.readBits(8), cValues);

		  for (int i = 0; i < 3; i++)
		  {
			int cValue = cValues[i];
			switch (shift)
			{
			  case 0:
				if (cValue < 3)
				{
				  shift = cValue + 1;
				}
				else if (cValue < C40_BASIC_SET_CHARS.Length)
				{
				  char c40char = C40_BASIC_SET_CHARS[cValue];
				  if (upperShift)
				  {
					result.Append((char)(c40char + 128));
					upperShift = false;
				  }
				  else
				  {
					result.Append(c40char);
				  }
				}
				else
				{
				  throw FormatException.FormatInstance;
				}
				break;
			  case 1:
				if (upperShift)
				{
				  result.Append((char)(cValue + 128));
				  upperShift = false;
				}
				else
				{
				  result.Append((char) cValue);
				}
				shift = 0;
				break;
			  case 2:
				if (cValue < C40_SHIFT2_SET_CHARS.Length)
				{
				  char c40char = C40_SHIFT2_SET_CHARS[cValue];
				  if (upperShift)
				  {
					result.Append((char)(c40char + 128));
					upperShift = false;
				  }
				  else
				  {
					result.Append(c40char);
				  }
				} // FNC1
				else if (cValue == 27)
				{
				  result.Append((char) 29); // translate as ASCII 29
				} // Upper Shift
				else if (cValue == 30)
				{
				  upperShift = true;
				}
				else
				{
				  throw FormatException.FormatInstance;
				}
				shift = 0;
				break;
			  case 3:
				if (upperShift)
				{
				  result.Append((char)(cValue + 224));
				  upperShift = false;
				}
				else
				{
				  result.Append((char)(cValue + 96));
				}
				shift = 0;
				break;
			  default:
				throw FormatException.FormatInstance;
			}
		  }
		} while (bits.available() > 0);
	  }

	  /// <summary>
	  /// See ISO 16022:2006, 5.2.6 and Annex C, Table C.2
	  /// </summary>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void decodeTextSegment(com.google.zxing.common.BitSource bits, StringBuilder result) throws com.google.zxing.FormatException
	  private static void decodeTextSegment(BitSource bits, StringBuilder result)
	  {
		// Three Text values are encoded in a 16-bit value as
		// (1600 * C1) + (40 * C2) + C3 + 1
		// TODO(bbrown): The Upper Shift with Text doesn't work in the 4 value scenario all the time
		bool upperShift = false;

		int[] cValues = new int[3];
		int shift = 0;
		do
		{
		  // If there is only one byte left then it will be encoded as ASCII
		  if (bits.available() == 8)
		  {
			return;
		  }
		  int firstByte = bits.readBits(8);
		  if (firstByte == 254) // Unlatch codeword
		  {
			return;
		  }

		  parseTwoBytes(firstByte, bits.readBits(8), cValues);

		  for (int i = 0; i < 3; i++)
		  {
			int cValue = cValues[i];
			switch (shift)
			{
			  case 0:
				if (cValue < 3)
				{
				  shift = cValue + 1;
				}
				else if (cValue < TEXT_BASIC_SET_CHARS.Length)
				{
				  char textChar = TEXT_BASIC_SET_CHARS[cValue];
				  if (upperShift)
				  {
					result.Append((char)(textChar + 128));
					upperShift = false;
				  }
				  else
				  {
					result.Append(textChar);
				  }
				}
				else
				{
				  throw FormatException.FormatInstance;
				}
				break;
			  case 1:
				if (upperShift)
				{
				  result.Append((char)(cValue + 128));
				  upperShift = false;
				}
				else
				{
				  result.Append((char) cValue);
				}
				shift = 0;
				break;
			  case 2:
				// Shift 2 for Text is the same encoding as C40
				if (cValue < C40_SHIFT2_SET_CHARS.Length)
				{
				  char c40char = C40_SHIFT2_SET_CHARS[cValue];
				  if (upperShift)
				  {
					result.Append((char)(c40char + 128));
					upperShift = false;
				  }
				  else
				  {
					result.Append(c40char);
				  }
				} // FNC1
				else if (cValue == 27)
				{
				  result.Append((char) 29); // translate as ASCII 29
				} // Upper Shift
				else if (cValue == 30)
				{
				  upperShift = true;
				}
				else
				{
				  throw FormatException.FormatInstance;
				}
				shift = 0;
				break;
			  case 3:
				if (cValue < TEXT_SHIFT3_SET_CHARS.Length)
				{
				  char textChar = TEXT_SHIFT3_SET_CHARS[cValue];
				  if (upperShift)
				  {
					result.Append((char)(textChar + 128));
					upperShift = false;
				  }
				  else
				  {
					result.Append(textChar);
				  }
				  shift = 0;
				}
				else
				{
				  throw FormatException.FormatInstance;
				}
				break;
			  default:
				throw FormatException.FormatInstance;
			}
		  }
		} while (bits.available() > 0);
	  }

	  /// <summary>
	  /// See ISO 16022:2006, 5.2.7
	  /// </summary>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void decodeAnsiX12Segment(com.google.zxing.common.BitSource bits, StringBuilder result) throws com.google.zxing.FormatException
	  private static void decodeAnsiX12Segment(BitSource bits, StringBuilder result)
	  {
		// Three ANSI X12 values are encoded in a 16-bit value as
		// (1600 * C1) + (40 * C2) + C3 + 1

		int[] cValues = new int[3];
		do
		{
		  // If there is only one byte left then it will be encoded as ASCII
		  if (bits.available() == 8)
		  {
			return;
		  }
		  int firstByte = bits.readBits(8);
		  if (firstByte == 254) // Unlatch codeword
		  {
			return;
		  }

		  parseTwoBytes(firstByte, bits.readBits(8), cValues);

		  for (int i = 0; i < 3; i++)
		  {
			int cValue = cValues[i];
			if (cValue == 0) // X12 segment terminator <CR>
			{
			  result.Append('\r');
			} // X12 segment separator *
			else if (cValue == 1)
			{
			  result.Append('*');
			} // X12 sub-element separator >
			else if (cValue == 2)
			{
			  result.Append('>');
			} // space
			else if (cValue == 3)
			{
			  result.Append(' ');
			} // 0 - 9
			else if (cValue < 14)
			{
			  result.Append((char)(cValue + 44));
			} // A - Z
			else if (cValue < 40)
			{
			  result.Append((char)(cValue + 51));
			}
			else
			{
			  throw FormatException.FormatInstance;
			}
		  }
		} while (bits.available() > 0);
	  }

	  private static void parseTwoBytes(int firstByte, int secondByte, int[] result)
	  {
		int fullBitValue = (firstByte << 8) + secondByte - 1;
		int temp = fullBitValue / 1600;
		result[0] = temp;
		fullBitValue -= temp * 1600;
		temp = fullBitValue / 40;
		result[1] = temp;
		result[2] = fullBitValue - temp * 40;
	  }

	  /// <summary>
	  /// See ISO 16022:2006, 5.2.8 and Annex C Table C.3
	  /// </summary>
	  private static void decodeEdifactSegment(BitSource bits, StringBuilder result)
	  {
		do
		{
		  // If there is only two or less bytes left then it will be encoded as ASCII
		  if (bits.available() <= 16)
		  {
			return;
		  }

		  for (int i = 0; i < 4; i++)
		  {
			int edifactValue = bits.readBits(6);

			// Check for the unlatch character
			if (edifactValue == 0x1F) // 011111
			{
			  // Read rest of byte, which should be 0, and stop
			  int bitsLeft = 8 - bits.BitOffset;
			  if (bitsLeft != 8)
			  {
				bits.readBits(bitsLeft);
			  }
			  return;
			}

			if ((edifactValue & 0x20) == 0) // no 1 in the leading (6th) bit
			{
			  edifactValue |= 0x40; // Add a leading 01 to the 6 bit binary value
			}
			result.Append((char) edifactValue);
		  }
		} while (bits.available() > 0);
	  }

	  /// <summary>
	  /// See ISO 16022:2006, 5.2.9 and Annex B, B.2
	  /// </summary>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void decodeBase256Segment(com.google.zxing.common.BitSource bits, StringBuilder result, java.util.Collection<byte[]> byteSegments) throws com.google.zxing.FormatException
	  private static void decodeBase256Segment(BitSource bits, StringBuilder result, ICollection<sbyte[]> byteSegments)
	  {
		// Figure out how long the Base 256 Segment is.
		int codewordPosition = 1 + bits.ByteOffset; // position is 1-indexed
		int d1 = unrandomize255State(bits.readBits(8), codewordPosition++);
		int count;
		if (d1 == 0) // Read the remainder of the symbol
		{
		  count = bits.available() / 8;
		}
		else if (d1 < 250)
		{
		  count = d1;
		}
		else
		{
		  count = 250 * (d1 - 249) + unrandomize255State(bits.readBits(8), codewordPosition++);
		}

		// We're seeing NegativeArraySizeException errors from users.
		if (count < 0)
		{
		  throw FormatException.FormatInstance;
		}

		sbyte[] bytes = new sbyte[count];
		for (int i = 0; i < count; i++)
		{
		  // Have seen this particular error in the wild, such as at
		  // http://www.bcgen.com/demo/IDAutomationStreamingDataMatrix.aspx?MODE=3&D=Fred&PFMT=3&PT=F&X=0.3&O=0&LM=0.2
		  if (bits.available() < 8)
		  {
			throw FormatException.FormatInstance;
		  }
		  bytes[i] = (sbyte) unrandomize255State(bits.readBits(8), codewordPosition++);
		}
		byteSegments.Add(bytes);
		try
		{
          //result.Append(new string(bytes, "ISO8859_1"));
            result.Append(GetEncodedStringFromBuffer(bytes, "ISO-8859-1"));
		}
        catch (System.IO.IOException uee)
		{
		  throw new InvalidOperationException("Platform does not support required encoding: " + uee);
		}
	  }
      private static string GetEncodedStringFromBuffer(sbyte[] buffer, string encoding)
      {
          byte[] bytes = buffer.ToBytes();
          Encoding en = Encoding.GetEncoding(encoding);
          return en.GetString(bytes);
      }

	  /// <summary>
	  /// See ISO 16022:2006, Annex B, B.2
	  /// </summary>
	  private static int unrandomize255State(int randomizedBase256Codeword, int base256CodewordPosition)
	  {
		int pseudoRandomNumber = ((149 * base256CodewordPosition) % 255) + 1;
		int tempVariable = randomizedBase256Codeword - pseudoRandomNumber;
		return tempVariable >= 0 ? tempVariable : tempVariable + 256;
	  }

	}

}