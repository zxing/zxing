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

using System;
#if SILVERLIGHT4 || SILVERLIGHT5 || NET40 || NET45 || NETFX_CORE
using System.Numerics;
#else
using BigIntegerLibrary;
#endif
using System.Text;

namespace ZXing.PDF417.Internal
{
   /// <summary>
   /// PDF417 high-level encoder following the algorithm described in ISO/IEC 15438:2001(E) in
   /// annex P.
   /// </summary>
   sealed class PDF417HighLevelEncoder
   {
      /// <summary>
      /// code for Text compaction
      /// </summary>
      private const int TEXT_COMPACTION = 0;

      /// <summary>
      /// code for Byte compaction
      /// </summary>
      private const int BYTE_COMPACTION = 1;

      /// <summary>
      /// code for Numeric compaction
      /// </summary>
      private const int NUMERIC_COMPACTION = 2;

      /// <summary>
      /// Text compaction submode Alpha
      /// </summary>
      private const int SUBMODE_ALPHA = 0;

      /// <summary>
      /// Text compaction submode Lower
      /// </summary>
      private const int SUBMODE_LOWER = 1;

      /// <summary>
      /// Text compaction submode Mixed
      /// </summary>
      private const int SUBMODE_MIXED = 2;

      /// <summary>
      /// Text compaction submode Punctuation
      /// </summary>
      private const int SUBMODE_PUNCTUATION = 3;

      /// <summary>
      /// mode latch to Text Compaction mode
      /// </summary>
      private const int LATCH_TO_TEXT = 900;

      /// <summary>
      /// mode latch to Byte Compaction mode (number of characters NOT a multiple of 6)
      /// </summary>
      private const int LATCH_TO_BYTE_PADDED = 901;

      /// <summary>
      /// mode latch to Numeric Compaction mode
      /// </summary>
      private const int LATCH_TO_NUMERIC = 902;

      /// <summary>
      /// mode shift to Byte Compaction mode
      /// </summary>
      private const int SHIFT_TO_BYTE = 913;

      /// <summary>
      /// mode latch to Byte Compaction mode (number of characters a multiple of 6)
      /// </summary>
      private const int LATCH_TO_BYTE = 924;

      /// <summary>
      /// Raw code table for text compaction Mixed sub-mode
      /// </summary>
      private static readonly sbyte[] TEXT_MIXED_RAW = {
                                                          48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 38, 13, 9, 44, 58,
                                                          35, 45, 46, 36, 47, 43, 37, 42, 61, 94, 0, 32, 0, 0, 0
                                                       };

      /// <summary>
      /// Raw code table for text compaction: Punctuation sub-mode
      /// </summary>
      private static readonly sbyte[] TEXT_PUNCTUATION_RAW = {
                                                                59, 60, 62, 64, 91, 92, 93, 95, 96, 126, 33, 13, 9, 44, 58,
                                                                10, 45, 46, 36, 47, 34, 124, 42, 40, 41, 63, 123, 125, 39, 0
                                                             };

      private static readonly sbyte[] MIXED = new sbyte[128];
      private static readonly sbyte[] PUNCTUATION = new sbyte[128];

      private PDF417HighLevelEncoder()
      {
      }

      static PDF417HighLevelEncoder()
      {
         //Construct inverse lookups
         for (int idx = 0; idx < MIXED.Length; idx++)
            MIXED[idx] = -1;
         for (sbyte i = 0; i < TEXT_MIXED_RAW.Length; i++)
         {
            sbyte b = TEXT_MIXED_RAW[i];
            if (b > 0)
            {
               MIXED[b] = i;
            }
         }
         for (int idx = 0; idx < PUNCTUATION.Length; idx++)
            PUNCTUATION[idx] = -1;
         for (sbyte i = 0; i < TEXT_PUNCTUATION_RAW.Length; i++)
         {
            sbyte b = TEXT_PUNCTUATION_RAW[i];
            if (b > 0)
            {
               PUNCTUATION[b] = i;
            }
         }
      }

