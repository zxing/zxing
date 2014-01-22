/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.qrcode.encoder
{
      public class BlockPair 
      {
		import com.google.zxing.common.zxingByteArray;
		
        protected var dataBytes:zxingByteArray;
        protected var errorCorrectionBytes:zxingByteArray ;

        public function BlockPair(data:zxingByteArray,  errorCorrection:zxingByteArray) {
          dataBytes = data;
          errorCorrectionBytes = errorCorrection;
        }

        public function getDataBytes():zxingByteArray {
          return dataBytes;
        }

        public function getErrorCorrectionBytes():zxingByteArray {
          return errorCorrectionBytes;
        }

      }
}