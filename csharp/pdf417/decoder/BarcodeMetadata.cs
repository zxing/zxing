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
    /// Metadata about a PDF417 Barcode
    /// </summary>
    /// <author>Guenther Grau (Java Core)</author>
    /// <author>Stephen Furlani (C# Port)</author>
    public sealed class BarcodeMetadata
    {
        public int ColumnCount { get; private set; }
        public int ErrorCorrectionLevel { get; private set; }
        public int RowCountUpper { get; private set; }
        public int RowCountLower { get; private set; }
        public int RowCount { get; private set; }

        public BarcodeMetadata(int columnCount, int rowCountUpperPart, int rowCountLowerPart, int errorCorrectionLevel)
        {
            this.ColumnCount = columnCount;
            this.ErrorCorrectionLevel = errorCorrectionLevel;
            this.RowCountUpper = rowCountUpperPart;
            this.RowCountLower = rowCountLowerPart;
            this.RowCount = rowCountLowerPart + rowCountUpperPart;
        }
    }
}

