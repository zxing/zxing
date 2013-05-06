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

namespace ZXing.PDF417
{
    /// <summary>
    /// PDF 417 result meta data.  Skipped private backing stores.
    /// <author>Guenther Grau (Java Core)</author> 
    /// <author>Stephen Furlani (C# Port)</author> 
    /// </summary>
    public sealed class PDF417ResultMetadata
    {
        public int SegmentIndex { get; set; }
        public string FileId { get; set; }
        public int[] OptionalData { get; set; }
        public bool IsLastSegment { get; set; }
    }
}

