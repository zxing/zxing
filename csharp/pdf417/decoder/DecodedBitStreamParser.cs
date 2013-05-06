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

using System;
using System.Text;

#if SILVERLIGHT4 || SILVERLIGHT5 || NET40 || NET45 || NETFX_CORE
using System.Numerics;
#else
using BigIntegerLibrary;
#endif

using ZXing.Common;

namespace ZXing.PDF417.Internal
{
    /// <summary>
    /// <p>This class contains the methods for decoding the PDF417 codewords.</p>
    ///
    /// <author>SITA Lab (kevin.osullivan@sita.aero)</author>
    /// </summary>
    internal static class DecodedBitStreamParser
    {
        private enum Mode
        {
            ALPHA,
            LOWER,
            MIXED,
            PUNCT,
            ALPHA_SHIFT,
            PUNCT_SHIFT
        }

        private const int TEXT_COMPACTION_MODE_LATCH = 900;
        private const int BYTE_COMPACTION_MODE_LATCH = 901;
        private const int NUMERIC_COMPACTION_MODE_LATCH = 902;
        private const int BYTE_COMPACTION_MODE_LATCH_6 = 924;
        private const int BEGIN_MACRO_PDF417_CONTROL_BLOCK = 928;
        private const int BEGIN_MACRO_PDF417_OPTIONAL_FIELD = 923;
        private const int MACRO_PDF417_TERMINATOR = 922;
        private const int MODE_SHIFT_TO_BYTE_COMPACTION_MODE = 913;
        private const int MAX_NUMERIC_CODEWORDS = 15;

        private const int PL = 25;
        private const int LL = 27;
        private const int AS = 27;
        private const int ML = 28;
        private const int AL = 28;
        private const int PS = 29;
        private const int PAL = 29;

        private static readonly char[] PUNCT_CHARS = {
                                                      ';', '<', '>', '@', '[', '\\', '}', '_', '`', '~', '!',
                                                      '\r', '\t', ',', ':', '\n', '-', '.', '$', '/', '"', '|', '*',
                                                      '(', ')', '?', '{', '}', '\''
                                                   };

        private static readonly char[] MIXED_CHARS = {
                                                      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '&',
                                                      '\r', '\t', ',', ':', '#', '-', '.', '$', '/', '+', '%', '*',
                                                      '=', '^'
                                                   };

#if SILVERLIGHT4 || SILVERLIGHT5 || NET40 || NET45 || NETFX_CORE
      /// <summary>
      /// Table containing values for the exponent of 900.
      /// This is used in the numeric compaction decode algorithm.
      /// </summary>
      private static readonly BigInteger[] EXP900;
      static DecodedBitStreamParser()
      {
         EXP900 = new BigInteger[16];
         EXP900[0] = BigInteger.One;
         BigInteger nineHundred = new BigInteger(900);
         EXP900[1] = nineHundred;
         for (int i = 2; i < EXP900.Length; i++)
         {
            EXP900[i] = BigInteger.Multiply(EXP900[i - 1], nineHundred);
         }
      }
#else
        /// <summary>
        /// Table containing values for the exponent of 900.
        /// This is used in the numeric compaction decode algorithm.
        /// </summary>
        private static readonly BigInteger[] EXP900;
        static DecodedBitStreamParser()
        {
            EXP900 = new BigInteger[16];
            EXP900[0] = BigInteger.One;
            BigInteger nineHundred = new BigInteger(900);
            EXP900[1] = nineHundred;
            for (int i = 2; i < EXP900.Length; i++)
            {
                EXP900[i] = BigInteger.Multiplication(EXP900[i - 1], nineHundred);
            }
        }
#endif

