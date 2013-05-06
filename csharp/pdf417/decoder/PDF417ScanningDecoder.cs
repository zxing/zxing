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
using System.Collections.Generic;
using System.Linq;

using ZXing.Common;
using ZXing.PDF417.Internal.EC;

using Log = System.Diagnostics.Debug;
using System.Text;

namespace ZXing.PDF417.Internal
{
    /// <summary>
    /// 
    /// </summary>
    /// <author>Guenther Grau (Java Core)</author>
    /// <author>Stephen Furlani (C# Port)</author>
    public static class PDF417ScanningDecoder
    {
        private static readonly int CODEWORD_SKEW_SIZE = 2;
        
        private static readonly int MAX_ERRORS = 3;
        private static readonly int MAX_EC_CODEWORDS = 512;
        private static readonly ErrorCorrection errorCorrection = new ErrorCorrection();

        /// <summary>
        /// Decode the specified image, imageTopLeft, imageBottomLeft, imageTopRight, imageBottomRight, minCodewordWidth
        /// and maxCodewordWidth.
        /// TODO: don't pass in minCodewordWidth and maxCodewordWidth, pass in barcode columns for start and stop pattern
        /// columns. That way width can be deducted from the pattern column.
        /// This approach also allows to detect more details about the barcode, e.g. if a bar type (white or black) is wider 
        /// than it should be. This can happen if the scanner used a bad blackpoint.
        /// </summary>
        /// <param name="image">Image.</param>
        /// <param name="imageTopLeft">Image top left.</param>
        /// <param name="imageBottomLeft">Image bottom left.</param>
        /// <param name="imageTopRight">Image top right.</param>
        /// <param name="imageBottomRight">Image bottom right.</param>
        /// <param name="minCodewordWidth">Minimum codeword width.</param>
        /// <param name="maxCodewordWidth">Max codeword width.</param>
        public static DecoderResult Decode(BitMatrix image,
                                           ResultPoint imageTopLeft,
                                           ResultPoint imageBottomLeft,
                                           ResultPoint imageTopRight,
                                           ResultPoint imageBottomRight,
                                           int minCodewordWidth,
                                           int maxCodewordWidth)
        {
            BoundingBox boundingBox = new BoundingBox(image, imageTopLeft, imageBottomLeft, imageTopRight, imageBottomRight);
            DetectionResultRowIndicatorColumn leftRowIndicatorColumn = null;
            DetectionResultRowIndicatorColumn rightRowIndicatorColumn = null;
            DetectionResult detectionResult = null;
            for (int i = 0; i < 2; i++)
            {
                if (imageTopLeft != null)
                {
                    leftRowIndicatorColumn = GetRowIndicatorColumn(image, boundingBox, imageTopLeft, true, minCodewordWidth, maxCodewordWidth);
                    Log.WriteLine("Before setRowNumbers\n" + leftRowIndicatorColumn);
                    leftRowIndicatorColumn.SetRowNumbers();
                    Log.WriteLine("After setRowNumbers\n" + leftRowIndicatorColumn);
                }
                if (imageTopRight != null)
                {
                    rightRowIndicatorColumn = GetRowIndicatorColumn(image, boundingBox, imageTopRight, false, minCodewordWidth, maxCodewordWidth);
                    Log.WriteLine("Before setRowNumbers\n" + rightRowIndicatorColumn);
                    rightRowIndicatorColumn.SetRowNumbers();
                    Log.WriteLine("After setRowNumbers\n" + rightRowIndicatorColumn);
                }
                detectionResult = Merge(leftRowIndicatorColumn, rightRowIndicatorColumn);
                if (detectionResult == null)
                {
                    // TODO Based on Owen's Comments in <see cref="ZXing.ReaderException"/>, this method has been modified to continue silently
                    // if a barcode was not decoded where it was detected instead of throwing a new exception object.
                    // return null;
                    throw ReaderException.Instance;
                }
                if (i == 0 &&
                    (detectionResult.Box.MinY < boundingBox.MinY || detectionResult.Box.MaxY > boundingBox.MaxY))
                {
                    boundingBox = detectionResult.Box;
                } else
                {
                    detectionResult.Box = boundingBox;
                    break;
                }
            }
            int maxBarcodeColumn = detectionResult.ColumnCount + 1;
            detectionResult.DetectionResultColumns[0] = leftRowIndicatorColumn;

            detectionResult.DetectionResultColumns[maxBarcodeColumn] = rightRowIndicatorColumn;
            
            bool leftToRight = leftRowIndicatorColumn != null;
            for (int barcodeColumnCount = 1; barcodeColumnCount <= maxBarcodeColumn; barcodeColumnCount++)
            {
                int barcodeColumn = leftToRight ? barcodeColumnCount : maxBarcodeColumn - barcodeColumnCount;
                if (detectionResult.DetectionResultColumns[barcodeColumn] != null)
                {
                    // This will be the case for the opposite row indicator column, which doesn't need to be decoded again.
                    continue;
                }
                DetectionResultColumn detectionResultColumn;
                if (barcodeColumn == 0 || barcodeColumn == maxBarcodeColumn)
                {
                    detectionResultColumn = new DetectionResultRowIndicatorColumn(boundingBox, barcodeColumn == 0);
                } else
                {
                    detectionResultColumn = new DetectionResultColumn(boundingBox);
                }
                detectionResult.DetectionResultColumns[barcodeColumn] = detectionResultColumn;
                int startColumn = -1;
                int previousStartColumn = startColumn;
                // TODO start at a row for which we know the start position, then detect upwards and downwards from there.
                for (int imageRow = boundingBox.MinY; imageRow <= boundingBox.MaxY; imageRow++)
                {
                    startColumn = GetStartColumn(detectionResult, barcodeColumn, imageRow, leftToRight);
                    if (startColumn < 0 || startColumn > boundingBox.MaxX)
                    {
                        if (previousStartColumn == -1)
                        {
                            continue;
                        }
                        startColumn = previousStartColumn;
                    }
                    Codeword codeword = DetectCodeword(image, boundingBox.MinX, boundingBox.MaxX, leftToRight,
                                                       startColumn, imageRow, minCodewordWidth, maxCodewordWidth);
                    if (codeword != null)
                    {
                        detectionResultColumn.SetCodeword(imageRow, codeword);
                        previousStartColumn = startColumn;
                        minCodewordWidth = Math.Min(minCodewordWidth, codeword.Width);
                        maxCodewordWidth = Math.Max(maxCodewordWidth, codeword.Width);
                    }
                }
            }
            return CreateDecoderResult(detectionResult);
        }

