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

using System.Collections.Generic;

using ZXing.Common;
using ZXing.PDF417.Internal;
using System;

namespace ZXing.PDF417
{
    /// <summary>
    /// This implementation can detect and decode PDF417 codes in an image.
    ///
    /// <author>SITA Lab (kevin.osullivan@sita.aero)</author>
    /// <author>Guenther Grau (java Core)</author>
    /// <author>Stephen Furlani (C# Port)</author>
    /// </summary>
    public sealed class PDF417Reader : Reader
    {
        /// <summary>
        /// NO_POINTS found.
        /// </summary>
        private static readonly ResultPoint[] NO_POINTS = new ResultPoint[0];

        /// <summary>
        /// Locates and decodes a PDF417 code in an image.
        ///
        /// <returns>a String representing the content encoded by the PDF417 code</returns>
        /// <exception cref="FormatException">if a PDF417 cannot be decoded</exception>
        /// </summary>
        public Result Decode(BinaryBitmap image)
        {
            return Decode(image, null);
        }

        /// <summary>
        /// Locates and decodes a barcode in some format within an image. This method also accepts
        /// hints, each possibly associated to some data, which may help the implementation decode.
        /// **Note** this will return the FIRST barcode discovered if there are many.
        /// </summary>
        /// <param name="image">image of barcode to decode</param>
        /// <param name="hints">passed as a <see cref="IDictionary{TKey, TValue}"/> from <see cref="DecodeHintType"/>
        /// to arbitrary data. The
        /// meaning of the data depends upon the hint type. The implementation may or may not do
        /// anything with these hints.</param>
        /// <returns>
        /// String which the barcode encodes
        /// </returns>
        public Result Decode(BinaryBitmap image,
                           IDictionary<DecodeHintType, object> hints)
        {
            Result[] results = Decode(image, hints, false);
            if (results.Length == 0)
            {
                return null;
            } else
            {
                return results[0]; // First barcode discovered.
            }
        }

        /// <summary>
        /// Locates and decodes Multiple PDF417 codes in an image.
        ///
        /// <returns>an array of Strings representing the content encoded by the PDF417 codes</returns>
        /// </summary>
        public Result[] DecodeMultiple(BinaryBitmap image)
        {
            return DecodeMultiple(image, null);
        }
        
        /// <summary>
        /// Locates and decodes multiple barcodes in some format within an image. This method also accepts
        /// hints, each possibly associated to some data, which may help the implementation decode.
        /// </summary>
        /// <param name="image">image of barcode to decode</param>
        /// <param name="hints">passed as a <see cref="IDictionary{TKey, TValue}"/> from <see cref="DecodeHintType"/>
        /// to arbitrary data. The
        /// meaning of the data depends upon the hint type. The implementation may or may not do
        /// anything with these hints.</param>
        /// <returns>
        /// String which the barcodes encode
        /// </returns>
        public Result[] DecodeMultiple(BinaryBitmap image, 
                                       IDictionary<DecodeHintType, object> hints)
        {
            return Decode(image, hints, true);
        }

        /// <summary>
        /// Decode the specified image, with the hints and optionally multiple barcodes.
        /// Based on Owen's Comments in <see cref="ZXing.ReaderException"/>, this method has been modified to continue silently
        /// if a barcode was not decoded where it was detected instead of throwing a new exception object.
        /// </summary>
        /// <param name="image">Image.</param>
        /// <param name="hints">Hints.</param>
        /// <param name="multiple">If set to <c>true</c> multiple.</param>
        private static Result[] Decode(BinaryBitmap image, IDictionary<DecodeHintType, object> hints, bool multiple)
        {
            List<Result> results = new List<Result>();
            PDF417DetectorResult detectorResult = Detector.Detect(image, hints, multiple);
            foreach (ResultPoint[] points in detectorResult.Points)
            {
                DecoderResult decoderResult = PDF417ScanningDecoder.Decode(detectorResult.Bits, points[4], points[5],
                                                                           points[6], points[7], GetMinCodewordWidth(points), GetMaxCodewordWidth(points));
                if (decoderResult == null)
                {
                    // See comments re: Exceptions above
                    // continue;
                    throw ReaderException.Instance;
                }
                System.Diagnostics.Debug.WriteLine("Result " + points.ToString() + " > " + decoderResult.Text + " " + decoderResult.RawBytes);
                Result result = new Result(decoderResult.Text, decoderResult.RawBytes, points, BarcodeFormat.PDF_417);
                result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, decoderResult.ECLevel);
                PDF417ResultMetadata pdf417ResultMetadata = (PDF417ResultMetadata)decoderResult.Other;
                if (pdf417ResultMetadata != null)
                {
                    result.putMetadata(ResultMetadataType.PDF417_EXTRA_METADATA, pdf417ResultMetadata);
                }
                results.Add(result);
            }
            return results.ToArray();
        }