        internal static DecoderResult Decode(int[] codewords)
        {
            StringBuilder result = new StringBuilder(100);
            // Get compaction mode
            int codeIndex = 1;
            int code = codewords[codeIndex++];
            while (codeIndex < codewords[0])
            {
                switch (code)
                {
                    case TEXT_COMPACTION_MODE_LATCH:
                        codeIndex = TextCompaction(codewords, codeIndex, result);
                        break;
                    case BYTE_COMPACTION_MODE_LATCH:
                        codeIndex = ByteCompaction(code, codewords, codeIndex, result);
                        break;
                    case NUMERIC_COMPACTION_MODE_LATCH:
                        codeIndex = NumericCompaction(codewords, codeIndex, result);
                        break;
                    case MODE_SHIFT_TO_BYTE_COMPACTION_MODE:
                        codeIndex = ByteCompaction(code, codewords, codeIndex, result);
                        break;
                    case BYTE_COMPACTION_MODE_LATCH_6:
                        codeIndex = ByteCompaction(code, codewords, codeIndex, result);
                        break;
                    default:
                  // Default to text compaction. During testing numerous barcodes
                  // appeared to be missing the starting mode. In these cases defaulting
                  // to text compaction seems to work.
                        codeIndex--;
                        codeIndex = TextCompaction(codewords, codeIndex, result);
                        break;
                }
                if (codeIndex < 0)
                    return null;
                if (codeIndex < codewords.Length)
                {
                    code = codewords[codeIndex++];
                } else
                {
                    return null;
                }
            }

            if (result.Length == 0)
            {
                return null;
            }

            return new DecoderResult(null, result.ToString(), null, null);
        }

        /// <summary>
        /// Text Compaction mode (see 5.4.1.5) permits all printable ASCII characters to be
        /// encoded, i.e. values 32 - 126 inclusive in accordance with ISO/IEC 646 (IRV), as
        /// well as selected control characters.
        ///
        /// <param name="codewords">The array of codewords (data + error)</param>
        /// <param name="codeIndex">The current index into the codeword array.</param>
        /// <param name="result">The decoded data is appended to the result.</param>
        /// <returns>The next index into the codeword array.</returns>
        /// </summary>
        private static int TextCompaction(int[] codewords, int codeIndex, StringBuilder result)
        {
            // 2 character per codeword
            int[] textCompactionData = new int[codewords[0] << 1];
            // Used to hold the byte compaction value if there is a mode shift
            int[] byteCompactionData = new int[codewords[0] << 1];

            int index = 0;
            bool end = false;
            while ((codeIndex < codewords[0]) && !end)
            {
                int code = codewords[codeIndex++];
                if (code < TEXT_COMPACTION_MODE_LATCH)
                {
                    textCompactionData[index] = code / 30;
                    textCompactionData[index + 1] = code % 30;
                    index += 2;
                } else
                {
                    switch (code)
                    {
                        case TEXT_COMPACTION_MODE_LATCH:
                     // reinitialize text compaction mode to alpha sub mode
                            textCompactionData[index++] = TEXT_COMPACTION_MODE_LATCH;
                            break;
                        case BYTE_COMPACTION_MODE_LATCH:
                            codeIndex--;
                            end = true;
                            break;
                        case NUMERIC_COMPACTION_MODE_LATCH:
                            codeIndex--;
                            end = true;
                            break;
                        case MODE_SHIFT_TO_BYTE_COMPACTION_MODE:
                     // The Mode Shift codeword 913 shall cause a temporary
                     // switch from Text Compaction mode to Byte Compaction mode.
                     // This switch shall be in effect for only the next codeword,
                     // after which the mode shall revert to the prevailing sub-mode
                     // of the Text Compaction mode. Codeword 913 is only available
                     // in Text Compaction mode; its use is described in 5.4.2.4.
                            textCompactionData[index] = MODE_SHIFT_TO_BYTE_COMPACTION_MODE;
                            code = codewords[codeIndex++];
                            byteCompactionData[index] = code; //Integer.toHexString(code);
                            index++;
                            break;
                        case BYTE_COMPACTION_MODE_LATCH_6:
                            codeIndex--;
                            end = true;
                            break;
                    }
                }
            }
            DecodeTextCompaction(textCompactionData, byteCompactionData, index, result);
            return codeIndex;
        }

