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
using ReaderException = com.google.zxing.ReaderException;
using BitSource = com.google.zxing.common.BitSource;
using DecoderResult = com.google.zxing.common.DecoderResult;
namespace com.google.zxing.datamatrix.decoder
{
	
	/// <summary> <p>Data Matrix Codes can encode text as bits in one of several modes, and can use multiple modes
	/// in one Data Matrix Code. This class decodes the bits back into text.</p>
	/// 
	/// <p>See ISO 16022:2006, 5.2.1 - 5.2.9.2</p>
	/// 
	/// </summary>
	/// <author>  bbrown@google.com (Brian Brown)
	/// </author>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class DecodedBitStreamParser
	{
		
		/// <summary> See ISO 16022:2006, Annex C Table C.1
		/// The C40 Basic Character Set (*'s used for placeholders for the shift values)
		/// </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'C40_BASIC_SET_CHARS'. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly char[] C40_BASIC_SET_CHARS = new char[]{'*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'C40_SHIFT2_SET_CHARS'. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly char[] C40_SHIFT2_SET_CHARS = new char[]{'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_'};
		
		/// <summary> See ISO 16022:2006, Annex C Table C.2
		/// The Text Basic Character Set (*'s used for placeholders for the shift values)
		/// </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'TEXT_BASIC_SET_CHARS'. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly char[] TEXT_BASIC_SET_CHARS = new char[]{'*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
		
		private static char[] TEXT_SHIFT3_SET_CHARS = new char[]{'\'', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '{', '|', '}', '~', (char) 127};
		
		private const int PAD_ENCODE = 0; // Not really an encoding
		private const int ASCII_ENCODE = 1;
		private const int C40_ENCODE = 2;
		private const int TEXT_ENCODE = 3;
		private const int ANSIX12_ENCODE = 4;
		private const int EDIFACT_ENCODE = 5;
		private const int BASE256_ENCODE = 6;
		
		private DecodedBitStreamParser()
		{
		}
		
		internal static DecoderResult decode(sbyte[] bytes)
		{
			BitSource bits = new BitSource(bytes);
			System.Text.StringBuilder result = new System.Text.StringBuilder(100);
			System.Text.StringBuilder resultTrailer = new System.Text.StringBuilder(0);
			System.Collections.ArrayList byteSegments = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(1));
			int mode = ASCII_ENCODE;
			do 
			{
				if (mode == ASCII_ENCODE)
				{
					mode = decodeAsciiSegment(bits, result, resultTrailer);
				}
				else
				{
					switch (mode)
					{
						
						case C40_ENCODE: 
							decodeC40Segment(bits, result);
							break;
						
						case TEXT_ENCODE: 
							decodeTextSegment(bits, result);
							break;
						
						case ANSIX12_ENCODE: 
							decodeAnsiX12Segment(bits, result);
							break;
						
						case EDIFACT_ENCODE: 
							decodeEdifactSegment(bits, result);
							break;
						
						case BASE256_ENCODE: 
							decodeBase256Segment(bits, result, byteSegments);
							break;
						
						default: 
							throw ReaderException.Instance;
						
					}
					mode = ASCII_ENCODE;
				}
			}
			while (mode != PAD_ENCODE && bits.available() > 0);
			if (resultTrailer.Length > 0)
			{
				result.Append(resultTrailer.ToString());
			}
			return new DecoderResult(bytes, result.ToString(), (byteSegments.Count == 0)?null:byteSegments, null);
		}
		