        /// <summary>
        /// Merge the specified leftRowIndicatorColumn and rightRowIndicatorColumn.
        /// </summary>
        /// <param name="leftRowIndicatorColumn">Left row indicator column.</param>
        /// <param name="rightRowIndicatorColumn">Right row indicator column.</param>
        private static DetectionResult Merge(DetectionResultRowIndicatorColumn leftRowIndicatorColumn,
                                             DetectionResultRowIndicatorColumn rightRowIndicatorColumn)
        {
            if (leftRowIndicatorColumn == null && rightRowIndicatorColumn == null)
            {
                return null;
            }
            BarcodeMetadata barcodeMetadata = GetBarcodeMetadata(leftRowIndicatorColumn, rightRowIndicatorColumn);
            if (barcodeMetadata == null)
            {
                return null;
            }
            BoundingBox boundingBox = GetBoundingBox(leftRowIndicatorColumn, rightRowIndicatorColumn);
            return new DetectionResult(barcodeMetadata, boundingBox);
        }

        /// <summary>
        /// Gets the bounding box.
        /// </summary>
        /// <returns>The bounding box.</returns>
        /// <param name="leftRowIndicatorColumn">Left row indicator column.</param>
        /// <param name="rightRowIndicatorColumn">Right row indicator column.</param>
        private static BoundingBox GetBoundingBox(DetectionResultRowIndicatorColumn leftRowIndicatorColumn,
                                                  DetectionResultRowIndicatorColumn rightRowIndicatorColumn)
        {
            BoundingBox box1 = AdjustBoundingBox(leftRowIndicatorColumn);
            BoundingBox box2 = AdjustBoundingBox(rightRowIndicatorColumn);
            
            return BoundingBox.Merge(box1, box2);
        }

        /// <summary>
        /// Adjusts the bounding box.
        /// </summary>
        /// <returns>The bounding box.</returns>
        /// <param name="rowIndicatorColumn">Row indicator column.</param>
        private static BoundingBox AdjustBoundingBox(DetectionResultRowIndicatorColumn rowIndicatorColumn)
        {
            if (rowIndicatorColumn == null)
            {
                return null;
            }
            int[] rowHeights = rowIndicatorColumn.GetRowHeights();
            int maxRowHeight = rowHeights.Max();
            int missingStartRows = 0;
            foreach (int rowHeight in rowHeights)
            {
                missingStartRows += maxRowHeight - rowHeight;
                if (rowHeight > 0)
                {
                    break;
                }
            }
            Codeword[] codewords = rowIndicatorColumn.Codewords;
            for (int row = 0; missingStartRows > 0 && codewords[row] == null; row++)
            {
                missingStartRows--;
            }
            int missingEndRows = 0;
            for (int row = rowHeights.Length - 1; row >= 0; row--)
            {
                missingEndRows += maxRowHeight - rowHeights[row];
                if (rowHeights[row] > 0)
                {
                    break;
                }
            }
            for (int row = codewords.Length - 1; missingEndRows > 0 && codewords[row] == null; row--)
            {
                missingEndRows--;
            }
            return rowIndicatorColumn.Box.AddMissingRows(missingStartRows, missingEndRows, rowIndicatorColumn.IsLeft);
        }

