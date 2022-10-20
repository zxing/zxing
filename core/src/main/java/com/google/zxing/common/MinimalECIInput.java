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

package com.google.zxing.common;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that converts a character string into a sequence of ECIs and bytes
 *
 * The implementation uses the Dijkstra algorithm to produce minimal encodings
 *
 * @author Alex Geller
 */
public class MinimalECIInput implements ECIInput {

  private static final int COST_PER_ECI = 3; // approximated (latch + 2 codewords)
  private final int[] bytes;
  private final int fnc1;

 /**
  * Constructs a minimal input
  *
  * @param stringToEncode the character string to encode
  * @param priorityCharset The preferred {@link Charset}. When the value of the argument is null, the algorithm
  *   chooses charsets that leads to a minimal representation. Otherwise the algorithm will use the priority
  *   charset to encode any character in the input that can be encoded by it if the charset is among the
  *   supported charsets.
  * @param fnc1 denotes the character in the input that represents the FNC1 character or -1 if this is not GS1
  *   input.
  */
  public MinimalECIInput(String stringToEncode, Charset priorityCharset, int fnc1) {
    this.fnc1 = fnc1;
    ECIEncoderSet encoderSet = new ECIEncoderSet(stringToEncode, priorityCharset, fnc1);
    if (encoderSet.length() == 1) { //optimization for the case when all can be encoded without ECI in ISO-8859-1
      bytes = new int[stringToEncode.length()];
      for (int i = 0; i < bytes.length; i++) {
        char c = stringToEncode.charAt(i);
        bytes[i] = c == fnc1 ? 1000 : (int) c;
      }
    } else {
      bytes = encodeMinimally(stringToEncode, encoderSet, fnc1);
    }
  }

  public int getFNC1Character() {
    return fnc1;
  }

 /**
  * Returns the length of this input.  The length is the number
  * of {@code byte}s, FNC1 characters or ECIs in the sequence.
  *
  * @return  the number of {@code char}s in this sequence
  */
  public int length() {
    return bytes.length;
  }

  public boolean haveNCharacters(int index, int n) {
    if (index + n - 1 >= bytes.length) {
      return false;
    }
    for (int i = 0; i < n; i++) {
      if (isECI(index + i)) {
        return false;
      }
    }
    return true;
  }

 /**
  * Returns the {@code byte} value at the specified index.  An index ranges from zero
  * to {@code length() - 1}.  The first {@code byte} value of the sequence is at
  * index zero, the next at index one, and so on, as for array
  * indexing.
  *
  * @param   index the index of the {@code byte} value to be returned
  *
  * @return  the specified {@code byte} value as character or the FNC1 character
  *
  * @throws  IndexOutOfBoundsException
  *          if the {@code index} argument is negative or not less than
  *          {@code length()}
  * @throws  IllegalArgumentException
  *          if the value at the {@code index} argument is an ECI (@see #isECI)
  */
  public char charAt(int index) {
    if (index < 0 || index >= length()) {
      throw new IndexOutOfBoundsException("" + index);
    }
    if (isECI(index)) {
      throw new IllegalArgumentException("value at " + index + " is not a character but an ECI");
    }
    return isFNC1(index) ? (char) fnc1 : (char) bytes[index];
  }

 /**
  * Returns a {@code CharSequence} that is a subsequence of this sequence.
  * The subsequence starts with the {@code char} value at the specified index and
  * ends with the {@code char} value at index {@code end - 1}.  The length
  * (in {@code char}s) of the
  * returned sequence is {@code end - start}, so if {@code start == end}
  * then an empty sequence is returned.
  *
  * @param   start   the start index, inclusive
  * @param   end     the end index, exclusive
  *
  * @return  the specified subsequence
  *
  * @throws  IndexOutOfBoundsException
  *          if {@code start} or {@code end} are negative,
  *          if {@code end} is greater than {@code length()},
  *          or if {@code start} is greater than {@code end}
  * @throws  IllegalArgumentException
  *          if a value in the range {@code start}-{@code end} is an ECI (@see #isECI)
  */
  public CharSequence subSequence(int start, int end) {
    if (start < 0 || start > end || end > length()) {
      throw new IndexOutOfBoundsException("" + start);
    }
    StringBuilder result = new StringBuilder();
    for (int i = start; i < end; i++) {
      if (isECI(i)) {
        throw new IllegalArgumentException("value at " + i + " is not a character but an ECI");
      }
      result.append(charAt(i));
    }
    return result;
  }

 /**
  * Determines if a value is an ECI
  *
  * @param   index the index of the value
  *
  * @return  true if the value at position {@code index} is an ECI
  *
  * @throws  IndexOutOfBoundsException
  *          if the {@code index} argument is negative or not less than
  *          {@code length()}
  */
  public boolean isECI(int index) {
    if (index < 0 || index >= length()) {
      throw new IndexOutOfBoundsException("" + index);
    }
    return bytes[index] > 255 && bytes[index] <= 999;
  }

 /**
  * Determines if a value is the FNC1 character
  *
  * @param   index the index of the value
  *
  * @return  true if the value at position {@code index} is the FNC1 character
  *
  * @throws  IndexOutOfBoundsException
  *          if the {@code index} argument is negative or not less than
  *          {@code length()}
  */
  public boolean isFNC1(int index) {
    if (index < 0 || index >= length()) {
      throw new IndexOutOfBoundsException("" + index);
    }
    return bytes[index] == 1000;
  }