        /// <summary>
        /// Gets the maximum width of the barcode
        /// </summary>
        /// <returns>The max width.</returns>
        /// <param name="p1">P1.</param>
        /// <param name="p2">P2.</param>
        private static int GetMaxWidth(ResultPoint p1, ResultPoint p2)
        {
            if (p1 == null || p2 == null)
            {
                return 0;
            }
            return (int)Math.Abs(p1.X - p2.X);
        }

        /// <summary>
        /// Gets the minimum width of the barcode
        /// </summary>
        /// <returns>The minimum width.</returns>
        /// <param name="p1">P1.</param>
        /// <param name="p2">P2.</param>
        private static int GetMinWidth(ResultPoint p1, ResultPoint p2)
        {
            if (p1 == null || p2 == null)
            {
                return int.MaxValue;
            }
            return (int)Math.Abs(p1.X - p2.X);
        }

        /// <summary>
        /// Gets the maximum width of the codeword.
        /// </summary>
        /// <returns>The max codeword width.</returns>
        /// <param name="p">P.</param>
        private static int GetMaxCodewordWidth(ResultPoint[] p)
        {
            return Math.Max(
                Math.Max(GetMaxWidth(p[0], p[4]), GetMaxWidth(p[6], p[2]) * PDF417Common.MODULES_IN_CODEWORD /
                PDF417Common.MODULES_IN_STOP_PATTERN),
                Math.Max(GetMaxWidth(p[1], p[5]), GetMaxWidth(p[7], p[3]) * PDF417Common.MODULES_IN_CODEWORD /
                PDF417Common.MODULES_IN_STOP_PATTERN));
        }

        /// <summary>
        /// Gets the minimum width of the codeword.
        /// </summary>
        /// <returns>The minimum codeword width.</returns>
        /// <param name="p">P.</param>
        private static int GetMinCodewordWidth(ResultPoint[] p)
        {
            return Math.Min(
                Math.Min(GetMinWidth(p[0], p[4]), GetMinWidth(p[6], p[2]) * PDF417Common.MODULES_IN_CODEWORD /
                PDF417Common.MODULES_IN_STOP_PATTERN),
                Math.Min(GetMinWidth(p[1], p[5]), GetMinWidth(p[7], p[3]) * PDF417Common.MODULES_IN_CODEWORD /
                PDF417Common.MODULES_IN_STOP_PATTERN));
        }

        /// <summary>
        /// Resets any internal state the implementation has after a decode, to prepare it
        /// for reuse.
        /// </summary>
        public void Reset()
        {
            // do nothing
        }

