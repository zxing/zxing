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
namespace com.google.zxing.qrcode.decoder
{

    /// <summary> <p>Encapsulates data masks for the data bits in a QR code, per ISO 18004:2006 6.8. Implementations
    /// of this class can un-mask a raw BitMatrix. For simplicity, they will unmask the entire BitMatrix,
    /// including areas used for finder patterns, timing patterns, etc. These areas should be unused
    /// after the point they are unmasked anyway.</p>
    /// 
    /// <p>Note that the diagram in section 6.8.1 is misleading since it indicates that i is column position
    /// and j is row position. In fact, as the text says, i is row position and j is column position.</p>
    /// 
    /// </summary>
    /// <author>  srowen@google.com (Sean Owen)
    /// </author>
    abstract class DataMask
    {

        /// <summary> See ISO 18004:2006 6.8.1</summary>
        //UPGRADE_NOTE: Final was removed from the declaration of 'DATA_MASKS '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
        private static readonly DataMask[] DATA_MASKS = new DataMask[] { new DataMask000(), new DataMask001(), new DataMask010(), new DataMask011(), new DataMask100(), new DataMask101(), new DataMask110(), new DataMask111() };

        private DataMask()
        {
        }

        /// <summary> <p>Implementations of this method reverse the data masking process applied to a QR Code and
        /// make its bits ready to read.</p>
        /// 
        /// </summary>
        /// <param name="bits">representation of QR Code bits from {@link com.google.zxing.common.BitMatrix#getBits()}
        /// </param>
        /// <param name="dimension">dimension of QR Code, represented by bits, being unmasked
        /// </param>
        internal abstract void unmaskBitMatrix(int[] bits, int dimension);

        /// <param name="reference">a value between 0 and 7 indicating one of the eight possible
        /// data mask patterns a QR Code may use
        /// </param>
        /// <returns> {@link DataMask} encapsulating the data mask pattern
        /// </returns>
        internal static DataMask forReference(int reference)
        {
            if (reference < 0 || reference > 7)
            {
                throw new System.ArgumentException();
            }
            return DATA_MASKS[reference];
        }

        /// <summary> 000: mask bits for which (i + j) mod 2 == 0</summary>
        private class DataMask000 : DataMask
        {
            private const int BITMASK = 0x55555555; // = 010101...

            internal override void unmaskBitMatrix(int[] bits, int dimension)
            {
                // This one's easy. Because the dimension of BitMatrix is always odd,
                // we can merely flip every other bit
                int max = bits.Length;
                for (int i = 0; i < max; i++)
                {
                    bits[i] ^= BITMASK;
                }
            }
        }

        /// <summary> 001: mask bits for which i mod 2 == 0</summary>
        private class DataMask001 : DataMask
        {
            internal override void unmaskBitMatrix(int[] bits, int dimension)
            {
                int bitMask = 0;
                int count = 0;
                int offset = 0;
                for (int j = 0; j < dimension; j++)
                {
                    for (int i = 0; i < dimension; i++)
                    {
                        if ((i & 0x01) == 0)
                        {
                            bitMask |= 1 << count;
                        }
                        if (++count == 32)
                        {
                            bits[offset++] ^= bitMask;
                            count = 0;
                            bitMask = 0;
                        }
                    }
                }
                bits[offset] ^= bitMask;
            }
        }

        /// <summary> 010: mask bits for which j mod 3 == 0</summary>
        private class DataMask010 : DataMask
        {
            internal override void unmaskBitMatrix(int[] bits, int dimension)
            {
                int bitMask = 0;
                int count = 0;
                int offset = 0;
                for (int j = 0; j < dimension; j++)
                {
                    bool columnMasked = j % 3 == 0;
                    for (int i = 0; i < dimension; i++)
                    {
                        if (columnMasked)
                        {
                            bitMask |= 1 << count;
                        }
                        if (++count == 32)
                        {
                            bits[offset++] ^= bitMask;
                            count = 0;
                            bitMask = 0;
                        }
                    }
                }
                bits[offset] ^= bitMask;
            }
        }

