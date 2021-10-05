/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.qrcode.encoder;

import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.Version;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;

/** Encoder that encodes minimally
 *
 * Version selection:
 * The version can be preset in the constructor. If it isn't specified then the algorithm will compute three solutions for the three different version classes 1-9, 10-26 and 27-40.
 *
 * It is not clear to me if ever a solution using for example Medium (Versions 10-26) could be smaller than a Small solution (Versions 1-9) (proof for or against would be nice to have).
 * With hypothetical values for the number of length bits, the number of bits per mode and the number of bits per encoded character it can be shown that it can happen at all as follows:
 * We hypothetically assume that a mode is encoded using 1 bit (instead of 4) and a character is encoded in BYTE mode using 2 bit (instead of 8). Using these values we now attempt to encode the 
 * four characters "1234".
 * If we furthermore assume that in Version 1-9 the length field has 1 bit length so that it can encode up to 2 characters and that in Version 10-26 it has 2 bits length so that we can encode up
 * to 2 characters then it is more efficient to encode with Version 10-26 than with Version 1-9 as shown below:
 *
 * Number of length bits small version (1-9): 1
 * Number of length bits large version (10-26): 2
 * Number of bits per mode item: 1
 * Number of bits per character item: 2
 * BYTE(1,2),BYTE(3,4): 1+1+2+2,1+1+2+2=12 bits
 * BYTE(1,2,3,4): 1+2+2+2+2+2          =11 bits
 *
 * If we however change the capacity of the large encoding from 2 bit to 4 bit so that it potentially can encode 16 items, then it is more efficient to encode using the small encoding
 * as shown below:
 *
 * Number of length bits small version (1-9): 1
 * Number of length bits large version (10-26): 4
 * Number of bits per mode item: 1
 * Number of bits per character item: 2
 * BYTE(1,2),BYTE(3,4): 1+1+2+2,1+1+2+2=12 bits
 * BYTE(1,2,3,4): 1+4+2+2+2+2          =13 bits
 *
 * But as mentioned, it is not clear to me if this can ever happen with the actual values.
 *
 * ECI switching:
 *
 * In multi language content the algorithm selects the most compact representation using ECI modes. For example the
 * it is more compactly represented using one ECI to UTF-8 rather than two ECIs to ISO-8859-6 and ISO-8859-1 if the text contains more ASCII characters (since they are represented as
 * one byte sequence) as opposed to the case where there are proportionally more Arabic characters that require two bytes in UTF-8 and only one in ISO-8859-6.
 *
 * @author Alex Geller
 */
final class MinimalEncoder {

  static final boolean DEBUG = false;

  private enum VersionSize {
    SMALL("Version 1-9)"),
    MEDIUM("Version 10-26)"),
    LARGE("Version 27-40");

    String description;

    VersionSize(String description) {
        this.description = description;
    }

    public String toString() {
        return description;
    }
  }