        /// <summary>
        /// This method detects a code in a "pure" image -- that is, pure monochrome image
        /// which contains only an unrotated, unskewed, image of a code, with some white border
        /// around it. This is a specialized method that works exceptionally fast in this special
        /// case.
        ///
        /// <see cref="QrCode.QRCodeReader.extractPureBits(BitMatrix)" />
        /// <see cref="Datamatrix.DataMatrixReader.extractPureBits(BitMatrix)" />
        /// </summary>
        private static BitMatrix ExtractPureBits(BitMatrix image)
        {

            int[] leftTopBlack = image.getTopLeftOnBit();
            int[] rightBottomBlack = image.getBottomRightOnBit();
            if (leftTopBlack == null || rightBottomBlack == null)
            {
                return null;
            }

            int moduleSize;
            if (!PDF417Reader.ModuleSize(leftTopBlack, image, out moduleSize))
                return null;

            int top = leftTopBlack[1];
            int bottom = rightBottomBlack[1];
            int left;
            if (!FindPatternStart(leftTopBlack[0], top, image, out left))
                return null;
            int right;
            if (!FindPatternEnd(leftTopBlack[0], top, image, out right))
                return null;

            int matrixWidth = (right - left + 1) / moduleSize;
            int matrixHeight = (bottom - top + 1) / moduleSize;
            if (matrixWidth <= 0 || matrixHeight <= 0)
            {
                return null;
            }

            // Push in the "border" by half the module width so that we start
            // sampling in the middle of the module. Just in case the image is a
            // little off, this will help recover.
            int nudge = moduleSize >> 1;
            top += nudge;
            left += nudge;

            // Now just read off the bits
            var bits = new BitMatrix(matrixWidth, matrixHeight);
            for (int y = 0; y < matrixHeight; y++)
            {
                int iOffset = top + y * moduleSize;
                for (int x = 0; x < matrixWidth; x++)
                {
                    if (image[left + x * moduleSize, iOffset])
                    {
                        bits[x, y] = true;
                    }
                }
            }
            return bits;
        }

        /// <summary>
        /// Computes the Module Size
        /// </summary>
        /// <returns><c>true</c>, if size was moduled, <c>false</c> otherwise.</returns>
        /// <param name="leftTopBlack">Left top black.</param>
        /// <param name="image">Image.</param>
        /// <param name="msize">Msize.</param>
        private static bool ModuleSize(int[] leftTopBlack, BitMatrix image, out int msize)
        {
            int x = leftTopBlack[0];
            int y = leftTopBlack[1];
            int width = image.Width;
            while (x < width && image[x, y])
            {
                x++;
            }
            if (x == width)
            {
                msize = 0;
                return false;
            }

            msize = (int)((uint)(x - leftTopBlack[0]) >> 3); // (x - leftTopBlack[0]) >>> 3// We've crossed left first bar, which is 8x
            if (msize == 0)
            {
                return false;
            }
            return true;
        }

        /// <summary>
        /// Finds the pattern start.
        /// </summary>
        /// <returns><c>true</c>, if pattern start was found, <c>false</c> otherwise.</returns>
        /// <param name="x">The x coordinate.</param>
        /// <param name="y">The y coordinate.</param>
        /// <param name="image">Image.</param>
        /// <param name="start">Start.</param>
        private static bool FindPatternStart(int x, int y, BitMatrix image, out int start)
        {
            int width = image.Width;
            start = x;
            // start should be on black
            int transitions = 0;
            bool black = true;
            while (start < width - 1 && transitions < 8)
            {
                start++;
                bool newBlack = image[start, y];
                if (black != newBlack)
                {
                    transitions++;
                }
                black = newBlack;
            }
            if (start == width - 1)
            {
                return false;
            }
            return true;
        }

        /// <summary>
        /// Finds the pattern end.
        /// </summary>
        /// <returns><c>true</c>, if pattern end was found, <c>false</c> otherwise.</returns>
        /// <param name="x">The x coordinate.</param>
        /// <param name="y">The y coordinate.</param>
        /// <param name="image">Image.</param>
        /// <param name="end">End.</param>
        private static bool FindPatternEnd(int x, int y, BitMatrix image, out int end)
        {
            int width = image.Width;
            end = width - 1;
            // end should be on black
            while (end > x && !image[end, y])
            {
                end--;
            }
            int transitions = 0;
            bool black = true;
            while (end > x && transitions < 9)
            {
                end--;
                bool newBlack = image[end, y];
                if (black != newBlack)
                {
                    transitions++;
                }
                black = newBlack;
            }
            if (end == x)
            {
                return false;
            }
            return true;
        }
    }
}