      /// <summary>
      /// Converts the message to a byte array using the default encoding (cp437) as defined by the
      /// specification
      ///
      /// <param name="msg">the message</param>
      /// <returns>the byte array of the message</returns>
      /// </summary>
      private static byte[] getBytesForMessage(String msg)
      {
#if WindowsCE
         try
         {
            return Encoding.GetEncoding("CP437").GetBytes(msg);
         }
         catch (PlatformNotSupportedException)
         {
            // WindowsCE doesn't support all encodings. But it is device depended.
            // So we try here the some different ones
            return Encoding.GetEncoding(1252).GetBytes(msg);
         }
#else
         return Encoding.GetEncoding("CP437").GetBytes(msg);
#endif
      }

      /// <summary>
      /// Performs high-level encoding of a PDF417 message using the algorithm described in annex P
      /// of ISO/IEC 15438:2001(E). If byte compaction has been selected, then only byte compaction
      /// is used.
      ///
      /// <param name="msg">the message</param>
      /// <returns>the encoded message (the char values range from 0 to 928)</returns>
      /// </summary>
      internal static String encodeHighLevel(String msg, Compaction compaction)
      {
         byte[] bytes = null; //Fill later and only if needed

         //the codewords 0..928 are encoded as Unicode characters
         StringBuilder sb = new StringBuilder(msg.Length);

         int len = msg.Length;
         int p = 0;
         int textSubMode = SUBMODE_ALPHA;

         // User selected encoding mode
         if (compaction == Compaction.TEXT)
         {
            encodeText(msg, p, len, sb, textSubMode);

         }
         else if (compaction == Compaction.BYTE)
         {
            bytes = getBytesForMessage(msg);
            encodeBinary(bytes, p, bytes.Length, BYTE_COMPACTION, sb);

         }
         else if (compaction == Compaction.NUMERIC)
         {
            sb.Append((char)LATCH_TO_NUMERIC);
            encodeNumeric(msg, p, len, sb);

         }
         else
         {
            int encodingMode = TEXT_COMPACTION; //Default mode, see 4.4.2.1
            while (p < len)
            {
               int n = determineConsecutiveDigitCount(msg, p);
               if (n >= 13)
               {
                  sb.Append((char)LATCH_TO_NUMERIC);
                  encodingMode = NUMERIC_COMPACTION;
                  textSubMode = SUBMODE_ALPHA; //Reset after latch
                  encodeNumeric(msg, p, n, sb);
                  p += n;
               }
               else
               {
                  int t = determineConsecutiveTextCount(msg, p);
                  if (t >= 5 || n == len)
                  {
                     if (encodingMode != TEXT_COMPACTION)
                     {
                        sb.Append((char)LATCH_TO_TEXT);
                        encodingMode = TEXT_COMPACTION;
                        textSubMode = SUBMODE_ALPHA; //start with submode alpha after latch
                     }
                     textSubMode = encodeText(msg, p, t, sb, textSubMode);
                     p += t;
                  }
                  else
                  {
                     if (bytes == null)
                     {
                        bytes = getBytesForMessage(msg);
                     }
                     int b = determineConsecutiveBinaryCount(msg, bytes, p);
                     if (b == 0)
                     {
                        b = 1;
                     }
                     if (b == 1 && encodingMode == TEXT_COMPACTION)
                     {
                        //Switch for one byte (instead of latch)
                        encodeBinary(bytes, p, 1, TEXT_COMPACTION, sb);
                     }
                     else
                     {
                        //Mode latch performed by encodeBinary()
                        encodeBinary(bytes, p, b, encodingMode, sb);
                        encodingMode = BYTE_COMPACTION;
                        textSubMode = SUBMODE_ALPHA; //Reset after latch
                     }
                     p += b;
                  }
               }
            }
         }

         return sb.ToString();
      }