  private String stringToEncode;
  private Version version = null;
  private boolean isGS1 = false;
  private CharsetEncoder[] encoders;
    

/**Encoding is optional (default ISO-8859-1) and version is optional (minimal version is computed if not specified*/
  MinimalEncoder(String stringToEncode,Version version,boolean isGS1) {
    this.stringToEncode = stringToEncode;
    if (version != null) {
      this.version = version;
    }
    this.isGS1 = isGS1;
    CharsetEncoder[] isoEncoders = new CharsetEncoder[15];
    isoEncoders[0] = StandardCharsets.ISO_8859_1.newEncoder();
    for (int i = 0; i < stringToEncode.length(); i++) {
      int cnt = 0;
      int j;
      for (j = 0; j < 15; j++) {
        if (isoEncoders[j] != null) {
          cnt++;
          if (isoEncoders[j].canEncode(stringToEncode.charAt(i))) {
            break;
          }
        }
      }
      if (cnt == 14) { //we need all. Can stop looking further.
        break;
      }
      if (j >= 15) { //no encoder found 
        for (j = 0; j < 15; j++) {
          if (j != 11 && isoEncoders[j] == null) { // ISO-8859-12 doesn't exist
            try {
              CharsetEncoder ce = Charset.forName("ISO-8859-" + (j + 1)).newEncoder();
              if (ce.canEncode(stringToEncode.charAt(i))) {
                isoEncoders[j] = ce;
                break;
              }
            } catch (UnsupportedCharsetException e) { }
          }
        }
      }
    }
    int numberOfEncoders = 0;
    for (int j = 0; j < 15; j++) {
      if (isoEncoders[j] != null) {
        numberOfEncoders++;
      }
    }
    if (numberOfEncoders == 1) {
      encoders = new CharsetEncoder[1];
      encoders[0] = isoEncoders[0];
    } else {
      encoders = new CharsetEncoder[numberOfEncoders + 2];
      int index = 0;
      for (int j = 0; j < 15; j++) {
        if (isoEncoders[j] != null) {
          encoders[index++] = isoEncoders[j];
        }
      }
      encoders[index++] = StandardCharsets.UTF_8.newEncoder();
      encoders[index++] = StandardCharsets.UTF_16BE.newEncoder();
    }
  }
  static ResultNode encode(String stringToEncode,Version version,boolean isGS1) throws WriterException {
    return new MinimalEncoder(stringToEncode,version,isGS1).encode();
  }
  ResultNode encode() throws WriterException {
    if (version == null) { //compute minimal encoding trying the three version sizes.
      ResultNode[] results = {encode(getVersion(VersionSize.SMALL)),
                              encode(getVersion(VersionSize.MEDIUM)),
                              encode(getVersion(VersionSize.LARGE))};
      return postProcess(smallest(results));
    } else { //compute minimal encoding for a given version
      return postProcess(encode(version));
    }
  }

  static VersionSize getVersionSize(Version version) {
    return version.getVersionNumber() <= 9 ? VersionSize.SMALL : version.getVersionNumber() <= 26 ? VersionSize.MEDIUM : VersionSize.LARGE;
  }

  static Version getVersion(VersionSize versionSize) {
    switch (versionSize) {
      case SMALL: return Version.getVersionForNumber(9);
      case MEDIUM: return Version.getVersionForNumber(26);
      case LARGE: 
      default: return Version.getVersionForNumber(40);
    }
  }

  static boolean isNumeric(char c) {
    return c >= '0' && c <= '9';
  }

  static boolean isDoubleByteKanji(char c) {
    return Encoder.isOnlyDoubleByteKanji("" + c);
  }

  static boolean isAlphanumeric(char c) {
    return Encoder.getAlphanumericCode(c) != -1;
  }

/** Example: to encode alphanumerically at least 2 characters are needed (5.5 bits per character). Similarily three digits are needed to encode numerically (3+1/3 bits per digit)*/
  static int getEncodingGranularity(Mode mode) {
    switch (mode) {
      case KANJI: return 1;
      case ALPHANUMERIC: return 2;
      case NUMERIC: return 3;
      case BYTE: return 1;
      default:
        return 0;
    }
  }

/** Example: to encode alphanumerically 11 bits are used per 2 characters. Similarily 10 bits are used to encode 3 numeric digits.*/
  static int getBitsPerEncodingUnit(Mode mode) {
    switch (mode) {
      case KANJI: return 16;
      case ALPHANUMERIC: return 11;
      case NUMERIC: return 10;
      case BYTE: return 8;
      case ECI:
      default:
        return 0;
    }
  }

/** Returns the maximum number of encodeable characters in the given mode for the given version. Example: in Version 1, 2^10 digits or 2^8 bytes can be encoded. In Version 3 it is 2^14 digits and 2^16 bytes*/
  static int getMaximumNumberOfEncodeableCharacters(Version version,Mode mode) {
    int count = mode.getCharacterCountBits(version);
    return count == 0 ? 0 : 1 << count;
  }
  static int getMaximumNumberOfEncodeableCharacters(VersionSize versionSize,Mode mode) {
    return getMaximumNumberOfEncodeableCharacters(getVersion(versionSize),mode);
  }

  boolean canEncode(Mode mode,char c) {
    switch (mode) {
      case KANJI: return isDoubleByteKanji(c) ;
      case ALPHANUMERIC: return isAlphanumeric(c) ;
      case NUMERIC: return isNumeric(c) ;
      case BYTE: return true; //any character can be encoded as byte(s). Up to the caller to manage splitting into multiple bytes when String.getBytes(Charset) return more than one byte.
      default:
        return false;
    }
  }

