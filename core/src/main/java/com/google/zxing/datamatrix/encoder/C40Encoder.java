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

class C40Encoder implements Encoder {

  @Override
  public int getEncodingMode() {
    return HighLevelEncoder.C40_ENCODATION;
  }

  void encodeMaximal(EncoderContext context) {
    StringBuilder buffer = new StringBuilder();
    int lastCharSize = 0;
    int backtrackStartPosition = context.pos;
    int backtrackBufferLength = 0;
    while (context.hasMoreCharacters()) {
      char c = context.getCurrentChar();
      context.pos++;
      lastCharSize = encodeChar(c, buffer);
      if (buffer.length() % 3 == 0) {
        backtrackStartPosition = context.pos;
        backtrackBufferLength = buffer.length();
      }
    }
    if (backtrackBufferLength != buffer.length()) {
      int unwritten = (buffer.length() / 3) * 2;

      int curCodewordCount = context.getCodewordCount() + unwritten + 1; // +1 for the latch to C40
      context.updateSymbolInfo(curCodewordCount);
      int available = context.getSymbolInfo().getDataCapacity() - curCodewordCount;
      int rest = buffer.length() % 3;
      if ((rest == 2 && available != 2) ||
          (rest == 1 && (lastCharSize > 3 || available != 1))) {
        buffer.setLength(backtrackBufferLength);
        context.pos = backtrackStartPosition;
      }
    }
    if (buffer.length() > 0) {
      context.writeCodeword(HighLevelEncoder.LATCH_TO_C40);
    }
  
    handleEOD(context, buffer);
  }

  @Override
  public void encode(EncoderContext context) {
    //step C
    StringBuilder buffer = new StringBuilder();
    while (context.hasMoreCharacters()) {
      char c = context.getCurrentChar();
      context.pos++;

      int lastCharSize = encodeChar(c, buffer);

      int unwritten = (buffer.length() / 3) * 2;

      int curCodewordCount = context.getCodewordCount() + unwritten;
      context.updateSymbolInfo(curCodewordCount);
      int available = context.getSymbolInfo().getDataCapacity() - curCodewordCount;

      if (!context.hasMoreCharacters()) {
        //Avoid having a single C40 value in the last triplet
        StringBuilder removed = new StringBuilder();
        if ((buffer.length() % 3) == 2 && available != 2) {
          lastCharSize = backtrackOneCharacter(context, buffer, removed, lastCharSize);
        }
        while ((buffer.length() % 3) == 1 && (lastCharSize > 3 || available != 1)) {
          lastCharSize = backtrackOneCharacter(context, buffer, removed, lastCharSize);
        }
        break;
      }

      int count = buffer.length();
      if ((count % 3) == 0) {
        int newMode = HighLevelEncoder.lookAheadTest(context.getMessage(), context.pos, getEncodingMode());
        if (newMode != getEncodingMode()) {
          // Return to ASCII encodation, which will actually handle latch to new mode
          context.signalEncoderChange(HighLevelEncoder.ASCII_ENCODATION);
          break;
        }
      }
    }
    handleEOD(context, buffer);
  }

  private int backtrackOneCharacter(EncoderContext context,
                                    StringBuilder buffer, StringBuilder removed, int lastCharSize) {
    int count = buffer.length();
    buffer.delete(count - lastCharSize, count);
    context.pos--;
    char c = context.getCurrentChar();
    lastCharSize = encodeChar(c, removed);
    context.resetSymbolInfo(); //Deal with possible reduction in symbol size
    return lastCharSize;
  }

  static void writeNextTriplet(EncoderContext context, StringBuilder buffer) {
    context.writeCodewords(encodeToCodewords(buffer));
    buffer.delete(0, 3);
  }

  /**
   * Handle "end of data" situations
   *
   * @param context the encoder context
   * @param buffer  the buffer with the remaining encoded characters
   */
  void handleEOD(EncoderContext context, StringBuilder buffer) {
    int unwritten = (buffer.length() / 3) * 2;
    int rest = buffer.length() % 3;

    int curCodewordCount = context.getCodewordCount() + unwritten;
    context.updateSymbolInfo(curCodewordCount);
    int available = context.getSymbolInfo().getDataCapacity() - curCodewordCount;

    if (rest == 2) {
      buffer.append('\0'); //Shift 1
      while (buffer.length() >= 3) {
        writeNextTriplet(context, buffer);
      }
      if (context.hasMoreCharacters()) {
        context.writeCodeword(HighLevelEncoder.C40_UNLATCH);
      }
    } else if (available == 1 && rest == 1) {
      while (buffer.length() >= 3) {
        writeNextTriplet(context, buffer);
      }
      if (context.hasMoreCharacters()) {
        context.writeCodeword(HighLevelEncoder.C40_UNLATCH);
      }
      // else no unlatch
      context.pos--;
    } else if (rest == 0) {
      while (buffer.length() >= 3) {
        writeNextTriplet(context, buffer);
      }
      if (available > 0 || context.hasMoreCharacters()) {
        context.writeCodeword(HighLevelEncoder.C40_UNLATCH);
      }
    } else {
      throw new IllegalStateException("Unexpected case. Please report!");
    }
    context.signalEncoderChange(HighLevelEncoder.ASCII_ENCODATION);
  }

  int encodeChar(char c, StringBuilder sb) {
    if (c == ' ') {
      sb.append('\3');
      return 1;
    }
    if (c >= '0' && c <= '9') {
      sb.append((char) (c - 48 + 4));
      return 1;
    }
    if (c >= 'A' && c <= 'Z') {
      sb.append((char) (c - 65 + 14));
      return 1;
    }
    if (c < ' ') {
      sb.append('\0'); //Shift 1 Set
      sb.append(c);
      return 2;
    }
    if (c <= '/') {
      sb.append('\1'); //Shift 2 Set
      sb.append((char) (c - 33));
      return 2;
    }
    if (c <= '@') {
      sb.append('\1'); //Shift 2 Set
      sb.append((char) (c - 58 + 15));
      return 2;
    }
    if (c <= '_') {
      sb.append('\1'); //Shift 2 Set
      sb.append((char) (c - 91 + 22));
      return 2;
    }
    if (c <= 127) {
      sb.append('\2'); //Shift 3 Set
      sb.append((char) (c - 96));
      return 2;
    }
    sb.append("\1\u001e"); //Shift 2, Upper Shift
    int len = 2;
    len += encodeChar((char) (c - 128), sb);
    return len;
  }

  private static String encodeToCodewords(CharSequence sb) {
    int v = (1600 * sb.charAt(0)) + (40 * sb.charAt(1)) + sb.charAt(2) + 1;
    char cw1 = (char) (v / 256);
    char cw2 = (char) (v % 256);
    return new String(new char[] {cw1, cw2});
  }

}
