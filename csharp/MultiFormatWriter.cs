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
using com.google.zxing.qrcode;
using com.google.zxing.common;

namespace com.google.zxing
{
    public sealed class MultiFormatWriter : Writer
    { 
        public ByteMatrix encode(String contents, BarcodeFormat format, int width,int height) {
            return encode(contents, format, width, height,null);
        }

        public ByteMatrix encode(String contents, BarcodeFormat format, int width, int height,Hashtable hints){
            if (format == BarcodeFormat.QR_CODE) {
              return new QRCodeWriter().encode(contents, format, width, height, hints);
            } else {
              throw new ArgumentException("No encoder available for format " + format);
            }
        }   
    }
}