  static int getCompactedOrdinal(Mode mode) {
    if (mode == null) {
      return 0;
    }
    switch (mode) {
      case KANJI: return 0;
      case ALPHANUMERIC: return 1;
      case NUMERIC: return 2;
      case ECI:
      case BYTE: return 3;
      default:
        assert false;
        return -1;
    }
  }

  static ResultNode smallest(ResultNode[] results) {
    ResultNode smallestResult = null;
    for (int i = 0; i < results.length; i++) {
      if (smallestResult == null || (results[i] != null && results[i].getSize() < smallestResult.getSize())) {
        smallestResult = results[i];
      }
    }
    return smallestResult;
  }

  static ResultNode smallest(Vector<ResultNode> results) {
    ResultNode smallestResult = null;
    for (int i = 0; i < results.size(); i++) {
      if (smallestResult == null || (results.get(i) != null && results.get(i).getSize() < smallestResult.getSize())) {
        smallestResult = results.get(i);
      }
    }
    return smallestResult;
  }

  ResultNode postProcess(ResultNode result) {
    if (isGS1) {
      if (result.mode != Mode.ECI) {
        ResultNode current = result.next;
        while (current != null && current.mode != Mode.ECI) {
          current = current.next;
        }
        if (current != null) { // there is an ECI somewhere
          //prepend a default character set ECI
          result = new ResultNode(Mode.ECI,result.version,true,0,0,result);
        }
      }
      if (result.mode != Mode.ECI) {
        //prepend a FNC1_FIRST_POSITION
        result = new ResultNode(Mode.FNC1_FIRST_POSITION,result.version,true,0,0,result);
      } else {
        //insert a FNC1_FIRST_POSITION after the ECI
        result.next = new ResultNode(Mode.FNC1_FIRST_POSITION,result.version,true,0,0,result.next);
      }
    }
    ResultNode current = result;
    while (current.next != null) {
      current = current.next;
    }
    //Add TERMINATOR according to "8.4.8 Terminator"
    current.next = new ResultNode(Mode.TERMINATOR,result.version,true,stringToEncode.length(),result.charsetEncoderIndex,null);
    return result;
  }
    
  void addEdge(Vector<ResultNode>[][][] vertices,ResultNode rn,ResultNode previous) {
    int vertexIndex = rn.position + (rn.mode == Mode.ECI ? 1 : getEncodingGranularity(rn.mode));
    if (vertices[vertexIndex][rn.charsetEncoderIndex][getCompactedOrdinal(rn.mode)] == null) {
      vertices[vertexIndex][rn.charsetEncoderIndex][getCompactedOrdinal(rn.mode)] = new Vector<ResultNode>();
    }
    vertices[vertexIndex][rn.charsetEncoderIndex][getCompactedOrdinal(rn.mode)].add(rn);
    rn = rn.getHead();
    rn.setPrevious(previous);

    if (DEBUG) {
      if (rn.previous == null) {
        System.err.println("DEBUG adding edge " + rn + " from " + rn.position + " to " + vertexIndex + " with an accumulated size of " + rn.getSize(true));
      } else {
        System.err.println("DEBUG adding edge " + rn + " from " + vertexToString(previous.position,previous) + " to " + vertexToString(vertexIndex,rn) + " with an accumulated size of " + rn.getSize(true));
      }
    }
  }

