/*
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
using com.google.zxing.common;
namespace com.google.zxing.qrcode.decoder
{

    /// <summary> <p>QR Codes can encode text as bits in one of several modes, and can use multiple modes
    /// in one QR Code. This class decodes the bits back into text.</p>
    /// 
    /// <p>See ISO 18004:2006, 6.4.3 - 6.4.7</p>
    /// 
    /// </summary>
    /// <author>  srowen@google.com (Sean Owen)
    /// </author>
    public sealed class DecodedBitStreamParser
    {

        /// <summary> See ISO 18004:2006, 6.4.4 Table 5</summary>
        //UPGRADE_NOTE: Final was removed from the declaration of 'ALPHANUMERIC_CHARS '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
        private static readonly char[] ALPHANUMERIC_CHARS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':' };
        private const System.String SHIFT_JIS = "Shift_JIS";
        private static bool ASSUME_SHIFT_JIS;

        private DecodedBitStreamParser()
        {
        }

        internal static System.String decode(sbyte[] bytes, Version version)
        {
            BitSource bits = new BitSource(bytes);
            System.Text.StringBuilder result = new System.Text.StringBuilder();
            Mode mode;
            do
            {
                // While still another segment to read...
                mode = Mode.forBits(bits.readBits(4)); // mode is encoded by 4 bits
                if (!mode.Equals(Mode.TERMINATOR))
                {
                    // How many characters will follow, encoded in this mode?
                    int count = bits.readBits(mode.getCharacterCountBits(version));
                    if (mode.Equals(Mode.NUMERIC))
                    {
                        decodeNumericSegment(bits, result, count);
                    }
                    else if (mode.Equals(Mode.ALPHANUMERIC))
                    {
                        decodeAlphanumericSegment(bits, result, count);
                    }
                    else if (mode.Equals(Mode.BYTE))
                    {
                        decodeByteSegment(bits, result, count);
                    }
                    else if (mode.Equals(Mode.KANJI))
                    {
                        decodeKanjiSegment(bits, result, count);
                    }
                    else
                    {
                        throw new ReaderException("Unsupported mode indicator");
                    }
                }
            }
            while (!mode.Equals(Mode.TERMINATOR));

            // I thought it wasn't allowed to leave extra bytes after the terminator but it happens
            /*
            int bitsLeft = bits.available();
            if (bitsLeft > 0) {
            if (bitsLeft > 6 || bits.readBits(bitsLeft) != 0) {
            throw new ReaderException("Excess bits or non-zero bits after terminator mode indicator");
            }
            }
            */
            return result.ToString();
        }

        private static void decodeKanjiSegment(BitSource bits, System.Text.StringBuilder result, int count)
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
                buffer[offset] = (sbyte)(assembledTwoBytes >> 8);
                buffer[offset + 1] = (sbyte)assembledTwoBytes;
                offset += 2;
                count--;
            }
            // Shift_JIS may not be supported in some environments:
            try
            {
                byte[] bytes = SupportClass.ToByteArray(buffer);
                //UPGRADE_TODO: The differences in the Format  of parameters for constructor 'java.lang.String.String'  may cause compilation errors.  "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1092'"
                result.Append(System.Text.Encoding.GetEncoding("Shift_JIS").GetString(bytes, 0, bytes.Length));
            }
            catch (System.IO.IOException uee)
            {
                throw new ReaderException("SHIFT_JIS encoding is not supported on this device");
            }
        }

        private static void decodeByteSegment(BitSource bits, System.Text.StringBuilder result, int count)
        {
            sbyte[] readBytes = new sbyte[count];
            if (count << 3 > bits.available())
            {
                throw new ReaderException("Count too large: " + count);
            }
            for (int i = 0; i < count; i++)
            {
                readBytes[i] = (sbyte)bits.readBits(8);
            }
            // The spec isn't clear on this mode; see
            // section 6.4.5: t does not say which encoding to assuming
            // upon decoding. I have seen ISO-8859-1 used as well as
            // Shift_JIS -- without anything like an ECI designator to
            // give a hint.
            System.String encoding = guessEncoding(readBytes);
            try
            {
                byte[] bytes = SupportClass.ToByteArray(readBytes);
                //System.Windows.Forms.MessageBox.Show("encodings: "+ System.Text.Encoding.());
                //UPGRADE_TODO: The differences in the Format  of parameters for constructor 'java.lang.String.String'  may cause compilation errors.  "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1092'"
                result.Append(System.Text.Encoding.GetEncoding(encoding).GetString(bytes, 0, bytes.Length));
            }
            catch (System.IO.IOException uce)
            {
                //UPGRADE_TODO: The equivalent in .NET for method 'java.lang.Throwable.toString' may return a different value. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1043'"
                throw new ReaderException(uce.ToString());
            }
        }

        private static void decodeAlphanumericSegment(BitSource bits, System.Text.StringBuilder result, int count)
        {
            // Read two characters at a time
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
        }

        private static void decodeNumericSegment(BitSource bits, System.Text.StringBuilder result, int count)
        {
            // Read three digits at a time
            while (count >= 3)
            {
                // Each 10 bits encodes three digits
                int threeDigitsBits = bits.readBits(10);
                if (threeDigitsBits >= 1000)
                {
                    throw new ReaderException("Illegal value for 3-digit unit: " + threeDigitsBits);
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
                    throw new ReaderException("Illegal value for 2-digit unit: " + twoDigitsBits);
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
                    throw new ReaderException("Illegal value for digit unit: " + digitBits);
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
            // For now, merely tries to distinguish ISO-8859-1 and Shift_JIS,
            // which should be by far the most common encodings. ISO-8859-1
            // should not have bytes in the 0x80 - 0x9F range, while Shift_JIS
            // uses this as a first byte of a two-byte character. If we see this
            // followed by a valid second byte in Shift_JIS, assume it is Shift_JIS.
            int length = bytes.Length;
            for (int i = 0; i < length; i++)
            {
                int value_Renamed = bytes[i] & 0xFF;
                if (value_Renamed >= 0x80 && value_Renamed <= 0x9F && i < length - 1)
                {
                    // ISO-8859-1 shouldn't use this, but before we decide it is Shift_JIS,
                    // just double check that it is followed by a byte that's valid in
                    // the Shift_JIS encoding
                    int nextValue = bytes[i + 1] & 0xFF;
                    if ((value_Renamed & 0x1) == 0)
                    {
                        // if even,
                        if (nextValue >= 0x40 && nextValue <= 0x9E)
                        {
                            return SHIFT_JIS;
                        }
                    }
                    else
                    {
                        if (nextValue >= 0x9F && nextValue <= 0x7C)
                        {
                            return SHIFT_JIS;
                        }
                    }
                }
            }
            return "ASCII";
        }
        //static DecodedBitStreamParser()
        //{
        //  {
        //    //UPGRADE_ISSUE: Method 'java.lang.System.getProperty' was not converted. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1000_javalangSystem'"
        //    System.String platformDefault = System_Renamed.getProperty("file.encoding");
        //    ASSUME_SHIFT_JIS = SHIFT_JIS.ToUpper().Equals(platformDefault.ToUpper()) || "EUC-JP".ToUpper().Equals(platformDefault.ToUpper());
        //  }
        //}
    }
}