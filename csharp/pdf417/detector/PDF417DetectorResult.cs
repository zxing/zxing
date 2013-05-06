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
using System.Collections.Generic;

namespace ZXing.PDF417.Internal
{
    /// <summary>
    /// PDF 417 Detector Result class.  Skipped private backing stores.
    /// <author>Guenther Grau (Java Core)</author> 
    /// <author>Stephen Furlani (C# Port)</author> 
    /// </summary>
    public sealed class PDF417DetectorResult
    {
        public BitMatrix Bits { get; private set; }
        public List<ResultPoint[]> Points { get; private set; }

        /// <summary>
        /// Initializes a new instance of the <see cref="ZXing.PDF417.Internal.PDF417DetectorResult"/> class.
        /// </summary>
        /// <param name="bits">Bits.</param>
        /// <param name="points">Points.</param>
        public PDF417DetectorResult(BitMatrix bits, List<ResultPoint[]> points)
        {
            this.Bits = bits;
            this.Points = points;
        }
    }
}

