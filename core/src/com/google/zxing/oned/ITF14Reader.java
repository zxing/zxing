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

package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.GenericResultPoint;

import java.util.Hashtable;

/**
 * <p>Implements decoding of the ITF-14 format.</p>
 * <p/>
 * <p>"ITF" stands for Interleaved Two of Five. The "-14" part indicates there are 14 digits encoded in the barcode.</p>
 * <p/>
 * <p><a href="http://en.wikipedia.org/wiki/Interleaved_2_of_5">http://en.wikipedia.org/wiki/Interleaved_2_of_5</a>
 * is a great reference for Interleaved 2 of 5 information. </p>
 * <p/>
 * <p>TODO: ITF-14 is an implementation of
 * <a href="http://en.wikipedia.org/wiki/Interleaved_2_of_5">Interleaved 2 of 5</a> barcode.
 * It stipulates that there is 14 digits in the bar code. A more abstract class should be implemented
 * which will scan an arbritary number of digits encoded in two of five format.</p>
 *
 * @author kevin.osullivan@sita.aero
 */
public final class ITF14Reader extends AbstractOneDReader {

  private static final int MAX_AVG_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42f);
  private static final int MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7f);

  private static final int W = 3; // Pixel width of a wide line
  private static final int N = 1; // Pixed width of a narrow line

  private final int DIGIT_COUNT = 14;  // There are 14 digits in ITF-14
  /**
   * Start/end guard pattern.
   * <p/>
   * Note: The end pattern is reversed because the
   * row is reversed before searching for the END_PATTERN
   */
  private static final int[] START_PATTERN = {N, N, N, N};
  private static final int[] END_PATTERN_REVERSED = {N, N, W};

  /**
   * Patterns of Wide / Narrow lines to
   * indicate each digit
   */
  static final int[][] PATTERNS = {
      {N, N, W, W, N}, // 0
      {W, N, N, N, W}, // 1
      {N, W, N, N, W}, // 2
      {W, W, N, N, N}, // 3
      {N, N, W, N, W}, // 4
      {W, N, W, N, N}, // 5
      {N, W, W, N, N}, // 6
      {N, N, N, W, W}, // 7
      {W, N, N, W, N}, // 8
      {N, W, N, W, N}  // 9
  };

  public final Result decodeRow(int rowNumber, BitArray row, Hashtable hints) throws ReaderException {

    StringBuffer result = new StringBuffer(20);

    /**
     * Find out where the Middle section (payload) starts & ends
     */
    int[] startRange = decodeStart(row);
    int[] endRange = decodeEnd(row);

    decodeMiddle(row, startRange[1], endRange[0], result);

    String resultString = result.toString();
    if (!AbstractUPCEANReader.checkStandardUPCEANChecksum(resultString)) {
      throw ReaderException.getInstance();
    }

    return new Result(resultString, null, // no natural byte representation
        // for these barcodes
        new ResultPoint[]{new GenericResultPoint(startRange[1], (float) rowNumber),
            new GenericResultPoint(startRange[0], (float) rowNumber)},
        BarcodeFormat.ITF_14);
  }


  /**
   * @param row          row of black/white values to search
   * @param payloadStart offset of start pattern
   * @param resultString {@link StringBuffer} to append decoded chars to
   * @throws ReaderException if decoding could not complete successfully
   */
  protected void decodeMiddle(BitArray row, int payloadStart, int payloadEnd, StringBuffer resultString)
      throws ReaderException {

    // Digits are interleaved in pairs - 5 black lines for one digit, and the 5
    // interleaved white lines for the second digit.
    // Therefore, need to scan 10 lines and then
    // split these into two arrays
    int[] counterDigitPair = new int[10];
    int[] counterBlack = new int[5];
    int[] counterWhite = new int[5];


    for (int x = 0; x < DIGIT_COUNT / 2 && payloadStart < payloadEnd; x++) {

      // Get 10 runs of black/white.
      recordPattern(row, payloadStart, counterDigitPair);
      // Split them into each array
      for (int k = 0; k < 5; k++) {
        counterBlack[k] = counterDigitPair[k * 2];
        counterWhite[k] = counterDigitPair[(k * 2) + 1];
      }

      int bestMatch = decodeDigit(counterBlack);
      resultString.append((char) ('0' + bestMatch % 10));
      bestMatch = decodeDigit(counterWhite);
      resultString.append((char) ('0' + bestMatch % 10));

      for (int i = 0; i < counterDigitPair.length; i++) {
        payloadStart += counterDigitPair[i];
      }
    }
  }

  /**
   * Identify where the start of the middle / payload section starts.
   *
   * @param row row of black/white values to search
   * @return Array, containing index of start of 'start block' and end of 'start block'
   * @throws ReaderException
   */
  int[] decodeStart(BitArray row) throws ReaderException {
    int endStart = skipWhiteSpace(row);
    return findGuardPattern(row, endStart, START_PATTERN);
  }

  /**
   * Skip all whitespace until we get to the first black line.
   *
   * @param row row of black/white values to search
   * @return index of the first black line.
   * @throws ReaderException Throws exception if no black lines are found in the row
   */
  private int skipWhiteSpace(BitArray row) throws ReaderException {
    int width = row.getSize();
    int endStart = 0;
    while (endStart < width) {
      if (row.get(endStart)) {
        break;
      }
      endStart++;
    }
    if (endStart == width)
      throw ReaderException.getInstance();

    return endStart;
  }

  /**
   * Identify where the start of the middle / payload section ends.
   *
   * @param row row of black/white values to search
   * @return Array, containing index of start of 'end block' and end of 'end block'
   * @throws ReaderException
   */

  int[] decodeEnd(BitArray row) throws ReaderException {

    // For convenience, reverse the row and then
    // search from 'the start' for the end block
    row.reverse();

    int endStart = skipWhiteSpace(row);
    int end[];
    try {
      end = findGuardPattern(row, endStart, END_PATTERN_REVERSED);
    } catch (ReaderException e) {
      // Put our row of data back the right way before throwing
      row.reverse();
      throw e;
    }

    // Now recalc the indicies of where the 'endblock' starts & stops to accomodate
    // the reversed nature of the search
    int temp = end[0];
    end[0] = row.getSize() - end[1];
    end[1] = row.getSize() - temp;

    // Put the row back the righ way.
    row.reverse();
    return end;
  }

  /**
   * @param row       row of black/white values to search
   * @param rowOffset position to start search
   * @param pattern   pattern of counts of number of black and white pixels that are
   *                  being searched for as a pattern
   * @return start/end horizontal offset of guard pattern, as an array of two ints
   * @throws ReaderException if pattern is not found
   */
  int[] findGuardPattern(BitArray row, int rowOffset, int[] pattern) throws ReaderException {
    int patternLength = pattern.length;
    int[] counters = new int[patternLength];
    int width = row.getSize();
    boolean isWhite = false;

    int counterPosition = 0;
    int patternStart = rowOffset;
    for (int x = rowOffset; x < width; x++) {
      boolean pixel = row.get(x);
      if ((!pixel && isWhite) || (pixel && !isWhite)) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
            return new int[]{patternStart, x};
          }
          patternStart += counters[0] + counters[1];
          for (int y = 2; y < patternLength; y++) {
            counters[y - 2] = counters[y];
          }
          counters[patternLength - 2] = 0;
          counters[patternLength - 1] = 0;
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        isWhite = !isWhite;
      }
    }
    throw ReaderException.getInstance();
  }

  /**
   * Attempts to decode a sequence of ITF-14 black/white lines into single digit.
   *
   * @param counters the counts of runs of observed black/white/black/... values
   * @return The decoded digit
   * @throws ReaderException if digit cannot be decoded
   */
  static int decodeDigit(int[] counters) throws ReaderException {

    int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
    int bestMatch = -1;
    int max = PATTERNS.length;
    for (int i = 0; i < max; i++) {
      int[] pattern = PATTERNS[i];
      int variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
      if (variance < bestVariance) {
        bestVariance = variance;
        bestMatch = i;
      }
    }
    if (bestMatch >= 0) {
      return bestMatch;
    } else {
      throw ReaderException.getInstance();
		}
	}

}