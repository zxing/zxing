/*
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

namespace com.google.zxing
{

    /// <summary> The general exception class throw when something goes wrong during decoding of a barcode.
    /// This includes, but is not limited to, failing checksums / error correction algorithms, being
    /// unable to locate finder timing patterns, and so on.
    /// 
    /// </summary>
    /// <author>  srowen@google.com (Sean Owen)
    /// </author>
    //[Serializable]
    public sealed class ResultMetadataType
    {
          // No, we can't use an enum here. J2ME doesn't support it.

          /**
           * Unspecified, application-specific metadata. Maps to an unspecified {@link Object}.
           */
          public static ResultMetadataType OTHER = new ResultMetadataType();

          /**
           * Denotes the likely approximate orientation of the barcode in the image. This value
           * is given as degrees rotated clockwise from the normal, upright orientation.
           * For example a 1D barcode which was found by reading top-to-bottom would be
           * said to have orientation "90". This key maps to an {@link Integer} whose
           * value is in the range [0,360).
           */
          public static ResultMetadataType ORIENTATION = new ResultMetadataType();

          /**
           * <p>2D barcode formats typically encode text, but allow for a sort of 'byte mode'
           * which is sometimes used to encode binary data. While {@link Result} makes available
           * the complete raw bytes in the barcode for these formats, it does not offer the bytes
           * from the byte segments alone.</p>
           *
           * <p>This maps to a {@link java.util.Vector} of byte arrays corresponding to the
           * raw bytes in the byte segments in the barcode, in order.</p>
           */
          public static ResultMetadataType BYTE_SEGMENTS = new ResultMetadataType();

          private ResultMetadataType() {
          }
    
    
    }
}