  void addEdges(Version version,Vector<ResultNode>[][][] vertices,int from,ResultNode previous) {
    for (int i = 0; i < encoders.length; i++) {
      if (encoders[i].canEncode(stringToEncode.charAt(from))) {
        ResultNode edge = new ResultNode(Mode.BYTE,version,true,from,i,null);
        boolean needECI = (previous == null && i > 0) ||
                          (previous != null && previous.charsetEncoderIndex != i);
        if (needECI) {
          ResultNode eci = new ResultNode(Mode.ECI,version,true,from,i,null);
          edge.setPrevious(eci);
        }
        addEdge(vertices,edge,previous);
      }
    }
    if (canEncode(Mode.KANJI,stringToEncode.charAt(from))) {
      addEdge(vertices,new ResultNode(Mode.KANJI,version,true,from,0,null),previous);
    }
    int inputLength = stringToEncode.length();
    if (from + 1 < inputLength && canEncode(Mode.ALPHANUMERIC,stringToEncode.charAt(from)) && canEncode(Mode.ALPHANUMERIC, stringToEncode.charAt(from + 1))) {
      addEdge(vertices,new ResultNode(Mode.ALPHANUMERIC,version,true,from,0,null),previous);
    }
    if (from + 2 < inputLength && canEncode(Mode.NUMERIC,stringToEncode.charAt(from)) && canEncode(Mode.NUMERIC, stringToEncode.charAt(from + 1)) && canEncode(Mode.NUMERIC, stringToEncode.charAt(from + 2))) {
      addEdge(vertices,new ResultNode(Mode.NUMERIC,version,true,from,0,null),previous);
    }
  }

/** used for debugging*/
  static String edgeToString(ResultNode rn) {
    String result = rn.toString();
    ResultNode current = rn.previous;
    while (current != null) {
      result = current.toString() + "," + result;
      current = current.previous;
    }
    return result;
  }

/** used for debugging*/
  String vertexToString(int position, ResultNode rn) {
    return (position >= stringToEncode.length() ? "end vertex" : "vertex for character '" + stringToEncode.charAt(position) + "' at position " + position) + " with encoding " + encoders[rn.charsetEncoderIndex].charset().name() + " and mode " + rn.mode;
  }
/** used for debugging*/
  void printEdges(Vector<ResultNode>[][][] vertices) {
    boolean willHaveECI = encoders.length > 1;
    int inputLength = stringToEncode.length();
    for (int i = 1; i <= inputLength; i++) {
      for (int j = 0; j < encoders.length; j++) {
        for (int k = 0; k < 4; k++) {
          if (vertices[i][j][k] != null) {
            Vector<ResultNode> edges = vertices[i][j][k];
            assert edges.size() > 0;
            if (edges.size() > 0) {
              ResultNode rn = edges.get(0);
              String vertexKey = "" + i + "_" + rn.mode + (willHaveECI ? "_" + encoders[rn.charsetEncoderIndex].charset().name() : "");
              int fromPosition = rn.position;
              ResultNode previous = rn.previous == null ? null : rn.previous.mode == Mode.ECI ? rn.previous.previous : rn.previous;
              String fromKEY = previous == null ? "initial" : "" + fromPosition + "_" + previous.mode + (willHaveECI ? "_" + encoders[previous.charsetEncoderIndex].charset().name() : "");
              int toPosition = fromPosition + getEncodingGranularity(rn.mode);
              System.err.println("DEBUG: (" + fromKEY + ") -- " + rn.mode + (toPosition - fromPosition > 0 ? "(" + stringToEncode.substring(fromPosition, toPosition) + ")" : "") + " (" + rn.getSize(true) + ")" + " --> " + "(" + vertexKey + ")");
            }
          }
        }
      }
    }
  }

/** encodes minimally using Dijkstra.*/
  ResultNode encode(Version version) throws WriterException {
/* A vertex represents a tuple a character in the input, a mode and an encoding. An edge leading to such a vertex encodes the character left of the one that the
 * vertex represents and encodes it in the same encoding and mode as the vertex.
 * The edges leading to a vertex are stored in such a way that there is a fast way to enumerate the edges ending on a particular vertex.
 * The algorithm processes the input string from left to right.
 * For every vertex repesenting the character to the left of the current character, the algorithm enumerates the edges ending on the vertex and removes all but the shortest from that list.
 * Then it creates a vertex for the current character and computes all possible outgoing edges.
 * Examples:
 * The process is illustrated by showing the graph (edges) after each iteration from left to right over the input: 
 * An edge is drawn as follows "(" + fromVertex + ") -- " + encodingMode + "(" + encodedInput + ") (" + accumulatedSize + ") --> (" + toVertex + ")"
 *
 * Example 1 encoding the string "ABCDE":
 *
 * Initial situation
 * (initial) -- BYTE(A) (20) --> (1_BYTE)
 * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
 * 
 * Situation after adding edges to vertices at position 1
 * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE)
                               * (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC)
 * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
 * 
 * Situation after adding edges to vertices at position 2
 * (initial) -- BYTE(A) (20) --> (1_BYTE)
 * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
 * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE)
                               * (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC)
 * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- BYTE(C) (44) --> (3_BYTE)
                                                            * (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
 * 
 * Situation after adding edges to vertices at position 3
 * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE)
                               * (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- BYTE(D) (64) --> (4_BYTE)
                                                                                                 * (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
 * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
                                                            * (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
 * 
 * Situation after adding edges to vertices at position 4
 * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE) -- BYTE(D)         (44) --> (4_BYTE)
                               * (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
 * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC) -- BYTE(E) (55) --> (5_BYTE)
 * 
 * Situation after adding edges to vertices at position 5
 * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE) -- BYTE(D)         (44) --> (4_BYTE) -- BYTE(E)         (52) --> (5_BYTE)
                               * (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
 * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
 *
 * Encoding as BYTE(ABCDE) has the smallest size of 52 and is hence chosen. The encodation ALPHANUMERIC(ABCD), BYTE(E) is longer with a size of 55.
 *
 * Example 2 encoding the string "XXYY" where X denotes a character unique to character set ISO-8859-2 and Y a character unique to ISO-8859-3. Both characters encode as double byte in UTF-8:
 *
 * Initial situation
 * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2)
 * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8)
 * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE)
 * 
 * Situation after adding edges to vertices at position 1
 * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2)
                               * (1_BYTE_ISO-8859-2) -- BYTE(X) (72) --> (2_BYTE_UTF-8)
                               * (1_BYTE_ISO-8859-2) -- BYTE(X) (72) --> (2_BYTE_UTF-16BE)
 * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8)
 * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE)
 * 
 * Situation after adding edges to vertices at position 2
 * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2)
                                                                       * (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3)
                                                                       * (2_BYTE_ISO-8859-2) -- BYTE(Y) (80) --> (3_BYTE_UTF-8)
                                                                       * (2_BYTE_ISO-8859-2) -- BYTE(Y) (80) --> (3_BYTE_UTF-16BE)
 * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8)
 * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE)
 * 
 * Situation after adding edges to vertices at position 3
 * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3)
                                                                                                               * (3_BYTE_ISO-8859-3) -- BYTE(Y) (80) --> (4_BYTE_ISO-8859-3)
                                                                                                               * (3_BYTE_ISO-8859-3) -- BYTE(Y) (112) --> (4_BYTE_UTF-8)
                                                                                                               * (3_BYTE_ISO-8859-3) -- BYTE(Y) (112) --> (4_BYTE_UTF-16BE)
 * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8) -- BYTE(Y) (72) --> (3_BYTE_UTF-8)
 * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE) -- BYTE(Y) (72) --> (3_BYTE_UTF-16BE)
 * 
 * Situation after adding edges to vertices at position 4
 * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3) -- BYTE(Y) (80) --> (4_BYTE_ISO-8859-3)
                                                                                                               * (3_BYTE_UTF-8) -- BYTE(Y) (88) --> (4_BYTE_UTF-8)
                                                                                                               * (3_BYTE_UTF-16BE) -- BYTE(Y) (88) --> (4_BYTE_UTF-16BE)
 * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8) -- BYTE(Y) (72) --> (3_BYTE_UTF-8)
 * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE) -- BYTE(Y) (72) --> (3_BYTE_UTF-16BE)
 *
 * Encoding as ECI(ISO-8859-2),BYTE(XX),ECI(ISO-8859-3),BYTE(YY) has the smallest size of 80 and is hence chosen. The encodation ECI(UTF-8),BYTE(XXYY) is longer with a size of 88.
 */
    int inputLength = stringToEncode.length();
//Array that represents vertices. There is a vertex for every character, encoding and mode. The vertex contains a list of
//all edges that lead to it that have the same encoding and mode.
//The lists are created lazily
    Vector<ResultNode>[][][] vertices = new Vector[inputLength + 1][encoders.length][4];
    addEdges(version,vertices,0,null);
    if (DEBUG) {
        System.err.println("DEBUG Initial situation");
        printEdges(vertices);
    }
    for (int i = 1; i <= inputLength; i++) {
      for (int j = 0; j < encoders.length; j++) {
        for (int k = 0; k < 4; k++) {
          ResultNode minimalEdge = null;
          if (vertices[i][j][k] != null) {
            Vector<ResultNode> edges = vertices[i][j][k];
            if (edges.size() == 1) { //Optimization: if there is only one edge then that one is the minimal one
              minimalEdge = edges.get(0);
            } else {
              int minimalIndex = -1;
              int minimalSize = Integer.MAX_VALUE;
              for (int l = 0; l < edges.size(); l++) {
                ResultNode rn = edges.get(l);
                if (rn.getSize(true) < minimalSize) {
                  minimalIndex = l;
                  minimalSize = rn.getSize(true);
                }
              }
              assert minimalIndex != -1;
              minimalEdge = edges.get(minimalIndex);
              edges.clear();
              edges.add(minimalEdge);
            }
            if (i < inputLength) {
              assert minimalEdge != null;
              if (DEBUG && minimalEdge != null) {
                System.err.println("DEBUG processing " + vertexToString(i,minimalEdge) + ". The minimal edge leading to this vertex is " + edgeToString(minimalEdge) + " with a size of " + minimalEdge.getSize(true));
              }
              addEdges(version,vertices,i,minimalEdge);
            }
          }
        }
      }
      if (DEBUG) {
        System.err.println("DEBUG situation after adding edges to vertices at position " + i);
        printEdges(vertices);
      }
    }
    int minimalJ = -1;
    int minimalK = -1;
    int minimalSize = Integer.MAX_VALUE;
    for (int j = 0; j < encoders.length; j++) {
      for (int k = 0; k < 4; k++) {
        if (vertices[inputLength][j][k] != null) {
          Vector<ResultNode> edges = vertices[inputLength][j][k];
          assert edges.size() == 1;
          ResultNode result = edges.get(0);
          if (result.getSize(true) < minimalSize) {
            minimalSize = result.getSize(true);
            minimalJ = j;
            minimalK = k;
          }
        }
      }
    }
    assert minimalJ != -1;
    if (minimalJ >= 0) {
      ResultNode result = vertices[inputLength][minimalJ][minimalK].get(0);
      while (result.previous != null) {
        result.previous.append(result);
        result = result.previous;
      }
      return result;
    } else {
//TODO: we get here if the input contains the replacement character \uFFFD (357 277 275).
        throw new WriterException("Internal error: failed to encode");
    }
  }
    