        /// <summary>
        /// Gets the barcode metadata.
        /// </summary>
        /// <returns>The barcode metadata.</returns>
        /// <param name="leftRowIndicatorColumn">Left row indicator column.</param>
        /// <param name="rightRowIndicatorColumn">Right row indicator column.</param>
        private static BarcodeMetadata GetBarcodeMetadata(DetectionResultRowIndicatorColumn leftRowIndicatorColumn,
                                                          DetectionResultRowIndicatorColumn rightRowIndicatorColumn)
        {
            if (leftRowIndicatorColumn == null || leftRowIndicatorColumn.GetBarcodeMetadata() == null)
            {
                return rightRowIndicatorColumn == null ? null : rightRowIndicatorColumn.GetBarcodeMetadata();
            }
            if (rightRowIndicatorColumn == null || rightRowIndicatorColumn.GetBarcodeMetadata() == null)
            {
                return leftRowIndicatorColumn == null ? null : leftRowIndicatorColumn.GetBarcodeMetadata();
            }
            BarcodeMetadata leftBarcodeMetadata = leftRowIndicatorColumn.GetBarcodeMetadata();
            BarcodeMetadata rightBarcodeMetadata = rightRowIndicatorColumn.GetBarcodeMetadata();

            if (leftBarcodeMetadata.ColumnCount != rightBarcodeMetadata.ColumnCount &&
                leftBarcodeMetadata.ErrorCorrectionLevel != rightBarcodeMetadata.ErrorCorrectionLevel &&
                leftBarcodeMetadata.RowCount != rightBarcodeMetadata.RowCount)
            {
                return null;
            }
            return leftBarcodeMetadata;
        }

        /// <summary>
        /// Gets the row indicator column.
        /// </summary>
        /// <returns>The row indicator column.</returns>
        /// <param name="image">Image.</param>
        /// <param name="boundingBox">Bounding box.</param>
        /// <param name="startPoint">Start point.</param>
        /// <param name="leftToRight">If set to <c>true</c> left to right.</param>
        /// <param name="minCodewordWidth">Minimum codeword width.</param>
        /// <param name="maxCodewordWidth">Max codeword width.</param>
        private static DetectionResultRowIndicatorColumn GetRowIndicatorColumn(BitMatrix image,
                                                                               BoundingBox boundingBox,
                                                                               ResultPoint startPoint,
                                                                               bool leftToRight,
                                                                               int minCodewordWidth,
                                                                               int maxCodewordWidth)
        {
            DetectionResultRowIndicatorColumn rowIndicatorColumn = new DetectionResultRowIndicatorColumn(boundingBox, leftToRight);
            for (int i = 0; i < 2; i++)
            {
                int increment = i == 0 ? 1 : -1;
                int startColumn = (int)startPoint.X;
                for (int imageRow = (int) startPoint.Y; imageRow <= boundingBox.MaxY &&
                     imageRow >= boundingBox.MinY; imageRow += increment)
                {
                    Codeword codeword = DetectCodeword(image, 0, image.Width, leftToRight, startColumn, imageRow,
                                                       minCodewordWidth, maxCodewordWidth);
                    if (codeword != null)
                    {
                        rowIndicatorColumn.SetCodeword(imageRow, codeword);
                        if (leftToRight)
                        {
                            startColumn = codeword.StartX;
                        } else
                        {
                            startColumn = codeword.EndX;
                        }
                    }
                }
            }
            return rowIndicatorColumn;
        }

        /// <summary>
        /// Adjusts the codeword count.
        /// </summary>
        /// <param name="detectionResult">Detection result.</param>
        /// <param name="barcodeMatrix">Barcode matrix.</param>
        private static void AdjustCodewordCount(DetectionResult detectionResult, BarcodeValue[][] barcodeMatrix)
        {
            int[] numberOfCodewords = barcodeMatrix[0][1].GetValue();
            int calculatedNumberOfCodewords = detectionResult.ColumnCount *
                detectionResult.RowCount -
                GetNumberOfECCodeWords(detectionResult.ErrorCorrectionLevel);
            if (numberOfCodewords.Length == 0)
            {
                if (calculatedNumberOfCodewords < 1 || calculatedNumberOfCodewords > PDF417Common.MAX_CODEWORDS_IN_BARCODE)
                {
                    throw ReaderException.Instance;
                }
                barcodeMatrix[0][1].SetValue(calculatedNumberOfCodewords);
            } else if (numberOfCodewords[0] != calculatedNumberOfCodewords)
            {
                // The calculated one is more reliable as it is derived from the row indicator columns
                barcodeMatrix[0][1].SetValue(calculatedNumberOfCodewords);
            }
        }

