/*
 * Copyright 2011 ZXing authors
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
using System.Collections;
using BarcodeFormat = com.google.zxing.BarcodeFormat;
using ReaderException = com.google.zxing.ReaderException;
using Result = com.google.zxing.Result;
using ResultPoint = com.google.zxing.ResultPoint;
using ByteMatrix = com.google.zxing.common.ByteMatrix;
namespace com.google.zxing.oned
{
    /// <summary> <p>Implements decoding of the EAN-13 format.</p>
    ///
    /// </summary>
    /// <author>  erik.barbara@gmail.com (Erik Barbara)
    /// </author>
    /// <author>  em@nerd.ocracy.org (Emanuele Aina) - Ported from ZXING Java Source
    /// </author>
    public sealed class Code39Writer:UPCEANWriter
    {
      public override ByteMatrix encode(string contents, BarcodeFormat format, int width, int height, Hashtable hints)  {
            if (format != BarcodeFormat.CODE_39) {
                throw new ArgumentException("Can only encode CODE_39, but got " + format);
            }
            return base.encode(contents, format, width, height, hints);
      }
    
      public override sbyte[] encode(string contents) {
            int length = contents.Length;
            if (length > 80) {
                throw new ArgumentException("Requested contents should be less than 80 digits long, but got " + length);
            }

            int[] widths = new int[9];
            int codeWidth = 24 + 1 + length;
            for (int i = 0; i < length; i++) {
                int indexInString = Code39Reader.ALPHABET_STRING.IndexOf(contents[i]);
                toIntArray(Code39Reader.CHARACTER_ENCODINGS[indexInString], widths);
                for(int j = 0; j < widths.Length; j++) {
                    codeWidth += widths[j];
                }
            }
            sbyte[] result = new sbyte[codeWidth];
            toIntArray(Code39Reader.CHARACTER_ENCODINGS[39], widths);
            int pos = appendPattern(result, 0, widths, 1);
            int[] narrowWhite = {1};
            pos += appendPattern(result, pos, narrowWhite, 0);
            //append next character to bytematrix
            for(int i = length-1; i >= 0; i--) {
                int indexInString = Code39Reader.ALPHABET_STRING.IndexOf(contents[i]);
                toIntArray(Code39Reader.CHARACTER_ENCODINGS[indexInString], widths);
                pos += appendPattern(result, pos, widths, 1);
                pos += appendPattern(result, pos, narrowWhite, 0);
            }
            toIntArray(Code39Reader.CHARACTER_ENCODINGS[39], widths);
            pos += appendPattern(result, pos, widths, 1);
            return result;
        }

        private static void toIntArray(int a, int[] toReturn) {
            for (int i = 0; i < 9; i++) {
                int temp = a & (1 << i);
                toReturn[i] = (temp == 0) ? 1 : 2;
            }
        }
    }
}
