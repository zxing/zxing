/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing.oned.rss
{

	import com.google.zxing.BarcodeFormat;
	import com.google.zxing.DecodeHintType;
	import com.google.zxing.NotFoundException;
	import com.google.zxing.Result;
	import com.google.zxing.ResultPoint;
	import com.google.zxing.ResultPointCallback;
	import com.google.zxing.common.BitArray;
	import com.google.zxing.common.flexdatatypes.ArrayList;
	import com.google.zxing.common.flexdatatypes.Enumeration;
	import com.google.zxing.common.flexdatatypes.HashTable;
	import com.google.zxing.common.flexdatatypes.StringBuilder;
	
	/**
	 * Decodes RSS-14, including truncated and stacked variants. See ISO/IEC 24724:2006.
	 */
	public class RSS14Reader extends AbstractRSSReader 
	{
	
	  private static var OUTSIDE_EVEN_TOTAL_SUBSET:Array = [1,10,34,70,126];
	  private static var INSIDE_ODD_TOTAL_SUBSET:Array = [4,20,48,81];
	  private static var OUTSIDE_GSUM:Array = [0,161,961,2015,2715];
	  private static var INSIDE_GSUM:Array = [0,336,1036,1516];
	  private static var OUTSIDE_ODD_WIDEST:Array = [8,6,4,3,1];
	  private static var INSIDE_ODD_WIDEST:Array = [2,4,6,8];
	
	  private static var FINDER_PATTERNS:Array = [
	      [3,8,2,1],
	      [3,5,5,1],
	      [3,3,7,1],
	      [3,1,9,1],
	      [2,7,4,1],
	      [2,5,6,1],
	      [2,3,8,1],
	      [1,5,7,1],
	      [1,3,9,1],
	  ];
	
	  private var possibleLeftPairs:ArrayList;
	  private var possibleRightPairs:ArrayList;
	
	  public function RSS14Reader() 
	  {
	    possibleLeftPairs = new ArrayList();
	    possibleRightPairs = new ArrayList();
	  }
	
	  public override function decodeRow(rowNumber:Object , row:BitArray , o:Object ):Result 
	  {
	    var leftPair:Pair = decodePair(row, false, rowNumber as int, o as HashTable);
    
	    addOrTally(possibleLeftPairs, leftPair);
	    row.reverse();
	    var rightPair:Pair = decodePair(row, true, rowNumber as int, o as HashTable);
	    addOrTally(possibleRightPairs, rightPair);
	    row.reverse();
	    var numLeftPairs:int = possibleLeftPairs.size();
	    var numRightPairs:int = possibleRightPairs.size();
	    for (var l:int = 0; l < numLeftPairs; l++) {
	      var left:Pair = possibleLeftPairs.elementAt(l) as Pair;
	      if (left.getCount() > 1) {
	        for (var r:int = 0; r < numRightPairs; r++) {
	          var right:Pair = possibleRightPairs.elementAt(r) as Pair;
	          if (right.getCount() > 1) {
	            if (checkChecksum(left, right)) {
	              return constructResult(left, right);
	            }
	          }
	        }
	      }
	    }
	    throw NotFoundException.getNotFoundInstance();
	  }
	
	  private static function addOrTally(possiblePairs:ArrayList, pair:Pair):void 
	  {
	    if (pair != null) 
	    {
		    //var e:Enumeration = new Enumeration(possiblePairs.elements());
		    var found:Boolean = false;
		    var cntr:int = 0;
		    var max:int = possiblePairs.Count;
		    
		    for(var i:int=0;i<max;i++) 
		    {
		      var other:Pair = possiblePairs.elementAt(i) as Pair;
		      if (other.getValue() == pair.getValue()) 
		      {
		        other.incrementCount();
		        found = true;
		        break;
		      }
		    }
		    if (!found) {
		      possiblePairs.addElement(pair);
		    }
	    }
	    }
	
	  public override function reset():void {
	    possibleLeftPairs.clearAll();//.setSize(0);
	    possibleRightPairs.clearAll();//.setSize(0);
	  }
	
	  private static function constructResult(leftPair:Pair, rightPair:Pair):Result {
	    var symbolValue:Number = 4537077 * leftPair.getValue() + rightPair.getValue();
	    var text:String = symbolValue.toString();
	
	    var buffer:StringBuilder  = new StringBuilder(14);
	    for (var i:int = 13 - text.length; i > 0; i--) {
	      buffer.Append('0');
	    }
	    buffer.Append(text);
	
	    var checkDigit:int = 0;
	    for (i = 0; i < 13; i++) {
	      var digit:int = (buffer.charAt(i)).charCodeAt(0) - ('0' as String).charCodeAt(0);
	      checkDigit += (i & 0x01) == 0 ? 3 * digit : digit;
	    }
	    checkDigit = 10 - (checkDigit % 10);
	    if (checkDigit == 10) {
	      checkDigit = 0;
	    }
	    buffer.Append(checkDigit);
	
	    var leftPoints:Array = leftPair.getFinderPattern().getResultPoints();
	    var rightPoints:Array = rightPair.getFinderPattern().getResultPoints();
	    return new Result(
	        buffer.toString(),
	        null,
	        [ leftPoints[0], leftPoints[1], rightPoints[0], rightPoints[1], ],
	        BarcodeFormat.RSS_14);
	  }
	
	  private static function checkChecksum(leftPair:Pair, rightPair:Pair):Boolean {
	    var leftFPValue:int = leftPair.getFinderPattern().getValue();
	    var rightFPValue:int = rightPair.getFinderPattern().getValue();
	    if ((leftFPValue == 0 && rightFPValue == 8) ||
	        (leftFPValue == 8 && rightFPValue == 0)) {
	    }
	    var checkValue:int = (leftPair.getChecksumPortion() + 16 * rightPair.getChecksumPortion()) % 79;
	    var targetCheckValue:int =
	        9 * leftPair.getFinderPattern().getValue() + rightPair.getFinderPattern().getValue();
	    if (targetCheckValue > 72) {
	      targetCheckValue--;
	    }
	    if (targetCheckValue > 8) {
	      targetCheckValue--;
	    }
	    return checkValue == targetCheckValue;
	  }
	
	  private function decodePair( row:BitArray,  right:Boolean, rowNumber:int,  hints:HashTable):Pair 
	  {
	    try {
	      var startEnd:Array = findFinderPattern(row, 0, right);
	      var pattern:FinderPattern = parseFoundFinderPattern(row, rowNumber, right, startEnd);
	
	      var resultPointCallback:ResultPointCallback  = hints == null ? null :
	        (hints._get(DecodeHintType.NEED_RESULT_POINT_CALLBACK) as ResultPointCallback);
	
	      if (resultPointCallback != null) {
	        var center:Number = (startEnd[0] + startEnd[1]) / 2.0;
	        if (right) {
	          // row is actually reversed
	          center = row.getSize() - 1 - center;
	        }
	        resultPointCallback.foundPossibleResultPoint(new ResultPoint(center, rowNumber));
	      }
	
	      var outside:DataCharacter = decodeDataCharacter(row, pattern, true);
	      var inside:DataCharacter = decodeDataCharacter(row, pattern, false);
	      return new Pair(1597 * outside.getValue() + inside.getValue(),
	                      outside.getChecksumPortion() + 4 * inside.getChecksumPortion(),
	                      pattern);
	    } catch (re:NotFoundException) {
	      return null;
	    }
	    return null;//
	  }
	
	  private function decodeDataCharacter(row:BitArray, pattern:FinderPattern , outsideChar:Boolean ):DataCharacter {
	
	    var counters:Array = dataCharacterCounters;
	    counters[0] = 0;
	    counters[1] = 0;
	    counters[2] = 0;
	    counters[3] = 0;
	    counters[4] = 0;
	    counters[5] = 0;
	    counters[6] = 0;
	    counters[7] = 0;
	
	    if (outsideChar) {
	      recordPatternInReverse(row, pattern.getStartEnd()[0], counters);
	    } else {
	      recordPattern(row, pattern.getStartEnd()[1] + 1, counters);
	      // reverse it
	      var j:int;
	      for (i = 0, j = counters.length - 1; i < j; i++, j--) {
	        var temp:int = counters[i];
	        counters[i] = counters[j];
	        counters[j] = temp;
	      }
	    }
	
	    var numModules:int = outsideChar ? 16 : 15;
	    var elementWidth:Number = count(counters) / numModules;
	
	    var oddCounts:Array = this.oddCounts;
	    var evenCounts:Array = this.evenCounts;
	    var oddRoundingErrors:Array = this.oddRoundingErrors;
	    var evenRoundingErrors:Array = this.evenRoundingErrors;
	
	    for (var i:int = 0; i < counters.length; i++) 
	    {
	      var value:Number = counters[i] / elementWidth;
	      var count:int = int(value + 0.5); // Round
	      if (count < 1) {
	        count = 1;
	      } else if (count > 8) {
	        count = 8;
	      }
	      var offset:int = i >> 1;
	      if ((i & 0x01) == 0) {
	        oddCounts[offset] = count;
	        oddRoundingErrors[offset] = value - count;
	      } else {
	        evenCounts[offset] = count;
	        evenRoundingErrors[offset] = value - count;
	      }
	    }
	
	    adjustOddEvenCounts(outsideChar, numModules);
	
	    var oddSum:int = 0;
	    var oddChecksumPortion:int = 0;
	    for (i = oddCounts.length - 1; i >= 0; i--) {
	      oddChecksumPortion *= 9;
	      oddChecksumPortion += oddCounts[i];
	      oddSum += oddCounts[i];
	    }
	    var evenChecksumPortion:int = 0;
	    var evenSum:int = 0;
	    for (i = evenCounts.length - 1; i >= 0; i--) {
	      evenChecksumPortion *= 9;
	      evenChecksumPortion += evenCounts[i];
	      evenSum += evenCounts[i];
	    }
	    var checksumPortion:int = oddChecksumPortion + 3*evenChecksumPortion;
	
	    if (outsideChar) {
	      if ((oddSum & 0x01) != 0 || oddSum > 12 || oddSum < 4) {
	        throw NotFoundException.getNotFoundInstance();
	      }
	      var group:int = (12 - oddSum) / 2;
	      var oddWidest:int = OUTSIDE_ODD_WIDEST[group];
	      var evenWidest:int = 9 - oddWidest;
	      var vOdd:int = RSSUtils.getRSSvalue(oddCounts, oddWidest, false);
	      var vEven:int = RSSUtils.getRSSvalue(evenCounts, evenWidest, true);
	      var tEven:int = OUTSIDE_EVEN_TOTAL_SUBSET[group];
	      var gSum:int = OUTSIDE_GSUM[group];
	      return new DataCharacter(vOdd * tEven + vEven + gSum, checksumPortion);
	    } else {
	      if ((evenSum & 0x01) != 0 || evenSum > 10 || evenSum < 4) {
	        throw NotFoundException.getNotFoundInstance();
	      }
	      group = (10 - evenSum) / 2;
	      oddWidest = INSIDE_ODD_WIDEST[group];
	      evenWidest = 9 - oddWidest;
	      vOdd = RSSUtils.getRSSvalue(oddCounts, oddWidest, true);
	      vEven = RSSUtils.getRSSvalue(evenCounts, evenWidest, false);
	      var tOdd:int = INSIDE_ODD_TOTAL_SUBSET[group];
	      gSum = INSIDE_GSUM[group];
	      return new DataCharacter(vEven * tOdd + vOdd + gSum, checksumPortion);
	    }
	
	  }
	
	  private function findFinderPattern(row:BitArray , rowOffset:int, rightFinderPattern:Boolean ):Array 
	  {
	
	    var counters:Array = decodeFinderCounters;
	    counters[0] = 0;
	    counters[1] = 0;
	    counters[2] = 0;
	    counters[3] = 0;
	
	    var width:int = row.getSize();
	    var isWhite:Boolean = false;
	    while (rowOffset < width) {
	      isWhite = !row._get(rowOffset);
	      if (rightFinderPattern == isWhite) {
	        // Will encounter white first when searching for right finder pattern
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
	          if (isFinderPattern(counters)) {
	            return [patternStart, x];
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
	
	  private function parseFoundFinderPattern(row:BitArray, rowNumber:int, right:Boolean, startEnd:Array):FinderPattern {
	    // Actually we found elements 2-5
	    var firstIsBlack:Boolean = row._get(startEnd[0]);
	    var firstElementStart:int = startEnd[0] - 1;
	    // Locate element 1
	    while (firstElementStart >= 0 && (firstIsBlack != row._get(firstElementStart))) {
	      firstElementStart--;
	    }
	    firstElementStart++;
	    var firstCounter:int = startEnd[0] - firstElementStart;
	    // Make 'counters' hold 1-4
	    var counters:Array = decodeFinderCounters;
	    for (var i:int = counters.length - 1; i > 0; i--) {
	      counters[i] = counters[i-1];
	    }
	    counters[0] = firstCounter;
	    var value:int = parseFinderValue(counters, FINDER_PATTERNS);
	    var start :int= firstElementStart;
	    var end:int = startEnd[1];
	    if (right) {
	      // row is actually reversed
	      start = row.getSize() - 1 - start;
	      end = row.getSize() - 1 - end;
	    }
	    return new FinderPattern(value, [firstElementStart, startEnd[1]], start, end, rowNumber);
	  }
	
	  /*
	  private static int[] normalizeE2SEValues(int[] counters) {
	    int p = 0;
	    for (int i = 0; i < counters.length; i++) {
	      p += counters[i];
	    }
	    int[] normalized = new int[counters.length - 2];
	    for (int i = 0; i < normalized.length; i++) {
	      int e = counters[i] + counters[i+1];
	      float eRatio = (float) e / (float) p;
	      float E = ((eRatio * 32.0f) + 1.0f) / 2.0f;
	      normalized[i] = (int) E;
	    }
	    return normalized;
	  }
	   */
	
	  private function adjustOddEvenCounts(outsideChar:Boolean, numModules:int):void {
	
	    var oddSum:int = count(oddCounts);
	    var evenSum:int = count(evenCounts);
	    var mismatch:int = oddSum + evenSum - numModules;
	    var oddParityBad:Boolean = (oddSum & 0x01) == (outsideChar ? 1 : 0);
	    var evenParityBad:Boolean = (evenSum & 0x01) == 1;
	
	    var incrementOdd:Boolean = false;
	    var decrementOdd:Boolean = false;
	    var incrementEven:Boolean = false;
	    var decrementEven:Boolean = false;
	
	    if (outsideChar) {
	      if (oddSum > 12) {
	        decrementOdd = true;
	      } else if (oddSum < 4) {
	        incrementOdd = true;
	      }
	      if (evenSum > 12) {
	        decrementEven = true;
	      } else if (evenSum < 4) {
	        incrementEven = true;
	      }
	    } else {
	      if (oddSum > 11) {
	        decrementOdd = true;
	      } else if (oddSum < 5) {
	        incrementOdd = true;
	      }
	      if (evenSum > 10) {
	        decrementEven = true;
	      } else if (evenSum < 4) {
	        incrementEven = true;
	      }
	    }
	
	    /*if (mismatch == 2) {
	      if (!(oddParityBad && evenParityBad)) {
	        throw ReaderException.getInstance();
	      }
	      decrementOdd = true;
	      decrementEven = true;
	    } else if (mismatch == -2) {
	      if (!(oddParityBad && evenParityBad)) {
	        throw ReaderException.getInstance();
	      }
	      incrementOdd = true;
	      incrementEven = true;
	    } else */
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
	      increment(oddCounts, oddRoundingErrors);
	    }
	    if (decrementOdd) {
	      decrement(oddCounts, oddRoundingErrors);
	    }
	    if (incrementEven) {
	      if (decrementEven) {
	        throw NotFoundException.getNotFoundInstance();
	      }
	      increment(evenCounts, oddRoundingErrors);
	    }
	    if (decrementEven) {
	      decrement(evenCounts, evenRoundingErrors);
	    }
	
	  }
	
	}
}
