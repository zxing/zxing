// /*
//  * Copyright 2009 ZXing authors
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
using System;
using ZXing.Common;

namespace ZXing.PDF417
{
    /// <summary>
    /// Bit matrix extensions to assist in PDF417 Detection
    /// </summary>
    public static class BitMatrixExtensions
    {
        /// <summary>
        /// Rotates the Matrix by 180 degrees in-place
        /// </summary>
        /// <param name="bitMatrix">Bit matrix.</param>
        public static void Rotate180(this BitMatrix bitMatrix)
        {
            int width = bitMatrix.Width;
            int height = bitMatrix.Height;
            BitArray firstRowBitArray = new BitArray(width);
            BitArray secondRowBitArray = new BitArray(width);
            BitArray tmpBitArray = new BitArray(width); // re-use this to save on 'new' calls
            for (int y = 0; y < height + 1 >> 1; y++)
            {
                firstRowBitArray = bitMatrix.getRow(y, firstRowBitArray);

                Mirror(bitMatrix.getRow(height - 1 - y, secondRowBitArray), ref tmpBitArray);
                bitMatrix.setRow(y, tmpBitArray);

                Mirror(firstRowBitArray, ref tmpBitArray);
                bitMatrix.setRow(height - 1 - y, tmpBitArray);
            }
        }
        
        /// <summary>
        /// Copies the bits from the input to the result BitArray in reverse order.
        /// SF: Not sure how this is different than BitArray.Reverse();
        /// </summary>
        /// <param name="input">Input.</param>
        /// <param name="result">Result.</param>
        private static void Mirror(BitArray input, ref BitArray result)
        {
            result.clear();
            int size = input.Size;
            for (int i = 0; i < size; i++)
            {
                result[size - 1 - i] = input[i];
            }
        }
    }
}

