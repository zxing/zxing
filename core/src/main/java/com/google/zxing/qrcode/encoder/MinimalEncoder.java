/*
 * Copyright 2021 ZXing authors
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
import com.google.zxing.common.ECIEncoderSet;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


/**
 * Encoder that encodes minimally
 *
 * Algorithm:
 *
 * The eleventh commandment was "Thou Shalt Compute" or "Thou Shalt Not Compute" - I forget which (Alan Perilis).
 *
 * This implementation computes. As an alternative, the QR-Code specification suggests heuristics like this one:
 *
 * If initial input data is in the exclusive subset of the Alphanumeric character set AND if there are less than
 * [6,7,8] characters followed by data from the remainder of the 8-bit byte character set, THEN select the 8-
 * bit byte mode ELSE select Alphanumeric mode;
 *
 * This is probably right for 99.99% of cases but there is at least this one counter example: The string "AAAAAAa"
 * encodes 2 bits smaller as ALPHANUMERIC(AAAAAA), BYTE(a) than by encoding it as BYTE(AAAAAAa).
 * Perhaps that is the only counter example but without having proof, it remains unclear.
 *
 * ECI switching:
 *
 * In multi language content the algorithm selects the most compact representation using ECI modes.
 * For example the most compact representation of the string "\u0150\u015C" (O-double-acute, S-circumflex) is
 * ECI(UTF-8), BYTE(\u0150\u015C) while prepending one or more times the same leading character as in
 * "\u0150\u0150\u015C", the most compact representation uses two ECIs so that the string is encoded as
 * ECI(ISO-8859-2), BYTE(\u0150\u0150), ECI(ISO-8859-3), BYTE(\u015C).
 *
 * @author Alex Geller
 */
final class MinimalEncoder {

  private enum VersionSize {
    SMALL("version 1-9"),
    MEDIUM("version 10-26"),
    LARGE("version 27-40");

    private final String description;

    VersionSize(String description) {
      this.description = description;
    }

    public String toString() {
      return description;
    }
  }

  private final String stringToEncode;
  private final boolean isGS1;
  private final ECIEncoderSet encoders;
  private final ErrorCorrectionLevel ecLevel;

  /**
   * Creates a MinimalEncoder
   *
   * @param stringToEncode The string to encode
   * @param priorityCharset The preferred {@link Charset}. When the value of the argument is null, the algorithm
   *   chooses charsets that leads to a minimal representation. Otherwise the algorithm will use the priority
   *   charset to encode any character in the input that can be encoded by it if the charset is among the
   *   supported charsets.
   * @param isGS1 {@code true} if a FNC1 is to be prepended; {@code false} otherwise
   * @param ecLevel The error correction level.
   * @see ResultList#getVersion
   */
  MinimalEncoder(String stringToEncode, Charset priorityCharset, boolean isGS1, ErrorCorrectionLevel ecLevel) {
    this.stringToEncode = stringToEncode;
    this.isGS1 = isGS1;
    this.encoders = new ECIEncoderSet(stringToEncode, priorityCharset, -1);
    this.ecLevel = ecLevel;
  }

  /**
   * Encodes the string minimally
   *
   * @param stringToEncode The string to encode
   * @param version The preferred {@link Version}. A minimal version is computed (see
   *   {@link ResultList#getVersion method} when the value of the argument is null
   * @param priorityCharset The preferred {@link Charset}. When the value of the argument is null, the algorithm
   *   chooses charsets that leads to a minimal representation. Otherwise the algorithm will use the priority
   *   charset to encode any character in the input that can be encoded by it if the charset is among the
   *   supported charsets.
   * @param isGS1 {@code true} if a FNC1 is to be prepended; {@code false} otherwise
   * @param ecLevel The error correction level.
   * @return An instance of {@code ResultList} representing the minimal solution.
   * @see ResultList#getBits
   * @see ResultList#getVersion
   * @see ResultList#getSize
   */
  static ResultList encode(String stringToEncode, Version version, Charset priorityCharset, boolean isGS1,
      ErrorCorrectionLevel ecLevel) throws WriterException {
    return new MinimalEncoder(stringToEncode, priorityCharset, isGS1, ecLevel).encode(version);
  }