      /// <summary>
      /// Encode parts of the message using Text Compaction as described in ISO/IEC 15438:2001(E),
      /// chapter 4.4.2.
      ///
      /// <param name="msg">the message</param>
      /// <param name="startpos">the start position within the message</param>
      /// <param name="count">the number of characters to encode</param>
      /// <param name="sb">receives the encoded codewords</param>
      /// <param name="initialSubmode">should normally be SUBMODE_ALPHA</param>
      /// <returns>the text submode in which this method ends</returns>
      /// </summary>
      private static int encodeText(String msg,
                                    int startpos,
                                    int count,
                                    StringBuilder sb,
                                    int initialSubmode)
      {
         StringBuilder tmp = new StringBuilder(count);
         int submode = initialSubmode;
         int idx = 0;
         while (true)
         {
            char ch = msg[startpos + idx];
            switch (submode)
            {
               case SUBMODE_ALPHA:
                  if (isAlphaUpper(ch))
                  {
                     if (ch == ' ')
                     {
                        tmp.Append((char)26); //space
                     }
                     else
                     {
                        tmp.Append((char)(ch - 65));
                     }
                  }
                  else
                  {
                     if (isAlphaLower(ch))
                     {
                        submode = SUBMODE_LOWER;
                        tmp.Append((char)27); //ll
                        continue;
                     }
                     else if (isMixed(ch))
                     {
                        submode = SUBMODE_MIXED;
                        tmp.Append((char)28); //ml
                        continue;
                     }
                     else
                     {
                        tmp.Append((char)29); //ps
                        tmp.Append((char)PUNCTUATION[ch]);
                        break;
                     }
                  }
                  break;
               case SUBMODE_LOWER:
                  if (isAlphaLower(ch))
                  {
                     if (ch == ' ')
                     {
                        tmp.Append((char)26); //space
                     }
                     else
                     {
                        tmp.Append((char)(ch - 97));
                     }
                  }
                  else
                  {
                     if (isAlphaUpper(ch))
                     {
                        tmp.Append((char)27); //as
                        tmp.Append((char)(ch - 65));
                        //space cannot happen here, it is also in "Lower"
                        break;
                     }
                     else if (isMixed(ch))
                     {
                        submode = SUBMODE_MIXED;
                        tmp.Append((char)28); //ml
                        continue;
                     }
                     else
                     {
                        tmp.Append((char)29); //ps
                        tmp.Append((char)PUNCTUATION[ch]);
                        break;
                     }
                  }
                  break;
               case SUBMODE_MIXED:
                  if (isMixed(ch))
                  {
                     tmp.Append((char)MIXED[ch]);
                  }
                  else
                  {
                     if (isAlphaUpper(ch))
                     {
                        submode = SUBMODE_ALPHA;
                        tmp.Append((char)28); //al
                        continue;
                     }
                     else if (isAlphaLower(ch))
                     {
                        submode = SUBMODE_LOWER;
                        tmp.Append((char)27); //ll
                        continue;
                     }
                     else
                     {
                        if (startpos + idx + 1 < count)
                        {
                           char next = msg[startpos + idx + 1];
                           if (isPunctuation(next))
                           {
                              submode = SUBMODE_PUNCTUATION;
                              tmp.Append((char)25); //pl
                              continue;
                           }
                        }
                        tmp.Append((char)29); //ps
                        tmp.Append((char)PUNCTUATION[ch]);
                     }
                  }
                  break;
               default: //SUBMODE_PUNCTUATION
                  if (isPunctuation(ch))
                  {
                     tmp.Append((char)PUNCTUATION[ch]);
                  }
                  else
                  {
                     submode = SUBMODE_ALPHA;
                     tmp.Append((char)29); //al
                     continue;
                  }
                  break;
            }
            idx++;
            if (idx >= count)
            {
               break;
            }
         }
         char h = (char)0;
         int len = tmp.Length;
         for (int i = 0; i < len; i++)
         {
            bool odd = (i % 2) != 0;
            if (odd)
            {
               h = (char)((h * 30) + tmp[i]);
               sb.Append(h);
            }
            else
            {
               h = tmp[i];
            }
         }
         if ((len % 2) != 0)
         {
            sb.Append((char)((h * 30) + 29)); //ps
         }
         return submode;
      }

