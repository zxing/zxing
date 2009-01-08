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
namespace com.google.zxing
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
    public sealed class DecodeHintType
    { 
          // No, we can't use an enum here. J2ME doesn't support it.
          /**
           * Unspecified, application-specific hint. Maps to an unspecified {@link Object}.
           */
          public static DecodeHintType OTHER = new DecodeHintType();

          /**
           * Image is a pure monochrome image of a barcode. Doesn't matter what it maps to;
           * use {@link Boolean#TRUE}.
           */
          public static DecodeHintType PURE_BARCODE = new DecodeHintType();

          /**
           * Image is known to be of one of a few possible formats.
           * Maps to a {@link java.util.Vector} of {@link BarcodeFormat}s.
           */
          public static DecodeHintType POSSIBLE_FORMATS = new DecodeHintType();

          /**
           * Spend more time to try to find a barcode; optimize for accuracy, not speed.
           * Doesn't matter what it maps to; use {@link Boolean#TRUE}.
           */
          public static DecodeHintType TRY_HARDER = new DecodeHintType();

          private DecodeHintType() {
          }
    
    }
}