        /// <summary>
        /// Creates the decoder result.
        /// </summary>
        /// <returns>The decoder result.</returns>
        /// <param name="detectionResult">Detection result.</param>
        private static DecoderResult CreateDecoderResult(DetectionResult detectionResult)
        {
            BarcodeValue[][] barcodeMatrix = CreateBarcodeMatrix(detectionResult);
            AdjustCodewordCount(detectionResult, barcodeMatrix);

            List<int> erasures = new List<int>();
            int[] codewords = new int[detectionResult.RowCount * detectionResult.ColumnCount];
            List<int[]> ambiguousIndexValuesList = new List<int[]>();
            List<int> ambiguousIndexesList = new List<int>();
            for (int row = 0; row < detectionResult.RowCount; row++)
            {
                for (int column = 0; column < detectionResult.ColumnCount; column++)
                {
                    int[] values = barcodeMatrix[row][column + 1].GetValue();
                    int codewordIndex = row * detectionResult.ColumnCount + column;
                    if (values.Length == 0)
                    {
                        erasures.Add(codewordIndex);
                    } else if (values.Length == 1)
                    {
                        codewords[codewordIndex] = values[0];
                    } else
                    {
                        ambiguousIndexesList.Add(codewordIndex);
                        ambiguousIndexValuesList.Add(values);
                    }
                }
            }
            int[][] ambiguousIndexValues = new int[ambiguousIndexValuesList.Count][];
            for (int i = 0; i < ambiguousIndexValues.Length; i++)
            {
                ambiguousIndexValues[i] = ambiguousIndexValuesList[i];
            }
            return CreateDecoderResultFromAmbiguousValues(detectionResult.ErrorCorrectionLevel, codewords,
                                                          erasures.ToArray(), ambiguousIndexesList.ToArray(), ambiguousIndexValues);
        }

        /**
   * This method deals with the fact, that the decoding process doesn't always yield a single most likely value. The
   * current error correction implementation doesn't deal with erasures very well, so it's better to provide a value
   * for these ambiguous codewords instead of treating it as an erasure. The problem is that we don't know which of
   * the ambiguous values to choose. We try decode using the first value, and if that fails, we use another of the
   * ambiguous values and try to decode again. This usually only happens on very hard to read and decode barcodes,
   * so decoding the normal barcodes is not affected by this. 
   * @param ecLevel
   * @param codewords
   * @param erasureArray contains the indexes of erasures
   * @param ambiguousIndexes array with the indexes that have more than one most likely value
   * @param ambiguousIndexValues two dimensional array that contains the ambiguous values. The first dimension must
   * be the same Length as the ambiguousIndexes array
   * @return
   * @throws FormatException
   * @throws ChecksumException
   */
        /// <summary>
        /// This method deals with the fact, that the decoding process doesn't always yield a single most likely value. The
        /// current error correction implementation doesn't deal with erasures very well, so it's better to provide a value
        /// for these ambiguous codewords instead of treating it as an erasure. The problem is that we don't know which of
        /// the ambiguous values to choose. We try decode using the first value, and if that fails, we use another of the
        /// ambiguous values and try to decode again. This usually only happens on very hard to read and decode barcodes,
        /// so decoding the normal barcodes is not affected by this.
        /// </summary>
        /// <returns>The decoder result from ambiguous values.</returns>
        /// <param name="ecLevel">Ec level.</param>
        /// <param name="codewords">Codewords.</param>
        /// <param name="erasureArray">contains the indexes of erasures.</param>
        /// <param name="ambiguousIndexes">array with the indexes that have more than one most likely value.</param>
        /// <param name="ambiguousIndexValues">two dimensional array that contains the ambiguous values. The first dimension must
        /// be the same Length as the ambiguousIndexes array.</param>
        /// <exception cref="Zxing.ReaderException"></exception>
        private static DecoderResult CreateDecoderResultFromAmbiguousValues(int ecLevel,
                                                                            int[] codewords,
                                                                            int[] erasureArray,
                                                                            int[] ambiguousIndexes,
                                                                            int[][] ambiguousIndexValues)
        {
            int[] ambiguousIndexCount = new int[ambiguousIndexes.Length];
            
            int tries = 100;
            while (tries-- > 0)
            {
                for (int i = 0; i < ambiguousIndexCount.Length; i++)
                {
                    codewords[ambiguousIndexes[i]] = ambiguousIndexValues[i][ambiguousIndexCount[i]];
                }
                try
                {
                    return DecodeCodewords(codewords, ecLevel, erasureArray);
                } catch (ReaderException ignored)
                {
                    //
                }
                if (ambiguousIndexCount.Length == 0)
                {
                    throw ReaderException.Instance;
                }
                for (int i = 0; i < ambiguousIndexCount.Length; i++)
                {
                    if (ambiguousIndexCount[i] < ambiguousIndexValues[i].Length - 1)
                    {
                        ambiguousIndexCount[i]++;
                        break;
                    } else
                    {
                        ambiguousIndexCount[i] = 0;
                        if (i == ambiguousIndexCount.Length - 1)
                        {
                            throw ReaderException.Instance;
                        }
                    }
                }
            }
            throw ReaderException.Instance;
        }