  ResultList encode(Version version) throws WriterException {
    if (version == null) { // compute minimal encoding trying the three version sizes.
      Version[] versions = { getVersion(VersionSize.SMALL),
                             getVersion(VersionSize.MEDIUM),
                             getVersion(VersionSize.LARGE) };
      ResultList[] results = { encodeSpecificVersion(versions[0]),
                               encodeSpecificVersion(versions[1]),
                               encodeSpecificVersion(versions[2]) };
      int smallestSize = Integer.MAX_VALUE;
      int smallestResult = -1;
      for (int i = 0; i < 3; i++) {
        int size = results[i].getSize();
        if (Encoder.willFit(size, versions[i], ecLevel) && size < smallestSize) {
          smallestSize = size;
          smallestResult = i;
        }
      }
      if (smallestResult < 0) {
        throw new WriterException("Data too big for any version");
      }
      return results[smallestResult];
    } else { // compute minimal encoding for a given version
      ResultList result = encodeSpecificVersion(version);
      if (!Encoder.willFit(result.getSize(), getVersion(getVersionSize(result.getVersion())), ecLevel)) {
        throw new WriterException("Data too big for version" + version);
      }
      return result;
    }
  }

  static VersionSize getVersionSize(Version version) {
    return version.getVersionNumber() <= 9 ? VersionSize.SMALL : version.getVersionNumber() <= 26 ?
      VersionSize.MEDIUM : VersionSize.LARGE;
  }

  static Version getVersion(VersionSize versionSize) {
    switch (versionSize) {
      case SMALL:
        return Version.getVersionForNumber(9);
      case MEDIUM:
        return Version.getVersionForNumber(26);
      case LARGE:
      default:
        return Version.getVersionForNumber(40);
    }
  }

  static boolean isNumeric(char c) {
    return c >= '0' && c <= '9';
  }

  static boolean isDoubleByteKanji(char c) {
    return Encoder.isOnlyDoubleByteKanji(String.valueOf(c));
  }

  static boolean isAlphanumeric(char c) {
    return Encoder.getAlphanumericCode(c) != -1;
  }

  boolean canEncode(Mode mode, char c) {
    switch (mode) {
      case KANJI: return isDoubleByteKanji(c);
      case ALPHANUMERIC: return isAlphanumeric(c);
      case NUMERIC: return isNumeric(c);
      case BYTE: return true; // any character can be encoded as byte(s). Up to the caller to manage splitting into
                              // multiple bytes when String.getBytes(Charset) return more than one byte.
      default:
        return false;
    }
  }

  static int getCompactedOrdinal(Mode mode) {
    if (mode == null) {
      return 0;
    }
    switch (mode) {
      case KANJI:
        return 0;
      case ALPHANUMERIC:
        return 1;
      case NUMERIC:
        return 2;
      case BYTE:
        return 3;
      default:
        throw new IllegalStateException("Illegal mode " + mode);
    }
  }

  void addEdge(Edge[][][] edges, int position, Edge edge) {
    int vertexIndex = position + edge.characterLength;
    Edge[] modeEdges = edges[vertexIndex][edge.charsetEncoderIndex];
    int modeOrdinal = getCompactedOrdinal(edge.mode);
    if (modeEdges[modeOrdinal] == null || modeEdges[modeOrdinal].cachedTotalSize > edge.cachedTotalSize) {
      modeEdges[modeOrdinal] = edge;
    }
  }