		/// <summary> See ISO 16022:2006, 5.2.3 and Annex C, Table C.2</summary>
		private static int decodeAsciiSegment(BitSource bits, System.Text.StringBuilder result, System.Text.StringBuilder resultTrailer)
		{
			bool upperShift = false;
			do 
			{
				int oneByte = bits.readBits(8);
				if (oneByte == 0)
				{
					throw ReaderException.Instance;
				}
				else if (oneByte <= 128)
				{
					// ASCII data (ASCII value + 1)
					oneByte = upperShift?(oneByte + 128):oneByte;
					upperShift = false;
					result.Append((char) (oneByte - 1));
					return ASCII_ENCODE;
				}
				else if (oneByte == 129)
				{
					// Pad
					return PAD_ENCODE;
				}
				else if (oneByte <= 229)
				{
					// 2-digit data 00-99 (Numeric Value + 130)
					int value_Renamed = oneByte - 130;
					if (value_Renamed < 10)
					{
						// padd with '0' for single digit values
						result.Append('0');
					}
					result.Append(value_Renamed);
				}
				else if (oneByte == 230)
				{
					// Latch to C40 encodation
					return C40_ENCODE;
				}
				else if (oneByte == 231)
				{
					// Latch to Base 256 encodation
					return BASE256_ENCODE;
				}
				else if (oneByte == 232)
				{
					// FNC1
					//throw ReaderException.getInstance();
					// Ignore this symbol for now
				}
				else if (oneByte == 233)
				{
					// Structured Append
					//throw ReaderException.getInstance();
					// Ignore this symbol for now
				}
				else if (oneByte == 234)
				{
					// Reader Programming
					//throw ReaderException.getInstance();
					// Ignore this symbol for now
				}
				else if (oneByte == 235)
				{
					// Upper Shift (shift to Extended ASCII)
					upperShift = true;
				}
				else if (oneByte == 236)
				{
					// 05 Macro
					result.Append("[)>\u001E05\u001D");
					resultTrailer.Insert(0, "\u001E\u0004");
				}
				else if (oneByte == 237)
				{
					// 06 Macro
					result.Append("[)>\u001E06\u001D");
					resultTrailer.Insert(0, "\u001E\u0004");
				}
				else if (oneByte == 238)
				{
					// Latch to ANSI X12 encodation
					return ANSIX12_ENCODE;
				}
				else if (oneByte == 239)
				{
					// Latch to Text encodation
					return TEXT_ENCODE;
				}
				else if (oneByte == 240)
				{
					// Latch to EDIFACT encodation
					return EDIFACT_ENCODE;
				}
				else if (oneByte == 241)
				{
					// ECI Character
					// TODO(bbrown): I think we need to support ECI
					//throw ReaderException.getInstance();
					// Ignore this symbol for now
				}
				else if (oneByte >= 242)
				{
					// Not to be used in ASCII encodation
					throw ReaderException.Instance;
				}
			}
			while (bits.available() > 0);
			return ASCII_ENCODE;
		}
		