  byte[] getBytesOfCharacter(int position,int charsetEncoderIndex) {
    //TODO: Is there a more efficient way for a single character?
    return stringToEncode.substring(position,position + 1).getBytes(encoders[charsetEncoderIndex].charset());
 }

  class ResultNode {
    Mode mode;
    Version version;
    boolean declaresMode;
    int position;
    int charsetEncoderIndex;
    ResultNode next;
    ResultNode previous;
    ResultNode(Mode mode,Version version,boolean declaresMode,int position,int charsetEncoderIndex,ResultNode next) {
      assert mode != null;
      this.mode = mode;
      this.version = version;
      this.declaresMode = declaresMode;
      this.position = position;
      this.charsetEncoderIndex = charsetEncoderIndex;
      this.next = next;
    }

    ResultNode getHead() {
      ResultNode current = this;
      while (current.previous != null) {
        current = current.previous;
      }
      return current;
    }
 
    void setPrevious(ResultNode previous) {
      this.previous = previous;
      declaresMode = true;
      if (previous != null) {
//TODO: Verify that it is correct to call getLength() (character count) here instead of using number of bytes for a BYTE mode depending on the character set.
        if (previous.mode == mode && mode != Mode.ECI && previous.getLength(true) + getLength(true) < getMaximumNumberOfEncodeableCharacters(version,mode)) {
          declaresMode = false;
        }
      }
    }

