/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * This object renders a CODE128 code as a {@link BitMatrix}.
 *
 * @author erik.barbara@gmail.com (Erik Barbara)
 */
public final class Code128Writer extends OneDimensionalCodeWriter {

  private static final int CODE_START_A = 103;
  private static final int CODE_START_B = 104;
  private static final int CODE_START_C = 105;
  private static final int CODE_CODE_A = 101;
  private static final int CODE_CODE_B = 100;
  private static final int CODE_CODE_C = 99;
  private static final int CODE_STOP = 106;

  // Dummy characters used to specify control characters in input
  private static final char ESCAPE_FNC_1 = '\u00f1';
  private static final char ESCAPE_FNC_2 = '\u00f2';
  private static final char ESCAPE_FNC_3 = '\u00f3';
  private static final char ESCAPE_FNC_4 = '\u00f4';

  private static final int CODE_FNC_1 = 102;   // Code A, Code B, Code C
  private static final int CODE_FNC_2 = 97;    // Code A, Code B
  private static final int CODE_FNC_3 = 96;    // Code A, Code B
  private static final int CODE_FNC_4_A = 101; // Code A
  private static final int CODE_FNC_4_B = 100; // Code B

  // Results of minimal lookahead for code C
  private enum CType {
    UNCODABLE,
    ONE_DIGIT,
    TWO_DIGITS,
    FNC_1
  }

  @Override
  protected Collection<BarcodeFormat> getSupportedWriteFormats() {
    return Collections.singleton(BarcodeFormat.CODE_128);
  }

  @Override
  public boolean[] encode(String contents) {
    return encode(contents, null);
  }

  @Override
  public boolean[] encode(String contents, Map<EncodeHintType,?> hints) {

    int forcedCodeSet = check(contents, hints);

    boolean hasCompactionHint = hints != null && hints.containsKey(EncodeHintType.CODE128_COMPACT) &&
        Boolean.parseBoolean(hints.get(EncodeHintType.CODE128_COMPACT).toString());

    return hasCompactionHint ? new MinimalEncoder().encode(contents) : encodeFast(contents, forcedCodeSet);
  }

  private static int check(String contents, Map<EncodeHintType,?> hints) {
    // Check for forced code set hint.
    int forcedCodeSet = -1;
    if (hints != null && hints.containsKey(EncodeHintType.FORCE_CODE_SET)) {
      String codeSetHint = hints.get(EncodeHintType.FORCE_CODE_SET).toString();
      switch (codeSetHint) {
        case "A":
          forcedCodeSet = CODE_CODE_A;
          break;
        case "B":
          forcedCodeSet = CODE_CODE_B;
          break;
        case "C":
          forcedCodeSet = CODE_CODE_C;
          break;
        default:
          throw new IllegalArgumentException("Unsupported code set hint: " + codeSetHint);
      }
    }

    // Check content
    int length = contents.length();
    for (int i = 0; i < length; i++) {
      char c = contents.charAt(i);
      // check for non ascii characters that are not special GS1 characters
      switch (c) {
        // special function characters
        case ESCAPE_FNC_1:
        case ESCAPE_FNC_2:
        case ESCAPE_FNC_3:
        case ESCAPE_FNC_4:
          break;
        // non ascii characters
        default:
          if (c > 127) {
            // no full Latin-1 character set available at the moment
            // shift and manual code change are not supported
            throw new IllegalArgumentException("Bad character in input: ASCII value=" + (int) c);
          }
      }
      // check characters for compatibility with forced code set
      switch (forcedCodeSet) {
        case CODE_CODE_A:
          // allows no ascii above 95 (no lower caps, no special symbols)
          if (c > 95 && c <= 127) {
            throw new IllegalArgumentException("Bad character in input for forced code set A: ASCII value=" + (int) c);
          }
          break;
        case CODE_CODE_B:
          // allows no ascii below 32 (terminal symbols)
          if (c < 32) {
            throw new IllegalArgumentException("Bad character in input for forced code set B: ASCII value=" + (int) c);
          }
          break;
        case CODE_CODE_C:
          // allows only numbers and no FNC 2/3/4
          if (c < 48 || (c > 57 && c <= 127) || c == ESCAPE_FNC_2 || c == ESCAPE_FNC_3 || c == ESCAPE_FNC_4) {
            throw new IllegalArgumentException("Bad character in input for forced code set C: ASCII value=" + (int) c);
          }
          break;
      }
    }
    return forcedCodeSet;
  }