        /// <summary>
        /// Creates the barcode matrix.
        /// </summary>
        /// <returns>The barcode matrix.</returns>
        /// <param name="detectionResult">Detection result.</param>
        private static BarcodeValue[][] CreateBarcodeMatrix(DetectionResult detectionResult)
        {
            // Manually setup Jagged Array in C#
            BarcodeValue[][] barcodeMatrix = new BarcodeValue[detectionResult.RowCount][];
            for (int row = 0; row < barcodeMatrix.Length; row++)
            {
                barcodeMatrix[row] = new BarcodeValue[detectionResult.ColumnCount + 2];// Enumerable.Repeat(new BarcodeValue(), detectionResult.ColumnCount + 2).ToArray();
                for (int col = 0; col < barcodeMatrix[row].Length; col++)
                {
                    barcodeMatrix[row][col] = new BarcodeValue();
                }
            }
            
            int column = -1;
            foreach (DetectionResultColumn detectionResultColumn in detectionResult.GetDetectionResultColumns())
            {
                column++;
                if (detectionResultColumn == null)
                {
                    continue;
                }
                foreach (Codeword codeword in detectionResultColumn.Codewords)
                {
                    if (codeword == null || codeword.RowNumber == -1)
                    {
                        continue;
                    }
                    barcodeMatrix[codeword.RowNumber][column].SetValue(codeword.Value);
                }
            }
            return barcodeMatrix;
        }

        /// <summary>
        /// Tests to see if the Barcode Column is Valid
        /// </summary>
        /// <returns><c>true</c>, if barcode column is valid, <c>false</c> otherwise.</returns>
        /// <param name="detectionResult">Detection result.</param>
        /// <param name="barcodeColumn">Barcode column.</param>
        private static bool IsValidBarcodeColumn(DetectionResult detectionResult, int barcodeColumn)
        {
            return (barcodeColumn >= 0) && (barcodeColumn < detectionResult.DetectionResultColumns.Length);
        }

        /// <summary>
        /// Gets the start column.
        /// </summary>
        /// <returns>The start column.</returns>
        /// <param name="detectionResult">Detection result.</param>
        /// <param name="barcodeColumn">Barcode column.</param>
        /// <param name="imageRow">Image row.</param>
        /// <param name="leftToRight">If set to <c>true</c> left to right.</param>
        private static int GetStartColumn(DetectionResult detectionResult,
                                          int barcodeColumn,
                                          int imageRow,
                                          bool leftToRight)
        {
            int offset = leftToRight ? 1 : -1;
            Codeword codeword = null;
            if (IsValidBarcodeColumn(detectionResult, barcodeColumn - offset))
            {
                codeword = detectionResult.DetectionResultColumns[barcodeColumn - offset].GetCodeword(imageRow);
            }
            if (codeword != null)
            {
                return leftToRight ? codeword.EndX : codeword.StartX;
            }
            codeword = detectionResult.DetectionResultColumns[barcodeColumn].GetCodewordNearby(imageRow);
            if (codeword != null)
            {
                return leftToRight ? codeword.EndX : codeword.StartX;
            }
            if (IsValidBarcodeColumn(detectionResult, barcodeColumn - offset))
            {
                codeword = detectionResult.DetectionResultColumns[barcodeColumn - offset].GetCodewordNearby(imageRow);
            }
            if (codeword != null)
            {
                return leftToRight ? codeword.EndX : codeword.StartX;
            }
            int skippedColumns = 0;
            
            while (IsValidBarcodeColumn(detectionResult, barcodeColumn - offset))
            {
                barcodeColumn -= offset;
                foreach (Codeword previousRowCodeword in detectionResult.DetectionResultColumns[barcodeColumn].Codewords)
                {
                    if (previousRowCodeword != null)
                    {
                        return (leftToRight ? previousRowCodeword.EndX : previousRowCodeword.StartX) +
                            offset *
                            skippedColumns *
                            (previousRowCodeword.EndX - previousRowCodeword.StartX);
                    }
                }
                skippedColumns++;
            }
            return leftToRight ? detectionResult.Box.MinX : detectionResult.Box.MaxX;
        }

