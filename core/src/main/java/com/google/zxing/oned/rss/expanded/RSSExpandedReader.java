/*
 * Copyright (C) 2010 ZXing authors
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

/*
 * These authors would like to acknowledge the Spanish Ministry of Industry,
 * Tourism and Trade, for the support in the project TSI020301-2008-2
 * "PIRAmIDE: Personalizable Interactions with Resources on AmI-enabled
 * Mobile Dynamic Environments", led by Treelogic
 * ( http://www.treelogic.com/ ):
 *
 *   http://www.piramidepse.com/
 */

package com.google.zxing.oned.rss.expanded;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.detector.MathUtils;
import com.google.zxing.oned.rss.AbstractRSSReader;
import com.google.zxing.oned.rss.DataCharacter;
import com.google.zxing.oned.rss.FinderPattern;
import com.google.zxing.oned.rss.RSSUtils;
import com.google.zxing.oned.rss.expanded.decoders.AbstractExpandedDecoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public final class RSSExpandedReader extends AbstractRSSReader {

  private static final int[] SYMBOL_WIDEST = {7, 5, 4, 3, 1};
  private static final int[] EVEN_TOTAL_SUBSET = {4, 20, 52, 104, 204};
  private static final int[] GSUM = {0, 348, 1388, 2948, 3988};

  private static final int[][] FINDER_PATTERNS = {
    {1,8,4,1}, // A
    {3,6,4,1}, // B
    {3,4,6,1}, // C
    {3,2,8,1}, // D
    {2,6,5,1}, // E
    {2,2,9,1}  // F
  };

  private static final int[][] WEIGHTS = {
    {  1,   3,   9,  27,  81,  32,  96,  77},
    { 20,  60, 180, 118, 143,   7,  21,  63},
    {189, 145,  13,  39, 117, 140, 209, 205},
    {193, 157,  49, 147,  19,  57, 171,  91},
    { 62, 186, 136, 197, 169,  85,  44, 132},
    {185, 133, 188, 142,   4,  12,  36, 108},
    {113, 128, 173,  97,  80,  29,  87,  50},
    {150,  28,  84,  41, 123, 158,  52, 156},
    { 46, 138, 203, 187, 139, 206, 196, 166},
    { 76,  17,  51, 153,  37, 111, 122, 155},
    { 43, 129, 176, 106, 107, 110, 119, 146},
    { 16,  48, 144,  10,  30,  90,  59, 177},
    {109, 116, 137, 200, 178, 112, 125, 164},
    { 70, 210, 208, 202, 184, 130, 179, 115},
    {134, 191, 151,  31,  93,  68, 204, 190},
    {148,  22,  66, 198, 172,   94, 71,   2},
    {  6,  18,  54, 162,  64,  192,154,  40},
    {120, 149,  25,  75,  14,   42,126, 167},
    { 79,  26,  78,  23,  69,  207,199, 175},
    {103,  98,  83,  38, 114, 131, 182, 124},
    {161,  61, 183, 127, 170,  88,  53, 159},
    { 55, 165,  73,   8,  24,  72,   5,  15},
    { 45, 135, 194, 160,  58, 174, 100,  89}
  };

  private static final int FINDER_PAT_A = 0;
  private static final int FINDER_PAT_B = 1;
  private static final int FINDER_PAT_C = 2;
  private static final int FINDER_PAT_D = 3;
  private static final int FINDER_PAT_E = 4;
  private static final int FINDER_PAT_F = 5;

  private static final int[][] FINDER_PATTERN_SEQUENCES = {
    { FINDER_PAT_A, FINDER_PAT_A },
    { FINDER_PAT_A, FINDER_PAT_B, FINDER_PAT_B },
    { FINDER_PAT_A, FINDER_PAT_C, FINDER_PAT_B, FINDER_PAT_D },
    { FINDER_PAT_A, FINDER_PAT_E, FINDER_PAT_B, FINDER_PAT_D, FINDER_PAT_C },
    { FINDER_PAT_A, FINDER_PAT_E, FINDER_PAT_B, FINDER_PAT_D, FINDER_PAT_D, FINDER_PAT_F },
    { FINDER_PAT_A, FINDER_PAT_E, FINDER_PAT_B, FINDER_PAT_D, FINDER_PAT_E, FINDER_PAT_F, FINDER_PAT_F },
    { FINDER_PAT_A, FINDER_PAT_A, FINDER_PAT_B, FINDER_PAT_B, FINDER_PAT_C, FINDER_PAT_C, FINDER_PAT_D, FINDER_PAT_D },
    { FINDER_PAT_A, FINDER_PAT_A, FINDER_PAT_B, FINDER_PAT_B, FINDER_PAT_C, FINDER_PAT_C, FINDER_PAT_D, FINDER_PAT_E, FINDER_PAT_E },
    { FINDER_PAT_A, FINDER_PAT_A, FINDER_PAT_B, FINDER_PAT_B, FINDER_PAT_C, FINDER_PAT_C, FINDER_PAT_D, FINDER_PAT_E, FINDER_PAT_F, FINDER_PAT_F },
    { FINDER_PAT_A, FINDER_PAT_A, FINDER_PAT_B, FINDER_PAT_B, FINDER_PAT_C, FINDER_PAT_D, FINDER_PAT_D, FINDER_PAT_E, FINDER_PAT_E, FINDER_PAT_F, FINDER_PAT_F },
  };

  private static final int MAX_PAIRS = 11;

  private final List<ExpandedPair> pairs = new ArrayList<>(MAX_PAIRS);
  private final List<ExpandedRow> rows = new ArrayList<>();
  private final int [] startEnd = new int[2];
  private boolean startFromEven;

  @Override
  public Result decodeRow(int rowNumber,
                          BitArray row,
                          Map<DecodeHintType,?> hints) throws NotFoundException, FormatException {
    // Rows can start with even pattern in case in prev rows there where odd number of patters.
    // So lets try twice
    this.pairs.clear();
    this.startFromEven = false;
    try {
      return constructResult(decodeRow2pairs(rowNumber, row));
    } catch (NotFoundException e) {
      // OK
    }

    this.pairs.clear();
    this.startFromEven = true;
    return constructResult(decodeRow2pairs(rowNumber, row));
  }

  @Override
  public void reset() {
    this.pairs.clear();
    this.rows.clear();
  }

  // Not private for testing
  List<ExpandedPair> decodeRow2pairs(int rowNumber, BitArray row) throws NotFoundException {
    boolean done = false;
    while (!done) {
      try {
        this.pairs.add(retrieveNextPair(row, this.pairs, rowNumber));
      } catch (NotFoundException nfe) {
        if (this.pairs.isEmpty()) {
          throw nfe;
        }
        // exit this loop when retrieveNextPair() fails and throws
        done = true;
      }
    }

    // TODO: verify sequence of finder patterns as in checkPairSequence()
    if (checkChecksum()) {
      return this.pairs;
    }

    boolean tryStackedDecode = !this.rows.isEmpty();
    storeRow(rowNumber, false); // TODO: deal with reversed rows
    if (tryStackedDecode) {
      // When the image is 180-rotated, then rows are sorted in wrong direction.
      // Try twice with both the directions.
      List<ExpandedPair> ps = checkRows(false);
      if (ps != null) {
        return ps;
      }
      ps = checkRows(true);
      if (ps != null) {
        return ps;
      }
    }

    throw NotFoundException.getNotFoundInstance();
  }

  private List<ExpandedPair> checkRows(boolean reverse) {
    // Limit number of rows we are checking
    // We use recursive algorithm with pure complexity and don't want it to take forever
    // Stacked barcode can have up to 11 rows, so 25 seems reasonable enough
    if (this.rows.size() > 25) {
      this.rows.clear();  // We will never have a chance to get result, so clear it
      return null;
    }

    this.pairs.clear();
    if (reverse) {
      Collections.reverse(this.rows);
    }

    List<ExpandedPair> ps = null;
    try {
      ps = checkRows(new ArrayList<ExpandedRow>(), 0);
    } catch (NotFoundException e) {
      // OK
    }

    if (reverse) {
      Collections.reverse(this.rows);
    }

    return ps;
  }

  // Try to construct a valid rows sequence
  // Recursion is used to implement backtracking
  private List<ExpandedPair> checkRows(List<ExpandedRow> collectedRows, int currentRow) throws NotFoundException {
    for (int i = currentRow; i < rows.size(); i++) {
      ExpandedRow row = rows.get(i);
      this.pairs.clear();
      for (ExpandedRow collectedRow : collectedRows) {
        this.pairs.addAll(collectedRow.getPairs());
      }
      this.pairs.addAll(row.getPairs());

      if (!isValidSequence(this.pairs)) {
        continue;
      }

      if (checkChecksum()) {
        return this.pairs;
      }

      List<ExpandedRow> rs = new ArrayList<>(collectedRows);
      rs.add(row);
      try {
        // Recursion: try to add more rows
        return checkRows(rs, i + 1);
      } catch (NotFoundException e) {
        // We failed, try the next candidate
      }
    }

    throw NotFoundException.getNotFoundInstance();
  }

  // Whether the pairs form a valid find pattern sequence,
  // either complete or a prefix
  private static boolean isValidSequence(List<ExpandedPair> pairs) {
    for (int[] sequence : FINDER_PATTERN_SEQUENCES) {
      if (pairs.size() > sequence.length) {
        continue;
      }

      boolean stop = true;
      for (int j = 0; j < pairs.size(); j++) {
        if (pairs.get(j).getFinderPattern().getValue() != sequence[j]) {
          stop = false;
          break;
        }
      }

      if (stop) {
        return true;
      }
    }

    return false;
  }

  private void storeRow(int rowNumber, boolean wasReversed) {
    // Discard if duplicate above or below; otherwise insert in order by row number.
    int insertPos = 0;
    boolean prevIsSame = false;
    boolean nextIsSame = false;
    while (insertPos < this.rows.size()) {
      ExpandedRow erow = this.rows.get(insertPos);
      if (erow.getRowNumber() > rowNumber) {
        nextIsSame = erow.isEquivalent(this.pairs);
        break;
      }
      prevIsSame = erow.isEquivalent(this.pairs);
      insertPos++;
    }
    if (nextIsSame || prevIsSame) {
      return;
    }

    // When the row was partially decoded (e.g. 2 pairs found instead of 3),
    // it will prevent us from detecting the barcode.
    // Try to merge partial rows

    // Check whether the row is part of an allready detected row
    if (isPartialRow(this.pairs, this.rows)) {
      return;
    }

    this.rows.add(insertPos, new ExpandedRow(this.pairs, rowNumber, wasReversed));

    removePartialRows(this.pairs, this.rows);
  }

  // Remove all the rows that contains only specified pairs
  private static void removePartialRows(List<ExpandedPair> pairs, List<ExpandedRow> rows) {
    for (Iterator<ExpandedRow> iterator = rows.iterator(); iterator.hasNext();) {
      ExpandedRow r = iterator.next();
      if (r.getPairs().size() == pairs.size()) {
        continue;
      }
      boolean allFound = true;
      for (ExpandedPair p : r.getPairs()) {
        boolean found = false;
        for (ExpandedPair pp : pairs) {
          if (p.equals(pp)) {
            found = true;
            break;
          }
        }
        if (!found) {
          allFound = false;
          break;
        }
      }
      if (allFound) {
        // 'pairs' contains all the pairs from the row 'r'
        iterator.remove();
      }
    }
  }

  // Returns true when one of the rows already contains all the pairs
  private static boolean isPartialRow(Iterable<ExpandedPair> pairs, Iterable<ExpandedRow> rows) {
    for (ExpandedRow r : rows) {
      boolean allFound = true;
      for (ExpandedPair p : pairs) {
        boolean found = false;
        for (ExpandedPair pp : r.getPairs()) {
          if (p.equals(pp)) {
            found = true;
            break;
          }
        }
        if (!found) {
          allFound = false;
          break;
        }
      }
      if (allFound) {
        // the row 'r' contain all the pairs from 'pairs'
        return true;
      }
    }
    return false;
  }

  // Only used for unit testing
  List<ExpandedRow> getRows() {
    return this.rows;
  }

  // Not private for unit testing
  static Result constructResult(List<ExpandedPair> pairs) throws NotFoundException, FormatException {
    BitArray binary = BitArrayBuilder.buildBitArray(pairs);

    AbstractExpandedDecoder decoder = AbstractExpandedDecoder.createDecoder(binary);
    String resultingString = decoder.parseInformation();

    ResultPoint[] firstPoints = pairs.get(0).getFinderPattern().getResultPoints();
    ResultPoint[] lastPoints  = pairs.get(pairs.size() - 1).getFinderPattern().getResultPoints();

    return new Result(
          resultingString,
          null,
          new ResultPoint[]{firstPoints[0], firstPoints[1], lastPoints[0], lastPoints[1]},
          BarcodeFormat.RSS_EXPANDED
      );
  }

  private boolean checkChecksum() {
    ExpandedPair firstPair = this.pairs.get(0);
    DataCharacter checkCharacter = firstPair.getLeftChar();
    DataCharacter firstCharacter = firstPair.getRightChar();

    if (firstCharacter == null) {
      return false;
    }

    int checksum = firstCharacter.getChecksumPortion();
    int s = 2;

    for (int i = 1; i < this.pairs.size(); ++i) {
      ExpandedPair currentPair = this.pairs.get(i);
      checksum += currentPair.getLeftChar().getChecksumPortion();
      s++;
      DataCharacter currentRightChar = currentPair.getRightChar();
      if (currentRightChar != null) {
        checksum += currentRightChar.getChecksumPortion();
        s++;
      }
    }

    checksum %= 211;

    int checkCharacterValue = 211 * (s - 4) + checksum;

    return checkCharacterValue == checkCharacter.getValue();
  }

  private static int getNextSecondBar(BitArray row, int initialPos) {
    int currentPos;
    if (row.get(initialPos)) {
      currentPos = row.getNextUnset(initialPos);
      currentPos = row.getNextSet(currentPos);
    } else {
      currentPos = row.getNextSet(initialPos);
      currentPos = row.getNextUnset(currentPos);
    }
    return currentPos;
  }

  // not private for testing
  ExpandedPair retrieveNextPair(BitArray row, List<ExpandedPair> previousPairs, int rowNumber)
      throws NotFoundException {
    boolean isOddPattern  = previousPairs.size() % 2 == 0;
    if (startFromEven) {
      isOddPattern = !isOddPattern;
    }

    FinderPattern pattern;

    boolean keepFinding = true;
    int forcedOffset = -1;
    do {
      this.findNextPair(row, previousPairs, forcedOffset);
      pattern = parseFoundFinderPattern(row, rowNumber, isOddPattern);
      if (pattern == null) {
        forcedOffset = getNextSecondBar(row, this.startEnd[0]);
      } else {
        keepFinding = false;
      }
    } while (keepFinding);

    // When stacked symbol is split over multiple rows, there's no way to guess if this pair can be last or not.
    // boolean mayBeLast = checkPairSequence(previousPairs, pattern);

    DataCharacter leftChar  = this.decodeDataCharacter(row, pattern, isOddPattern, true);

    if (!previousPairs.isEmpty() && previousPairs.get(previousPairs.size() - 1).mustBeLast()) {
      throw NotFoundException.getNotFoundInstance();
    }

    DataCharacter rightChar;
    try {
      rightChar = this.decodeDataCharacter(row, pattern, isOddPattern, false);
    } catch (NotFoundException ignored) {
      rightChar = null;
    }
    return new ExpandedPair(leftChar, rightChar, pattern);
  }

  private void findNextPair(BitArray row, List<ExpandedPair> previousPairs, int forcedOffset)
      throws NotFoundException {
    int[] counters = this.getDecodeFinderCounters();
    counters[0] = 0;
    counters[1] = 0;
    counters[2] = 0;
    counters[3] = 0;

    int width = row.getSize();

    int rowOffset;
    if (forcedOffset >= 0) {
      rowOffset = forcedOffset;
    } else if (previousPairs.isEmpty()) {
      rowOffset = 0;
    } else {
      ExpandedPair lastPair = previousPairs.get(previousPairs.size() - 1);
      rowOffset = lastPair.getFinderPattern().getStartEnd()[1];
    }
    boolean searchingEvenPair = previousPairs.size() % 2 != 0;
    if (startFromEven) {
      searchingEvenPair = !searchingEvenPair;
    }

    boolean isWhite = false;
    while (rowOffset < width) {
      isWhite = !row.get(rowOffset);
      if (!isWhite) {
        break;
      }
      rowOffset++;
    }

    int counterPosition = 0;
    int patternStart = rowOffset;
    for (int x = rowOffset; x < width; x++) {
      if (row.get(x) != isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == 3) {
          if (searchingEvenPair) {
            reverseCounters(counters);
          }

          if (isFinderPattern(counters)) {
            this.startEnd[0] = patternStart;
            this.startEnd[1] = x;
            return;
          }

          if (searchingEvenPair) {
            reverseCounters(counters);
          }

          patternStart += counters[0] + counters[1];
          counters[0] = counters[2];
          counters[1] = counters[3];
          counters[2] = 0;
          counters[3] = 0;
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

  private static void reverseCounters(int [] counters) {
    int length = counters.length;
    for (int i = 0; i < length / 2; ++i) {
      int tmp = counters[i];
      counters[i] = counters[length - i - 1];
      counters[length - i - 1] = tmp;
    }
  }

  private FinderPattern parseFoundFinderPattern(BitArray row, int rowNumber, boolean oddPattern) {
    // Actually we found elements 2-5.
    int firstCounter;
    int start;
    int end;

    if (oddPattern) {
      // If pattern number is odd, we need to locate element 1 *before* the current block.

      int firstElementStart = this.startEnd[0] - 1;
      // Locate element 1
      while (firstElementStart >= 0 && !row.get(firstElementStart)) {
        firstElementStart--;
      }

      firstElementStart++;
      firstCounter = this.startEnd[0] - firstElementStart;
      start = firstElementStart;
      end = this.startEnd[1];

    } else {
      // If pattern number is even, the pattern is reversed, so we need to locate element 1 *after* the current block.

      start = this.startEnd[0];

      end = row.getNextUnset(this.startEnd[1] + 1);
      firstCounter = end - this.startEnd[1];
    }

    // Make 'counters' hold 1-4
    int [] counters = this.getDecodeFinderCounters();
    System.arraycopy(counters, 0, counters, 1, counters.length - 1);

    counters[0] = firstCounter;
    int value;
    try {
      value = parseFinderValue(counters, FINDER_PATTERNS);
    } catch (NotFoundException ignored) {
      return null;
    }
    return new FinderPattern(value, new int[] {start, end}, start, end, rowNumber);
  }

  DataCharacter decodeDataCharacter(BitArray row,
                                    FinderPattern pattern,
                                    boolean isOddPattern,
                                    boolean leftChar) throws NotFoundException {
    int[] counters = this.getDataCharacterCounters();
    for (int x = 0; x < counters.length; x++) {
      counters[x] = 0;
    }

    if (leftChar) {
      recordPatternInReverse(row, pattern.getStartEnd()[0], counters);
    } else {
      recordPattern(row, pattern.getStartEnd()[1], counters);
      // reverse it
      for (int i = 0, j = counters.length - 1; i < j; i++, j--) {
        int temp = counters[i];
        counters[i] = counters[j];
        counters[j] = temp;
      }
    } //counters[] has the pixels of the module

    int numModules = 17; //left and right data characters have all the same length
    float elementWidth = MathUtils.sum(counters) / (float) numModules;

    // Sanity check: element width for pattern and the character should match
    float expectedElementWidth = (pattern.getStartEnd()[1] - pattern.getStartEnd()[0]) / 15.0f;
    if (Math.abs(elementWidth - expectedElementWidth) / expectedElementWidth > 0.3f) {
      throw NotFoundException.getNotFoundInstance();
    }

    int[] oddCounts = this.getOddCounts();
    int[] evenCounts = this.getEvenCounts();
    float[] oddRoundingErrors = this.getOddRoundingErrors();
    float[] evenRoundingErrors = this.getEvenRoundingErrors();

    for (int i = 0; i < counters.length; i++) {
      float value = 1.0f * counters[i] / elementWidth;
      int count = (int) (value + 0.5f); // Round
      if (count < 1) {
        if (value < 0.3f) {
          throw NotFoundException.getNotFoundInstance();
        }
        count = 1;
      } else if (count > 8) {
        if (value > 8.7f) {
          throw NotFoundException.getNotFoundInstance();
        }
        count = 8;
      }
      int offset = i / 2;
      if ((i & 0x01) == 0) {
        oddCounts[offset] = count;
        oddRoundingErrors[offset] = value - count;
      } else {
        evenCounts[offset] = count;
        evenRoundingErrors[offset] = value - count;
      }
    }

    adjustOddEvenCounts(numModules);

    int weightRowNumber = 4 * pattern.getValue() + (isOddPattern ? 0 : 2) + (leftChar ? 0 : 1) - 1;

    int oddSum = 0;
    int oddChecksumPortion = 0;
    for (int i = oddCounts.length - 1; i >= 0; i--) {
      if (isNotA1left(pattern, isOddPattern, leftChar)) {
        int weight = WEIGHTS[weightRowNumber][2 * i];
        oddChecksumPortion += oddCounts[i] * weight;
      }
      oddSum += oddCounts[i];
    }
    int evenChecksumPortion = 0;
    //int evenSum = 0;
    for (int i = evenCounts.length - 1; i >= 0; i--) {
      if (isNotA1left(pattern, isOddPattern, leftChar)) {
        int weight = WEIGHTS[weightRowNumber][2 * i + 1];
        evenChecksumPortion += evenCounts[i] * weight;
      }
      //evenSum += evenCounts[i];
    }
    int checksumPortion = oddChecksumPortion + evenChecksumPortion;

    if ((oddSum & 0x01) != 0 || oddSum > 13 || oddSum < 4) {
      throw NotFoundException.getNotFoundInstance();
    }

    int group = (13 - oddSum) / 2;
    int oddWidest = SYMBOL_WIDEST[group];
    int evenWidest = 9 - oddWidest;
    int vOdd = RSSUtils.getRSSvalue(oddCounts, oddWidest, true);
    int vEven = RSSUtils.getRSSvalue(evenCounts, evenWidest, false);
    int tEven = EVEN_TOTAL_SUBSET[group];
    int gSum = GSUM[group];
    int value = vOdd * tEven + vEven + gSum;

    return new DataCharacter(value, checksumPortion);
  }

  private static boolean isNotA1left(FinderPattern pattern, boolean isOddPattern, boolean leftChar) {
    // A1: pattern.getValue is 0 (A), and it's an oddPattern, and it is a left char
    return !(pattern.getValue() == 0 && isOddPattern && leftChar);
  }

  private void adjustOddEvenCounts(int numModules) throws NotFoundException {

    int oddSum = MathUtils.sum(this.getOddCounts());
    int evenSum = MathUtils.sum(this.getEvenCounts());

    boolean incrementOdd = false;
    boolean decrementOdd = false;

    if (oddSum > 13) {
      decrementOdd = true;
    } else if (oddSum < 4) {
      incrementOdd = true;
    }
    boolean incrementEven = false;
    boolean decrementEven = false;
    if (evenSum > 13) {
      decrementEven = true;
    } else if (evenSum < 4) {
      incrementEven = true;
    }

    int mismatch = oddSum + evenSum - numModules;
    boolean oddParityBad = (oddSum & 0x01) == 1;
    boolean evenParityBad = (evenSum & 0x01) == 0;
    if (mismatch == 1) {
      if (oddParityBad) {
        if (evenParityBad) {
          throw NotFoundException.getNotFoundInstance();
        }
        decrementOdd = true;
      } else {
        if (!evenParityBad) {
          throw NotFoundException.getNotFoundInstance();
        }
        decrementEven = true;
      }
    } else if (mismatch == -1) {
      if (oddParityBad) {
        if (evenParityBad) {
          throw NotFoundException.getNotFoundInstance();
        }
        incrementOdd = true;
      } else {
        if (!evenParityBad) {
          throw NotFoundException.getNotFoundInstance();
        }
        incrementEven = true;
      }
    } else if (mismatch == 0) {
      if (oddParityBad) {
        if (!evenParityBad) {
          throw NotFoundException.getNotFoundInstance();
        }
        // Both bad
        if (oddSum < evenSum) {
          incrementOdd = true;
          decrementEven = true;
        } else {
          decrementOdd = true;
          incrementEven = true;
        }
      } else {
        if (evenParityBad) {
          throw NotFoundException.getNotFoundInstance();
        }
        // Nothing to do!
      }
    } else {
      throw NotFoundException.getNotFoundInstance();
    }

    if (incrementOdd) {
      if (decrementOdd) {
        throw NotFoundException.getNotFoundInstance();
      }
      increment(this.getOddCounts(), this.getOddRoundingErrors());
    }
    if (decrementOdd) {
      decrement(this.getOddCounts(), this.getOddRoundingErrors());
    }
    if (incrementEven) {
      if (decrementEven) {
        throw NotFoundException.getNotFoundInstance();
      }
      increment(this.getEvenCounts(), this.getOddRoundingErrors());
    }
    if (decrementEven) {
      decrement(this.getEvenCounts(), this.getEvenRoundingErrors());
    }
  }
}