  void addEdges(Version version, Edge[][][] edges, int from, Edge previous) {
    int start = 0;
    int end = encoders.length();
    int priorityEncoderIndex = encoders.getPriorityEncoderIndex();
    if (priorityEncoderIndex >= 0 && encoders.canEncode(stringToEncode.charAt(from),priorityEncoderIndex)) {
      start = priorityEncoderIndex;
      end = priorityEncoderIndex + 1;
    }

    for (int i = start; i < end; i++) {
      if (encoders.canEncode(stringToEncode.charAt(from), i)) {
        addEdge(edges, from, new Edge(Mode.BYTE, from, i, 1, previous, version));
      }
    }

    if (canEncode(Mode.KANJI, stringToEncode.charAt(from))) {
      addEdge(edges, from, new Edge(Mode.KANJI, from, 0, 1, previous, version));
    }

    int inputLength = stringToEncode.length();
    if (canEncode(Mode.ALPHANUMERIC, stringToEncode.charAt(from))) {
      addEdge(edges, from, new Edge(Mode.ALPHANUMERIC, from, 0, from + 1 >= inputLength ||
          !canEncode(Mode.ALPHANUMERIC, stringToEncode.charAt(from + 1)) ? 1 : 2, previous, version));
    }

    if (canEncode(Mode.NUMERIC, stringToEncode.charAt(from))) {
      addEdge(edges, from, new Edge(Mode.NUMERIC, from, 0, from + 1 >= inputLength ||
          !canEncode(Mode.NUMERIC, stringToEncode.charAt(from + 1)) ? 1 : from + 2 >= inputLength ||
          !canEncode(Mode.NUMERIC, stringToEncode.charAt(from + 2)) ? 2 : 3, previous, version));
    }
  }
  ResultList encodeSpecificVersion(Version version) throws WriterException {

    @SuppressWarnings("checkstyle:lineLength")
    /* A vertex represents a tuple of a position in the input, a mode and a character encoding where position 0
     * denotes the position left of the first character, 1 the position left of the second character and so on.
     * Likewise the end vertices are located after the last character at position stringToEncode.length().
     *
     * An edge leading to such a vertex encodes one or more of the characters left of the position that the vertex
     * represents and encodes it in the same encoding and mode as the vertex on which the edge ends. In other words,
     * all edges leading to a particular vertex encode the same characters in the same mode with the same character
     * encoding. They differ only by their source vertices who are all located at i+1 minus the number of encoded
     * characters.
     *
     * The edges leading to a vertex are stored in such a way that there is a fast way to enumerate the edges ending
     * on a particular vertex.
     *
     * The algorithm processes the vertices in order of their position thereby performing the following:
     *
     * For every vertex at position i the algorithm enumerates the edges ending on the vertex and removes all but the
     * shortest from that list.
     * Then it processes the vertices for the position i+1. If i+1 == stringToEncode.length() then the algorithm ends
     * and chooses the the edge with the smallest size from any of the edges leading to vertices at this position.
     * Otherwise the algorithm computes all possible outgoing edges for the vertices at the position i+1
     *
     * Examples:
     * The process is illustrated by showing the graph (edges) after each iteration from left to right over the input:
     * An edge is drawn as follows "(" + fromVertex + ") -- " + encodingMode + "(" + encodedInput + ") (" +
     * accumulatedSize + ") --> (" + toVertex + ")"
     *
     * Example 1 encoding the string "ABCDE":
     * Note: This example assumes that alphanumeric encoding is only possible in multiples of two characters so that
     * the example is both short and showing the principle. In reality this restriction does not exist.
     *
     * Initial situation
     * (initial) -- BYTE(A) (20) --> (1_BYTE)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
     *
     * Situation after adding edges to vertices at position 1
     * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE)
     *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
     *
     * Situation after adding edges to vertices at position 2
     * (initial) -- BYTE(A) (20) --> (1_BYTE)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
     * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE)
                                   * (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- BYTE(C) (44) --> (3_BYTE)
     *                                                            (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
     *
     * Situation after adding edges to vertices at position 3
     * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE)
     *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- BYTE(D) (64) --> (4_BYTE)
     *                                                                                                 (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
     *                                                            (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
     *
     * Situation after adding edges to vertices at position 4
     * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE) -- BYTE(D) (44) --> (4_BYTE)
     *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC) -- BYTE(E) (55) --> (5_BYTE)
     *
     * Situation after adding edges to vertices at position 5
     * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE) -- BYTE(D)         (44) --> (4_BYTE) -- BYTE(E)         (52) --> (5_BYTE)
     *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
     *
     * Encoding as BYTE(ABCDE) has the smallest size of 52 and is hence chosen. The encodation ALPHANUMERIC(ABCD),
     * BYTE(E) is longer with a size of 55.
     *
     * Example 2 encoding the string "XXYY" where X denotes a character unique to character set ISO-8859-2 and Y a
     * character unique to ISO-8859-3. Both characters encode as double byte in UTF-8:
     *
     * Initial situation
     * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE)
     *
     * Situation after adding edges to vertices at position 1
     * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2)
     *                               (1_BYTE_ISO-8859-2) -- BYTE(X) (72) --> (2_BYTE_UTF-8)
     *                               (1_BYTE_ISO-8859-2) -- BYTE(X) (72) --> (2_BYTE_UTF-16BE)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE)
     *
     * Situation after adding edges to vertices at position 2
     * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2)
     *                                                                       (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3)
     *                                                                       (2_BYTE_ISO-8859-2) -- BYTE(Y) (80) --> (3_BYTE_UTF-8)
     *                                                                       (2_BYTE_ISO-8859-2) -- BYTE(Y) (80) --> (3_BYTE_UTF-16BE)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE)
     *
     * Situation after adding edges to vertices at position 3
     * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3)
     *                                                                                                               (3_BYTE_ISO-8859-3) -- BYTE(Y) (80) --> (4_BYTE_ISO-8859-3)
     *                                                                                                               (3_BYTE_ISO-8859-3) -- BYTE(Y) (112) --> (4_BYTE_UTF-8)
     *                                                                                                               (3_BYTE_ISO-8859-3) -- BYTE(Y) (112) --> (4_BYTE_UTF-16BE)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8) -- BYTE(Y) (72) --> (3_BYTE_UTF-8)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE) -- BYTE(Y) (72) --> (3_BYTE_UTF-16BE)
     *
     * Situation after adding edges to vertices at position 4
     * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3) -- BYTE(Y) (80) --> (4_BYTE_ISO-8859-3)
     *                                                                                                               (3_BYTE_UTF-8) -- BYTE(Y) (88) --> (4_BYTE_UTF-8)
     *                                                                                                               (3_BYTE_UTF-16BE) -- BYTE(Y) (88) --> (4_BYTE_UTF-16BE)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8) -- BYTE(Y) (72) --> (3_BYTE_UTF-8)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE) -- BYTE(Y) (72) --> (3_BYTE_UTF-16BE)
     *
     * Encoding as ECI(ISO-8859-2),BYTE(XX),ECI(ISO-8859-3),BYTE(YY) has the smallest size of 80 and is hence chosen.
     * The encodation ECI(UTF-8),BYTE(XXYY) is longer with a size of 88.
     */

    int inputLength = stringToEncode.length();

    // Array that represents vertices. There is a vertex for every character, encoding and mode. The vertex contains
    // a list of all edges that lead to it that have the same encoding and mode.
    // The lists are created lazily

    // The last dimension in the array below encodes the 4 modes KANJI, ALPHANUMERIC, NUMERIC and BYTE via the
    // function getCompactedOrdinal(Mode)
    Edge[][][] edges = new Edge[inputLength + 1][encoders.length()][4];
    addEdges(version, edges, 0, null);

    for (int i = 1; i <= inputLength; i++) {
      for (int j = 0; j < encoders.length(); j++) {
        for (int k = 0; k < 4; k++) {
          if (edges[i][j][k] != null && i < inputLength) {
            addEdges(version, edges, i, edges[i][j][k]);
          }
        }
      }

    }
    int minimalJ = -1;
    int minimalK = -1;
    int minimalSize = Integer.MAX_VALUE;
    for (int j = 0; j < encoders.length(); j++) {
      for (int k = 0; k < 4; k++) {
        if (edges[inputLength][j][k] != null) {
          Edge edge = edges[inputLength][j][k];
          if (edge.cachedTotalSize < minimalSize) {
            minimalSize = edge.cachedTotalSize;
            minimalJ = j;
            minimalK = k;
          }
        }
      }
    }
    if (minimalJ < 0) {
      throw new WriterException("Internal error: failed to encode \"" + stringToEncode + "\"");
    }
    return new ResultList(version, edges[inputLength][minimalJ][minimalK]);
  }