      /// <summary>
      /// Encode parts of the message using Byte Compaction as described in ISO/IEC 15438:2001(E),
      /// chapter 4.4.3. The Unicode characters will be converted to binary using the cp437
      /// codepage.
      ///
      /// <param name="bytes">the message converted to a byte array</param>
      /// <param name="startpos">the start position within the message</param>
      /// <param name="count">the number of bytes to encode</param>
      /// <param name="startmode">the mode from which this method starts</param>
      /// <param name="sb">receives the encoded codewords</param>
      /// </summary>
      private static void encodeBinary(byte[] bytes,
                                       int startpos,
                                       int count,
                                       int startmode,
                                       StringBuilder sb)
      {
         if (count == 1 && startmode == TEXT_COMPACTION)
         {
            sb.Append((char)SHIFT_TO_BYTE);
         }

         int idx = startpos;
         // Encode sixpacks
         if (count >= 6)
         {
            sb.Append((char)LATCH_TO_BYTE);
            char[] chars = new char[5];
            while ((startpos + count - idx) >= 6)
            {
               long t = 0;
               for (int i = 0; i < 6; i++)
               {
                  t <<= 8;
                  t += bytes[idx + i] & 0xff;
               }
               for (int i = 0; i < 5; i++)
               {
                  chars[i] = (char)(t % 900);
                  t /= 900;
               }
               for (int i = chars.Length - 1; i >= 0; i--)
               {
                  sb.Append(chars[i]);
               }
               idx += 6;
            }
         }
         //Encode rest (remaining n<5 bytes if any)
         if (idx < startpos + count)
         {
            sb.Append((char)LATCH_TO_BYTE_PADDED);
         }
         for (int i = idx; i < startpos + count; i++)
         {
            int ch = bytes[i] & 0xff;
            sb.Append((char)ch);
         }
      }

      private static void encodeNumeric(String msg, int startpos, int count, StringBuilder sb)
      {
#if SILVERLIGHT4 || SILVERLIGHT5 || NET40 || NET45 || NETFX_CORE
         int idx = 0;
         StringBuilder tmp = new StringBuilder(count / 3 + 1);
         BigInteger num900 = new BigInteger(900);
         BigInteger num0 = new BigInteger(0);
         while (idx < count - 1)
         {
            tmp.Length = 0;
            int len = Math.Min(44, count - idx);
            String part = '1' + msg.Substring(startpos + idx, len);
#if SILVERLIGHT4 || SILVERLIGHT5
            BigInteger bigint = BigIntegerExtensions.Parse(part);
#else
            BigInteger bigint = BigInteger.Parse(part);
#endif
            do
            {
               BigInteger c = bigint % num900;
               tmp.Append((char)c);
               bigint = BigInteger.Divide(bigint, num900);
            } while (!bigint.Equals(num0));

            //Reverse temporary string
            for (int i = tmp.Length - 1; i >= 0; i--)
            {
               sb.Append(tmp[i]);
            }
            idx += len;
         }
#else
         int idx = 0;
         StringBuilder tmp = new StringBuilder(count / 3 + 1);
         BigInteger num900 = new BigInteger(900);
         BigInteger num0 = new BigInteger(0);
         while (idx < count - 1)
         {
            tmp.Length = 0;
            int len = Math.Min(44, count - idx);
            String part = '1' + msg.Substring(startpos + idx, len);
            BigInteger bigint = BigInteger.Parse(part);
            do
            {
               BigInteger c = BigInteger.Modulo(bigint, num900);
               tmp.Append((char)c.GetHashCode());
               bigint = BigInteger.Division(bigint, num900);
            } while (!bigint.Equals(num0));

            //Reverse temporary string
            for (int i = tmp.Length - 1; i >= 0; i--)
            {
               sb.Append(tmp[i]);
            }
            idx += len;
         }
#endif
      }


