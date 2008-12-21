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
 * <p>Implements decoding of the ITF format.</p>
 *
 * <p>"ITF" stands for Interleaved Two of Five. This Reader will scan ITF barcode with 6, 10 or 14 digits.
 * The checksum is optional and is not applied by this Reader. The consumer of the decoded value
 * will have to apply a checksum if required.</p>
 *
 * <p><a href="http://en.wikipedia.org/wiki/Interleaved_2_of_5">http://en.wikipedia.org/wiki/Interleaved_2_of_5</a>
 * is a great reference for Interleaved 2 of 5 information.</p>
 *
 * @author kevin.osullivan@sita.aero, SITA Lab.
 */
public final class ITFReader extends AbstractOneDReader {

  private static final int MAX_AVG_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42f);
  private static final int MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.8f);

  private static final int W = 3; // Pixel width of a wide line
  private static final int N = 1; // Pixed width of a narrow line

  // Stores the actual narrow line width of the image being decoded.
  private int narrowLineWidth = -1;

  /**
   * Start/end guard pattern.
   *
   * Note: The end pattern is reversed because the row is reversed before
   * searching for the END_PATTERN
   */
  private static final int[] START_PATTERN = {N, N, N, N};
  private static final int[] END_PATTERN_REVERSED = {N, N, W};

  /**
   * Patterns of Wide / Narrow lines to indicate each digit
   */
  private static final int[][] PATTERNS = {
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

  public Result decodeRow(int rowNumber, BitArray row, Hashtable hints) throws ReaderException {

    StringBuffer result = new StringBuffer(20);

    // Find out where the Middle section (payload) starts & ends
    int[] startRange = decodeStart(row);
    int[] endRange = decodeEnd(row);

    decodeMiddle(row, startRange[1], endRange[0], result);

    String resultString = result.toString();

    // To avoid false positives with 2D barcodes (and other patterns), make
    // an assumption that the decoded string must be 6, 10 or 14 digits.
    int length = resultString.length();
    if (length != 6 && length != 10 && length != 14) {
      throw ReaderException.getInstance();
    }

    return new Result(
        resultString,
        null, // no natural byte representation for these barcodes
        new ResultPoint[] { new GenericResultPoint(startRange[1], (float) rowNumber),
                            new GenericResultPoint(startRange[0], (float) rowNumber)},
        BarcodeFormat.ITF);
  }

  /**
   * @param row          row of black/white values to search
   * @param payloadStart offset of start pattern
   * @param resultString {@link StringBuffer} to append decoded chars to
   * @throws ReaderException if decoding could not complete successfully
   */
  static void decodeMiddle(BitArray row, int payloadStart, int payloadEnd, StringBuffer resultString) throws ReaderException {

    // Digits are interleaved in pairs - 5 black lines for one digit, and the
    // 5
    // interleaved white lines for the second digit.
    // Therefore, need to scan 10 lines and then
    // split these into two arrays
    int[] counterDigitPair = new int[10];
    int[] counterBlack = new int[5];
    int[] counterWhite = new int[5];

    while (payloadStart < payloadEnd) {

      // Get 10 runs of black/white.
      recordPattern(row, payloadStart, counterDigitPair);
      // Split them into each array
      for (int k = 0; k < 5; k++) {
        int twoK = k << 1;
        counterBlack[k] = counterDigitPair[twoK];
        counterWhite[k] = counterDigitPair[twoK + 1];
      }

      int bestMatch = decodeDigit(counterBlack);
      resultString.append((char) ('0' + bestMatch));
      bestMatch = decodeDigit(counterWhite);
      resultString.append((char) ('0' + bestMatch));

      for (int i = 0; i < counterDigitPair.length; i++) {
        payloadStart += counterDigitPair[i];
      }
    }
  }

  /**
   * Identify where the start of the middle / payload section starts.
   *
   * @param row row of black/white values to search
   * @return Array, containing index of start of 'start block' and end of
   *         'start block'
   * @throws ReaderException
   */
  int[] decodeStart(BitArray row) throws ReaderException {
    int endStart = skipWhiteSpace(row);
    int[] startPattern = findGuardPattern(row, endStart, START_PATTERN);

    // Determine the width of a narrow line in pixels. We can do this by
    // getting the width of the start pattern and dividing by 4 because its
    // made up of 4 narrow lines.
    this.narrowLineWidth = (startPattern[1] - startPattern[0]) >> 2;

    validateQuietZone(row, startPattern[0]);

    return startPattern;
  }

  /**
   * The start & end patterns must be pre/post fixed by a quiet zone. This
   * zone must be at least 10 times the width of a narrow line.  Scan back until
   * we either get to the start of the barcode or match the necessary number of
   * quiet zone pixels.
   *
   * Note: Its assumed the row is reversed when using this method to find
   * quiet zone after the end pattern.
   *
   * ref: http://www.barcode-1.net/i25code.html
   *
   * @param row bit array representing the scanned barcode.
   * @param startPattern index into row of the start or end pattern.
   * @throws ReaderException if the quiet zone cannot be found, a ReaderException is thrown.
   */
  private void validateQuietZone(BitArray row, int startPattern) throws ReaderException {

    int quietCount = this.narrowLineWidth * 10;  // expect to find this many pixels of quiet zone

    for (int i = startPattern - 1; quietCount > 0 && i >= 0; i--) {
      if (row.get(i)) {
        break;
      }
      quietCount--;
    }
    if (quietCount != 0) {
      // Unable to find the necessary number of quiet zone pixels.
      throw ReaderException.getInstance();
    }
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
    if (endStart == width) {
      throw ReaderException.getInstance();
    }

    return endStart;
  }

  /**
   * Identify where the end of the middle / payload section ends.
   *
   * @param row row of black/white values to search
   * @return Array, containing index of start of 'end block' and end of 'end
   *         block'
   * @throws ReaderException
   */

  int[] decodeEnd(BitArray row) throws ReaderException {

    // For convenience, reverse the row and then
    // search from 'the start' for the end block
    row.reverse();

    int endStart = skipWhiteSpace(row);
    int[] endPattern;
    try {
      endPattern = findGuardPattern(row, endStart, END_PATTERN_REVERSED);
    } catch (ReaderException e) {
      // Put our row of data back the right way before throwing
      row.reverse();
      throw e;
    }

    // The start & end patterns must be pre/post fixed by a quiet zone. This
    // zone must be at least 10 times the width of a narrow line.
    // ref: http://www.barcode-1.net/i25code.html
    validateQuietZone(row, endPattern[0]);

    // Now recalc the indicies of where the 'endblock' starts & stops to
    // accomodate
    // the reversed nature of the search
    int temp = endPattern[0];
    endPattern[0] = row.getSize() - endPattern[1];
    endPattern[1] = row.getSize() - temp;

    // Put the row back the righ way.
    row.reverse();
    return endPattern;
  }

  /**
   * @param row       row of black/white values to search
   * @param rowOffset position to start search
   * @param pattern   pattern of counts of number of black and white pixels that are
   *                  being searched for as a pattern
   * @return start/end horizontal offset of guard pattern, as an array of two
   *         ints
   * @throws ReaderException if pattern is not found
   */
  int[] findGuardPattern(BitArray row, int rowOffset, int[] pattern) throws ReaderException {

    // TODO: This is very similar to implementation in AbstractUPCEANReader. Consider if they can be merged to
    // a single method.

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
   * Attempts to decode a sequence of ITF black/white lines into single
   * digit.
   *
   * @param counters the counts of runs of observed black/white/black/... values
   * @return The decoded digit
   * @throws ReaderException if digit cannot be decoded
   */
  private static int decodeDigit(int[] counters) throws ReaderException {

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