  private final class Edge {
    private final Mode mode;
    private final int fromPosition;
    private final int charsetEncoderIndex;
    private final int characterLength;
    private final Edge previous;
    private final int cachedTotalSize;

    private Edge(Mode mode, int fromPosition, int charsetEncoderIndex, int characterLength, Edge previous,
                 Version version) {
      this.mode = mode;
      this.fromPosition = fromPosition;
      this.charsetEncoderIndex = mode == Mode.BYTE || previous == null ? charsetEncoderIndex :
          previous.charsetEncoderIndex; // inherit the encoding if not of type BYTE
      this.characterLength = characterLength;
      this.previous = previous;

      int size = previous != null ? previous.cachedTotalSize : 0;

      boolean needECI = mode == Mode.BYTE &&
          (previous == null && this.charsetEncoderIndex != 0) || // at the beginning and charset is not ISO-8859-1
          (previous != null && this.charsetEncoderIndex != previous.charsetEncoderIndex);

      if (previous == null || mode != previous.mode || needECI) {
        size += 4 + mode.getCharacterCountBits(version);
      }
      switch (mode) {
        case KANJI:
          size += 13;
          break;
        case ALPHANUMERIC:
          size += characterLength == 1 ? 6 : 11;
          break;
        case NUMERIC:
          size += characterLength == 1 ? 4 : characterLength == 2 ? 7 : 10;
          break;
        case BYTE:
          size += 8 * encoders.encode(stringToEncode.substring(fromPosition, fromPosition + characterLength),
              charsetEncoderIndex).length;
          if (needECI) {
            size += 4 + 8; // the ECI assignment numbers for ISO-8859-x, UTF-8 and UTF-16 are all 8 bit long
          }
          break;
      }
      cachedTotalSize = size;
    }
  }

