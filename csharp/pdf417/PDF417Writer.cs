/*
 * Copyright 2012 ZXing authors
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
using System.Collections.Generic;

using ZXing.Common;
using ZXing.PDF417.Internal;

namespace ZXing.PDF417
{
   /// <summary>
   /// <author>Jacob Haynes</author>
   /// <author>qwandor@google.com (Andrew Walbran)</author>
   /// </summary>
   public sealed class PDF417Writer : Writer
   {
      /// <summary>
      /// </summary>
      /// <param name="contents">The contents to encode in the barcode</param>
      /// <param name="format">The barcode format to generate</param>
      /// <param name="width">The preferred width in pixels</param>
      /// <param name="height">The preferred height in pixels</param>
      /// <param name="hints">Additional parameters to supply to the encoder</param>
      /// <returns>
      /// The generated barcode as a Matrix of unsigned bytes (0 == black, 255 == white)
      /// </returns>
      public BitMatrix encode(String contents,
                              BarcodeFormat format,
                              int width,
                              int height,
                              IDictionary<EncodeHintType, object> hints)
      {
         if (format != BarcodeFormat.PDF_417)
         {
            throw new ArgumentException("Can only encode PDF_417, but got " + format);
         }

         var encoder = new Internal.PDF417();

         if (hints != null)
         {
            if (hints.ContainsKey(EncodeHintType.PDF417_COMPACT))
            {
               encoder.setCompact((Boolean)hints[EncodeHintType.PDF417_COMPACT]);
            }
            if (hints.ContainsKey(EncodeHintType.PDF417_COMPACTION))
            {
               encoder.setCompaction((Compaction)hints[EncodeHintType.PDF417_COMPACTION]);
            }
            if (hints.ContainsKey(EncodeHintType.PDF417_DIMENSIONS))
            {
               Dimensions dimensions = (Dimensions)hints[EncodeHintType.PDF417_DIMENSIONS];
               encoder.setDimensions(dimensions.MaxCols,
                                     dimensions.MinCols,
                                     dimensions.MaxRows,
                                     dimensions.MinRows);
            }
         }

         return bitMatrixFromEncoder(encoder, contents, width, height);
      }

      /// <summary>
      /// Encode a barcode using the default settings.
      /// </summary>
      /// <param name="contents">The contents to encode in the barcode</param>
      /// <param name="format">The barcode format to generate</param>
      /// <param name="width">The preferred width in pixels</param>
      /// <param name="height">The preferred height in pixels</param>
      /// <returns>
      /// The generated barcode as a Matrix of unsigned bytes (0 == black, 255 == white)
      /// </returns>
      public BitMatrix encode(String contents,
                              BarcodeFormat format,
                              int width,
                              int height)
      {
         return encode(contents, format, width, height, null);
      }

      /// <summary>
      /// Takes encoder, accounts for width/height, and retrieves bit matrix
      /// </summary>
      private static BitMatrix bitMatrixFromEncoder(Internal.PDF417 encoder,
                                                    String contents,
                                                    int width,
                                                    int height)
      {
         const int errorCorrectionLevel = 2;
         encoder.generateBarcodeLogic(contents, errorCorrectionLevel);

         const int lineThickness = 2;
         const int aspectRatio = 4;
         sbyte[][] originalScale = encoder.BarcodeMatrix.getScaledMatrix(lineThickness, aspectRatio * lineThickness);
         bool rotated = false;
         if ((height > width) ^ (originalScale[0].Length < originalScale.Length))
         {
            originalScale = rotateArray(originalScale);
            rotated = true;
         }

         int scaleX = width / originalScale[0].Length;
         int scaleY = height / originalScale.Length;

         int scale;
         if (scaleX < scaleY)
         {
            scale = scaleX;
         }
         else
         {
            scale = scaleY;
         }

         if (scale > 1)
         {
            sbyte[][] scaledMatrix =
                encoder.BarcodeMatrix.getScaledMatrix(scale * lineThickness, scale * aspectRatio * lineThickness);
            if (rotated)
            {
               scaledMatrix = rotateArray(scaledMatrix);
            }
            return bitMatrixFrombitArray(scaledMatrix);
         }
         return bitMatrixFrombitArray(originalScale);
      }

      /// <summary>
      /// This takes an array holding the values of the PDF 417
      ///
      /// <param name="input">a byte array of information with 0 is black, and 1 is white</param>
      /// <returns>BitMatrix of the input</returns>
      /// </summary>
      private static BitMatrix bitMatrixFrombitArray(sbyte[][] input)
      {
         // Creates a small whitespace border around the barcode
         const int whiteSpace = 30;

         // Creates the bitmatrix with extra space for whitespace
         var output = new BitMatrix(input[0].Length + 2 * whiteSpace, input.Length + 2 * whiteSpace);
         var yOutput = output.Height - whiteSpace;
         for (int y = 0; y < input.Length; y++)
         {
            for (int x = 0; x < input[0].Length; x++)
            {
               // Zero is white in the bytematrix
               if (input[y][x] == 1)
               {
                  output[x + whiteSpace, yOutput] = true;
               }
            }
            yOutput--;
         }
         return output;
      }

      /// <summary>
      /// Takes and rotates the it 90 degrees
      /// </summary>
      private static sbyte[][] rotateArray(sbyte[][] bitarray)
      {
         sbyte[][] temp = new sbyte[bitarray[0].Length][];
         for (int idx = 0; idx < bitarray[0].Length; idx++)
            temp[idx] = new sbyte[bitarray.Length];
         for (int ii = 0; ii < bitarray.Length; ii++)
         {
            // This makes the direction consistent on screen when rotating the
            // screen;
            int inverseii = bitarray.Length - ii - 1;
            for (int jj = 0; jj < bitarray[0].Length; jj++)
            {
               temp[jj][inverseii] = bitarray[ii][jj];
            }
         }
         return temp;
      }
   }
}
