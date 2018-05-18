/*
 * Copyright 2006-2007 Jeremias Maerki.
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

package com.google.zxing.datamatrix.encoder;

final class TextEncoder extends C40Encoder {

  @Override
  public int getEncodingMode() {
    return HighLevelEncoder.TEXT_ENCODATION;
  }

  @Override
  int encodeChar(char c, StringBuilder sb) {
    if (c == ' ') {
      sb.append('\3');
      return 1;
    }
    if (c >= '0' && c <= '9') {
      sb.append((char) (c - 48 + 4));
      return 1;
    }
    if (c >= 'a' && c <= 'z') {
      sb.append((char) (c - 97 + 14));
      return 1;
    }
    if (c < ' ') {
      sb.append('\0'); //Shift 1 Set
      sb.append(c);
      return 2;
    }
    if (c >= '!' && c <= '/') {
      sb.append('\1'); //Shift 2 Set
      sb.append((char) (c - 33));
      return 2;
    }
    if (c >= ':' && c <= '@') {
      sb.append('\1'); //Shift 2 Set
      sb.append((char) (c - 58 + 15));
      return 2;
    }
    if (c >= '[' && c <= '_') {
      sb.append('\1'); //Shift 2 Set
      sb.append((char) (c - 91 + 22));
      return 2;
    }
    if (c == '`') {
      sb.append('\2'); //Shift 3 Set
      sb.append((char) (c - 96));
      return 2;
    }
    if (c >= 'A' && c <= 'Z') {
      sb.append('\2'); //Shift 3 Set
      sb.append((char) (c - 65 + 1));
      return 2;
    }
    if (c >= '{' && c <= 127) {
      sb.append('\2'); //Shift 3 Set
      sb.append((char) (c - 123 + 27));
      return 2;
    }
    sb.append("\1\u001e"); //Shift 2, Upper Shift
    int len = 2;
    len += encodeChar((char) (c - 128), sb);
    return len;
  }

}