        /// <summary> 011: mask bits for which (i + j) mod 3 == 0</summary>
        private class DataMask011 : DataMask
        {
            internal override void unmaskBitMatrix(int[] bits, int dimension)
            {
                int bitMask = 0;
                int count = 0;
                int offset = 0;
                for (int j = 0; j < dimension; j++)
                {
                    for (int i = 0; i < dimension; i++)
                    {
                        if ((i + j) % 3 == 0)
                        {
                            bitMask |= 1 << count;
                        }
                        if (++count == 32)
                        {
                            bits[offset++] ^= bitMask;
                            count = 0;
                            bitMask = 0;
                        }
                    }
                }
                bits[offset] ^= bitMask;
            }
        }

        /// <summary> 100: mask bits for which (i/2 + j/3) mod 2 == 0</summary>
        private class DataMask100 : DataMask
        {
            internal override void unmaskBitMatrix(int[] bits, int dimension)
            {
                int bitMask = 0;
                int count = 0;
                int offset = 0;
                for (int j = 0; j < dimension; j++)
                {
                    int jComponentParity = (j / 3) & 0x01;
                    for (int i = 0; i < dimension; i++)
                    {
                        if (((i >> 1) & 0x01) == jComponentParity)
                        {
                            bitMask |= 1 << count;
                        }
                        if (++count == 32)
                        {
                            bits[offset++] ^= bitMask;
                            count = 0;
                            bitMask = 0;
                        }
                    }
                }
                bits[offset] ^= bitMask;
            }
        }

        /// <summary> 101: mask bits for which ij mod 2 + ij mod 3 == 0</summary>
        private class DataMask101 : DataMask
        {
            internal override void unmaskBitMatrix(int[] bits, int dimension)
            {
                int bitMask = 0;
                int count = 0;
                int offset = 0;
                for (int j = 0; j < dimension; j++)
                {
                    for (int i = 0; i < dimension; i++)
                    {
                        int product = i * j;
                        if (((product & 0x01) == 0) && product % 3 == 0)
                        {
                            bitMask |= 1 << count;
                        }
                        if (++count == 32)
                        {
                            bits[offset++] ^= bitMask;
                            count = 0;
                            bitMask = 0;
                        }
                    }
                }
                bits[offset] ^= bitMask;
            }
        }

        /// <summary> 110: mask bits for which (ij mod 2 + ij mod 3) mod 2 == 0</summary>
        private class DataMask110 : DataMask
        {
            internal override void unmaskBitMatrix(int[] bits, int dimension)
            {
                int bitMask = 0;
                int count = 0;
                int offset = 0;
                for (int j = 0; j < dimension; j++)
                {
                    for (int i = 0; i < dimension; i++)
                    {
                        int product = i * j;
                        if ((((product & 0x01) + product % 3) & 0x01) == 0)
                        {
                            bitMask |= 1 << count;
                        }
                        if (++count == 32)
                        {
                            bits[offset++] ^= bitMask;
                            count = 0;
                            bitMask = 0;
                        }
                    }
                }
                bits[offset] ^= bitMask;
            }
        }

        /// <summary> 111: mask bits for which ((i+j)mod 2 + ij mod 3) mod 2 == 0</summary>
        private class DataMask111 : DataMask
        {
            internal override void unmaskBitMatrix(int[] bits, int dimension)
            {
                int bitMask = 0;
                int count = 0;
                int offset = 0;
                for (int j = 0; j < dimension; j++)
                {
                    for (int i = 0; i < dimension; i++)
                    {
                        if (((((i + j) & 0x01) + (i * j) % 3) & 0x01) == 0)
                        {
                            bitMask |= 1 << count;
                        }
                        if (++count == 32)
                        {
                            bits[offset++] ^= bitMask;
                            count = 0;
                            bitMask = 0;
                        }
                    }
                }
                bits[offset] ^= bitMask;
            }
        }
    }
}