  private static boolean[] encodeFast(String contents, int forcedCodeSet) {
    int length = contents.length();

    Collection<int[]> patterns = new ArrayList<>(); // temporary storage for patterns
    int checkSum = 0;
    int checkWeight = 1;
    int codeSet = 0; // selected code (CODE_CODE_B or CODE_CODE_C)
    int position = 0; // position in contents

    while (position < length) {
      //Select code to use
      int newCodeSet;
      if (forcedCodeSet == -1) {
        newCodeSet = chooseCode(contents, position, codeSet);
      } else {
        newCodeSet = forcedCodeSet;
      }

      //Get the pattern index
      int patternIndex;
      if (newCodeSet == codeSet) {
        // Encode the current character
        // First handle escapes
        switch (contents.charAt(position)) {
          case ESCAPE_FNC_1:
            patternIndex = CODE_FNC_1;
            break;
          case ESCAPE_FNC_2:
            patternIndex = CODE_FNC_2;
            break;
          case ESCAPE_FNC_3:
            patternIndex = CODE_FNC_3;
            break;
          case ESCAPE_FNC_4:
            if (codeSet == CODE_CODE_A) {
              patternIndex = CODE_FNC_4_A;
            } else {
              patternIndex = CODE_FNC_4_B;
            }
            break;
          default:
            // Then handle normal characters otherwise
            switch (codeSet) {
              case CODE_CODE_A:
                patternIndex = contents.charAt(position) - ' ';
                if (patternIndex < 0) {
                  // everything below a space character comes behind the underscore in the code patterns table
                  patternIndex += '`';
                }
                break;
              case CODE_CODE_B:
                patternIndex = contents.charAt(position) - ' ';
                break;
              default:
                // CODE_CODE_C
                if (position + 1 == length) {
                  // this is the last character, but the encoding is C, which always encodes two characers
                  throw new IllegalArgumentException("Bad number of characters for digit only encoding.");
                }
                patternIndex = Integer.parseInt(contents.substring(position, position + 2));
                position++; // Also incremented below
                break;
            }
        }
        position++;
      } else {
        // Should we change the current code?
        // Do we have a code set?
        if (codeSet == 0) {
          // No, we don't have a code set
          switch (newCodeSet) {
            case CODE_CODE_A:
              patternIndex = CODE_START_A;
              break;
            case CODE_CODE_B:
              patternIndex = CODE_START_B;
              break;
            default:
              patternIndex = CODE_START_C;
              break;
          }
        } else {
          // Yes, we have a code set
          patternIndex = newCodeSet;
        }
        codeSet = newCodeSet;
      }

      // Get the pattern
      patterns.add(Code128Reader.CODE_PATTERNS[patternIndex]);

      // Compute checksum
      checkSum += patternIndex * checkWeight;
      if (position != 0) {
        checkWeight++;
      }
    }
    return produceResult(patterns, checkSum);
  }

  static boolean[] produceResult(Collection<int[]> patterns, int checkSum) {
    // Compute and append checksum
    checkSum %= 103;
    if (checkSum < 0) {
      throw new IllegalArgumentException("Unable to compute a valid input checksum");
    }
    patterns.add(Code128Reader.CODE_PATTERNS[checkSum]);

    // Append stop code
    patterns.add(Code128Reader.CODE_PATTERNS[CODE_STOP]);

    // Compute code width
    int codeWidth = 0;
    for (int[] pattern : patterns) {
      for (int width : pattern) {
        codeWidth += width;
      }
    }

    // Compute result
    boolean[] result = new boolean[codeWidth];
    int pos = 0;
    for (int[] pattern : patterns) {
      pos += appendPattern(result, pos, pattern, true);
    }

    return result;
  }

  private static CType findCType(CharSequence value, int start) {
    int last = value.length();
    if (start >= last) {
      return CType.UNCODABLE;
    }
    char c = value.charAt(start);
    if (c == ESCAPE_FNC_1) {
      return CType.FNC_1;
    }
    if (c < '0' || c > '9') {
      return CType.UNCODABLE;
    }
    if (start + 1 >= last) {
      return CType.ONE_DIGIT;
    }
    c = value.charAt(start + 1);
    if (c < '0' || c > '9') {
      return CType.ONE_DIGIT;
    }
    return CType.TWO_DIGITS;
  }