  final class ResultList {

    private final List<ResultList.ResultNode> list = new ArrayList<>();
    private final Version version;

    ResultList(Version version, Edge solution) {
      int length = 0;
      Edge current = solution;
      boolean containsECI = false;

      while (current != null) {
        length += current.characterLength;
        Edge previous = current.previous;

        boolean needECI = current.mode == Mode.BYTE &&
            (previous == null && current.charsetEncoderIndex != 0) || // at the beginning and charset is not ISO-8859-1
            (previous != null && current.charsetEncoderIndex != previous.charsetEncoderIndex);

        if (needECI) {
          containsECI = true;
        }

        if (previous == null || previous.mode != current.mode || needECI) {
          list.add(0, new ResultNode(current.mode, current.fromPosition, current.charsetEncoderIndex, length));
          length = 0;
        }

        if (needECI) {
          list.add(0, new ResultNode(Mode.ECI, current.fromPosition, current.charsetEncoderIndex, 0));
        }
        current = previous;
      }

      // prepend FNC1 if needed. If the bits contain an ECI then the FNC1 must be preceeded by an ECI.
      // If there is no ECI at the beginning then we put an ECI to the default charset (ISO-8859-1)
      if (isGS1) {
        ResultNode first = list.get(0);
        if (first != null && first.mode != Mode.ECI && containsECI) {
          // prepend a default character set ECI
          list.add(0, new ResultNode(Mode.ECI, 0, 0, 0));
        }
        first = list.get(0);
        // prepend or insert a FNC1_FIRST_POSITION after the ECI (if any)
        list.add(first.mode != Mode.ECI ? 0 : 1, new ResultNode(Mode.FNC1_FIRST_POSITION, 0, 0, 0));
      }

      // set version to smallest version into which the bits fit.
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
      int size = getSize(version);
      // increase version if needed
      while (versionNumber < upperLimit && !Encoder.willFit(size, Version.getVersionForNumber(versionNumber),
        ecLevel)) {
        versionNumber++;
      }
      // shrink version if possible
      while (versionNumber > lowerLimit && Encoder.willFit(size, Version.getVersionForNumber(versionNumber - 1),
        ecLevel)) {
        versionNumber--;
      }
      this.version = Version.getVersionForNumber(versionNumber);
    }

