/*
 * Copyright 2006 Jeremias Maerki in part, and ZXing Authors in part
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file has been modified from its original form in Barcode4J.
 */

package com.google.zxing.pdf417.encoder;

import com.google.zxing.WriterException;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.common.ECIInput;
import com.google.zxing.common.MinimalECIInput;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * PDF417 high-level encoder following the algorithm described in ISO/IEC 15438:2001(E) in
 * annex P.
 */
final class PDF417HighLevelEncoder {

  /**
   * code for Text compaction
   */
  private static final int TEXT_COMPACTION = 0;

  /**
   * code for Byte compaction
   */
  private static final int BYTE_COMPACTION = 1;

  /**
   * code for Numeric compaction
   */
  private static final int NUMERIC_COMPACTION = 2;

  /**
   * Text compaction submode Alpha
   */
  private static final int SUBMODE_ALPHA = 0;

  /**
   * Text compaction submode Lower
   */
  private static final int SUBMODE_LOWER = 1;

  /**
   * Text compaction submode Mixed
   */
  private static final int SUBMODE_MIXED = 2;

  /**
   * Text compaction submode Punctuation
   */
  private static final int SUBMODE_PUNCTUATION = 3;

  /**
   * mode latch to Text Compaction mode
   */
  private static final int LATCH_TO_TEXT = 900;

  /**
   * mode latch to Byte Compaction mode (number of characters NOT a multiple of 6)
   */
  private static final int LATCH_TO_BYTE_PADDED = 901;

  /**
   * mode latch to Numeric Compaction mode
   */
  private static final int LATCH_TO_NUMERIC = 902;

  /**
   * mode shift to Byte Compaction mode
   */
  private static final int SHIFT_TO_BYTE = 913;

  /**
   * mode latch to Byte Compaction mode (number of characters a multiple of 6)
   */
  private static final int LATCH_TO_BYTE = 924;

  /**
   * identifier for a user defined Extended Channel Interpretation (ECI)
   */
  private static final int ECI_USER_DEFINED = 925;

  /**
   * identifier for a general purpose ECO format
   */
  private static final int ECI_GENERAL_PURPOSE = 926;

  /**
   * identifier for an ECI of a character set of code page
   */
  private static final int ECI_CHARSET = 927;

  /**
   * Raw code table for text compaction Mixed sub-mode
   */
  private static final byte[] TEXT_MIXED_RAW = {
      48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 38, 13, 9, 44, 58,
      35, 45, 46, 36, 47, 43, 37, 42, 61, 94, 0, 32, 0, 0, 0};

  /**
   * Raw code table for text compaction: Punctuation sub-mode
   */
  private static final byte[] TEXT_PUNCTUATION_RAW = {
      59, 60, 62, 64, 91, 92, 93, 95, 96, 126, 33, 13, 9, 44, 58,
      10, 45, 46, 36, 47, 34, 124, 42, 40, 41, 63, 123, 125, 39, 0};

  private static final byte[] MIXED = new byte[128];
  private static final byte[] PUNCTUATION = new byte[128];

  private static final Charset DEFAULT_ENCODING = StandardCharsets.ISO_8859_1;

  private PDF417HighLevelEncoder() {
  }

  static {
    //Construct inverse lookups
    Arrays.fill(MIXED, (byte) -1);
    for (int i = 0; i < TEXT_MIXED_RAW.length; i++) {
      byte b = TEXT_MIXED_RAW[i];
      if (b > 0) {
        MIXED[b] = (byte) i;
      }
    }
    Arrays.fill(PUNCTUATION, (byte) -1);
    for (int i = 0; i < TEXT_PUNCTUATION_RAW.length; i++) {
      byte b = TEXT_PUNCTUATION_RAW[i];
      if (b > 0) {
        PUNCTUATION[b] = (byte) i;
      }
    }
  }

