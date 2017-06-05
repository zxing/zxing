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
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;

import java.util.Map;

/**
 * <p>Implements decoding of the STF format, or Standard Two of Five.</p>
 *
 * <p>This Reader is a blatant copy-and-paste from the {@link ITFReader} with
 * modifications to account for the differences:</p>
 * <ul>
 * <li>Start and End patterns contain 2-wide wide lines.</li>
 * <li>The patterns contain both the black and white lines.</li>
 * </ul>
 *
 * <p>The checksum is optional and is not applied by this Reader. The consumer of the decoded
 * value will have to apply a checksum if required.</p>
 *
 * <p>No standard documentation is available, the start/stop and digit patterns
 * were obtained from http://www.barcodeisland.com/2of5.phtml</p>
 *
 * @author kevin.osullivan@sita.aero, SITA Lab. for the {@link ITFReader}
 * @author lachezar for the Non-Interleaved changes
 */
public final class STFReader extends OneDReader {

  private static final float MAX_AVG_VARIANCE = 0.38f;
  private static final float MAX_INDIVIDUAL_VARIANCE = 0.78f;

  private static final int W = 3; // Pixel width of a wide line
  private static final int S = 2; // Pixel width of a start/stop wide line
  private static final int N = 1; // Pixel width of a narrow line

  /** Valid ITF lengths. Anything longer than the largest value is also allowed. */
  private static final int[] DEFAULT_ALLOWED_LENGTHS = { 6, 8, 10, 12, 14 };

  // Stores the actual narrow line width of the image being decoded.
  private int narrowLineWidth = -1;

  /**
   * Start/end guard pattern.
   *
   * Note: The end pattern is reversed because the row is reversed before
   * searching for the END_PATTERN
   * Note: The end pattern for the Non-Interleaved variant is symmetric, so the
   * reversion yields the same pattern, but we still look for it backwards.
   */
  private static final int[] START_PATTERN = {S, N, S, N, N, N};
  private static final int[] END_PATTERN_REVERSED = {S, N, N, N, S};

  /**
   * Patterns of Wide / Narrow lines to indicate each digit.<br>
   * White lines are all narrow in the even spots
   */
  static final int[][] PATTERNS = {
      {N, N, N, N, W, N, W, N, N, N}, // 0
      {W, N, N, N, N, N, N, N, W, N}, // 1
      {N, N, W, N, N, N, N, N, W, N}, // 2
      {W, N, W, N, N, N, N, N, N, N}, // 3
      {N, N, N, N, W, N, N, N, W, N}, // 4
      {W, N, N, N, W, N, N, N, N, N}, // 5
      {N, N, W, N, W, N, N, N, N, N}, // 6
      {N, N, N, N, N, N, W, N, W, N}, // 7
      {W, N, N, N, N, N, W, N, N, N}, // 8
      {N, N, W, N, N, N, W, N, N, N}  // 9
  };