        /// <summary>
        /// Detects the codeword.
        /// </summary>
        /// <returns>The codeword.</returns>
        /// <param name="image">Image.</param>
        /// <param name="minColumn">Minimum column.</param>
        /// <param name="maxColumn">Max column.</param>
        /// <param name="leftToRight">If set to <c>true</c> left to right.</param>
        /// <param name="startColumn">Start column.</param>
        /// <param name="imageRow">Image row.</param>
        /// <param name="minCodewordWidth">Minimum codeword width.</param>
        /// <param name="maxCodewordWidth">Max codeword width.</param>
        private static Codeword DetectCodeword(BitMatrix image,
                                               int minColumn,
                                               int maxColumn,
                                               bool leftToRight,
                                               int startColumn,
                                               int imageRow,
                                               int minCodewordWidth,
                                               int maxCodewordWidth)
        {
            startColumn = AdjustCodewordStartColumn(image, minColumn, maxColumn, leftToRight, startColumn, imageRow);
            // we usually know fairly exact now how long a codeword is. We should provide minimum and maximum expected Length
            // and try to adjust the read pixels, e.g. remove single pixel errors or try to cut off exceeding pixels.
            // min and maxCodewordWidth should not be used as they are calculated for the whole barcode an can be inaccurate
            // for the current position
            int[] moduleBitCount = GetModuleBitCount(image, minColumn, maxColumn, leftToRight, startColumn, imageRow);
            if (moduleBitCount == null)
            {
                return null;
            }
            int endColumn;
            int codewordBitCount = PDF417Common.GetBitCountSum(moduleBitCount);
            if (leftToRight)
            {
                endColumn = startColumn + codewordBitCount;
            } else
            {
                for (int i = 0; i < moduleBitCount.Length >> 1; i++)
                {
                    int tmpCount = moduleBitCount[i];
                    moduleBitCount[i] = moduleBitCount[moduleBitCount.Length - 1 - i];
                    moduleBitCount[moduleBitCount.Length - 1 - i] = tmpCount;
                }
                endColumn = startColumn;
                startColumn = endColumn - codewordBitCount;
            }
            // TODO implement check for width and correction of black and white bars
            // use start (and maybe stop pattern) to determine if blackbars are wider than white bars. If so, adjust.
            // should probably done only for codewords with a lot more than 17 bits. 
            // The following fixes 10-1.png, which has wide black bars and small white bars
            //    for (int i = 0; i < moduleBitCount.Length; i++) {
            //      if (i % 2 == 0) {
            //        moduleBitCount[i]--;
            //      } else {
            //        moduleBitCount[i]++;
            //      }
            //    }
            
            // We could also use the width of surrounding codewords for more accurate results, but this seems
            // sufficient for now
            if (!CheckCodewordSkew(codewordBitCount, minCodewordWidth, maxCodewordWidth))
            {
                // We could try to use the startX and endX position of the codeword in the same column in the previous row,
                // create the bit count from it and normalize it to 8. This would help with single pixel errors.
                return null;
            }
            
            int decodedValue = PDF417CodewordDecoder.GetDecodedValue(moduleBitCount);
            int codeword = PDF417Common.GetCodeword(decodedValue);
            if (codeword == -1)
            {
                return null;
            }
            return new Codeword(startColumn, endColumn, GetCodewordBucketNumber(decodedValue), codeword);
        }