  /**
   * Performs high-level encoding of a PDF417 message using the algorithm described in annex P
   * of ISO/IEC 15438:2001(E). If byte compaction has been selected, then only byte compaction
   * is used.
   *
   * @param msg the message
   * @param compaction compaction mode to use
   * @param encoding character encoding used to encode in default or byte compaction
   *  or {@code null} for default / not applicable
   * @param autoECI encode input minimally using multiple ECIs if needed
   *   If autoECI encoding is specified and additionally {@code encoding} is specified, then the encoder
   *   will use the specified {@link Charset} for any character that can be encoded by it, regardless
   *   if a different encoding would lead to a more compact encoding. When no {@code encoding} is specified
   *   then charsets will be chosen so that the byte representation is minimal.
   * @return the encoded message (the char values range from 0 to 928)
   */
  static String encodeHighLevel(String msg, Compaction compaction, Charset encoding, boolean autoECI)
      throws WriterException {

    if (msg.isEmpty()) {
      throw new WriterException("Empty message not allowed");
    }

    if (encoding == null && !autoECI) {
      for (int i = 0; i < msg.length(); i++) {
        if (msg.charAt(i) > 255) {
          throw new WriterException("Non-encodable character detected: " + msg.charAt(i) + " (Unicode: " +
              (int) msg.charAt(i) +
              "). Consider specifying EncodeHintType.PDF417_AUTO_ECI and/or EncodeTypeHint.CHARACTER_SET.");
        }
      }
    }
    //the codewords 0..928 are encoded as Unicode characters
    StringBuilder sb = new StringBuilder(msg.length());

    ECIInput input;
    if (autoECI) {
      input = new MinimalECIInput(msg, encoding, -1);
    } else {
      input = new NoECIInput(msg);
      if (encoding == null) {
        encoding = DEFAULT_ENCODING;
      } else if (!DEFAULT_ENCODING.equals(encoding)) {
        CharacterSetECI eci = CharacterSetECI.getCharacterSetECI(encoding);
        if (eci != null) {
          encodingECI(eci.getValue(), sb);
        }
      }
    }

    int len = input.length();
    int p = 0;
    int textSubMode = SUBMODE_ALPHA;

    // User selected encoding mode
    switch (compaction) {
      case TEXT:
        encodeText(input, p, len, sb, textSubMode);
        break;
      case BYTE:
        if (autoECI) {
          encodeMultiECIBinary(input, 0, input.length(), TEXT_COMPACTION, sb);
        } else {
          byte[] msgBytes = input.toString().getBytes(encoding);
          encodeBinary(msgBytes, p, msgBytes.length, BYTE_COMPACTION, sb);
        }
        break;
      case NUMERIC:
        sb.append((char) LATCH_TO_NUMERIC);
        encodeNumeric(input, p, len, sb);
        break;
      default:
        int encodingMode = TEXT_COMPACTION; //Default mode, see 4.4.2.1
        while (p < len) {
          while (p < len && input.isECI(p)) {
            encodingECI(input.getECIValue(p), sb);
            p++;
          }
          if (p >= len) {
            break;
          }
          int n = determineConsecutiveDigitCount(input, p);
          if (n >= 13) {
            sb.append((char) LATCH_TO_NUMERIC);
            encodingMode = NUMERIC_COMPACTION;
            textSubMode = SUBMODE_ALPHA; //Reset after latch
            encodeNumeric(input, p, n, sb);
            p += n;
          } else {
            int t = determineConsecutiveTextCount(input, p);
            if (t >= 5 || n == len) {
              if (encodingMode != TEXT_COMPACTION) {
                sb.append((char) LATCH_TO_TEXT);
                encodingMode = TEXT_COMPACTION;
                textSubMode = SUBMODE_ALPHA; //start with submode alpha after latch
              }
              textSubMode = encodeText(input, p, t, sb, textSubMode);
              p += t;
            } else {
              int b = determineConsecutiveBinaryCount(input, p, autoECI ? null : encoding);
              if (b == 0) {
                b = 1;
              }
              byte[] bytes = autoECI ? null : input.subSequence(p, p + b).toString().getBytes(encoding);
              if (((bytes == null && b == 1) || (bytes != null && bytes.length == 1))
                  && encodingMode == TEXT_COMPACTION) {
                //Switch for one byte (instead of latch)
                if (autoECI) {
                  encodeMultiECIBinary(input, p, 1, TEXT_COMPACTION, sb);
                } else {
                  encodeBinary(bytes, 0, 1, TEXT_COMPACTION, sb);
                }
              } else {
                //Mode latch performed by encodeBinary()
                if (autoECI) {
                  encodeMultiECIBinary(input, p, p + b, encodingMode, sb);
                } else {
                  encodeBinary(bytes, 0, bytes.length, encodingMode, sb);
                }
                encodingMode = BYTE_COMPACTION;
                textSubMode = SUBMODE_ALPHA; //Reset after latch
              }
              p += b;
            }
          }
        }
        break;
    }

    return sb.toString();
  }

