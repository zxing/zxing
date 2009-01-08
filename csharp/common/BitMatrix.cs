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
namespace com.google.zxing.common
{
    using System;
    using System.Text;

    /// <summary> A class which wraps a 2D array of bytes. The default usage is signed. If you want to use it as a
    /// unsigned container, it's up to you to do byteValue & 0xff at each location.
    /// *
    /// JAVAPORT: I'm not happy about the argument ordering throughout the file, as I always like to have
    /// the horizontal component first, but this is for compatibility with the C++ code. The original
    /// code was a 2D array of ints, but since it only ever gets assigned -1, 0, and 1, I'm going to use
    /// less memory and go with bytes.
    /// *
    /// </summary>
    /// <author>  dswitkin@google.com (Daniel Switkin)
    /// 
    /// </author>
    public sealed class BitMatrix
    {
          private int dimension;
          private int[] bits;

          public BitMatrix(int dimension) {
            if (dimension < 1) {
              throw new Exception("dimension must be at least 1");
            }
            this.dimension = dimension;
            int numBits = dimension * dimension;
            int arraySize = numBits >> 5; // one int per 32 bits
            if ((numBits & 0x1F) != 0) { // plus one more if there are leftovers
              arraySize++;
            }
            bits = new int[arraySize];
          }

          /**
           * @param i row offset
           * @param j column offset
           * @return value of given bit in matrix
           */
          public bool get(int i, int j) {
            int offset = i + dimension * j;
            //return ((bits[offset >> 5] >>> (offset & 0x1F)) & 0x01) != 0;
			return ((SupportClass.URShift(bits[offset >> 5], (offset & 0x1F))) & 0x01) != 0;
          }

          /**
           * <p>Sets the given bit to true.</p>
           *
           * @param i row offset
           * @param j column offset
           */
          public void set(int i, int j) {
            int offset = i + dimension * j;
            bits[offset >> 5] |= 1 << (offset & 0x1F);
          }

          /**
           * <p>Sets a square region of the bit matrix to true.</p>
           *
           * @param topI row offset of region's top-left corner (inclusive)
           * @param leftJ column offset of region's top-left corner (inclusive)
           * @param height height of region
           * @param width width of region
           */
          public void setRegion(int topI, int leftJ, int height, int width) {
            if (topI < 0 || leftJ < 0) {
              throw new Exception("topI and leftJ must be nonnegative");
            }
            if (height < 1 || width < 1) {
                throw new Exception("height and width must be at least 1");
            }
            int maxJ = leftJ + width;
            int maxI = topI + height;
            if (maxI > dimension || maxJ > dimension) {
                throw new Exception(
                  "topI + height and leftJ + width must be <= matrix dimension");
            }
            for (int j = leftJ; j < maxJ; j++) {
              int jOffset = dimension * j;
              for (int i = topI; i < maxI; i++) {
                int offset = i + jOffset;
                bits[offset >> 5] |= 1 << (offset & 0x1F);
              }
            }
          }

          /**
           * @return row/column dimension of this matrix
           */
          public int getDimension() {
            return dimension;
          }

          /**
           * @return array of ints holding internal representation of this matrix's bits
           */
          public int[] getBits() {
            return bits;
          }

          public String toString() {
              StringBuilder result = new StringBuilder(dimension * (dimension + 1));
            for (int i = 0; i < dimension; i++) {
              for (int j = 0; j < dimension; j++) {
                result.Append(get(i, j) ? "X " : "  ");
              }
              result.Append('\n');
            }
            return result.ToString();
          }
   
    }
}