  private static int chooseCode(CharSequence value, int start, int oldCode) {
    CType lookahead = findCType(value, start);
    if (lookahead == CType.ONE_DIGIT) {
      if (oldCode == CODE_CODE_A) {
        return CODE_CODE_A;
      }
      return CODE_CODE_B;
    }
    if (lookahead == CType.UNCODABLE) {
      if (start < value.length()) {
        char c = value.charAt(start);
        if (c < ' ' || (oldCode == CODE_CODE_A && (c < '`' || (c >= ESCAPE_FNC_1 && c <= ESCAPE_FNC_4)))) {
          // can continue in code A, encodes ASCII 0 to 95 or FNC1 to FNC4
          return CODE_CODE_A;
        }
      }
      return CODE_CODE_B; // no choice
    }
    if (oldCode == CODE_CODE_A && lookahead == CType.FNC_1) {
      return CODE_CODE_A;
    }
    if (oldCode == CODE_CODE_C) { // can continue in code C
      return CODE_CODE_C;
    }
    if (oldCode == CODE_CODE_B) {
      if (lookahead == CType.FNC_1) {
        return CODE_CODE_B; // can continue in code B
      }
      // Seen two consecutive digits, see what follows
      lookahead = findCType(value, start + 2);
      if (lookahead == CType.UNCODABLE || lookahead == CType.ONE_DIGIT) {
        return CODE_CODE_B; // not worth switching now
      }
      if (lookahead == CType.FNC_1) { // two digits, then FNC_1...
        lookahead = findCType(value, start + 3);
        if (lookahead == CType.TWO_DIGITS) { // then two more digits, switch
          return CODE_CODE_C;
        } else {
          return CODE_CODE_B; // otherwise not worth switching
        }
      }
      // At this point, there are at least 4 consecutive digits.
      // Look ahead to choose whether to switch now or on the next round.
      int index = start + 4;
      while ((lookahead = findCType(value, index)) == CType.TWO_DIGITS) {
        index += 2;
      }
      if (lookahead == CType.ONE_DIGIT) { // odd number of digits, switch later
        return CODE_CODE_B;
      }
      return CODE_CODE_C; // even number of digits, switch now
    }
    // Here oldCode == 0, which means we are choosing the initial code
    if (lookahead == CType.FNC_1) { // ignore FNC_1
      lookahead = findCType(value, start + 1);
    }
    if (lookahead == CType.TWO_DIGITS) { // at least two digits, start in code C
      return CODE_CODE_C;
    }
    return CODE_CODE_B;
  }

  /**
   * Encodes minimally using Divide-And-Conquer with Memoization
   **/
  private static final class MinimalEncoder {

    private enum Charset { A, B, C, NONE }
    private enum Latch { A, B, C, SHIFT, NONE }

    static final String A = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_\u0000\u0001\u0002" +
                            "\u0003\u0004\u0005\u0006\u0007\u0008\u0009\n\u000B\u000C\r\u000E\u000F\u0010\u0011" +
                            "\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F" +
                            "\u00FF";
    static final String B = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqr" +
                            "stuvwxyz{|}~\u007F\u00FF";

    private static final int CODE_SHIFT = 98;

    private int[][] memoizedCost;
    private Latch[][] minPath;

    private boolean[] encode(String contents) {
      memoizedCost = new int[4][contents.length()];
      minPath = new Latch[4][contents.length()];

      encode(contents, Charset.NONE, 0);

      Collection<int[]> patterns = new ArrayList<>();
      int[] checkSum = new int[] {0};
      int[] checkWeight = new int[] {1};
      int length = contents.length();
      Charset charset = Charset.NONE;
      for (int i = 0; i < length; i++) {
        Latch latch = minPath[charset.ordinal()][i];
        switch (latch) {
          case A:
            charset = Charset.A;
            addPattern(patterns, i == 0 ? CODE_START_A : CODE_CODE_A, checkSum, checkWeight, i);
            break;
          case B:
            charset = Charset.B;
            addPattern(patterns, i == 0 ? CODE_START_B : CODE_CODE_B, checkSum, checkWeight, i);
            break;
          case C:
            charset = Charset.C;
            addPattern(patterns, i == 0 ? CODE_START_C : CODE_CODE_C, checkSum, checkWeight, i);
            break;
          case SHIFT:
            addPattern(patterns, CODE_SHIFT, checkSum, checkWeight, i);
            break;
        }
        if (charset == Charset.C) {
          if (contents.charAt(i) == ESCAPE_FNC_1) {
            addPattern(patterns, CODE_FNC_1, checkSum, checkWeight, i);
          } else {
            addPattern(patterns, Integer.parseInt(contents.substring(i, i + 2)), checkSum, checkWeight, i);
            assert i + 1 < length; //the algorithm never leads to a single trailing digit in character set C
            if (i + 1 < length) {
              i++;
            }
          }
        } else { // charset A or B
          int patternIndex;
          switch (contents.charAt(i)) {
            case ESCAPE_FNC_1:
              patternIndex = CODE_FNC_1;
              break;
            case ESCAPE_FNC_2:
              patternIndex = CODE_FNC_2;
              break;
            case ESCAPE_FNC_3:
              patternIndex = CODE_FNC_3;
              break;
            case ESCAPE_FNC_4:
              if (charset == Charset.A && latch != Latch.SHIFT ||
                  charset == Charset.B && latch == Latch.SHIFT) {
                patternIndex = CODE_FNC_4_A;
              } else {
                patternIndex = CODE_FNC_4_B;
              }
              break;
            default:
              patternIndex = contents.charAt(i) - ' ';
          }
          if ((charset == Charset.A && latch != Latch.SHIFT ||
               charset == Charset.B && latch == Latch.SHIFT) &&
               patternIndex < 0) {
            patternIndex += '`';
          }
          addPattern(patterns, patternIndex, checkSum, checkWeight, i);
        }
      }
      memoizedCost = null;
      minPath = null;
      return produceResult(patterns, checkSum[0]);
    }