  /**
   * Encode parts of the message using Text Compaction as described in ISO/IEC 15438:2001(E),
   * chapter 4.4.2.
   *
   * @param input          the input
   * @param startpos       the start position within the message
   * @param count          the number of characters to encode
   * @param sb             receives the encoded codewords
   * @param initialSubmode should normally be SUBMODE_ALPHA
   * @return the text submode in which this method ends
   */
  private static int encodeText(ECIInput input,
                                int startpos,
                                int count,
                                StringBuilder sb,
                                int initialSubmode) throws WriterException {
    StringBuilder tmp = new StringBuilder(count);
    int submode = initialSubmode;
    int idx = 0;
    while (true) {
      if (input.isECI(startpos + idx)) {
        encodingECI(input.getECIValue(startpos + idx), sb);
        idx++;
      } else {
        char ch = input.charAt(startpos + idx);
        switch (submode) {
          case SUBMODE_ALPHA:
            if (isAlphaUpper(ch)) {
              if (ch == ' ') {
                tmp.append((char) 26); //space
              } else {
                tmp.append((char) (ch - 65));
              }
            } else {
              if (isAlphaLower(ch)) {
                submode = SUBMODE_LOWER;
                tmp.append((char) 27); //ll
                continue;
              } else if (isMixed(ch)) {
                submode = SUBMODE_MIXED;
                tmp.append((char) 28); //ml
                continue;
              } else {
                tmp.append((char) 29); //ps
                tmp.append((char) PUNCTUATION[ch]);
                break;
              }
            }
            break;
          case SUBMODE_LOWER:
            if (isAlphaLower(ch)) {
              if (ch == ' ') {
                tmp.append((char) 26); //space
              } else {
                tmp.append((char) (ch - 97));
              }
            } else {
              if (isAlphaUpper(ch)) {
                tmp.append((char) 27); //as
                tmp.append((char) (ch - 65));
                //space cannot happen here, it is also in "Lower"
                break;
              } else if (isMixed(ch)) {
                submode = SUBMODE_MIXED;
                tmp.append((char) 28); //ml
                continue;
              } else {
                tmp.append((char) 29); //ps
                tmp.append((char) PUNCTUATION[ch]);
                break;
              }
            }
            break;
          case SUBMODE_MIXED:
            if (isMixed(ch)) {
              tmp.append((char) MIXED[ch]);
            } else {
              if (isAlphaUpper(ch)) {
                submode = SUBMODE_ALPHA;
                tmp.append((char) 28); //al
                continue;
              } else if (isAlphaLower(ch)) {
                submode = SUBMODE_LOWER;
                tmp.append((char) 27); //ll
                continue;
              } else {
                if (startpos + idx + 1 < count &&
                    !input.isECI(startpos + idx + 1) &&
                    isPunctuation(input.charAt(startpos + idx + 1))) {
                  submode = SUBMODE_PUNCTUATION;
                  tmp.append((char) 25); //pl
                  continue;
                }
                tmp.append((char) 29); //ps
                tmp.append((char) PUNCTUATION[ch]);
              }
            }
            break;
          default: //SUBMODE_PUNCTUATION
            if (isPunctuation(ch)) {
              tmp.append((char) PUNCTUATION[ch]);
            } else {
              submode = SUBMODE_ALPHA;
              tmp.append((char) 29); //al
              continue;
            }
        }
        idx++;
        if (idx >= count) {
          break;
        }
      }
    }
    char h = 0;
    int len = tmp.length();
    for (int i = 0; i < len; i++) {
      boolean odd = (i % 2) != 0;
      if (odd) {
        h = (char) ((h * 30) + tmp.charAt(i));
        sb.append(h);
      } else {
        h = tmp.charAt(i);
      }
    }
    if ((len % 2) != 0) {
      sb.append((char) ((h * 30) + 29)); //ps
    }
    return submode;
  }

