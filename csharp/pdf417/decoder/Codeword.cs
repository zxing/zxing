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

namespace ZXing.PDF417.Internal
{
    /// <summary>
    /// A Codeword in the PDF417 barcode
    /// </summary>
    /// <author>Guenther Grau (Java Core)</author>
    /// <author>Stephen Furlani (C# Port)</author>
    public sealed class Codeword
    {
        /// <summary>
        /// Default value for the RowNumber (-1 being an invalid real number)
        /// </summary>
        private static readonly int BARCODE_ROW_UNKNOWN = -1;

        public int StartX { get; private set; }
        public int EndX { get; private set; }
        public int Bucket { get; private set; }
        public int Value { get; private set; }
        public int RowNumber { get; set; }

        /// <summary>
        /// Initializes a new instance of the <see cref="ZXing.PDF417.Internal.Codeword"/> class.
        /// </summary>
        /// <param name="startX">Start x.</param>
        /// <param name="endX">End x.</param>
        /// <param name="bucket">Bucket.</param>
        /// <param name="value">Value.</param>
        public Codeword(int startX, int endX, int bucket, int value)
        {
            this.StartX = startX;
            this.EndX = endX;
            this.Bucket = bucket;
            this.Value = value;
            this.RowNumber = BARCODE_ROW_UNKNOWN;
        }

        /// <summary>
        /// Gets the width.
        /// </summary>
        /// <value>The width.</value>
        public int Width
        {
            get
            {
                return EndX - StartX;
            }
        }

        /// <summary>
        /// Gets a value indicating whether this instance has valid row number.
        /// </summary>
        /// <value><c>true</c> if this instance has valid row number; otherwise, <c>false</c>.</value>
        public bool HasValidRowNumber
        {
            get
            {
                return IsValidRowNumber(RowNumber);
            }
        }

        /// <summary>
        /// Determines whether this instance is valid row number the specified rowNumber.
        /// </summary>
        /// <returns><c>true</c> if this instance is valid row number the specified rowNumber; otherwise, <c>false</c>.</returns>
        /// <param name="rowNumber">Row number.</param>
        public bool IsValidRowNumber(int rowNumber)
        {
            return rowNumber != BARCODE_ROW_UNKNOWN && Bucket == (rowNumber % 3) * 3;
        }

        /// <summary>
        /// Sets the row number as the row's indicator column.
        /// </summary>
        public void SetRowNumberAsRowIndicatorColumn()
        {
            this.RowNumber = (Value / 30) * 3 + Bucket / 3;
        }

        /// <summary>
        /// Returns a <see cref="System.String"/> that represents the current <see cref="ZXing.PDF417.Internal.Codeword"/>.
        /// </summary>
        /// <returns>A <see cref="System.String"/> that represents the current <see cref="ZXing.PDF417.Internal.Codeword"/>.</returns>
        public override string ToString()
        {
            return RowNumber + "|" + Value;
        }
    }
}