    int getSize(boolean walkPrevious) {
      int result = 0;
      ResultNode current = this;
      if (walkPrevious) {
        while (current != null) {
          result += current.getLocalSize();
          current = current.previous;
        }
      } else {
        while (current != null) {
          result += current.getLocalSize();
          current = current.next;
        }
      }
      return result;
    }

    int getLength(boolean walkPrevious) {
      int result = 0;
      ResultNode current = this;
      if (walkPrevious) {
        while (current != null) {
          result += current.getLocalLength();
          if (current.declaresMode) {
            break;
          }
          current = current.previous;
        }
      } else {
        result = current.getLocalLength();
        current = current.next;
        while (current != null && !current.declaresMode) {
          result += current.getLocalLength();
          current = current.next;
        }
      }
      return result;
    }

/** returns the size of this one encoding unit in bits*/
    int getLocalSize() {
      int size = declaresMode ? 4 + mode.getCharacterCountBits(version) : 0;
      if (mode == Mode.ECI) {
        size += 8; // the ECI assignment numbers for ISO-8859-x, UTF-8 and UTF-16 are all 8 bit long
      } else if (mode == Mode.BYTE) {
        size += 8 * getBytesOfCharacter(position,charsetEncoderIndex).length;
      } else {
        size += getBitsPerEncodingUnit(mode);
      }
      return size;
    }

/** returns the size in bits*/
    int getSize() {
      return getSize(false);
    }

/** returns the length of this one uncoding unit in encoding units*/
    int getLocalLength() {
      return getBitsPerEncodingUnit(mode) == 0 ? 0 : 1;
    }

/** returns the length in encoding units*/
    int getLength() {
      return getLength(false);
    }