    /**
     * returns the size in bits
     */
    int getSize() {
      return getSize(version);
    }

    private int getSize(Version version) {
      int result = 0;
      for (ResultNode resultNode : list) {
        result += resultNode.getSize(version);
      }
      return result;
    }

    /**
     * appends the bits
     */
    void getBits(BitArray bits) throws WriterException {
      for (ResultNode resultNode : list) {
        resultNode.getBits(bits);
      }
    }

    Version getVersion() {
      return version;
    }

    public String toString() {
      StringBuilder result = new StringBuilder();
      ResultNode previous = null;
      for (ResultNode current : list) {
        if (previous != null) {
          result.append(",");
        }
        result.append(current.toString());
        previous = current;
      }
      return result.toString();
    }

    final class ResultNode {

      private final Mode mode;
      private final int fromPosition;
      private final int charsetEncoderIndex;
      private final int characterLength;

      ResultNode(Mode mode, int fromPosition, int charsetEncoderIndex, int characterLength) {
        this.mode = mode;
        this.fromPosition = fromPosition;
        this.charsetEncoderIndex = charsetEncoderIndex;
        this.characterLength = characterLength;
      }

      /**
       * returns the size in bits
       */
      private int getSize(Version version) {
        int size = 4 + mode.getCharacterCountBits(version);
        switch (mode) {
          case KANJI:
            size += 13 * characterLength;
            break;
          case ALPHANUMERIC:
            size += (characterLength / 2) * 11;
            size += (characterLength % 2) == 1 ? 6 : 0;
            break;
          case NUMERIC:
            size += (characterLength / 3) * 10;
            int rest = characterLength % 3;
            size += rest == 1 ? 4 : rest == 2 ? 7 : 0;
            break;
          case BYTE:
            size += 8 * getCharacterCountIndicator();
            break;
          case ECI:
            size += 8; // the ECI assignment numbers for ISO-8859-x, UTF-8 and UTF-16 are all 8 bit long
        }
        return size;
      }

      /**
       * returns the length in characters according to the specification (differs from getCharacterLength() in BYTE mode
       * for multi byte encoded characters)
       */
      private int getCharacterCountIndicator() {
        return mode == Mode.BYTE ?
            encoders.encode(stringToEncode.substring(fromPosition, fromPosition + characterLength),
            charsetEncoderIndex).length : characterLength;
      }

      /**
       * appends the bits
       */
      private void getBits(BitArray bits) throws WriterException {
        bits.appendBits(mode.getBits(), 4);
        if (characterLength > 0) {
          int length = getCharacterCountIndicator();
          bits.appendBits(length, mode.getCharacterCountBits(version));
        }
        if (mode == Mode.ECI) {
          bits.appendBits(encoders.getECIValue(charsetEncoderIndex), 8);
        } else if (characterLength > 0) {
          // append data
          Encoder.appendBytes(stringToEncode.substring(fromPosition, fromPosition + characterLength), mode, bits,
              encoders.getCharset(charsetEncoderIndex));
        }
      }

      public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(mode).append('(');
        if (mode == Mode.ECI) {
          result.append(encoders.getCharset(charsetEncoderIndex).displayName());
        } else {
          result.append(makePrintable(stringToEncode.substring(fromPosition, fromPosition + characterLength)));
        }
        result.append(')');
        return result.toString();
      }

      private String makePrintable(String s) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
          if (s.charAt(i) < 32 || s.charAt(i) > 126) {
            result.append('.');
          } else {
            result.append(s.charAt(i));
          }
        }
        return result.toString();
      }
    }
  }
}
