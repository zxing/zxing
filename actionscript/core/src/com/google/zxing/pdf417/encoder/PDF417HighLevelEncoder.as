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

package com.google.zxing.pdf417.encoder
{

import com.google.zxing.WriterException;
import com.google.zxing.common.flexdatatypes.StringBuilder;

/**
 * PDF417 high-level encoder following the algorithm described in ISO/IEC 15438:2001(E) in
 * annex P.
 */
public class PDF417HighLevelEncoder 
{

  /**
   * code for Text compaction
   */
  private static var TEXT_COMPACTION:int = 0;

  /**
   * code for Byte compaction
   */
  private static var BYTE_COMPACTION:int = 1;

  /**
   * code for Numeric compaction
   */
  private static var NUMERIC_COMPACTION:int = 2;

  /**
   * Text compaction submode Alpha
   */
  private static var SUBMODE_ALPHA:int = 0;

  /**
   * Text compaction submode Lower
   */
  private static var SUBMODE_LOWER:int = 1;

  /**
   * Text compaction submode Mixed
   */
  private static var SUBMODE_MIXED:int = 2;

  /**
   * Text compaction submode Punctuation
   */
  private static var SUBMODE_PUNCTUATION:int = 3;

  /**
   * mode latch to Text Compaction mode
   */
  private static var LATCH_TO_TEXT:int = 900;

  /**
   * mode latch to Byte Compaction mode (number of characters NOT a multiple of 6)
   */
  private static var LATCH_TO_BYTE_PADDED:int = 901;

  /**
   * mode latch to Numeric Compaction mode
   */
  private static var LATCH_TO_NUMERIC:int = 902;

  /**
   * mode shift to Byte Compaction mode
   */
  private static var SHIFT_TO_BYTE:int = 913;

  /**
   * mode latch to Byte Compaction mode (number of characters a multiple of 6)
   */
  private static var LATCH_TO_BYTE:int = 924;

  /**
   * Raw code table for text compaction Mixed sub-mode
   */
  private static var TEXT_MIXED_RAW:Array = [
      48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 38, 13, 9, 44, 58,
      35, 45, 46, 36, 47, 43, 37, 42, 61, 94, 0, 32, 0, 0, 0];

  /**
   * Raw code table for text compaction: Punctuation sub-mode
   */
  private static var TEXT_PUNCTUATION_RAW:Array = [
      59, 60, 62, 64, 91, 92, 93, 95, 96, 126, 33, 13, 9, 44, 58,
      10, 45, 46, 36, 47, 34, 124, 42, 40, 41, 63, 123, 125, 39, 0];

  private static var MIXED:Array = new Array(128);
  private static var PUNCTUATION:Array = new Array(128);

  public function PDF417HighLevelEncoder():void 
  {
 
    for (var i:int = 0; i < MIXED.length; i++) {
      MIXED[i] = -1;
    }
    for (i = 0; i < TEXT_MIXED_RAW.length; i++) {
      var b:int = TEXT_MIXED_RAW[i];
      if (b > 0) {
        MIXED[b] = i;
      }
    }
    for (i = 0; i < PUNCTUATION.length; i++) {
      PUNCTUATION[i] = -1;
    }
    for (i = 0; i < TEXT_PUNCTUATION_RAW.length; i++) {
      b = TEXT_PUNCTUATION_RAW[i];
      if (b > 0) {
        PUNCTUATION[b] = i;
      }
    }
  }

  /**
   * Converts the message to a byte array using the default encoding (cp437) as defined by the
   * specification
   *
   * @param msg the message
   * @return the byte array of the message
   */
  private static function getBytesForMessage(msg:String):Array {
    return msg.split("");
  }

  /**
   * Performs high-level encoding of a PDF417 message using the algorithm described in annex P
   * of ISO/IEC 15438:2001(E).
   *
   * @param msg the message
   * @return the encoded message (the char values range from 0 to 928)
   */
  public function encodeHighLevel(msg:String):String {
    var bytes:Array = null; //Fill later and only if needed

    //the codewords 0..928 are encoded as Unicode characters
    var sb:StringBuilder  = new StringBuilder(msg.length);

    var len:int = msg.length;
    var p:int = 0;
    var encodingMode:int = TEXT_COMPACTION; //Default mode, see 4.4.2.1
    var textSubMode:int = SUBMODE_ALPHA;
    while (p < len) {
      var n:int = determineConsecutiveDigitCount(msg, p);
      if (n >= 13) {
        sb.Append(String.fromCharCode(LATCH_TO_NUMERIC));
        encodingMode = NUMERIC_COMPACTION;
        textSubMode = SUBMODE_ALPHA; //Reset after latch
        encodeNumeric(msg, p, n, sb);
        p += n;
      } else {
        var t:int = determineConsecutiveTextCount(msg, p);
        if (t >= 5 || n == len) {
          if (encodingMode != TEXT_COMPACTION) {
            sb.Append(String.fromCharCode(LATCH_TO_TEXT));
            encodingMode = TEXT_COMPACTION;
            textSubMode = SUBMODE_ALPHA; //start with submode alpha after latch
          }
          textSubMode = encodeText(msg, p, t, sb, textSubMode);
          p += t;
        } else {
          if (bytes == null) {
            bytes = getBytesForMessage(msg);
          }
          var b:int = determineConsecutiveBinaryCount(msg, bytes, p);
          if (b == 0) {
            b = 1;
          }
          if (b == 1 && encodingMode == TEXT_COMPACTION) {
            //Switch for one byte (instead of latch)
            encodeBinary(bytes, p, 1, TEXT_COMPACTION, sb);
          } else {
            //Mode latch performed by encodeBinary()
            encodeBinary(bytes, p, b, encodingMode, sb);
            encodingMode = BYTE_COMPACTION;
            textSubMode = SUBMODE_ALPHA; //Reset after latch
          }
          p += b;
        }
      }
    }

    return sb.toString();
  }
	/*
	public static function toHexString(_string:String):String
	
	{
		var r="";
    	var e=_string.length;
    	var c=0;
    	var h;
    	while(c<e)
    	{
        	h=_string.charCodeAt(c++).toString(16);
        	while(h.length<3) h="0"+h;
        	r+=" ";
        	r+=h;
    	}
    	return r;
	}*/
  /**
   * Encode parts of the message using Text Compaction as described in ISO/IEC 15438:2001(E),
   * chapter 4.4.2.
   *
   * @param msg            the message
   * @param startpos       the start position within the message
   * @param count          the number of characters to encode
   * @param sb             receives the encoded codewords
   * @param initialSubmode should normally be SUBMODE_ALPHA
   * @return the text submode in which this method ends
   */
  private function encodeText(msg:String, startpos:int, count:int, sb:StringBuilder, initialSubmode:int):int {
    var tmp:StringBuilder  = new StringBuilder(count);
    var submode:int = initialSubmode;
    var idx:int = 0;
    while (true) {
      var ch:String = msg.charAt(startpos + idx);
      switch (submode) {
        case SUBMODE_ALPHA:
          if (isAlphaUpper(ch)) {
            if (ch == ' ') {
              tmp.Append(String.fromCharCode(26)); //space
            } else {
              tmp.Append(String.fromCharCode(ch.charCodeAt(0) - 65));
            }
          } else {
            if (isAlphaLower(ch)) {
              submode = SUBMODE_LOWER;
              tmp.Append(String.fromCharCode(27)); //ll
              continue;
            } else if (isMixed(ch)) {
              submode = SUBMODE_MIXED;
              tmp.Append(String.fromCharCode(28)); //ml
              continue;
            } else {
              tmp.Append(String.fromCharCode(29)); //ps
              tmp.Append(String.fromCharCode(PUNCTUATION[ch.charCodeAt(0)]));
              break;
            }
          }
          break;
        case SUBMODE_LOWER:
          if (isAlphaLower(ch)) {
            if (ch == ' ') {
              tmp.Append(String.fromCharCode(26)); //space
            } else {
              tmp.Append(String.fromCharCode(ch.charCodeAt(0) - 97));
            }
          } else {
            if (isAlphaUpper(ch)) {
              tmp.Append(String.fromCharCode(27)); //as
              tmp.Append(String.fromCharCode(ch.charCodeAt(0) - 65));
              //space cannot happen here, it is also in "Lower"
              break;
            } else if (isMixed(ch)) {
              submode = SUBMODE_MIXED;
              tmp.Append(String.fromCharCode(28)); //ml
              continue;
            } else {
              tmp.Append(String.fromCharCode(29)); //ps
              tmp.Append(String.fromCharCode(PUNCTUATION[ch.charCodeAt(0)]));
              break;
            }
          }
          break;
        case SUBMODE_MIXED:
          if (isMixed(ch)) {
          	var s:String =String.fromCharCode(MIXED[ch.charCodeAt(0)]);
          	var ii:int = s.charCodeAt(0); 
            tmp.Append(s);
          } else {
            if (isAlphaUpper(ch)) {
              submode = SUBMODE_ALPHA;
              tmp.Append(String.fromCharCode(28)); //al
              continue;
            } else if (isAlphaLower(ch)) {
              submode = SUBMODE_LOWER;
              tmp.Append(String.fromCharCode(27)); //ll
              continue;
            } else {
              if (startpos + idx + 1 < count) {
                var next:String = msg.charAt(startpos + idx + 1);
                if (isPunctuation(next)) {
                  submode = SUBMODE_PUNCTUATION;
                  tmp.Append(String.fromCharCode(25)); //pl
                  continue;
                }
              }
              tmp.Append(String.fromCharCode(29)); //ps
              tmp.Append(String.fromCharCode(PUNCTUATION[ch.charCodeAt(0)]));
            }
          }
          break;
        default: //SUBMODE_PUNCTUATION
          if (isPunctuation(ch)) {
            tmp.Append(String.fromCharCode(PUNCTUATION[ch.charCodeAt(0)]));
          } else {
            submode = SUBMODE_ALPHA;
            tmp.Append(String.fromCharCode(29)); //al
            continue;
          }
      }
      idx++;
      if (idx >= count) {
        break;
      }
    }
    var h:String = "";
    var len:int = tmp.length;
    
   
    for (var i:int = 0; i < len; i++) {
      var odd:Boolean = (i % 2) != 0;
      if (odd) {
      	
        h = String.fromCharCode((h.charCodeAt(0) * 30) + (tmp.charAt(i)).charCodeAt(0));
        var code:int = h.charCodeAt(0); 
        sb.Append(h);
      } else {
        h = tmp.charAt(i);
      }
    }
    if ((len % 2) != 0) {
      sb.Append(String.fromCharCode(((h.charCodeAt(0) * 30) + 29))); //ps
    }
    
    return submode;
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
  private function encodeBinary(bytes:Array, startpos:int , count:int , startmode:int, sb:StringBuilder):void {
    if (count == 1 && startmode == TEXT_COMPACTION) {
      sb.Append(String.fromCharCode(SHIFT_TO_BYTE));
    } else {
      var sixpack:Boolean = (count % 6) == 0;
      if (sixpack) {
        sb.Append(String.fromCharCode(LATCH_TO_BYTE));
      } else {
        sb.Append(String.fromCharCode(LATCH_TO_BYTE_PADDED));
      }
    }

    var chars:Array = new Array(5);
    var idx:int = startpos;
    while ((startpos + count - idx) >= 6) {
      var t:Number = 0;
      for (i = 0; i < 6; i++) {
        t <<= 8;
        t += bytes[idx + i] & 0xff;
      }
      for (i = 0; i < 5; i++) {
        chars[i] = (t % 900).toString();
        t /= 900;
      }
      for (i = chars.length - 1; i >= 0; i--) {
        sb.Append(chars[i]);
      }
      idx += 6;
    }
    //Encode rest (remaining n<5 bytes if any)
    for (var i:int = idx; i < startpos + count; i++) {
      var ch:int = bytes[i] & 0xff;
      sb.Append(String.fromCharCode(ch));
    }
  }

  /*
  private static void encodeNumeric(String msg, int startpos, int count, StringBuffer sb) {
    int idx = 0;
    StringBuffer tmp = new StringBuffer(count / 3 + 1);
    BigInteger num900 = BigInteger.valueOf(900);
    BigInteger num0 = BigInteger.valueOf(0);
    while (idx < count - 1) {
      tmp.setLength(0);
      int len = Math.min(44, count - idx);
      String part = '1' + msg.substring(startpos + idx, startpos + idx + len);
      BigInteger bigint = new BigInteger(part);
      do {
        BigInteger c = bigint.mod(num900);
        tmp.append((char) c.intValue());
        bigint = bigint.divide(num900);
      } while (!bigint.equals(num0));

      //Reverse temporary string
      for (int i = tmp.length() - 1; i >= 0; i--) {
        sb.append(tmp.charAt(i));
      }
      idx += len;
    }
  }
   */

  // TODO either this needs to reimplement BigInteger's functionality to properly handle very
  // large numeric strings, even in Java ME, or, we give up Java ME and use the version above
  // with BigInteger

  private function encodeNumeric(msg:String, startpos:int, count:int, sb:StringBuilder ):void {
    var idx:int = 0;
    var tmp:StringBuilder  = new StringBuilder(count / 3 + 1);
    while (idx < count - 1) {
      tmp.setLength(0);
      var len:int = Math.min(44, count - idx);
      var part:String = '1' + msg.substring(startpos + idx, startpos + idx + len);
      var bigint:Number = parseFloat(part);
      do {
        var c:Number = Math.round((bigint % 900)+0.5); // cast to long
        tmp.Append(String.fromCharCode(c));
        bigint = Math.round((bigint /  900)+0.5); // bigint /= 900
      } while (bigint != 0);

      //Reverse temporary string
      for (var i:int = tmp.length - 1; i >= 0; i--) {
        sb.Append(tmp.charAt(i));
      }
      idx += len;
    }
  }

  private function isDigit(ch:String):Boolean {
    return ch.charCodeAt(0) >= 48 && ch.charCodeAt(0) <= 57;
  }

  private function isAlphaUpper(ch:String):Boolean {
    return ch == ' ' || (ch.charCodeAt(0) >= 65 && ch.charCodeAt(0) <= 90);
  }

  private function isAlphaLower(ch:String):Boolean {
    return ch == ' ' || (ch.charCodeAt(0) >= 97 && ch.charCodeAt(0) <= 122);
  }

  private function isMixed(ch:String):Boolean {
    return MIXED[ch.charCodeAt(0)] != -1;
  }

  private function isPunctuation(ch:String):Boolean {
    return PUNCTUATION[ch.charCodeAt(0)] != -1;
  }

  private function isText(ch:String):Boolean {
    return ch == "\t" || ch == "\n" || ch == "\r" || (ch.charCodeAt(0) >= 32 && ch.charCodeAt(0) <= 126);
  }

  /**
   * Determines the number of consecutive characters that are encodable using numeric compaction.
   *
   * @param msg      the message
   * @param startpos the start position within the message
   * @return the requested character count
   */
  private function determineConsecutiveDigitCount(msg:String, startpos:int):int {
    var count:int = 0;
    var len:int = msg.length;
    var idx:int = startpos;
    if (idx < len) {
      var ch:String = msg.charAt(idx);
      while (isDigit(ch) && idx < len) {
        count++;
        idx++;
        if (idx < len) {
          ch = msg.charAt(idx);
        }
      }
    }
    return count;
  }

  /**
   * Determines the number of consecutive characters that are encodable using text compaction.
   *
   * @param msg      the message
   * @param startpos the start position within the message
   * @return the requested character count
   */
  private function determineConsecutiveTextCount(msg:String, startpos:int):int {
    var len:int = msg.length;
    var idx:int = startpos;
    while (idx < len) {
      var ch:String = msg.charAt(idx);
      var numericCount:int = 0;
      while (numericCount < 13 && isDigit(ch) && idx < len) {
        numericCount++;
        idx++;
        if (idx < len) {
          ch = msg.charAt(idx);
        }
      }
      if (numericCount >= 13) {
        return idx - startpos - numericCount;
      }
      if (numericCount > 0) {
        //Heuristic: All text-encodable chars or digits are binary encodable
        continue;
      }
      ch = msg.charAt(idx);

      //Check if character is encodable
      if (!isText(ch)) {
        break;
      }
      idx++;
    }
    return idx - startpos;
  }

  /**
   * Determines the number of consecutive characters that are encodable using binary compaction.
   *
   * @param msg      the message
   * @param bytes    the message converted to a byte array
   * @param startpos the start position within the message
   * @return the requested character count
   */
  private function determineConsecutiveBinaryCount(msg:String, bytes:Array, startpos:int):int {
    var len:int = msg.length;
    var idx:int = startpos;
    while (idx < len) {
      var ch:String = msg.charAt(idx);
      var numericCount:int = 0;

      while (numericCount < 13 && isDigit(ch)) {
        numericCount++;
        //textCount++;
        var i:int = idx + numericCount;
        if (i >= len) {
          break;
        }
        ch = msg.charAt(i);
      }
      if (numericCount >= 13) {
        return idx - startpos;
      }
      var textCount:int = 0;
      while (textCount < 5 && isText(ch)) {
        textCount++;
        i = idx + textCount;
        if (i >= len) {
          break;
        }
        ch = msg.charAt(i);
      }
      if (textCount >= 5) {
        return idx - startpos;
      }
      ch = msg.charAt(idx);

      //Check if character is encodable
      //Sun returns a ASCII 63 (?) for a character that cannot be mapped. Let's hope all
      //other VMs do the same
      if (bytes[idx] == 63 && ch != '?') {
        throw new WriterException("Non-encodable character detected: " + ch + " (Unicode: " + ch.toString() + ')');
      }
      idx++;
    }
    return idx - startpos;
  }
  
  	
	
}

}