        /// <summary>
        /// Gets the module bit count.
        /// </summary>
        /// <returns>The module bit count.</returns>
        /// <param name="image">Image.</param>
        /// <param name="minColumn">Minimum column.</param>
        /// <param name="maxColumn">Max column.</param>
        /// <param name="leftToRight">If set to <c>true</c> left to right.</param>
        /// <param name="startColumn">Start column.</param>
        /// <param name="imageRow">Image row.</param>
        private static int[] GetModuleBitCount(BitMatrix image,
                                               int minColumn,
                                               int maxColumn,
                                               bool leftToRight,
                                               int startColumn,
                                               int imageRow)
        {
            int imageColumn = startColumn;
            int[] moduleBitCount = new int[8];
            int moduleNumber = 0;
            int increment = leftToRight ? 1 : -1;
            bool previousPixelValue = leftToRight;
            while (((leftToRight && imageColumn < maxColumn) || (!leftToRight && imageColumn >= minColumn)) &&
                   moduleNumber < moduleBitCount.Length)
            {
                if (image[imageColumn, imageRow] == previousPixelValue)
                {
                    moduleBitCount[moduleNumber]++;
                    imageColumn += increment;
                } else
                {
                    moduleNumber++;
                    previousPixelValue = !previousPixelValue;
                }
            }
            if (moduleNumber == moduleBitCount.Length ||
                (((leftToRight && imageColumn == maxColumn) || (!leftToRight && imageColumn == minColumn)) && moduleNumber == moduleBitCount.Length - 1))
            {
                return moduleBitCount;
            }
            return null;
        }

        /// <summary>
        /// Gets the number of EC code words.
        /// </summary>
        /// <returns>The number of EC code words.</returns>
        /// <param name="barcodeECLevel">Barcode EC level.</param>
        private static int GetNumberOfECCodeWords(int barcodeECLevel)
        {
            return 2 << barcodeECLevel;
        }

        /// <summary>
        /// Adjusts the codeword start column.
        /// </summary>
        /// <returns>The codeword start column.</returns>
        /// <param name="image">Image.</param>
        /// <param name="minColumn">Minimum column.</param>
        /// <param name="maxColumn">Max column.</param>
        /// <param name="leftToRight">If set to <c>true</c> left to right.</param>
        /// <param name="codewordStartColumn">Codeword start column.</param>
        /// <param name="imageRow">Image row.</param>
        private static int AdjustCodewordStartColumn(BitMatrix image,
                                                     int minColumn,
                                                     int maxColumn,
                                                     bool leftToRight,
                                                     int codewordStartColumn,
                                                     int imageRow)
        {
            int correctedStartColumn = codewordStartColumn;
            int increment = leftToRight ? -1 : 1;
            // there should be no black pixels before the start column. If there are, then we need to start earlier.
            for (int i = 0; i < 2; i++)
            {
                while (((leftToRight && correctedStartColumn >= minColumn) || (!leftToRight && correctedStartColumn < maxColumn)) &&
                       leftToRight == image[correctedStartColumn, imageRow])
                {
                    if (Math.Abs(codewordStartColumn - correctedStartColumn) > CODEWORD_SKEW_SIZE)
                    {
                        return codewordStartColumn;
                    }
                    correctedStartColumn += increment;
                }
                increment = -increment;
                leftToRight = !leftToRight;
            }
            return correctedStartColumn;
        }

        /// <summary>
        /// Checks the codeword for any skew.
        /// </summary>
        /// <returns><c>true</c>, if codeword is within the skew, <c>false</c> otherwise.</returns>
        /// <param name="codewordSize">Codeword size.</param>
        /// <param name="minCodewordWidth">Minimum codeword width.</param>
        /// <param name="maxCodewordWidth">Max codeword width.</param>
        private static bool CheckCodewordSkew(int codewordSize, int minCodewordWidth, int maxCodewordWidth)
        {
            return minCodewordWidth - CODEWORD_SKEW_SIZE <= codewordSize &&
                codewordSize <= maxCodewordWidth + CODEWORD_SKEW_SIZE;
        }

        /// <summary>
        /// Decodes the codewords.
        /// </summary>
        /// <returns>The codewords.</returns>
        /// <param name="codewords">Codewords.</param>
        /// <param name="ecLevel">Ec level.</param>
        /// <param name="erasures">Erasures.</param>
        private static DecoderResult DecodeCodewords(int[] codewords, int ecLevel, int[] erasures)
        {
            if (codewords.Length == 0)
            {
                throw ReaderException.Instance;
            }
            
            int numECCodewords = 1 << (ecLevel + 1);
            Log.WriteLine("EC level: " + ecLevel + ", ec codewords: " + numECCodewords);
            
            int correctedErrorsCount = CorrectErrors(codewords, erasures, numECCodewords);
            Log.WriteLine("Corrected errors: " + correctedErrorsCount);
            VerifyCodewordCount(codewords, numECCodewords);
            
            // Decode the codewords
            DecoderResult decoderResult = DecodedBitStreamParser.Decode(codewords);
            decoderResult.ErrorsCorrected = (correctedErrorsCount >= 0) ? correctedErrorsCount : 0;
            decoderResult.Erasures = erasures.Length;
            return decoderResult;
        }