 /**
  * Returns the {@code int} ECI value at the specified index.  An index ranges from zero
  * to {@code length() - 1}.  The first {@code byte} value of the sequence is at
  * index zero, the next at index one, and so on, as for array
  * indexing.
  *
  * @param   index the index of the {@code int} value to be returned
  *
  * @return  the specified {@code int} ECI value.
  *          The ECI specified the encoding of all bytes with a higher index until the
  *          next ECI or until the end of the input if no other ECI follows.
  *
  * @throws  IndexOutOfBoundsException
  *          if the {@code index} argument is negative or not less than
  *          {@code length()}
  * @throws  IllegalArgumentException
  *          if the value at the {@code index} argument is not an ECI (@see #isECI)
  */
  public int getECIValue(int index) {
    if (index < 0 || index >= length()) {
      throw new IndexOutOfBoundsException("" + index);
    }
    if (!isECI(index)) {
      throw new IllegalArgumentException("value at " + index + " is not an ECI but a character");
    }
    return bytes[index] - 256;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < length(); i++) {
      if (i > 0) {
        result.append(", ");
      }
      if (isECI(i)) {
        result.append("ECI(");
        result.append(getECIValue(i));
        result.append(')');
      } else if (charAt(i) < 128) {
        result.append('\'');
        result.append(charAt(i));
        result.append('\'');
      } else {
        result.append((int) charAt(i));
      }
    }
    return result.toString();
  }
  static void addEdge(InputEdge[][] edges, int to, InputEdge edge) {
    if (edges[to][edge.encoderIndex] == null ||
        edges[to][edge.encoderIndex].cachedTotalSize > edge.cachedTotalSize) {
      edges[to][edge.encoderIndex] = edge;
    }
  }

  static void addEdges(String stringToEncode,
                       ECIEncoderSet encoderSet,
                       InputEdge[][] edges,
                       int from,
                       InputEdge previous,
                       int fnc1) {

    char ch = stringToEncode.charAt(from);

    int start = 0;
    int end = encoderSet.length();
    if (encoderSet.getPriorityEncoderIndex() >= 0 && (ch == fnc1 || encoderSet.canEncode(ch,
        encoderSet.getPriorityEncoderIndex()))) {
      start = encoderSet.getPriorityEncoderIndex();
      end = start + 1;
    }

    for (int i = start; i < end; i++) {
      if (ch == fnc1 || encoderSet.canEncode(ch,i)) {
        addEdge(edges, from + 1, new InputEdge(ch, encoderSet, i, previous, fnc1));
      }
    }
  }

  static int[] encodeMinimally(String stringToEncode, ECIEncoderSet encoderSet, int fnc1) {
    int inputLength = stringToEncode.length();

    // Array that represents vertices. There is a vertex for every character and encoding.
    InputEdge[][] edges = new InputEdge[inputLength + 1][encoderSet.length()];
    addEdges(stringToEncode, encoderSet, edges, 0, null, fnc1);

    for (int i = 1; i <= inputLength; i++) {
      for (int j = 0; j < encoderSet.length(); j++) {
        if (edges[i][j] != null && i < inputLength) {
          addEdges(stringToEncode, encoderSet, edges, i, edges[i][j], fnc1);
        }
      }
      //optimize memory by removing edges that have been passed.
      for (int j = 0; j < encoderSet.length(); j++) {
        edges[i - 1][j] = null;
      }
    }
    int minimalJ = -1;
    int minimalSize = Integer.MAX_VALUE;
    for (int j = 0; j < encoderSet.length(); j++) {
      if (edges[inputLength][j] != null) {
        InputEdge edge = edges[inputLength][j];
        if (edge.cachedTotalSize < minimalSize) {
          minimalSize = edge.cachedTotalSize;
          minimalJ = j;
        }
      }
    }
    if (minimalJ < 0) {
      throw new IllegalStateException("Failed to encode \"" + stringToEncode + "\"");
    }
    List<Integer> intsAL = new ArrayList<>();
    InputEdge current = edges[inputLength][minimalJ];
    while (current != null) {
      if (current.isFNC1()) {
        intsAL.add(0, 1000);
      } else {
        byte[] bytes = encoderSet.encode(current.c,current.encoderIndex);
        for (int i = bytes.length - 1; i >= 0; i--) {
          intsAL.add(0, (bytes[i] & 0xFF));
        }
      }
      int previousEncoderIndex = current.previous == null ? 0 : current.previous.encoderIndex;
      if (previousEncoderIndex != current.encoderIndex) {
        intsAL.add(0,256 + encoderSet.getECIValue(current.encoderIndex));
      }
      current = current.previous;
    }
    int[] ints = new int[intsAL.size()];
    for (int i = 0; i < ints.length; i++) {
      ints[i] = intsAL.get(i);
    }
    return ints;
  }

  private static final class InputEdge {
    private final char c;
    private final int encoderIndex; //the encoding of this edge
    private final InputEdge previous;
    private final int cachedTotalSize;

    private InputEdge(char c, ECIEncoderSet encoderSet, int encoderIndex, InputEdge previous, int fnc1) {
      this.c = c == fnc1 ? 1000 : c;
      this.encoderIndex = encoderIndex;
      this.previous = previous;

      int size = this.c == 1000 ? 1 : encoderSet.encode(c, encoderIndex).length;
      int previousEncoderIndex = previous == null ? 0 : previous.encoderIndex;
      if (previousEncoderIndex != encoderIndex) {
        size += COST_PER_ECI;
      }
      if (previous != null) {
        size += previous.cachedTotalSize;
      }
      this.cachedTotalSize = size;
    }

    boolean isFNC1() {
      return c == 1000;
    }

  }
}