  /**
   * Encode all of the message using Byte Compaction as described in ISO/IEC 15438:2001(E)
   *
   * @param input     the input
   * @param startpos  the start position within the message
   * @param count     the number of bytes to encode
   * @param startmode the mode from which this method starts
   * @param sb        receives the encoded codewords
   */
  private static void encodeMultiECIBinary(ECIInput input,
                                          int startpos,
                                          int count,
                                          int startmode,
                                          StringBuilder sb) throws WriterException {
    final int end = Math.min(startpos + count, input.length());
    int localStart = startpos;
    while (true) {
      //encode all leading ECIs and advance localStart
      while (localStart < end && input.isECI(localStart)) {
        encodingECI(input.getECIValue(localStart), sb);
        localStart++;
      }
      int localEnd = localStart;
      //advance end until before the next ECI
      while (localEnd < end && !input.isECI(localEnd)) {
        localEnd++;
      }

      final int localCount = localEnd - localStart;
      if (localCount <= 0) {
        //done
        break;
      } else {
        //encode the segment
        encodeBinary(subBytes(input, localStart, localEnd),
            0, localCount, localStart == startpos ? startmode : BYTE_COMPACTION, sb);
        localStart = localEnd;
      }
    }
  }

  static byte[] subBytes(ECIInput input, int start, int end) {
    final int count = end - start;
    byte[] result = new byte[count];
    for (int i = start; i < end; i++) {
      result[i - start] = (byte) (input.charAt(i) & 0xff);
    }
    return result;
  }

  /**
   * Encode parts of the message using Byte Compaction as described in ISO/IEC 15438:2001(E),
   * chapter 4.4.3. The Unicode characters will be converted to binary using the cp437
   * codepage.
   *
   * @param bytes     the message converted to a byte array
   * @param startpos  the start position within the message
   * @param count     the number of bytes to encode
   * @param startmode the mode from which this method starts
   * @param sb        receives the encoded codewords
   */
  private static void encodeBinary(byte[] bytes,
                                   int startpos,
                                   int count,
                                   int startmode,
                                   StringBuilder sb) {
    if (count == 1 && startmode == TEXT_COMPACTION) {
      sb.append((char) SHIFT_TO_BYTE);
    } else {
      if ((count % 6) == 0) {
        sb.append((char) LATCH_TO_BYTE);
      } else {
        sb.append((char) LATCH_TO_BYTE_PADDED);
      }
    }

    int idx = startpos;
    // Encode sixpacks
    if (count >= 6) {
      char[] chars = new char[5];
      while ((startpos + count - idx) >= 6) {
        long t = 0;
        for (int i = 0; i < 6; i++) {
          t <<= 8;
          t += bytes[idx + i] & 0xff;
        }
        for (int i = 0; i < 5; i++) {
          chars[i] = (char) (t % 900);
          t /= 900;
        }
        for (int i = chars.length - 1; i >= 0; i--) {
          sb.append(chars[i]);
        }
        idx += 6;
      }
    }
    //Encode rest (remaining n<5 bytes if any)
    for (int i = idx; i < startpos + count; i++) {
      int ch = bytes[i] & 0xff;
      sb.append((char) ch);
    }
  }

  private static void encodeNumeric(ECIInput input, int startpos, int count, StringBuilder sb) {
    int idx = 0;
    StringBuilder tmp = new StringBuilder(count / 3 + 1);
    BigInteger num900 = BigInteger.valueOf(900);
    BigInteger num0 = BigInteger.valueOf(0);
    while (idx < count) {
      tmp.setLength(0);
      int len = Math.min(44, count - idx);
      String part = "1" + input.subSequence(startpos + idx, startpos + idx + len);
      BigInteger bigint = new BigInteger(part);
      do {
        tmp.append((char) bigint.mod(num900).intValue());
        bigint = bigint.divide(num900);
      } while (!bigint.equals(num0));

      //Reverse temporary string
      for (int i = tmp.length() - 1; i >= 0; i--) {
        sb.append(tmp.charAt(i));
      }
      idx += len;
    }
  }


  private static boolean isDigit(char ch) {
    return ch >= '0' && ch <= '9';
  }

  private static boolean isAlphaUpper(char ch) {
    return ch == ' ' || (ch >= 'A' && ch <= 'Z');
  }

