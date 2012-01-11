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

package com.google.zxing.oned.rss
{

import com.google.zxing.NotFoundException;
import com.google.zxing.oned.OneDReader;

public  class AbstractRSSReader extends OneDReader {

  private static var MAX_AVG_VARIANCE:int = Math.round(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.2);
  private static var MAX_INDIVIDUAL_VARIANCE:int = Math.round(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.4);

  private static var MIN_FINDER_PATTERN_RATIO:Number = 9.5 / 12.0;
  private static var MAX_FINDER_PATTERN_RATIO:Number = 12.5 / 14.0;

  protected var decodeFinderCounters:Array;
  protected var dataCharacterCounters:Array;
  protected var oddRoundingErrors:Array;
  protected var evenRoundingErrors:Array;
  protected var oddCounts:Array;
  protected var evenCounts:Array;

  public function AbstractRSSReader(){
      decodeFinderCounters = new Array(4);
      dataCharacterCounters = new Array(8);
      oddRoundingErrors = new Array(4);
      evenRoundingErrors = new Array(4);
      oddCounts = new Array(dataCharacterCounters.length / 2);
      evenCounts = new Array(dataCharacterCounters.length / 2);
  }


  protected static function parseFinderValue(counters:Array, finderPatterns:Array):int {
    for (var value:int = 0; value < finderPatterns.length; value++) {
      if (patternMatchVariance(counters, finderPatterns[value], MAX_INDIVIDUAL_VARIANCE) <
          MAX_AVG_VARIANCE) {
        return value;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  protected static function count(array:Array):int {
    var count:int = 0;
    for (var i:int = 0; i < array.length; i++) {
      count += array[i];
    }
    return count;
  }

  protected static function increment(array:Array, errors:Array):void {
    var index:int = 0;
    var biggestError:Number = errors[0];
    for (var i:int = 1; i < array.length; i++) {
      if (errors[i] > biggestError) {
        biggestError = errors[i];
        index = i;
      }
    }
    array[index]++;
  }

  protected static function decrement(array:Array, errors:Array):void {
    var index:int = 0;
    var biggestError:Array = errors[0];
    for (var i:int = 1; i < array.length; i++) {
      if (errors[i] < biggestError) {
        biggestError = errors[i];
        index = i;
      }
    }
    array[index]--;
  }

  protected static function isFinderPattern(counters:Array):Boolean {
    var firstTwoSum:int = counters[0] + counters[1];
    var sum:int = firstTwoSum + counters[2] + counters[3];
    var ratio:Number = firstTwoSum / sum;
    if (ratio >= MIN_FINDER_PATTERN_RATIO && ratio <= MAX_FINDER_PATTERN_RATIO) {
      // passes ratio test in spec, but see if the counts are unreasonable
      var minCounter:int = int.MAX_VALUE;
      var maxCounter:int = int.MIN_VALUE;
      for (var i:int = 0; i < counters.length; i++) {
        var counter:int = counters[i];
        if (counter > maxCounter) {
          maxCounter = counter;
        }
        if (counter < minCounter) {
          minCounter = counter;
        }
      }
      return maxCounter < 10 * minCounter;
    }
    return false;
  }
}
}