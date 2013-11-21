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

final class EdifactEncoder implements Encoder {

  @Override
  public int getEncodingMode() {
    return HighLevelEncoder.EDIFACT_ENCODATION;
  }

  @Override
  public void encode(EncoderContext context) {
    //step F
    StringBuilder buffer = new StringBuilder();
    while (context.hasMoreCharacters()) {
      char c = context.getCurrentChar();
      encodeChar(c, buffer);
      context.pos++;

      int count = buffer.length();
      if (count >= 4) {
        context.writeCodewords(encodeToCodewords(buffer, 0));
        buffer.delete(0, 4);

        int newMode = HighLevelEncoder.lookAheadTest(context.getMessage(), context.pos, getEncodingMode());
        if (newMode != getEncodingMode()) {
          context.signalEncoderChange(HighLevelEncoder.ASCII_ENCODATION);
          break;
        }
      }
    }
    buffer.append((char) 31); //Unlatch
    handleEOD(context, buffer);
  }

  /**
   * Handle "end of data" situations
   *
   * @param context the encoder context
   * @param buffer  the buffer with the remaining encoded characters
   */
  private static void handleEOD(EncoderContext context, CharSequence buffer) {
    try {
      int count = buffer.length();
      if (count == 0) {
        return; //Already finished
      }
      if (count == 1) {
        //Only an unlatch at the end
        context.updateSymbolInfo();
        int available = context.getSymbolInfo().getDataCapacity() - context.getCodewordCount();
        int remaining = context.getRemainingCharacters();
        if (remaining == 0 && available <= 2) {
          return; //No unlatch
        }
      }

      if (count > 4) {
        throw new IllegalStateException("Count must not exceed 4");
      }
      int restChars = count - 1;
      String encoded = encodeToCodewords(buffer, 0);
      boolean endOfSymbolReached = !context.hasMoreCharacters();
      boolean restInAscii = endOfSymbolReached && restChars <= 2;

      if (restChars <= 2) {
        context.updateSymbolInfo(context.getCodewordCount() + restChars);
        int available = context.getSymbolInfo().getDataCapacity() - context.getCodewordCount();
        if (available >= 3) {
          restInAscii = false;
          context.updateSymbolInfo(context.getCodewordCount() + encoded.length());
          //available = context.symbolInfo.dataCapacity - context.getCodewordCount();
        }
      }

      if (restInAscii) {
        context.resetSymbolInfo();
        context.pos -= restChars;
      } else {
        context.writeCodewords(encoded);
      }
    } finally {
      context.signalEncoderChange(HighLevelEncoder.ASCII_ENCODATION);
    }
  }

  private static void encodeChar(char c, StringBuilder sb) {
    if (c >= ' ' && c <= '?') {
      sb.append(c);
    } else if (c >= '@' && c <= '^') {
      sb.append((char) (c - 64));
    } else {
      HighLevelEncoder.illegalCharacter(c);
    }
  }

  private static String encodeToCodewords(CharSequence sb, int startPos) {
    int len = sb.length() - startPos;
    if (len == 0) {
      throw new IllegalStateException("StringBuilder must not be empty");
    }
    char c1 = sb.charAt(startPos);
    char c2 = len >= 2 ? sb.charAt(startPos + 1) : 0;
    char c3 = len >= 3 ? sb.charAt(startPos + 2) : 0;
    char c4 = len >= 4 ? sb.charAt(startPos + 3) : 0;

    int v = (c1 << 18) + (c2 << 12) + (c3 << 6) + c4;
    char cw1 = (char) ((v >> 16) & 255);
    char cw2 = (char) ((v >> 8) & 255);
    char cw3 = (char) (v & 255);
    StringBuilder res = new StringBuilder(3);
    res.append(cw1);
    if (len >= 2) {
      res.append(cw2);
    }
    if (len >= 3) {
      res.append(cw3);
    }
    return res.toString();
  }

}