        /// <summary>
        /// Given data and error-correction codewords received, possibly corrupted by errors, attempts to
        /// correct the errors in-place.
        /// </summary>
        /// <returns>The errors.</returns>
        /// <param name="codewords">data and error correction codewords.</param>
        /// <param name="erasures">positions of any known erasures.</param>
        /// <param name="numECCodewords">number of error correction codewords that are available in codewords.</param>
        private static int CorrectErrors(int[] codewords, int[] erasures, int numECCodewords)
        {
            if (erasures != null &&
                erasures.Length > numECCodewords / 2 + MAX_ERRORS ||
                numECCodewords < 0 ||
                numECCodewords > MAX_EC_CODEWORDS)
            {
                // Too many errors or EC Codewords is corrupted
                throw ReaderException.Instance;
                //return -1;

            }
            return errorCorrection.Decode(codewords, numECCodewords, erasures);
        }

        /// <summary>
        /// Verifies that all is well with the the codeword array.
        /// </summary>
        /// <param name="codewords">Codewords.</param>
        /// <param name="numECCodewords">Number EC codewords.</param>
        private static void VerifyCodewordCount(int[] codewords, int numECCodewords)
        {
            if (codewords.Length < 4)
            {
                // Codeword array size should be at least 4 allowing for
                // Count CW, At least one Data CW, Error Correction CW, Error Correction CW
                throw ReaderException.Instance;
            }
            // The first codeword, the Symbol Length Descriptor, shall always encode the total number of data
            // codewords in the symbol, including the Symbol Length Descriptor itself, data codewords and pad
            // codewords, but excluding the number of error correction codewords.
            int numberOfCodewords = codewords[0];
            if (numberOfCodewords > codewords.Length)
            {
                throw ReaderException.Instance;
            }
            if (numberOfCodewords == 0)
            {
                // Reset to the Length of the array - 8 (Allow for at least level 3 Error Correction (8 Error Codewords)
                if (numECCodewords < codewords.Length)
                {
                    codewords[0] = codewords.Length - numECCodewords;
                } else
                {
                    throw ReaderException.Instance;
                }
            }
        }

        /// <summary>
        /// Gets the bit count for codeword.
        /// </summary>
        /// <returns>The bit count for codeword.</returns>
        /// <param name="codeword">Codeword.</param>
        private static int[] GetBitCountForCodeword(int codeword)
        {
            int[] result = new int[8];
            int previousValue = 0;
            int i = result.Length - 1;
            while (true)
            {
                if ((codeword & 0x1) != previousValue)
                {
                    previousValue = codeword & 0x1;
                    i--;
                    if (i < 0)
                    {
                        break;
                    }
                }
                result[i]++;
                codeword >>= 1;
            }
            return result;
        }

        /// <summary>
        /// Gets the codeword bucket number.
        /// </summary>
        /// <returns>The codeword bucket number.</returns>
        /// <param name="codeword">Codeword.</param>
        private static int GetCodewordBucketNumber(int codeword)
        {
            return GetCodewordBucketNumber(GetBitCountForCodeword(codeword));
        }

        /// <summary>
        /// Gets the codeword bucket number.
        /// </summary>
        /// <returns>The codeword bucket number.</returns>
        /// <param name="moduleBitCount">Module bit count.</param>
        private static int GetCodewordBucketNumber(int[] moduleBitCount)
        {
            return (moduleBitCount[0] - moduleBitCount[2] + moduleBitCount[4] - moduleBitCount[6] + 9) % 9;
        }

        /// <summary>
        /// Returns a <see cref="System.String"/> that represents the <see cref="ZXing.PDF417.Internal.BarcodeValue"/> jagged array.
        /// </summary>
        /// <returns>A <see cref="System.String"/> that represents the <see cref="ZXing.PDF417.Internal.BarcodeValue"/> jagged array.</returns>
        /// <param name="barcodeMatrix">Barcode matrix as a jagged array.</param>
        public static String ToString(BarcodeValue[][] barcodeMatrix)
        {
            StringBuilder formatter = new StringBuilder();
            for (int row = 0; row < barcodeMatrix.Length; row++)
            {
                formatter.AppendFormat("Row {0,2}: ", row);
                for (int column = 0; column < barcodeMatrix[row].Length; column++)
                {
                    BarcodeValue barcodeValue = barcodeMatrix[row][column];
                    if (barcodeValue.GetValue().Length == 0)
                    {
                        formatter.Append("        ");
                    } else
                    {
                        formatter.AppendFormat("{0,4}({0,3})", barcodeValue.GetValue()[0], barcodeValue.GetConfidence(barcodeValue.GetValue()[0]));
                    }
                }
                formatter.Append("\n");
            }
            return formatter.ToString();
        }
    }
}

