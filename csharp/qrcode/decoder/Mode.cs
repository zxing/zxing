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
using System;
using com.google.zxing;
using com.google.zxing.common;

namespace com.google.zxing.qrcode.decoder
{
    public sealed class Mode
    { 
          // No, we can't use an enum here. J2ME doesn't support it.
          public static Mode TERMINATOR = new Mode(new int[]{0, 0, 0}, 0x00, "TERMINATOR"); // Not really a mode...
          public static Mode NUMERIC = new Mode(new int[]{10, 12, 14}, 0x01, "NUMERIC");
          public static Mode ALPHANUMERIC = new Mode(new int[]{9, 11, 13}, 0x02, "ALPHANUMERIC");
          public static Mode BYTE = new Mode(new int[]{8, 16, 16}, 0x04, "BYTE");
          public static Mode ECI = new Mode(null, 0x07, "ECI"); // character counts don't apply
          public static Mode KANJI = new Mode(new int[]{8, 10, 12}, 0x08, "KANJI");
          public static Mode FNC1_FIRST_POSITION = new Mode(null, 0x05, "FNC1_FIRST_POSITION");
          public static Mode FNC1_SECOND_POSITION = new Mode(null, 0x09, "FNC1_SECOND_POSITION");

          private int[] characterCountBitsForVersions;
          private int bits;
          private string name;

          private Mode(int[] characterCountBitsForVersions, int bits, string name) {
            this.characterCountBitsForVersions = characterCountBitsForVersions;
            this.bits = bits;
            this.name = name;
          }

          /**
           * @param bits four bits encoding a QR Code data mode
           * @return {@link Mode} encoded by these bits
           * @throws ArgumentException if bits do not correspond to a known mode
           */
          public static Mode forBits(int bits) {
            switch (bits) {
              case 0x0:
                return TERMINATOR;
              case 0x1:
                return NUMERIC;
              case 0x2:
                return ALPHANUMERIC;
              case 0x4:
                return BYTE;
              case 0x5:
                return FNC1_FIRST_POSITION;
              case 0x7:
                return ECI;
              case 0x8:
                return KANJI;
              case 0x9:
                return FNC1_SECOND_POSITION;
              default:
                throw new ArgumentException();
            }
          }

          /**
           * @param version version in question
           * @return number of bits used, in this QR Code symbol {@link Version}, to encode the
           *         count of characters that will follow encoded in this {@link Mode}
           */
          public int getCharacterCountBits(Version version) {
            if (characterCountBitsForVersions == null) {
              throw new ArgumentException("Character count doesn't apply to this mode");
            }
            int number = version.getVersionNumber();
            int offset;
            if (number <= 9) {
              offset = 0;
            } else if (number <= 26) {
              offset = 1;
            } else {
              offset = 2;
            }
            return characterCountBitsForVersions[offset];
          }

          public int getBits() {
            return bits;
          }

          public string getName() {
            return name;
          }

          public string toString() {
            return name;
          }

    
    
    }
}