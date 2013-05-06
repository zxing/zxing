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
using System.Text;

namespace ZXing.PDF417.Internal
{
    /// <summary>
    /// 
    /// </summary>
    /// <author>Guenther Grau (Java Core)</author>
    /// <author>Stephen Furlani (C# Port)</author>
    public class DetectionResult
    {
        private static readonly int ADJUST_ROW_NUMBER_SKIP = 2;

        public BarcodeMetadata Metadata { get; private set; }
        public DetectionResultColumn[] DetectionResultColumns { get; set; }
        public BoundingBox Box { get; set; }
        public int ColumnCount { get; private set; }
        public int RowCount { get { return Metadata.RowCount; } }
        public int ErrorCorrectionLevel { get { return Metadata.ErrorCorrectionLevel; } }

        public DetectionResult(BarcodeMetadata metadata, BoundingBox box)
        {
            this.Metadata = metadata;
            this.Box = box;
            this.ColumnCount = metadata.ColumnCount;
            this.DetectionResultColumns = new DetectionResultColumn[ColumnCount + 2];
        }

        /// <summary>
        /// Returns the DetectionResult Columns.  This does a fair bit of calculation, so call it sparingly.
        /// </summary>
        /// <returns>The detection result columns.</returns>
        public DetectionResultColumn[] GetDetectionResultColumns()
        {
            AdjustIndicatorColumnRowNumbers(DetectionResultColumns[0]);
            AdjustIndicatorColumnRowNumbers(DetectionResultColumns[ColumnCount + 1]);
            int unadjustedCodewordCount = PDF417Common.MAX_CODEWORDS_IN_BARCODE;
            int previousUnadjustedCount;
            do
            {
                previousUnadjustedCount = unadjustedCodewordCount;
                unadjustedCodewordCount = AdjustRowNumbers();
            } while (unadjustedCodewordCount > 0 && unadjustedCodewordCount < previousUnadjustedCount);
            return DetectionResultColumns;
        }

        /// <summary>
        /// Adjusts the indicator column row numbers.
        /// </summary>
        /// <param name="detectionResultColumn">Detection result column.</param>
        private void AdjustIndicatorColumnRowNumbers(DetectionResultColumn detectionResultColumn)
        {
            if (detectionResultColumn != null)
            {
                ((DetectionResultRowIndicatorColumn)detectionResultColumn)
                    .AdjustCompleteIndicatorColumnRowNumbers(Metadata);
            }
        }
        /// <summary>
        /// return number of codewords which don't have a valid row number. Note that the count is not accurate as codewords .
        /// will be counted several times. It just serves as an indicator to see when we can stop adjusting row numbers
        /// </summary>
        /// <returns>The row numbers.</returns>
        private int AdjustRowNumbers()
        {
            // TODO ensure that no detected codewords with unknown row number are left
            // we should be able to estimate the row height and use it as a hint for the row number
            // we should also fill the rows top to bottom and bottom to top
            int unadjustedCount = AdjustRowNumbersByRow();
            if (unadjustedCount == 0)
            {
                return 0;
            }
            for (int barcodeColumn = 1; barcodeColumn < ColumnCount + 1; barcodeColumn++)
            {
                Codeword[] codewords = DetectionResultColumns[barcodeColumn].Codewords;
                for (int codewordsRow = 0; codewordsRow < codewords.Length; codewordsRow++)
                {
                    if (codewords[codewordsRow] == null)
                    {
                        continue;
                    }
                    if (!codewords[codewordsRow].HasValidRowNumber)
                    {
                        AdjustRowNumbers(barcodeColumn, codewordsRow, codewords);
                    }
                }
            }
            return unadjustedCount;
        }

        /// <summary>
        /// Adjusts the row numbers by row.
        /// </summary>
        /// <returns>The row numbers by row.</returns>
        private int AdjustRowNumbersByRow()
        {
            AdjustRowNumbersFromBothRI(); // RI = RowIndicators
            // TODO we should only do full row adjustments if row numbers of left and right row indicator column match.
            // Maybe it's even better to calculated the height (in codeword rows) and divide it by the number of barcode
            // rows. This, together with the LRI and RRI row numbers should allow us to get a good estimate where a row
            // number starts and ends.
            int unadjustedCount = AdjustRowNumbersFromLRI();
            return unadjustedCount + AdjustRowNumbersFromRRI();
        }