        /// <summary>
        /// The Text Compaction mode includes all the printable ASCII characters
        /// (i.e. values from 32 to 126) and three ASCII control characters: HT or tab
        /// (ASCII value 9), LF or line feed (ASCII value 10), and CR or carriage
        /// return (ASCII value 13). The Text Compaction mode also includes various latch
        /// and shift characters which are used exclusively within the mode. The Text
        /// Compaction mode encodes up to 2 characters per codeword. The compaction rules
        /// for converting data into PDF417 codewords are defined in 5.4.2.2. The sub-mode
        /// switches are defined in 5.4.2.3.
        ///
        /// <param name="textCompactionData">The text compaction data.</param>
        /// <param name="byteCompactionData">The byte compaction data if there</param>
        ///                           was a mode shift.
        /// <param name="length">The size of the text compaction and byte compaction data.</param>
        /// <param name="result">The decoded data is appended to the result.</param>
        /// </summary>
        private static void DecodeTextCompaction(int[] textCompactionData,
                                               int[] byteCompactionData,
                                               int length,
                                               StringBuilder result)
        {
            // Beginning from an initial state of the Alpha sub-mode
            // The default compaction mode for PDF417 in effect at the start of each symbol shall always be Text
            // Compaction mode Alpha sub-mode (uppercase alphabetic). A latch codeword from another mode to the Text
            // Compaction mode shall always switch to the Text Compaction Alpha sub-mode.
            Mode subMode = Mode.ALPHA;
            Mode priorToShiftMode = Mode.ALPHA;
            int i = 0;
            while (i < length)
            {
                int subModeCh = textCompactionData[i];
                char? ch = null;
                switch (subMode)
                {
                    case Mode.ALPHA:
                  // Alpha (uppercase alphabetic)
                        if (subModeCh < 26)
                        {
                            // Upper case Alpha Character
                            ch = (char)('A' + subModeCh);
                        } else
                        {
                            if (subModeCh == 26)
                            {
                                ch = ' ';
                            } else if (subModeCh == LL)
                            {
                                subMode = Mode.LOWER;
                            } else if (subModeCh == ML)
                            {
                                subMode = Mode.MIXED;
                            } else if (subModeCh == PS)
                            {
                                // Shift to punctuation
                                priorToShiftMode = subMode;
                                subMode = Mode.PUNCT_SHIFT;
                            } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE)
                            {
                                result.Append((char)byteCompactionData[i]);
                            } else if (subModeCh == TEXT_COMPACTION_MODE_LATCH)
                            {
                                subMode = Mode.ALPHA;
                            }
                        }
                        break;

                    case Mode.LOWER:
                  // Lower (lowercase alphabetic)
                        if (subModeCh < 26)
                        {
                            ch = (char)('a' + subModeCh);
                        } else
                        {
                            if (subModeCh == 26)
                            {
                                ch = ' ';
                            } else if (subModeCh == AS)
                            {
                                // Shift to alpha
                                priorToShiftMode = subMode;
                                subMode = Mode.ALPHA_SHIFT;
                            } else if (subModeCh == ML)
                            {
                                subMode = Mode.MIXED;
                            } else if (subModeCh == PS)
                            {
                                // Shift to punctuation
                                priorToShiftMode = subMode;
                                subMode = Mode.PUNCT_SHIFT;
                            } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE)
                            {
                                result.Append((char)byteCompactionData[i]);
                            } else if (subModeCh == TEXT_COMPACTION_MODE_LATCH)
                            {
                                subMode = Mode.ALPHA;
                            }
                        }
                        break;

                    case Mode.MIXED:
                  // Mixed (numeric and some punctuation)
                        if (subModeCh < PL)
                        {
                            ch = MIXED_CHARS[subModeCh];
                        } else
                        {
                            if (subModeCh == PL)
                            {
                                subMode = Mode.PUNCT;
                            } else if (subModeCh == 26)
                            {
                                ch = ' ';
                            } else if (subModeCh == LL)
                            {
                                subMode = Mode.LOWER;
                            } else if (subModeCh == AL)
                            {
                                subMode = Mode.ALPHA;
                            } else if (subModeCh == PS)
                            {
                                // Shift to punctuation
                                priorToShiftMode = subMode;
                                subMode = Mode.PUNCT_SHIFT;
                            } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE)
                            {
                                result.Append((char)byteCompactionData[i]);
                            } else if (subModeCh == TEXT_COMPACTION_MODE_LATCH)
                            {
                                subMode = Mode.ALPHA;
                            }
                        }
                        break;

