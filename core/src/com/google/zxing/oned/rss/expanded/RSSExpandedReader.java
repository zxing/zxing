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

import java.util.Hashtable;
import java.util.Vector;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.oned.rss.AbstractRSSReader;
import com.google.zxing.oned.rss.DataCharacter;
import com.google.zxing.oned.rss.FinderPattern;
import com.google.zxing.oned.rss.RSSUtils;
import com.google.zxing.oned.rss.expanded.decoders.AbstractExpandedDecoder;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public final class RSSExpandedReader extends AbstractRSSReader{

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

  private static final int [][] FINDER_PATTERN_SEQUENCES = {
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

  private static final int LONGEST_SEQUENCE_SIZE = FINDER_PATTERN_SEQUENCES[FINDER_PATTERN_SEQUENCES.length - 1].length;

  private static final int MAX_PAIRS = 11;
  private final Vector pairs = new Vector(MAX_PAIRS);
  private final int [] startEnd = new int[2];
  private final int [] currentSequence = new int[LONGEST_SEQUENCE_SIZE];

  public Result decodeRow(int rowNumber, BitArray row, Hashtable hints) throws NotFoundException {
    this.reset();
    decodeRow2pairs(rowNumber, row);
    return constructResult(this.pairs);
  }

  public void reset() {
    this.pairs.setSize(0);
  }

  // Not private for testing
  Vector decodeRow2pairs(int rowNumber, BitArray row) throws NotFoundException {
    while(true){
      ExpandedPair nextPair = retrieveNextPair(row, this.pairs, rowNumber);
      this.pairs.addElement(nextPair);

      if(nextPair.mayBeLast()){
        if(checkChecksum()) {
          return this.pairs;
        }
        if(nextPair.mustBeLast()) {
          throw NotFoundException.getNotFoundInstance();
        }
      }
    }
  }

  private static Result constructResult(Vector pairs) throws NotFoundException{
    BitArray binary = BitArrayBuilder.buildBitArray(pairs);

    AbstractExpandedDecoder decoder = AbstractExpandedDecoder.createDecoder(binary);
    String resultingString = decoder.parseInformation();

    ResultPoint [] firstPoints = ((ExpandedPair)pairs.elementAt(0)).getFinderPattern().getResultPoints();
    ResultPoint [] lastPoints  = ((ExpandedPair)pairs.lastElement()).getFinderPattern().getResultPoints();

    return new Result(
          resultingString,
          null,
          new ResultPoint[]{firstPoints[0], firstPoints[1], lastPoints[0], lastPoints[1]},
          BarcodeFormat.RSS_EXPANDED
      );
  }

  private boolean checkChecksum(){
    ExpandedPair firstPair = (ExpandedPair)this.pairs.elementAt(0);
    DataCharacter checkCharacter = firstPair.getLeftChar();
    DataCharacter firstCharacter = firstPair.getRightChar();

    int checksum = firstCharacter.getChecksumPortion();
    int S = 2;

    for(int i = 1; i < this.pairs.size(); ++i){
      ExpandedPair currentPair = (ExpandedPair)this.pairs.elementAt(i);
      checksum += currentPair.getLeftChar().getChecksumPortion();
      S++;
      if(currentPair.getRightChar() != null){
        checksum += currentPair.getRightChar().getChecksumPortion();
        S++;
      }
    }

    checksum %= 211;

    int checkCharacterValue = 211 * (S - 4) + checksum;

    return checkCharacterValue == checkCharacter.getValue();
  }

  private static int getNextSecondBar(BitArray row, int initialPos){
    int currentPos = initialPos;
    boolean current = row.get(currentPos);

    while(currentPos < row.size && row.get(currentPos) == current) {
      currentPos++;
    }

    current = !current;
    while(currentPos < row.size && row.get(currentPos) == current) {
      currentPos++;
    }

    return currentPos;
  }

  // not private for testing
  ExpandedPair retrieveNextPair(BitArray row, Vector previousPairs, int rowNumber) throws NotFoundException{
    boolean isOddPattern  = previousPairs.size() % 2 == 0;

    FinderPattern pattern;

    boolean keepFinding = true;
    int forcedOffset = -1;
    do{
      this.findNextPair(row, previousPairs, forcedOffset);
      pattern = parseFoundFinderPattern(row, rowNumber, isOddPattern);
      if (pattern == null){
        forcedOffset = getNextSecondBar(row, this.startEnd[0]);
      } else {
        keepFinding = false;
      }
    }while(keepFinding);

    boolean mayBeLast = checkPairSequence(previousPairs, pattern);

    DataCharacter leftChar  = this.decodeDataCharacter(row, pattern, isOddPattern, true);
    DataCharacter rightChar;
    try{
      rightChar = this.decodeDataCharacter(row, pattern, isOddPattern, false);
    }catch(NotFoundException nfe){
      if(mayBeLast) {
        rightChar = null;
      } else {
        throw nfe;
      }
    }

    return new ExpandedPair(leftChar, rightChar, pattern, mayBeLast);
  }

  private boolean checkPairSequence(Vector previousPairs, FinderPattern pattern) throws NotFoundException{
    int currentSequenceLength = previousPairs.size() + 1;
    if(currentSequenceLength > this.currentSequence.length) {
      throw NotFoundException.getNotFoundInstance();
    }

    for(int pos = 0; pos < previousPairs.size(); ++pos) {
      this.currentSequence[pos] = ((ExpandedPair) previousPairs.elementAt(pos)).getFinderPattern().getValue();
    }

    this.currentSequence[currentSequenceLength - 1] = pattern.getValue();

    for(int i = 0; i < FINDER_PATTERN_SEQUENCES.length; ++i){
      int [] validSequence = FINDER_PATTERN_SEQUENCES[i];
      if(validSequence.length >= currentSequenceLength){
        boolean valid = true;
        for(int pos = 0; pos < currentSequenceLength; ++pos) {
          if (this.currentSequence[pos] != validSequence[pos]) {
            valid = false;
            break;
          }
        }

        if(valid) {
          return currentSequenceLength == validSequence.length;
        }
      }
    }

    throw NotFoundException.getNotFoundInstance();
  }

  private void findNextPair(BitArray row, Vector previousPairs, int forcedOffset) throws NotFoundException{
    int[] counters = this.decodeFinderCounters;
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
    } else{
      ExpandedPair lastPair = ((ExpandedPair)previousPairs.lastElement());
      rowOffset = lastPair.getFinderPattern().getStartEnd()[1];
    }
    boolean searchingEvenPair = previousPairs.size() % 2 != 0;

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
      boolean pixel = row.get(x);
      if (pixel ^ isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == 3) {
          if (searchingEvenPair) {
            reverseCounters(counters);
          }

          if (isFinderPattern(counters)){
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

  private static void reverseCounters(int [] counters){
    int length = counters.length;
    for(int i = 0; i < length / 2; ++i){
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

    if(oddPattern){
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

    }else{
      // If pattern number is even, the pattern is reversed, so we need to locate element 1 *after* the current block.

      start = this.startEnd[0];

      int firstElementStart = this.startEnd[1] + 1;
      while(row.get(firstElementStart) && firstElementStart < row.size) {
        firstElementStart++;
      }

      end = firstElementStart;
      firstCounter = end - this.startEnd[1];
    }

    // Make 'counters' hold 1-4
    int [] counters = this.decodeFinderCounters;
    for (int i = counters.length - 1; i > 0; i--) {
      counters[i] = counters[i - 1];
    }

    counters[0] = firstCounter;
    int value;
    try {
      value = parseFinderValue(counters, FINDER_PATTERNS);
    } catch (NotFoundException nfe) {
      return null;
    }
    return new FinderPattern(value, new int[] {start, end}, start, end, rowNumber);
  }

  DataCharacter decodeDataCharacter(BitArray row, FinderPattern pattern, boolean isOddPattern, boolean leftChar)
    throws NotFoundException {
    int[] counters = this.dataCharacterCounters;
    counters[0] = 0;
    counters[1] = 0;
    counters[2] = 0;
    counters[3] = 0;
    counters[4] = 0;
    counters[5] = 0;
    counters[6] = 0;
    counters[7] = 0;

    if (leftChar) {
      recordPatternInReverse(row, pattern.getStartEnd()[0], counters);
    } else {
      recordPattern(row, pattern.getStartEnd()[1] + 1, counters);
      // reverse it
      for (int i = 0, j = counters.length - 1; i < j; i++, j--) {
        int temp = counters[i];
        counters[i] = counters[j];
        counters[j] = temp;
      }
    }//counters[] has the pixels of the module

    int numModules = 17; //left and right data characters have all the same length
    float elementWidth = (float) count(counters) / (float) numModules;

    int[] oddCounts = this.oddCounts;
    int[] evenCounts = this.evenCounts;
    float[] oddRoundingErrors = this.oddRoundingErrors;
    float[] evenRoundingErrors = this.evenRoundingErrors;

    for (int i = 0; i < counters.length; i++) {
      float value = 1.0f * counters[i] / elementWidth;
      int count = (int) (value + 0.5f); // Round
      if (count < 1) {
        count = 1;
      } else if (count > 8) {
        count = 8;
      }
      int offset = i >> 1;
      if ((i & 0x01) == 0) {
        oddCounts[offset] = count;
        oddRoundingErrors[offset] = value - count;
      } else {
        evenCounts[offset] = count;
        evenRoundingErrors[offset] = value - count;
      }
    }

    adjustOddEvenCounts(numModules);

    int weightRowNumber = 4 * pattern.getValue() + (isOddPattern?0:2) + (leftChar?0:1) - 1;

    int oddSum = 0;
    int oddChecksumPortion = 0;
    for (int i = oddCounts.length - 1; i >= 0; i--) {
      if(isNotA1left(pattern, isOddPattern, leftChar)){
        int weight = WEIGHTS[weightRowNumber][2 * i];
        oddChecksumPortion += oddCounts[i] * weight;
      }
      oddSum += oddCounts[i];
    }
    int evenChecksumPortion = 0;
    int evenSum = 0;
    for (int i = evenCounts.length - 1; i >= 0; i--) {
      if(isNotA1left(pattern, isOddPattern, leftChar)){
        int weight = WEIGHTS[weightRowNumber][2 * i + 1];
        evenChecksumPortion += evenCounts[i] * weight;
      }
      evenSum += evenCounts[i];
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

    int oddSum = count(this.oddCounts);
    int evenSum = count(this.evenCounts);
    int mismatch = oddSum + evenSum - numModules;
    boolean oddParityBad = (oddSum & 0x01) == 1;
    boolean evenParityBad = (evenSum & 0x01) == 0;

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
      increment(this.oddCounts, this.oddRoundingErrors);
    }
    if (decrementOdd) {
      decrement(this.oddCounts, this.oddRoundingErrors);
    }
    if (incrementEven) {
      if (decrementEven) {
        throw NotFoundException.getNotFoundInstance();
      }
      increment(this.evenCounts, this.oddRoundingErrors);
    }
    if (decrementEven) {
      decrement(this.evenCounts, this.evenRoundingErrors);
    }
  }
}