    private static void addPattern(Collection<int[]> patterns,
                                  int patternIndex,
                                  int[] checkSum,
                                  int[] checkWeight,
                                  int position) {
      patterns.add(Code128Reader.CODE_PATTERNS[patternIndex]);
      if (position != 0) {
        checkWeight[0]++;
      }
      checkSum[0] += patternIndex * checkWeight[0];
    }

    private static boolean isDigit(char c) {
      return c >= '0' && c <= '9';
    }

    private boolean canEncode(CharSequence contents, Charset charset,int position) {
      char c = contents.charAt(position);
      switch (charset) {
        case A: return c == ESCAPE_FNC_1 ||
                       c == ESCAPE_FNC_2 ||
                       c == ESCAPE_FNC_3 ||
                       c == ESCAPE_FNC_4 ||
                       A.indexOf(c) >= 0;
        case B: return c == ESCAPE_FNC_1 ||
                       c == ESCAPE_FNC_2 ||
                       c == ESCAPE_FNC_3 ||
                       c == ESCAPE_FNC_4 ||
                       B.indexOf(c) >= 0;
        case C: return c == ESCAPE_FNC_1 ||
                       (position + 1 < contents.length() &&
                        isDigit(c) &&
                        isDigit(contents.charAt(position + 1)));
        default: return false;
      }
    }

    /**
     * Encode the string starting at position position starting with the character set charset
     **/
    private int encode(CharSequence contents, Charset charset, int position) {
      assert position < contents.length();
      int mCost = memoizedCost[charset.ordinal()][position];
      if (mCost > 0) {
        return mCost;
      }

      int minCost = Integer.MAX_VALUE;
      Latch minLatch = Latch.NONE;
      boolean atEnd = position + 1 >= contents.length();

      Charset[] sets = new Charset[] { Charset.A, Charset.B };
      for (int i = 0; i <= 1; i++) {
        if (canEncode(contents, sets[i], position)) {
          int cost =  1;
          Latch latch = Latch.NONE;
          if (charset != sets[i]) {
            cost++;
            latch = Latch.valueOf(sets[i].toString());
          }
          if (!atEnd) {
            cost += encode(contents, sets[i], position + 1);
          }
          if (cost < minCost) {
            minCost = cost;
            minLatch = latch;
          }
          cost = 1;
          if (charset == sets[(i + 1) % 2]) {
            cost++;
            latch = Latch.SHIFT;
            if (!atEnd) {
              cost += encode(contents, charset, position + 1);
            }
            if (cost < minCost) {
              minCost = cost;
              minLatch = latch;
            }
          }
        }
      }
      if (canEncode(contents, Charset.C, position)) {
        int cost = 1;
        Latch latch = Latch.NONE;
        if (charset != Charset.C) {
          cost++;
          latch = Latch.C;
        }
        int advance = contents.charAt(position) == ESCAPE_FNC_1 ? 1 : 2;
        if (position + advance < contents.length()) {
          cost += encode(contents, Charset.C, position + advance);
        }
        if (cost < minCost) {
          minCost = cost;
          minLatch = latch;
        }
      }
      if (minCost == Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Bad character in input: ASCII value=" + (int) contents.charAt(position));
      }
      memoizedCost[charset.ordinal()][position] = minCost;
      minPath[charset.ordinal()][position] = minLatch;
      return minCost;
    }
  }
}