  private static boolean isAlphaLower(char ch) {
    return ch == ' ' || (ch >= 'a' && ch <= 'z');
  }

  private static boolean isMixed(char ch) {
    return MIXED[ch] != -1;
  }

  private static boolean isPunctuation(char ch) {
    return PUNCTUATION[ch] != -1;
  }

  private static boolean isText(char ch) {
    return ch == '\t' || ch == '\n' || ch == '\r' || (ch >= 32 && ch <= 126);
  }

  /**
   * Determines the number of consecutive characters that are encodable using numeric compaction.
   *
   * @param input      the input
   * @param startpos the start position within the input
   * @return the requested character count
   */
  private static int determineConsecutiveDigitCount(ECIInput input, int startpos) {
    int count = 0;
    final int len = input.length();
    int idx = startpos;
    if (idx < len) {
      while (idx < len && !input.isECI(idx) && isDigit(input.charAt(idx))) {
        count++;
        idx++;
      }
    }
    return count;
  }

  /**
   * Determines the number of consecutive characters that are encodable using text compaction.
   *
   * @param input      the input
   * @param startpos the start position within the input
   * @return the requested character count
   */
  private static int determineConsecutiveTextCount(ECIInput input, int startpos) {
    final int len = input.length();
    int idx = startpos;
    while (idx < len) {
      int numericCount = 0;
      while (numericCount < 13 && idx < len && !input.isECI(idx) && isDigit(input.charAt(idx))) {
        numericCount++;
        idx++;
      }
      if (numericCount >= 13) {
        return idx - startpos - numericCount;
      }
      if (numericCount > 0) {
        //Heuristic: All text-encodable chars or digits are binary encodable
        continue;
      }

      //Check if character is encodable
      if (input.isECI(idx) || !isText(input.charAt(idx))) {
        break;
      }
      idx++;
    }
    return idx - startpos;
  }

  /**
   * Determines the number of consecutive characters that are encodable using binary compaction.
   *
   * @param input    the input
   * @param startpos the start position within the message
   * @param encoding the charset used to convert the message to a byte array
   * @return the requested character count
   */
  private static int determineConsecutiveBinaryCount(ECIInput input, int startpos, Charset encoding)
      throws WriterException {
    CharsetEncoder encoder = encoding == null ? null : encoding.newEncoder();
    int len = input.length();
    int idx = startpos;
    while (idx < len) {
      int numericCount = 0;

      int i = idx;
      while (numericCount < 13 && !input.isECI(i) && isDigit(input.charAt(i))) {
        numericCount++;
        //textCount++;
        i = idx + numericCount;
        if (i >= len) {
          break;
        }
      }
      if (numericCount >= 13) {
        return idx - startpos;
      }

      if (encoder != null && !encoder.canEncode(input.charAt(idx))) {
        assert input instanceof NoECIInput;
        char ch = input.charAt(idx);
        throw new WriterException("Non-encodable character detected: " + ch + " (Unicode: " + (int) ch + ')');
      }
      idx++;
    }
    return idx - startpos;
  }

  private static void encodingECI(int eci, StringBuilder sb) throws WriterException {
    if (eci >= 0 && eci < 900) {
      sb.append((char) ECI_CHARSET);
      sb.append((char) eci);
    } else if (eci < 810900) {
      sb.append((char) ECI_GENERAL_PURPOSE);
      sb.append((char) (eci / 900 - 1));
      sb.append((char) (eci % 900));
    } else if (eci < 811800) {
      sb.append((char) ECI_USER_DEFINED);
      sb.append((char) (810900 - eci));
    } else {
      throw new WriterException("ECI number not in valid range from 0..811799, but was " + eci);
    }
  }

  private static final class NoECIInput implements ECIInput {

    String input;

    private NoECIInput(String input) {
      this.input = input;
    }

    public int length() {
      return input.length();
    }

    public char charAt(int index) {
      return input.charAt(index);
    }

    public boolean isECI(int index) {
      return false;
    }

    public int getECIValue(int index) {
      return -1;
    }

    public boolean haveNCharacters(int index, int n) {
      return index + n <= input.length();
    }

    public CharSequence subSequence(int start, int end) {
      return input.subSequence(start, end);
    }

    public String toString() {
      return input;
    }
  }
}