  @Override
  public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType,?> hints)
      throws FormatException, NotFoundException {

    // Find out where the Middle section (payload) starts & ends
    int[] startRange = decodeStart(row);
    int[] endRange = decodeEnd(row);

    StringBuilder result = new StringBuilder(20);
    decodeMiddle(row, startRange[1], endRange[0], result);
    String resultString = result.toString();

    int[] allowedLengths = null;
    if (hints != null) {
      allowedLengths = (int[]) hints.get(DecodeHintType.ALLOWED_LENGTHS);

    }
    if (allowedLengths == null) {
      allowedLengths = DEFAULT_ALLOWED_LENGTHS;
    }

    // To avoid false positives with 2D barcodes (and other patterns), make
    // an assumption that the decoded string must be a 'standard' length if it's short
    int length = resultString.length();
    boolean lengthOK = false;
    int maxAllowedLength = 0;
    for (int allowedLength : allowedLengths) {
      if (length == allowedLength) {
        lengthOK = true;
        break;
      }
      if (allowedLength > maxAllowedLength) {
        maxAllowedLength = allowedLength;
      }
    }
    if (!lengthOK && length > maxAllowedLength) {
      lengthOK = true;
    }
    if (!lengthOK) {
      throw FormatException.getFormatInstance();
    }

    return new Result(
        resultString,
        null, // no natural byte representation for these barcodes
        new ResultPoint[] { new ResultPoint(startRange[1], rowNumber),
                            new ResultPoint(endRange[0], rowNumber)},
        BarcodeFormat.STF);
  }

  /**
   * @param row          row of black/white values to search
   * @param payloadStart offset of start pattern
   * @param resultString {@link StringBuilder} to append decoded chars to
   * @throws NotFoundException if decoding could not complete successfully
   */
  private static void decodeMiddle(BitArray row,
                                   int payloadStart,
                                   int payloadEnd,
                                   StringBuilder resultString) throws NotFoundException {

    // Digits are 5 black and 5 white lines for one digit
    int[] digit = new int[10];

    while (payloadStart < payloadEnd) {

      // Get one digit worth of black and white lines.
      recordPattern(row, payloadStart, digit);

      int bestMatch = decodeDigit(digit);
      resultString.append((char) ('0' + bestMatch));

      for (int counterDigit : digit) {
        payloadStart += counterDigit;
      }
    }
  }

  /**
   * Identify where the start of the middle / payload section starts.
   *
   * @param row row of black/white values to search
   * @return Array, containing index of start of 'start block' and end of
   *         'start block'
   */
  private int[] decodeStart(BitArray row) throws NotFoundException {
    int endStart = skipWhiteSpace(row);
    int[] startPattern = findGuardPattern(row, endStart, START_PATTERN);

    // Determine the width of a narrow line in pixels. We can do this by
    // getting the width of the start pattern and dividing by 8 because its
    // made up of 2 double-wide and 4 narrow lines.
    this.narrowLineWidth = (startPattern[1] - startPattern[0]) / 8;

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
   * Note: The Standard variant has no quiet zone specification, so a 'safe'
   * choice was made to expect as much white space as a start/stop character.
   *
   * @param row bit array representing the scanned barcode.
   * @param startPattern index into row of the start or end pattern.
   * @throws NotFoundException if the quiet zone cannot be found
   */
  private void validateQuietZone(BitArray row, int startPattern) throws NotFoundException {

    int quietCount = this.narrowLineWidth * 8;  // expect to find this many pixels of quiet zone

    // if there are not so many pixel at all let's try as many as possible
    quietCount = quietCount < startPattern ? quietCount : startPattern;

    for (int i = startPattern - 1; quietCount > 0 && i >= 0; i--) {
      if (row.get(i)) {
        break;
      }
      quietCount--;
    }
    if (quietCount != 0) {
      // Unable to find the necessary number of quiet zone pixels.
      throw NotFoundException.getNotFoundInstance();
    }
  }

  /**
   * Skip all whitespace until we get to the first black line.
   *
   * @param row row of black/white values to search
   * @return index of the first black line.
   * @throws NotFoundException Throws exception if no black lines are found in the row
   */
  private static int skipWhiteSpace(BitArray row) throws NotFoundException {
    int width = row.getSize();
    int endStart = row.getNextSet(0);
    if (endStart == width) {
      throw NotFoundException.getNotFoundInstance();
    }

    return endStart;
  }

  /**
   * Identify where the end of the middle / payload section ends.
   *
   * @param row row of black/white values to search
   * @return Array, containing index of start of 'end block' and end of 'end
   *         block'
   */
  private int[] decodeEnd(BitArray row) throws NotFoundException {

    // For convenience, reverse the row and then
    // search from 'the start' for the end block
    row.reverse();
    try {
      int endStart = skipWhiteSpace(row);
      int[] endPattern = findGuardPattern(row, endStart, END_PATTERN_REVERSED);

      // The start & end patterns must be pre/post fixed by a quiet zone. This
      // zone must be at least 10 times the width of a narrow line.
      // ref: http://www.barcode-1.net/i25code.html
      validateQuietZone(row, endPattern[0]);

      // Now recalculate the indices of where the 'endblock' starts & stops to
      // accommodate
      // the reversed nature of the search
      int temp = endPattern[0];
      endPattern[0] = row.getSize() - endPattern[1];
      endPattern[1] = row.getSize() - temp;

      return endPattern;
    } finally {
      // Put the row back the right way.
      row.reverse();
    }
  }

  /**
   * @param row       row of black/white values to search
   * @param rowOffset position to start search
   * @param pattern   pattern of counts of number of black and white pixels that are
   *                  being searched for as a pattern
   * @return start/end horizontal offset of guard pattern, as an array of two
   *         ints
   * @throws NotFoundException if pattern is not found
   */
  private static int[] findGuardPattern(BitArray row,
                                        int rowOffset,
                                        int[] pattern) throws NotFoundException {
    int patternLength = pattern.length;
    int[] counters = new int[patternLength];
    int width = row.getSize();
    boolean isWhite = false;

    int counterPosition = 0;
    int patternStart = rowOffset;
    for (int x = rowOffset; x < width; x++) {
      if (row.get(x) != isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
            return new int[]{patternStart, x};
          }
          patternStart += counters[0] + counters[1];
          System.arraycopy(counters, 2, counters, 0, counterPosition - 1);
          counters[counterPosition - 1] = 0;
          counters[counterPosition] = 0;
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        isWhite = !isWhite;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  /**
   * Attempts to decode a sequence of ITF black/white lines into single
   * digit.
   *
   * @param counters the counts of runs of observed black/white/black/... values
   * @return The decoded digit
   * @throws NotFoundException if digit cannot be decoded
   */
  private static int decodeDigit(int[] counters) throws NotFoundException {
    float bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
    int bestMatch = -1;
    int max = PATTERNS.length;
    for (int i = 0; i < max; i++) {
      int[] pattern = PATTERNS[i];
      float variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
      if (variance < bestVariance) {
        bestVariance = variance;
        bestMatch = i;
      }
    }
    if (bestMatch >= 0) {
      return bestMatch;
    } else {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  @Override
  public void reset() {
    super.reset();
    narrowLineWidth = -1;
  }

}