      private static bool isDigit(char ch)
      {
         return ch >= '0' && ch <= '9';
      }

      private static bool isAlphaUpper(char ch)
      {
         return ch == ' ' || (ch >= 'A' && ch <= 'Z');
      }

      private static bool isAlphaLower(char ch)
      {
         return ch == ' ' || (ch >= 'a' && ch <= 'z');
      }

      private static bool isMixed(char ch)
      {
         return MIXED[ch] != -1;
      }

      private static bool isPunctuation(char ch)
      {
         return PUNCTUATION[ch] != -1;
      }

      private static bool isText(char ch)
      {
         return ch == '\t' || ch == '\n' || ch == '\r' || (ch >= 32 && ch <= 126);
      }

      /// <summary>
      /// Determines the number of consecutive characters that are encodable using numeric compaction.
      ///
      /// <param name="msg">the message</param>
      /// <param name="startpos">the start position within the message</param>
      /// <returns>the requested character count</returns>
      /// </summary>
      private static int determineConsecutiveDigitCount(String msg, int startpos)
      {
         int count = 0;
         int len = msg.Length;
         int idx = startpos;
         if (idx < len)
         {
            char ch = msg[idx];
            while (isDigit(ch) && idx < len)
            {
               count++;
               idx++;
               if (idx < len)
               {
                  ch = msg[idx];
               }
            }
         }
         return count;
      }

      /// <summary>
      /// Determines the number of consecutive characters that are encodable using text compaction.
      ///
      /// <param name="msg">the message</param>
      /// <param name="startpos">the start position within the message</param>
      /// <returns>the requested character count</returns>
      /// </summary>
      private static int determineConsecutiveTextCount(String msg, int startpos)
      {
         int len = msg.Length;
         int idx = startpos;
         while (idx < len)
         {
            char ch = msg[idx];
            int numericCount = 0;
            while (numericCount < 13 && isDigit(ch) && idx < len)
            {
               numericCount++;
               idx++;
               if (idx < len)
               {
                  ch = msg[idx];
               }
            }
            if (numericCount >= 13)
            {
               return idx - startpos - numericCount;
            }
            if (numericCount > 0)
            {
               //Heuristic: All text-encodable chars or digits are binary encodable
               continue;
            }
            ch = msg[idx];

            //Check if character is encodable
            if (!isText(ch))
            {
               break;
            }
            idx++;
         }
         return idx - startpos;
      }

      /// <summary>
      /// Determines the number of consecutive characters that are encodable using binary compaction.
      ///
      /// <param name="msg">the message</param>
      /// <param name="bytes">the message converted to a byte array</param>
      /// <param name="startpos">the start position within the message</param>
      /// <returns>the requested character count</returns>
      /// </summary>
      private static int determineConsecutiveBinaryCount(String msg, byte[] bytes, int startpos)
      {
         int len = msg.Length;
         int idx = startpos;
         while (idx < len)
         {
            char ch = msg[idx];
            int numericCount = 0;

            while (numericCount < 13 && isDigit(ch))
            {
               numericCount++;
               //textCount++;
               int i = idx + numericCount;
               if (i >= len)
               {
                  break;
               }
               ch = msg[i];
            }
            if (numericCount >= 13)
            {
               return idx - startpos;
            }
            int textCount = 0;
            while (textCount < 5 && isText(ch))
            {
               textCount++;
               int i = idx + textCount;
               if (i >= len)
               {
                  break;
               }
               ch = msg[i];
            }
            if (textCount >= 5)
            {
               return idx - startpos;
            }
            ch = msg[idx];

            //Check if character is encodable
            //Sun returns a ASCII 63 (?) for a character that cannot be mapped. Let's hope all
            //other VMs do the same
            if (bytes[idx] == 63 && ch != '?')
            {
               throw new WriterException("Non-encodable character detected: " + ch + " (Unicode: " + (int)ch + ')');
            }
            idx++;
         }
         return idx - startpos;
      }
   }
}