                    case Mode.PUNCT:
                  // Punctuation
                        if (subModeCh < PAL)
                        {
                            ch = PUNCT_CHARS[subModeCh];
                        } else
                        {
                            if (subModeCh == PAL)
                            {
                                subMode = Mode.ALPHA;
                            } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE)
                            {
                                result.Append((char)byteCompactionData[i]);
                            } else if (subModeCh == TEXT_COMPACTION_MODE_LATCH)
                            {
                                subMode = Mode.ALPHA;
                            }
                        }
                        break;

                    case Mode.ALPHA_SHIFT:
                  // Restore sub-mode
                        subMode = priorToShiftMode;
                        if (subModeCh < 26)
                        {
                            ch = (char)('A' + subModeCh);
                        } else
                        {
                            if (subModeCh == 26)
                            {
                                ch = ' ';
                            } else if (subModeCh == TEXT_COMPACTION_MODE_LATCH)
                            {
                                subMode = Mode.ALPHA;
                            }
                        }
                        break;

                    case Mode.PUNCT_SHIFT:
                  // Restore sub-mode
                        subMode = priorToShiftMode;
                        if (subModeCh < PAL)
                        {
                            ch = PUNCT_CHARS[subModeCh];
                        } else
                        {
                            if (subModeCh == PAL)
                            {
                                subMode = Mode.ALPHA;
                            } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE)
                            {
                                // PS before Shift-to-Byte is used as a padding character, 
                                // see 5.4.2.4 of the specification
                                result.Append((char)byteCompactionData[i]);
                            } else if (subModeCh == TEXT_COMPACTION_MODE_LATCH)
                            {
                                subMode = Mode.ALPHA;
                            }
                        }
                        break;
                }
                if (ch != null)
                {
                    // Append decoded character to result
                    result.Append(ch.Value);
                }
                i++;
            }
        }

        /// <summary>
        /// Byte Compaction mode (see 5.4.3) permits all 256 possible 8-bit byte values to be encoded.
        /// This includes all ASCII characters value 0 to 127 inclusive and provides for international
        /// character set support.
        ///
        /// <param name="mode">The byte compaction mode i.e. 901 or 924</param>
        /// <param name="codewords">The array of codewords (data + error)</param>
        /// <param name="codeIndex">The current index into the codeword array.</param>
        /// <param name="result">The decoded data is appended to the result.</param>
        /// <returns>The next index into the codeword array.</returns>
        /// </summary>
        private static int ByteCompaction(int mode, int[] codewords, int codeIndex, StringBuilder result)
        {
            if (mode == BYTE_COMPACTION_MODE_LATCH)
            {
                // Total number of Byte Compaction characters to be encoded
                // is not a multiple of 6
                int count = 0;
                long value = 0;
                char[] decodedData = new char[6];
                int[] byteCompactedCodewords = new int[6];
                bool end = false;
                int nextCode = codewords[codeIndex++];
                while ((codeIndex < codewords[0]) && !end)
                {
                    byteCompactedCodewords[count++] = nextCode;
                    // Base 900
                    value = 900 * value + nextCode;
                    nextCode = codewords[codeIndex++];
                    // perhaps it should be ok to check only nextCode >= TEXT_COMPACTION_MODE_LATCH
                    if (nextCode == TEXT_COMPACTION_MODE_LATCH ||
                        nextCode == BYTE_COMPACTION_MODE_LATCH ||
                        nextCode == NUMERIC_COMPACTION_MODE_LATCH ||
                        nextCode == BYTE_COMPACTION_MODE_LATCH_6 ||
                        nextCode == BEGIN_MACRO_PDF417_CONTROL_BLOCK ||
                        nextCode == BEGIN_MACRO_PDF417_OPTIONAL_FIELD ||
                        nextCode == MACRO_PDF417_TERMINATOR)
                    {
                        codeIndex--;
                        end = true;
                    } else
                    {
                        if ((count % 5 == 0) && (count > 0))
                        {
                            // Decode every 5 codewords
                            // Convert to Base 256
                            for (int j = 0; j < 6; ++j)
                            {
                                decodedData[5 - j] = (char)(value % 256);
                                value >>= 8;
                            }
                            result.Append(decodedData);
                            count = 0;
                        }
                    }
                }

                // if the end of all codewords is reached the last codeword needs to be added
                if (codeIndex == codewords[0] && nextCode < TEXT_COMPACTION_MODE_LATCH)
                    byteCompactedCodewords[count++] = nextCode;

                // If Byte Compaction mode is invoked with codeword 901,
                // the last group of codewords is interpreted directly
                // as one byte per codeword, without compaction.
                for (int i = 0; i < count; i++)
                {
                    result.Append((char)byteCompactedCodewords[i]);
                }
            } else if (mode == BYTE_COMPACTION_MODE_LATCH_6)
            {
                // Total number of Byte Compaction characters to be encoded
                // is an integer multiple of 6
                int count = 0;
                long value = 0;
                bool end = false;
                while (codeIndex < codewords[0] && !end)
                {
                    int code = codewords[codeIndex++];
                    if (code < TEXT_COMPACTION_MODE_LATCH)
                    {
                        count++;
                        // Base 900
                        value = 900 * value + code;
                    } else
                    {
                        if (code == TEXT_COMPACTION_MODE_LATCH ||
                            code == BYTE_COMPACTION_MODE_LATCH ||
                            code == NUMERIC_COMPACTION_MODE_LATCH ||
                            code == BYTE_COMPACTION_MODE_LATCH_6 ||
                            code == BEGIN_MACRO_PDF417_CONTROL_BLOCK ||
                            code == BEGIN_MACRO_PDF417_OPTIONAL_FIELD ||
                            code == MACRO_PDF417_TERMINATOR)
                        {
                            codeIndex--;
                            end = true;
                        }
                    }
                    if ((count % 5 == 0) && (count > 0))
                    {
                        // Decode every 5 codewords
                        // Convert to Base 256
                        char[] decodedData = new char[6];
                        for (int j = 0; j < 6; ++j)
                        {
                            decodedData[5 - j] = (char)(value & 0xFF);
                            value >>= 8;
                        }
                        result.Append(decodedData);
                        count = 0;
                    }
                }
            }
            return codeIndex;
        }

        /// <summary>
        /// Numeric Compaction mode (see 5.4.4) permits efficient encoding of numeric data strings.
        ///
        /// <param name="codewords">The array of codewords (data + error)</param>
        /// <param name="codeIndex">The current index into the codeword array.</param>
        /// <param name="result">The decoded data is appended to the result.</param>
        /// <returns>The next index into the codeword array.</returns>
        /// </summary>
        private static int NumericCompaction(int[] codewords, int codeIndex, StringBuilder result)
        {
            int count = 0;
            bool end = false;

            int[] numericCodewords = new int[MAX_NUMERIC_CODEWORDS];

            while (codeIndex < codewords[0] && !end)
            {
                int code = codewords[codeIndex++];
                if (codeIndex == codewords[0])
                {
                    end = true;
                }
                if (code < TEXT_COMPACTION_MODE_LATCH)
                {
                    numericCodewords[count] = code;
                    count++;
                } else
                {
                    if (code == TEXT_COMPACTION_MODE_LATCH ||
                        code == BYTE_COMPACTION_MODE_LATCH ||
                        code == BYTE_COMPACTION_MODE_LATCH_6 ||
                        code == BEGIN_MACRO_PDF417_CONTROL_BLOCK ||
                        code == BEGIN_MACRO_PDF417_OPTIONAL_FIELD ||
                        code == MACRO_PDF417_TERMINATOR)
                    {
                        codeIndex--;
                        end = true;
                    }
                }
                if (count % MAX_NUMERIC_CODEWORDS == 0 ||
                    code == NUMERIC_COMPACTION_MODE_LATCH ||
                    end)
                {
                    // Re-invoking Numeric Compaction mode (by using codeword 902
                    // while in Numeric Compaction mode) serves  to terminate the
                    // current Numeric Compaction mode grouping as described in 5.4.4.2,
                    // and then to start a new one grouping.
                    String s = DecodeBase900toBase10(numericCodewords, count);
                    if (s == null)
                        return -1;
                    result.Append(s);
                    count = 0;
                }
            }
            return codeIndex;
        }

        /// <summary>
        /// Convert a list of Numeric Compacted codewords from Base 900 to Base 10.
        /// EXAMPLE
        /// Encode the fifteen digit numeric string 000213298174000
        /// Prefix the numeric string with a 1 and set the initial value of
        /// t = 1 000 213 298 174 000
        /// Calculate codeword 0
        /// d0 = 1 000 213 298 174 000 mod 900 = 200
        /// 
        /// t = 1 000 213 298 174 000 div 900 = 1 111 348 109 082
        /// Calculate codeword 1
        /// d1 = 1 111 348 109 082 mod 900 = 282
        /// 
        /// t = 1 111 348 109 082 div 900 = 1 234 831 232
        /// Calculate codeword 2
        /// d2 = 1 234 831 232 mod 900 = 632
        /// 
        /// t = 1 234 831 232 div 900 = 1 372 034
        /// Calculate codeword 3
        /// d3 = 1 372 034 mod 900 = 434
        /// 
        /// t = 1 372 034 div 900 = 1 524
        /// Calculate codeword 4
        /// d4 = 1 524 mod 900 = 624
        /// 
        /// t = 1 524 div 900 = 1
        /// Calculate codeword 5
        /// d5 = 1 mod 900 = 1
        /// t = 1 div 900 = 0
        /// Codeword sequence is: 1, 624, 434, 632, 282, 200
        /// 
        /// Decode the above codewords involves
        ///   1 x 900 power of 5 + 624 x 900 power of 4 + 434 x 900 power of 3 +
        /// 632 x 900 power of 2 + 282 x 900 power of 1 + 200 x 900 power of 0 = 1000213298174000
        /// 
        /// Remove leading 1 =>  Result is 000213298174000
        /// <param name="codewords">The array of codewords</param>
        /// <param name="count">The number of codewords</param>
        /// <returns>The decoded string representing the Numeric data.</returns>
        /// </summary>
        private static String DecodeBase900toBase10(int[] codewords, int count)
        {
#if SILVERLIGHT4 || SILVERLIGHT5 || NET40 || NET45 || NETFX_CORE
         BigInteger result = BigInteger.Zero;
         for (int i = 0; i < count; i++)
         {
            result = BigInteger.Add(result, BigInteger.Multiply(EXP900[count - i - 1], new BigInteger(codewords[i])));
         }
         String resultString = result.ToString();
         if (resultString[0] != '1')
         {
            return null;
         }
         return resultString.Substring(1);
#else
            BigInteger result = BigInteger.Zero;
            for (int i = 0; i < count; i++)
            {
                result = BigInteger.Addition(result, BigInteger.Multiplication(EXP900[count - i - 1], new BigInteger(codewords[i])));
            }
            String resultString = result.ToString();
            if (resultString[0] != '1')
            {
                return null;
            }
            return resultString.Substring(1);
#endif
        }
    }
}
