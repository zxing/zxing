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

namespace com.google.zxing.qrcode.decoder
{

    /// <summary>
    /// <p>See ISO 18004:2006, 6.4.1, Tables 2 and 3. This enum encapsulates the various modes in which
    /// data can be encoded to bits in the QR code standard.</p>
    /// 
    /// @author Sean Owen
    /// </summary>
    public class Mode
    {
        private Mode()
        {
            
        }

        private class InnerMode : Mode
        {
            private readonly int[] characterCountBitsForVersions;
            private readonly int bits;

            public InnerMode(int[] characterCountBitsForVersions, int bits)
            {
                this.characterCountBitsForVersions = characterCountBitsForVersions;
                this.bits = bits;
            }

            public override int Bits
            {
                get { return bits; }
            }

            /// <param name="version"> version in question </param>
            /// <returns> number of bits used, in this QR Code symbol <seealso cref="Version"/>, to encode the
            ///         count of characters that will follow encoded in this Mode </returns>
            public override int getCharacterCountBits(Version version)
            {
                int number = version.VersionNumber;
                int offset;
                if (number <= 9)
                {
                    offset = 0;
                }
                else if (number <= 26)
                {
                    offset = 1;
                }
                else
                {
                    offset = 2;
                }
                return characterCountBitsForVersions[offset];
            }
        }

        public virtual int getCharacterCountBits(Version version)
        {
            throw new NotImplementedException();
        }

        public virtual int Bits
        {
            get
            {
                throw new NotImplementedException();
            }
        }
        public static Mode TERMINATOR = new InnerMode(new int[] { 0, 0, 0 }, 0x00); // Not really a mode...


        public static Mode NUMERIC = new InnerMode(new int[] { 10, 12, 14 }, 0x01);
        public static Mode ALPHANUMERIC = new InnerMode(new int[] { 9, 11, 13 }, 0x02);
        public static Mode STRUCTURED_APPEND = new InnerMode(new int[] { 0, 0, 0 }, 0x03); // Not supported

        public static Mode BYTE = new InnerMode(new int[] { 8, 16, 16 }, 0x04);
        public static Mode ECI = new InnerMode(new int[] { 0, 0, 0 }, 0x07); // character counts don't apply

        public static Mode KANJI = new InnerMode(new int[] { 8, 10, 12 }, 0x08);
        public static Mode FNC1_FIRST_POSITION = new InnerMode(new int[] { 0, 0, 0 }, 0x05);
        public static Mode FNC1_SECOND_POSITION = new InnerMode(new int[] { 0, 0, 0 }, 0x09);
        /// <summary>
        /// See GBT 18284-2000; "Hanzi" is a transliteration of this mode name. </summary>
        public static Mode HANZI = new InnerMode(new int[] { 8, 10, 12 }, 0x0D);



        /// <param name="bits"> four bits encoding a QR Code data mode </param>
        /// <returns> Mode encoded by these bits </returns>
        /// <exception cref="IllegalArgumentException"> if bits do not correspond to a known mode </exception>
        public static Mode forBits(int bits)
        {
            switch (bits)
            {
                case 0x0:
                    return TERMINATOR;
                case 0x1:
                    return NUMERIC;
                case 0x2:
                    return ALPHANUMERIC;
                case 0x3:
                    return STRUCTURED_APPEND;
                case 0x4:
                    return BYTE;
                case 0x5:
                    return FNC1_FIRST_POSITION;
                case 0x7:
                    return ECI;
                case 0x8:
                    return KANJI;
                case 0x9:
                    return FNC1_SECOND_POSITION;
                case 0xD:
                    // 0xD is defined in GBT 18284-2000, may not be supported in foreign country
                    return HANZI;
                default:
                    throw new System.ArgumentException();
            }
        }

        /// <param name="version"> version in question </param>
        /// <returns> number of bits used, in this QR Code symbol <seealso cref="Version"/>, to encode the
        ///         count of characters that will follow encoded in this Mode </returns>


    }
}