    public void getBits(BitArray bits) throws WriterException {
      ResultNode next = getLocalBits(bits);
      while (next != null) {
        next = next.getLocalBits(bits);
      }
    }
    ResultNode getLocalBits(BitArray bits) throws WriterException {
      // append mode
      bits.appendBits(mode.getBits(),4);
      if (mode == Mode.ECI) {
        String canonicalCharsetName = encoders[charsetEncoderIndex].charset().name();
        bits.appendBits(CharacterSetECI.getCharacterSetECIByName(canonicalCharsetName).getValue(),8);
        return next;
      } else {
        int characterLength = getLength() * getEncodingGranularity(mode);
        if (characterLength > 0) {
          String canonicalCharsetName = encoders[charsetEncoderIndex].charset().name();
          String pieceToEncode = stringToEncode.substring(position,position + characterLength);
          // append length
          try {
            bits.appendBits(mode == Mode.BYTE ? pieceToEncode.getBytes(canonicalCharsetName).length : characterLength,mode.getCharacterCountBits(version));
          } catch (UnsupportedEncodingException uee) {
            throw new WriterException(uee);
          }
          // append data
          Encoder.appendBytes(pieceToEncode,mode,bits,encoders[charsetEncoderIndex].charset());
          ResultNode current = next;
          while (current != null && !current.declaresMode) {
            current = current.next;
          }
          return current;
        } else {
          return next;
        }
      } 
    }

    public Version getVersion(ErrorCorrectionLevel ecLevel) {
      int versionNumber = version.getVersionNumber();
      int lowerLimit;
      int upperLimit;
      switch (getVersionSize(version)) {
        case SMALL:
          lowerLimit = 1;
          upperLimit = 9;
          break;
        case MEDIUM:
          lowerLimit = 10;
          upperLimit = 26;
          break;
        case LARGE:
        default:
          lowerLimit = 27;
          upperLimit = 40;
          break;
     }
 //increase version if needed
      while (versionNumber < upperLimit && !Encoder.willFit(getSize(), Version.getVersionForNumber(versionNumber), ecLevel)) {
        versionNumber++;
      }
//shrink version if possible
      while (versionNumber > lowerLimit && Encoder.willFit(getSize(), Version.getVersionForNumber(versionNumber - 1), ecLevel)) {
        versionNumber--;
      }
      return Version.getVersionForNumber(versionNumber);
    }

    void append(ResultNode rn) {
      ResultNode current = this;
      while (current.next != null) {
        current = current.next;
      }
      current.next = rn;
    }

//TODO: change this method to be iterative
    public String toString() {
      String result = "";
      if (declaresMode) {
        result += mode + "(";
      }
      if (mode == Mode.ECI) {
        result += encoders[charsetEncoderIndex].charset().displayName();
      } else {
        result += makePrintable(stringToEncode.substring(position,position + getEncodingGranularity(mode)));
      }
      if (next != null) {
        result += (next.declaresMode ? ")," : ",") + next.toString();
      } else {
        result += ")";
      }
      return result;
    }

    String makePrintable(String s) {
      String result = "";
      for (int i = 0; i < s.length(); i++) {
        if (s.charAt(i) < 32 || s.charAt(i) > 126) {
          result += ".";
        } else {
          result += s.charAt(i);
        }
      }
      return result;
    }
  }
}
