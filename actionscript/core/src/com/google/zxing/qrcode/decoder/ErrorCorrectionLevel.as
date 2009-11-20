/*
 * Copyright 2007 ZXing authors
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
package com.google.zxing.qrcode.decoder
{
	public class ErrorCorrectionLevel
    { 
    	  import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    	
          // No, we can't use an enum here. J2ME doesn't support it.
          /**
           * L = ~7% correction
           */
          public static var  L:ErrorCorrectionLevel = new ErrorCorrectionLevel(0, 0x01, "L");
          /**
           * M = ~15% correction
           */
          public static var M:ErrorCorrectionLevel = new ErrorCorrectionLevel(1, 0x00, "M");
          /**
           * Q = ~25% correction
           */
          public static var Q:ErrorCorrectionLevel = new ErrorCorrectionLevel(2, 0x03, "Q");
          /**
           * H = ~30% correction
           */
          public static  var H:ErrorCorrectionLevel = new ErrorCorrectionLevel(3, 0x02, "H");

          private static  var FOR_BITS:Array = [M, L, H, Q];

          private var Ordinal:int;
          private var bits:int;
          private var name:String;

          public function ErrorCorrectionLevel(ordinal:int, bits:int, name:String) {
            this.Ordinal = ordinal;
            this.bits = bits;
            this.name = name;
          }

          public function ordinal():int {
              return Ordinal;
          }

          public function getBits():int {
            return bits;
          }

          public function getName():String {
            return name;
          }

          public function toString():String {
            return name;
          }

          /**
           * @param bits int containing the two bits encoding a QR Code's error correction level
           * @return {@link ErrorCorrectionLevel} representing the encoded error correction level
           */
          public static function forBits(bits:int):ErrorCorrectionLevel {
            if (bits < 0 || bits >= FOR_BITS.length) {
              throw new IllegalArgumentException("ErrorCorrectionLevel : forBits : bits out of range (0 - " +FOR_BITS.length+ "):"+bits);
            }
            return FOR_BITS[bits];
          }
    }

}