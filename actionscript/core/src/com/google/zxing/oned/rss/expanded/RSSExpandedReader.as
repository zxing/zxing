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

package com.google.zxing.oned.rss.expanded
{

	import com.google.zxing.BarcodeFormat;
	import com.google.zxing.NotFoundException;
	import com.google.zxing.Result;
	import com.google.zxing.common.BitArray;
	import com.google.zxing.common.flexdatatypes.ArrayList;
	import com.google.zxing.oned.rss.AbstractRSSReader;
	import com.google.zxing.oned.rss.DataCharacter;
	import com.google.zxing.oned.rss.FinderPattern;
	import com.google.zxing.oned.rss.RSSUtils;
	import com.google.zxing.oned.rss.expanded.decoders.AbstractExpandedDecoder;
/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public class RSSExpandedReader extends AbstractRSSReader{

  private static var SYMBOL_WIDEST:Array = [7, 5, 4, 3, 1];
  private static var EVEN_TOTAL_SUBSET:Array = [4, 20, 52, 104, 204];
  private static var GSUM:Array = [0, 348, 1388, 2948, 3988];

  private static var FINDER_PATTERNS:Array = [
    [1,8,4,1], // A
    [3,6,4,1], // B
    [3,4,6,1], // C
    [3,2,8,1], // D
    [2,6,5,1], // E
    [2,2,9,1]  // F
  ];

  private static var WEIGHTS:Array = [
    [  1,   3,   9,  27,  81,  32,  96,  77],
    [ 20,  60, 180, 118, 143,   7,  21,  63],
    [189, 145,  13,  39, 117, 140, 209, 205],
    [193, 157,  49, 147,  19,  57, 171,  91],
    [ 62, 186, 136, 197, 169,  85,  44, 132],
    [185, 133, 188, 142,   4,  12,  36, 108],
    [113, 128, 173,  97,  80,  29,  87,  50],
    [150,  28,  84,  41, 123, 158,  52, 156],
    [ 46, 138, 203, 187, 139, 206, 196, 166],
    [ 76,  17,  51, 153,  37, 111, 122, 155],
    [ 43, 129, 176, 106, 107, 110, 119, 146],
    [ 16,  48, 144,  10,  30,  90,  59, 177],
    [109, 116, 137, 200, 178, 112, 125, 164],
    [ 70, 210, 208, 202, 184, 130, 179, 115],
    [134, 191, 151,  31,  93,  68, 204, 190],
    [148,  22,  66, 198, 172,   94, 71,   2],
    [  6,  18,  54, 162,  64,  192,154,  40],
    [120, 149,  25,  75,  14,   42,126, 167],
    [ 79,  26,  78,  23,  69,  207,199, 175],
    [103,  98,  83,  38, 114, 131, 182, 124],
    [161,  61, 183, 127, 170,  88,  53, 159],
    [ 55, 165,  73,   8,  24,  72,   5,  15],
    [ 45, 135, 194, 160,  58, 174, 100,  89]
  ];

  private static var FINDER_PAT_A:int = 0;
  private static var FINDER_PAT_B:int = 1;
  private static var FINDER_PAT_C:int = 2;
  private static var FINDER_PAT_D:int = 3;
  private static var FINDER_PAT_E:int = 4;
  private static var FINDER_PAT_F:int = 5;

  private static var FINDER_PATTERN_SEQUENCES:Array = [
    [ FINDER_PAT_A, FINDER_PAT_A ],
    [ FINDER_PAT_A, FINDER_PAT_B, FINDER_PAT_B ],
    [ FINDER_PAT_A, FINDER_PAT_C, FINDER_PAT_B, FINDER_PAT_D ],
    [ FINDER_PAT_A, FINDER_PAT_E, FINDER_PAT_B, FINDER_PAT_D, FINDER_PAT_C ],
    [ FINDER_PAT_A, FINDER_PAT_E, FINDER_PAT_B, FINDER_PAT_D, FINDER_PAT_D, FINDER_PAT_F ],
    [ FINDER_PAT_A, FINDER_PAT_E, FINDER_PAT_B, FINDER_PAT_D, FINDER_PAT_E, FINDER_PAT_F, FINDER_PAT_F ],
    [ FINDER_PAT_A, FINDER_PAT_A, FINDER_PAT_B, FINDER_PAT_B, FINDER_PAT_C, FINDER_PAT_C, FINDER_PAT_D, FINDER_PAT_D ],
    [ FINDER_PAT_A, FINDER_PAT_A, FINDER_PAT_B, FINDER_PAT_B, FINDER_PAT_C, FINDER_PAT_C, FINDER_PAT_D, FINDER_PAT_E, FINDER_PAT_E ],
    [ FINDER_PAT_A, FINDER_PAT_A, FINDER_PAT_B, FINDER_PAT_B, FINDER_PAT_C, FINDER_PAT_C, FINDER_PAT_D, FINDER_PAT_E, FINDER_PAT_F, FINDER_PAT_F ],
    [ FINDER_PAT_A, FINDER_PAT_A, FINDER_PAT_B, FINDER_PAT_B, FINDER_PAT_C, FINDER_PAT_D, FINDER_PAT_D, FINDER_PAT_E, FINDER_PAT_E, FINDER_PAT_F, FINDER_PAT_F ],
  ];

  private static var LONGEST_SEQUENCE_SIZE:int = FINDER_PATTERN_SEQUENCES[FINDER_PATTERN_SEQUENCES.length - 1].length;

  private static var MAX_PAIRS:int = 11;
  private var pairs:ArrayList = new ArrayList(MAX_PAIRS);
  private var startEnd:Array = new Array(2);
  private var currentSequence:Array = new Array(LONGEST_SEQUENCE_SIZE);

  public override function decodeRow(rowNumber:Object , row:BitArray , o:Object ):Result {
    this.reset();
    decodeRow2pairs(rowNumber as int, row);
    return constructResult(this.pairs);
  }

  public override function reset():void {
    this.pairs.clearAll();//.setSize(0);
  }

  // Not private for testing
 public function decodeRow2pairs(rowNumber:int , row:BitArray ): ArrayList {
    while(true){
      var nextPair:ExpandedPair  = retrieveNextPair(row, this.pairs, rowNumber);
      this.pairs.addElement(nextPair);

      if (nextPair.mayBeLast()) 
      {
        if (checkChecksum()) {
          return this.pairs;
        }
        if (nextPair.mustBeLast()) {
          throw NotFoundException.getNotFoundInstance();
        }
      }
    }
  	throw NotFoundException.getNotFoundInstance
  }
  

  private static function constructResult(pairs:ArrayList):Result{
    var binary:BitArray = BitArrayBuilder.buildBitArray(pairs);

    var decoder:AbstractExpandedDecoder = AbstractExpandedDecoder.createDecoder(binary);
    var resultingString:String = decoder.parseInformation();

    var firstPoints:Array = (pairs.elementAt(0) as ExpandedPair).getFinderPattern().getResultPoints();
    var lastPoints:Array  = (pairs.lastElement() as ExpandedPair).getFinderPattern().getResultPoints();

    return new Result(
          resultingString,
          null,
          [firstPoints[0], firstPoints[1], lastPoints[0], lastPoints[1]],
          BarcodeFormat.RSS_EXPANDED
      );
  }

  private function checkChecksum():Boolean
  {
    var firstPair:ExpandedPair = this.pairs.elementAt(0) as ExpandedPair;
    var checkCharacter:DataCharacter = firstPair.getLeftChar();
    var firstCharacter:DataCharacter = firstPair.getRightChar();

    var checksum:int = firstCharacter.getChecksumPortion();
    var S:int = 2;

    for(var i:int = 1; i < this.pairs.size(); ++i){
      var currentPair:ExpandedPair = this.pairs.elementAt(i) as ExpandedPair;
      checksum += currentPair.getLeftChar().getChecksumPortion();
      S++;
      if(currentPair.getRightChar() != null){
        checksum += currentPair.getRightChar().getChecksumPortion();
        S++;
      }
    }

    checksum %= 211;

    var checkCharacterValue:int = 211 * (S - 4) + checksum;

    return checkCharacterValue == checkCharacter.getValue();
  }

  private static function getNextSecondBar(row:BitArray, initialPos:int):int{
    var currentPos:int = initialPos;
    var current:Boolean = row._get(currentPos);

    while(currentPos < row.Size && row._get(currentPos) == current) {
      currentPos++;
    }

    current = !current;
    while(currentPos < row.Size && row._get(currentPos) == current) {
      currentPos++;
    }

    return currentPos;
  }

  // not private for testing
  public function retrieveNextPair(row:BitArray, previousPairs:ArrayList, rowNumber:int):ExpandedPair{
    var isOddPattern:Boolean  = previousPairs.size() % 2 == 0;

    var pattern:FinderPattern;

    var keepFinding:Boolean = true;
    var forcedOffset:int = -1;
    do{
      this.findNextPair(row, previousPairs, forcedOffset);
      pattern = parseFoundFinderPattern(row, rowNumber, isOddPattern);
      if (pattern == null){
        forcedOffset = getNextSecondBar(row, this.startEnd[0]);
      } else {
        keepFinding = false;
      }
    }while(keepFinding);

    var mayBeLast:Boolean = checkPairSequence(previousPairs, pattern);

    var leftChar:DataCharacter  = this.decodeDataCharacter(row, pattern, isOddPattern, true);
    var rightChar:DataCharacter;
    try{
      rightChar = this.decodeDataCharacter(row, pattern, isOddPattern, false);
    }catch(nfe:NotFoundException){
      if(mayBeLast) {
        rightChar = null;
      } else {
        throw nfe;
      }
    }

    return new ExpandedPair(leftChar, rightChar, pattern, mayBeLast);
  }

  private function checkPairSequence(previousPairs:ArrayList, pattern:FinderPattern):Boolean{
    var currentSequenceLength:int = previousPairs.size() + 1;
    if(currentSequenceLength > this.currentSequence.length) {
      throw NotFoundException.getNotFoundInstance();
    }

    for(var pos:int = 0; pos < previousPairs.size(); ++pos) {
      this.currentSequence[pos] = (previousPairs.elementAt(pos) as ExpandedPair).getFinderPattern().getValue();
    }

    this.currentSequence[currentSequenceLength - 1] = pattern.getValue();

    for(var i:int = 0; i < FINDER_PATTERN_SEQUENCES.length; ++i){
      var validSequence:Array = FINDER_PATTERN_SEQUENCES[i];
      if(validSequence.length >= currentSequenceLength){
        var valid:Boolean = true;
        for(pos = 0; pos < currentSequenceLength; ++pos) {
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

  private function findNextPair(row:BitArray, previousPairs:ArrayList, forcedOffset:int):void{
    var counters:Array = this.decodeFinderCounters;
    counters[0] = 0;
    counters[1] = 0;
    counters[2] = 0;
    counters[3] = 0;

    var width:int = row.getSize();

    var rowOffset:int;
    if (forcedOffset >= 0) {
      rowOffset = forcedOffset;
    } else if (previousPairs.isEmpty()) {
      rowOffset = 0;
    } else{
      var lastPair:ExpandedPair  = previousPairs.lastElement() as ExpandedPair;
      rowOffset = lastPair.getFinderPattern().getStartEnd()[1];
    }
    var searchingEvenPair:Boolean = ((previousPairs.size() % 2) != 0);

    var isWhite:Boolean = false;
    while (rowOffset < width) {
      isWhite = !row._get(rowOffset);
      if (!isWhite) {
        break;
      }
      rowOffset++;
    }

    var counterPosition:int = 0;
    var patternStart:int = rowOffset;
    for (var x:int = rowOffset; x < width; x++) {
      var pixel:Boolean = row._get(x);
      if (pixel != isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == 3) {
          if (searchingEvenPair) {
            reverseCounters(counters);
          }
		  var ifp:Boolean = isFinderPattern(counters);
          if (ifp)
          {
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

  private static function reverseCounters(counters:Array):void{
    var length:int = counters.length;
    for(var i:int = 0; i < int(length / 2); ++i){
      var tmp:int = counters[i];
      counters[i] = counters[length - i - 1];
      counters[length - i - 1] = tmp;
    }
  }

  private function parseFoundFinderPattern(row:BitArray, rowNumber:int, oddPattern:Boolean):FinderPattern {
    // Actually we found elements 2-5.
    var firstCounter:int;
    var start:int;
    var end:int;

    if(oddPattern){
      // If pattern number is odd, we need to locate element 1 *before* the current block.

      var firstElementStart:int = this.startEnd[0] - 1;
      // Locate element 1
      while (firstElementStart >= 0 && !row._get(firstElementStart)) {
        firstElementStart--;
      }

      firstElementStart++;
      firstCounter = this.startEnd[0] - firstElementStart;
      start = firstElementStart;
      end = this.startEnd[1];

    }else{
      // If pattern number is even, the pattern is reversed, so we need to locate element 1 *after* the current block.

      start = this.startEnd[0];

      firstElementStart = this.startEnd[1] + 1;
      while (firstElementStart < row.Size && row._get(firstElementStart)) {
        firstElementStart++;
      }

      end = firstElementStart;
      firstCounter = end - this.startEnd[1];
    }

    // Make 'counters' hold 1-4
    var counters:Array = this.decodeFinderCounters;
    for (var i:int = counters.length - 1; i > 0; i--) {
      counters[i] = counters[i - 1];
    }

    counters[0] = firstCounter;
    var value:int;
    try {
      value = parseFinderValue(counters, FINDER_PATTERNS);
    } catch (nfe:NotFoundException) {
      return null;
    }
    return new FinderPattern(value, [start, end], start, end, rowNumber);
  }

  public function decodeDataCharacter(row:BitArray, pattern:FinderPattern, isOddPattern:Boolean, leftChar:Boolean):DataCharacter
  {
    var counters:Array = this.dataCharacterCounters;
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
      var j:int = counters.length - 1;
      for (var i:int = 0; i < j; i++, j--) {
        var temp:int = counters[i];
        counters[i] = counters[j];
        counters[j] = temp;
      }
    }//counters[] has the pixels of the module

    var numModules:int = 17; //left and right data characters have all the same length
    var elementWidth:Number =  count(counters) / numModules;

    var oddCounts:Array = this.oddCounts;
    var evenCounts:Array = this.evenCounts;
    var oddRoundingErrors:Array = this.oddRoundingErrors;
    var evenRoundingErrors:Array = this.evenRoundingErrors;

    for (i = 0; i < counters.length; i++) {
      var value1:Number = 1.0 * counters[i] / elementWidth;
      var count:int = int(value1 + 0.5);
      if (count < 1) {
        count = 1;
      } else if (count > 8) {
        count = 8;
      }
      var offset:int = i >> 1;
      if ((i & 0x01) == 0) {
        oddCounts[offset] = count;
        oddRoundingErrors[offset] = value1 - count;
      } else {
        evenCounts[offset] = count;
        evenRoundingErrors[offset] = value1 - count;
      }
    }

    adjustOddEvenCounts(numModules);

    var weightRowNumber:int = 4 * pattern.getValue() + (isOddPattern?0:2) + (leftChar?0:1) - 1;

    var oddSum:int = 0;
    var oddChecksumPortion:int = 0;
    for (i = oddCounts.length - 1; i >= 0; i--) {
      if(isNotA1left(pattern, isOddPattern, leftChar)){
        var weight:int = WEIGHTS[weightRowNumber][2 * i];
        oddChecksumPortion += oddCounts[i] * weight;
      }
      oddSum += oddCounts[i];
    }
    var evenChecksumPortion:int = 0;
    var evenSum:int = 0;
    for (i = evenCounts.length - 1; i >= 0; i--) {
      if(isNotA1left(pattern, isOddPattern, leftChar)){
        weight = WEIGHTS[weightRowNumber][2 * i + 1];
        evenChecksumPortion += evenCounts[i] * weight;
      }
      evenSum += evenCounts[i];
    }
    var checksumPortion:int = oddChecksumPortion + evenChecksumPortion;

    if ((oddSum & 0x01) != 0 || oddSum > 13 || oddSum < 4) {
      throw NotFoundException.getNotFoundInstance();
    }

    var group:int = int((13 - oddSum) / 2);
    var oddWidest:int = SYMBOL_WIDEST[group];
    var evenWidest:int = 9 - oddWidest;
    var vOdd:int = RSSUtils.getRSSvalue(oddCounts, oddWidest, true);
    var vEven:int = RSSUtils.getRSSvalue(evenCounts, evenWidest, false);
    var tEven:int = EVEN_TOTAL_SUBSET[group];
    var gSum:int = GSUM[group];
    var value2:int = vOdd * tEven + vEven + gSum;

    return new DataCharacter(value2, checksumPortion);
  }

  private static function isNotA1left(pattern:FinderPattern, isOddPattern:Boolean, leftChar:Boolean):Boolean {
    // A1: pattern.getValue is 0 (A), and it's an oddPattern, and it is a left char
    return !(pattern.getValue() == 0 && isOddPattern && leftChar);
  }

  private function adjustOddEvenCounts(numModules:int):void {

    var oddSum:int = count(this.oddCounts);
    var evenSum:int = count(this.evenCounts);
    var mismatch:int = oddSum + evenSum - numModules;
    var oddParityBad:Boolean = (oddSum & 0x01) == 1;
    var evenParityBad:Boolean = (evenSum & 0x01) == 0;

    var incrementOdd:Boolean = false;
    var decrementOdd:Boolean = false;

    if (oddSum > 13) {
      decrementOdd = true;
    } else if (oddSum < 4) {
      incrementOdd = true;
    }
    var incrementEven:Boolean = false;
    var decrementEven:Boolean = false;
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
}