        /// <summary>
        /// Adjusts the row numbers from both Row Indicators
        /// </summary>
        /// <returns> zero </returns>
        private int AdjustRowNumbersFromBothRI()
        {
            if (DetectionResultColumns[0] == null || DetectionResultColumns[ColumnCount + 1] == null)
            {
                return 0;
            }
            Codeword[] LRIcodewords = DetectionResultColumns[0].Codewords;
            Codeword[] RRIcodewords = DetectionResultColumns[ColumnCount + 1].Codewords;
            for (int codewordsRow = 0; codewordsRow < LRIcodewords.Length; codewordsRow++)
            {
                if (LRIcodewords[codewordsRow] != null &&
                    RRIcodewords[codewordsRow] != null &&
                    LRIcodewords[codewordsRow].RowNumber == RRIcodewords[codewordsRow].RowNumber)
                {
                    for (int barcodeColumn = 1; barcodeColumn <= ColumnCount; barcodeColumn++)
                    {
                        Codeword codeword = DetectionResultColumns[barcodeColumn].Codewords[codewordsRow];
                        if (codeword == null)
                        {
                            continue;
                        }
                        codeword.RowNumber = LRIcodewords[codewordsRow].RowNumber;
                        if (!codeword.HasValidRowNumber)
                        {
                            // LOG.info("Removing codeword with invalid row number, cw[" + codewordsRow + "][" + barcodeColumn + "]");
                            DetectionResultColumns[barcodeColumn].Codewords[codewordsRow] = null;
                        }
                    }
                }
            }
            return 0;
        }

        /// <summary>
        /// Adjusts the row numbers from Right Row Indicator.
        /// </summary>
        /// <returns>The unadjusted row count.</returns>
        private int AdjustRowNumbersFromRRI()
        {
            if (DetectionResultColumns[ColumnCount + 1] == null)
            {
                return 0;
            }
            int unadjustedCount = 0;
            Codeword[] codewords = DetectionResultColumns[ColumnCount + 1].Codewords;
            for (int codewordsRow = 0; codewordsRow < codewords.Length; codewordsRow++)
            {
                if (codewords[codewordsRow] == null)
                {
                    continue;
                }
                int rowIndicatorRowNumber = codewords[codewordsRow].RowNumber;
                int invalidRowCounts = 0;
                for (int barcodeColumn = ColumnCount + 1; barcodeColumn > 0 && invalidRowCounts < ADJUST_ROW_NUMBER_SKIP; barcodeColumn--)
                {
                    Codeword codeword = DetectionResultColumns[barcodeColumn].Codewords[codewordsRow];
                    if (codeword != null)
                    {
                        invalidRowCounts = AdjustRowNumberIfValid(rowIndicatorRowNumber, invalidRowCounts, codeword);
                        if (!codeword.HasValidRowNumber)
                        {
                            unadjustedCount++;
                        }
                    }
                }
            }
            return unadjustedCount;
        }

        /// <summary>
        /// Adjusts the row numbers from Left Row Indicator.
        /// </summary>
        /// <returns> Unadjusted row Count.</returns>
        private int AdjustRowNumbersFromLRI()
        {
            if (DetectionResultColumns[0] == null)
            {
                return 0;
            }
            int unadjustedCount = 0;
            Codeword[] codewords = DetectionResultColumns[0].Codewords;
            for (int codewordsRow = 0; codewordsRow < codewords.Length; codewordsRow++)
            {
                if (codewords[codewordsRow] == null)
                {
                    continue;
                }
                int rowIndicatorRowNumber = codewords[codewordsRow].RowNumber;
                int invalidRowCounts = 0;
                for (int barcodeColumn = 1; barcodeColumn < ColumnCount + 1 && invalidRowCounts < ADJUST_ROW_NUMBER_SKIP; barcodeColumn++)
                {
                    Codeword codeword = DetectionResultColumns[barcodeColumn].Codewords[codewordsRow];
                    if (codeword != null)
                    {
                        invalidRowCounts = AdjustRowNumberIfValid(rowIndicatorRowNumber, invalidRowCounts, codeword);
                        if (!codeword.HasValidRowNumber)
                        {
                            unadjustedCount++;
                        }
                    }
                }
            }
            return unadjustedCount;
        }

        /// <summary>
        /// Adjusts the row number if valid.
        /// </summary>
        /// <returns>The invalid rows</returns>
        /// <param name="rowIndicatorRowNumber">Row indicator row number.</param>
        /// <param name="invalidRowCounts">Invalid row counts.</param>
        /// <param name="codeword">Codeword.</param>
        private static int AdjustRowNumberIfValid(int rowIndicatorRowNumber, int invalidRowCounts, Codeword codeword)
        {
            
            if (codeword == null)
            {
                return invalidRowCounts;
            }
            if (!codeword.HasValidRowNumber)
            {
                if (codeword.IsValidRowNumber(rowIndicatorRowNumber))
                {
                    codeword.RowNumber = rowIndicatorRowNumber;
                    invalidRowCounts = 0;
                } else
                {
                    ++invalidRowCounts;
                }
            }
            return invalidRowCounts;
        }