		/// <summary> See ISO 16022:2006, 5.2.5 and Annex C, Table C.1</summary>
		private static void  decodeC40Segment(BitSource bits, System.Text.StringBuilder result)
		{
			// Three C40 values are encoded in a 16-bit value as
			// (1600 * C1) + (40 * C2) + C3 + 1
			// TODO(bbrown): The Upper Shift with C40 doesn't work in the 4 value scenario all the time
			bool upperShift = false;
			
			int[] cValues = new int[3];
			do 
			{
				// If there is only one byte left then it will be encoded as ASCII
				if (bits.available() == 8)
				{
					return ;
				}
				int firstByte = bits.readBits(8);
				if (firstByte == 254)
				{
					// Unlatch codeword
					return ;
				}
				
				parseTwoBytes(firstByte, bits.readBits(8), cValues);
				
				int shift = 0;
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
							else
							{
								if (upperShift)
								{
									result.Append((char) (C40_BASIC_SET_CHARS[cValue] + 128));
									upperShift = false;
								}
								else
								{
									result.Append(C40_BASIC_SET_CHARS[cValue]);
								}
							}
							break;
						
						case 1: 
							if (upperShift)
							{
								result.Append((char) (cValue + 128));
								upperShift = false;
							}
							else
							{
								result.Append(cValue);
							}
							shift = 0;
							break;
						
						case 2: 
							if (cValue < 27)
							{
								if (upperShift)
								{
									result.Append((char) (C40_SHIFT2_SET_CHARS[cValue] + 128));
									upperShift = false;
								}
								else
								{
									result.Append(C40_SHIFT2_SET_CHARS[cValue]);
								}
							}
							else if (cValue == 27)
							{
								// FNC1
								throw ReaderException.Instance;
							}
							else if (cValue == 30)
							{
								// Upper Shift
								upperShift = true;
							}
							else
							{
								throw ReaderException.Instance;
							}
							shift = 0;
							break;
						
						case 3: 
							if (upperShift)
							{
								result.Append((char) (cValue + 224));
								upperShift = false;
							}
							else
							{
								result.Append((char) (cValue + 96));
							}
							shift = 0;
							break;
						
						default: 
							throw ReaderException.Instance;
						
					}
				}
			}
			while (bits.available() > 0);
		}
		
		/// <summary> See ISO 16022:2006, 5.2.6 and Annex C, Table C.2</summary>
		private static void  decodeTextSegment(BitSource bits, System.Text.StringBuilder result)
		{
			// Three Text values are encoded in a 16-bit value as
			// (1600 * C1) + (40 * C2) + C3 + 1
			// TODO(bbrown): The Upper Shift with Text doesn't work in the 4 value scenario all the time
			bool upperShift = false;
			
			int[] cValues = new int[3];
			do 
			{
				// If there is only one byte left then it will be encoded as ASCII
				if (bits.available() == 8)
				{
					return ;
				}
				int firstByte = bits.readBits(8);
				if (firstByte == 254)
				{
					// Unlatch codeword
					return ;
				}
				
				parseTwoBytes(firstByte, bits.readBits(8), cValues);
				
				int shift = 0;
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
							else
							{
								if (upperShift)
								{
									result.Append((char) (TEXT_BASIC_SET_CHARS[cValue] + 128));
									upperShift = false;
								}
								else
								{
									result.Append(TEXT_BASIC_SET_CHARS[cValue]);
								}
							}
							break;
						
						case 1: 
							if (upperShift)
							{
								result.Append((char) (cValue + 128));
								upperShift = false;
							}
							else
							{
								result.Append(cValue);
							}
							shift = 0;
							break;
						
						case 2: 
							// Shift 2 for Text is the same encoding as C40
							if (cValue < 27)
							{
								if (upperShift)
								{
									result.Append((char) (C40_SHIFT2_SET_CHARS[cValue] + 128));
									upperShift = false;
								}
								else
								{
									result.Append(C40_SHIFT2_SET_CHARS[cValue]);
								}
							}
							else if (cValue == 27)
							{
								// FNC1
								throw ReaderException.Instance;
							}
							else if (cValue == 30)
							{
								// Upper Shift
								upperShift = true;
							}
							else
							{
								throw ReaderException.Instance;
							}
							shift = 0;
							break;
						
						case 3: 
							if (upperShift)
							{
								result.Append((char) (TEXT_SHIFT3_SET_CHARS[cValue] + 128));
								upperShift = false;
							}
							else
							{
								result.Append(TEXT_SHIFT3_SET_CHARS[cValue]);
							}
							shift = 0;
							break;
						
						default: 
							throw ReaderException.Instance;
						
					}
				}
			}
			while (bits.available() > 0);
		}
		
		/// <summary> See ISO 16022:2006, 5.2.7</summary>
		private static void  decodeAnsiX12Segment(BitSource bits, System.Text.StringBuilder result)
		{
			// Three ANSI X12 values are encoded in a 16-bit value as
			// (1600 * C1) + (40 * C2) + C3 + 1
			
			int[] cValues = new int[3];
			do 
			{
				// If there is only one byte left then it will be encoded as ASCII
				if (bits.available() == 8)
				{
					return ;
				}
				int firstByte = bits.readBits(8);
				if (firstByte == 254)
				{
					// Unlatch codeword
					return ;
				}
				
				parseTwoBytes(firstByte, bits.readBits(8), cValues);
				
				for (int i = 0; i < 3; i++)
				{
					int cValue = cValues[i];
					if (cValue == 0)
					{
						// X12 segment terminator <CR>
						result.Append('\r');
					}
					else if (cValue == 1)
					{
						// X12 segment separator *
						result.Append('*');
					}
					else if (cValue == 2)
					{
						// X12 sub-element separator >
						result.Append('>');
					}
					else if (cValue == 3)
					{
						// space
						result.Append(' ');
					}
					else if (cValue < 14)
					{
						// 0 - 9
						result.Append((char) (cValue + 44));
					}
					else if (cValue < 40)
					{
						// A - Z
						result.Append((char) (cValue + 51));
					}
					else
					{
						throw ReaderException.Instance;
					}
				}
			}
			while (bits.available() > 0);
		}
		
		private static void  parseTwoBytes(int firstByte, int secondByte, int[] result)
		{
			int fullBitValue = (firstByte << 8) + secondByte - 1;
			int temp = fullBitValue / 1600;
			result[0] = temp;
			fullBitValue -= temp * 1600;
			temp = fullBitValue / 40;
			result[1] = temp;
			result[2] = fullBitValue - temp * 40;
		}
		
		/// <summary> See ISO 16022:2006, 5.2.8 and Annex C Table C.3</summary>
		private static void  decodeEdifactSegment(BitSource bits, System.Text.StringBuilder result)
		{
			bool unlatch = false;
			do 
			{
				// If there is only two or less bytes left then it will be encoded as ASCII
				if (bits.available() <= 16)
				{
					return ;
				}
				
				for (int i = 0; i < 4; i++)
				{
					int edifactValue = bits.readBits(6);
					
					// Check for the unlatch character
					if (edifactValue == 0x2B67)
					{
						// 011111
						unlatch = true;
						// If we encounter the unlatch code then continue reading because the Codeword triple
						// is padded with 0's
					}
					
					if (!unlatch)
					{
						if ((edifactValue & 32) == 0)
						{
							// no 1 in the leading (6th) bit
							edifactValue |= 64; // Add a leading 01 to the 6 bit binary value
						}
						result.Append(edifactValue);
					}
				}
			}
			while (!unlatch && bits.available() > 0);
		}
		
		/// <summary> See ISO 16022:2006, 5.2.9 and Annex B, B.2</summary>
		private static void  decodeBase256Segment(BitSource bits, System.Text.StringBuilder result, System.Collections.ArrayList byteSegments)
		{
			// Figure out how long the Base 256 Segment is.
			int d1 = bits.readBits(8);
			int count;
			if (d1 == 0)
			{
				// Read the remainder of the symbol
				count = bits.available() / 8;
			}
			else if (d1 < 250)
			{
				count = d1;
			}
			else
			{
				count = 250 * (d1 - 249) + bits.readBits(8);
			}
			sbyte[] bytes = new sbyte[count];
			for (int i = 0; i < count; i++)
			{
				bytes[i] = unrandomize255State(bits.readBits(8), i);
			}
			byteSegments.Add(SupportClass.ToByteArray(bytes));
			try
			{
				//UPGRADE_TODO: The differences in the Format  of parameters for constructor 'java.lang.String.String'  may cause compilation errors.  "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1092'"
				result.Append(System.Text.Encoding.GetEncoding("ISO8859_1").GetString(SupportClass.ToByteArray(bytes)));
			}
			catch (System.IO.IOException uee)
			{
				//UPGRADE_TODO: The equivalent in .NET for method 'java.lang.Throwable.toString' may return a different value. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1043'"
				throw new System.SystemException("Platform does not support required encoding: " + uee);
			}
		}
		
		/// <summary> See ISO 16022:2006, Annex B, B.2</summary>
		private static sbyte unrandomize255State(int randomizedBase256Codeword, int base256CodewordPosition)
		{
			int pseudoRandomNumber = ((149 * base256CodewordPosition) % 255) + 1;
			int tempVariable = randomizedBase256Codeword - pseudoRandomNumber;
			return (sbyte) (tempVariable >= 0?tempVariable:(tempVariable + 256));
		}
	}
}