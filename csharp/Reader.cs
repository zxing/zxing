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
    public interface Reader
    { 
        /**
       * Locates and decodes a barcode in some format within an image.
       *
       * @param image image of barcode to decode
       * @return String which the barcode encodes
       * @throws ReaderException if the barcode cannot be located or decoded for any reason
       */
      Result decode(MonochromeBitmapSource image);

      /**
       * Locates and decodes a barcode in some format within an image. This method also accepts
       * hints, each possibly associated to some data, which may help the implementation decode.
       *
       * @param image image of barcode to decode
       * @param hints passed as a {@link Hashtable} from {@link DecodeHintType} to aribtrary data. The
       * meaning of the data depends upon the hint type. The implementation may or may not do
       * anything with these hints.
       * @return String which the barcode encodes
       * @throws ReaderException if the barcode cannot be located or decoded for any reason
       */
      Result decode(MonochromeBitmapSource image, Hashtable hints);        
    }

}