        /// <summary>
        /// Adjusts the row numbers.
        /// </summary>
        /// <param name="barcodeColumn">Barcode column.</param>
        /// <param name="codewordsRow">Codewords row.</param>
        /// <param name="codewords">Codewords.</param>
        private void AdjustRowNumbers(int barcodeColumn, int codewordsRow, Codeword[] codewords)
        {
            Codeword codeword = codewords[codewordsRow];
            Codeword[] previousColumnCodewords = DetectionResultColumns[barcodeColumn - 1].Codewords;
            Codeword[] nextColumnCodewords = previousColumnCodewords;
            if (DetectionResultColumns[barcodeColumn + 1] != null)
            {
                nextColumnCodewords = DetectionResultColumns[barcodeColumn + 1].Codewords;
            }
            
            Codeword[] otherCodewords = new Codeword[14];
            
            otherCodewords[2] = previousColumnCodewords[codewordsRow];
            otherCodewords[3] = nextColumnCodewords[codewordsRow];
            
            if (codewordsRow > 0)
            {
                otherCodewords[0] = codewords[codewordsRow - 1];
                otherCodewords[4] = previousColumnCodewords[codewordsRow - 1];
                otherCodewords[5] = nextColumnCodewords[codewordsRow - 1];
            }
            if (codewordsRow > 1)
            {
                otherCodewords[8] = codewords[codewordsRow - 2];
                otherCodewords[10] = previousColumnCodewords[codewordsRow - 2];
                otherCodewords[11] = nextColumnCodewords[codewordsRow - 2];
            }
            if (codewordsRow < codewords.Length - 1)
            {
                otherCodewords[1] = codewords[codewordsRow + 1];
                otherCodewords[6] = previousColumnCodewords[codewordsRow + 1];
                otherCodewords[7] = nextColumnCodewords[codewordsRow + 1];
            }
            if (codewordsRow < codewords.Length - 2)
            {
                otherCodewords[9] = codewords[codewordsRow + 2];
                otherCodewords[12] = previousColumnCodewords[codewordsRow + 2];
                otherCodewords[13] = nextColumnCodewords[codewordsRow + 2];
            }
            foreach (Codeword otherCodeword in otherCodewords)
            {
                if (AdjustRowNumber(codeword, otherCodeword))
                {
                    return;
                }
            }
        }
        /// <summary>
        /// Adjusts the row number.
        /// </summary>
        /// <returns><c>true</c>, if row number was adjusted, <c>false</c> otherwise.</returns>
        /// <param name="codeword">Codeword.</param>
        /// <param name="otherCodeword">Other codeword.</param>
        private static bool AdjustRowNumber(Codeword codeword, Codeword otherCodeword)
        {
            if (otherCodeword == null)
            {
                return false;
            }
            if (otherCodeword.HasValidRowNumber && otherCodeword.Bucket == codeword.Bucket)
            {
                codeword.RowNumber = otherCodeword.RowNumber;
                return true;
            }
            return false;
        }

        /// <summary>
        /// Returns a <see cref="System.String"/> that represents the current <see cref="ZXing.PDF417.Internal.DetectionResult"/>.
        /// </summary>
        /// <returns>A <see cref="System.String"/> that represents the current <see cref="ZXing.PDF417.Internal.DetectionResult"/>.</returns>
        public override string ToString()
        {
            StringBuilder formatter = new StringBuilder();
            DetectionResultColumn rowIndicatorColumn = DetectionResultColumns[0];
            if (rowIndicatorColumn == null)
            {
                rowIndicatorColumn = DetectionResultColumns[ColumnCount + 1];
            }
            for (int codewordsRow = 0; codewordsRow < rowIndicatorColumn.Codewords.Length; codewordsRow++)
            {
                formatter.AppendFormat("CW {0,3}:", codewordsRow);
                for (int barcodeColumn = 0; barcodeColumn < ColumnCount + 2; barcodeColumn++)
                {
                    if (DetectionResultColumns[barcodeColumn] == null)
                    {
                        formatter.Append("    |   ");
                        continue;
                    }
                    Codeword codeword = DetectionResultColumns[barcodeColumn].Codewords[codewordsRow];
                    if (codeword == null)
                    {
                        formatter.Append("    |   ");
                        continue;
                    }
                    formatter.AppendFormat(" {0,3}|{1,3}", codeword.RowNumber, codeword.Value);
                }
                formatter.Append("\n");
            }

            return formatter.ToString();
        }

    }

}

