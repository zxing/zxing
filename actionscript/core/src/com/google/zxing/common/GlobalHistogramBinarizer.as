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
package com.google.zxing.common
{

import com.google.zxing.Binarizer;
import com.google.zxing.LuminanceSource;
import com.google.zxing.ReaderException;

/**
 * This Binarizer implementation uses the old ZXing global histogram approach. It is suitable
 * for low-end mobile devices which don't have enough CPU or memory to use a local thresholding
 * algorithm. However, because it picks a global black point, it cannot handle difficult shadows
 * and gradients.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class GlobalHistogramBinarizer extends Binarizer {


  private static var LUMINANCE_BITS:int = 5;
  private static var LUMINANCE_SHIFT:int = 8 - LUMINANCE_BITS;
  private static var LUMINANCE_BUCKETS:int = 1 << LUMINANCE_BITS;

  private var luminances:Array = null;
  private var buckets:Array = null;

  public function GlobalHistogramBinarizer(source:LuminanceSource ) {
    super(source);
  }

  // Applies simple sharpening to the row data to improve performance of the 1D Readers.
  public override function getBlackRow(y:int , row:BitArray ):BitArray {
    var source:LuminanceSource  = getLuminanceSource();
    var width:int = source.getWidth();
    if (row == null || row.getSize() < width) {
      row = new BitArray(width);
    } else {
      row.clear();
    }

    initArrays(width);
    var _localLuminances:Array = source.getRow(y, luminances);
    var localBuckets:Array = buckets;
    for (var x2:int = 0; x2 < width; x2++) 
    {
      var pixel:int = _localLuminances[x2] & 0xff;
      localBuckets[pixel >> LUMINANCE_SHIFT]++;
    }
    
    var blackPoint:int = estimateBlackPoint(localBuckets);

    var left:int = _localLuminances[0] & 0xff;
    var center:int = _localLuminances[1] & 0xff;
    for (var x:int = 1; x < width - 1; x++) 
    {
      var right:int = _localLuminances[x + 1] & 0xff;
      // A simple -1 4 -1 box filter with a weight of 2.
      var luminance:int = ((center << 2) - left - right) >> 1;
      if (luminance < blackPoint) {
        row._set(x);
      }
      left = center;
      center = right;
    }
    return row;
  }

  // Does not sharpen the data, as this call is intended to only be used by 2D Readers.
  public override function getBlackMatrix():BitMatrix {
    var source:LuminanceSource  = getLuminanceSource();
    var width:int = source.getWidth();
    var height:int = source.getHeight();
    var matrix:BitMatrix  = new BitMatrix(width, height);

    // Quickly calculates the histogram by sampling four rows from the image. This proved to be
    // more robust on the blackbox tests than sampling a diagonal as we used to do.
    initArrays(width);
    var _localLuminances:Array;
    var localBuckets:Array = buckets;//assign empty array
    for (var y2:int = 1; y2 < 5; y2++) 
    {
      var row:int = height * y2 / 5;
      _localLuminances = source.getRow(row, luminances);
      var right:int = (width << 2) / 5;
      for (var x:int = width / 5; x < right; x++) 
      {
        var pixel:int = _localLuminances[x] & 0xff;
        var index:int = Math.floor(pixel >> LUMINANCE_SHIFT);
        localBuckets[index]++;
      }
    }

    var blackPoint:int = estimateBlackPoint(localBuckets);

    // We delay reading the entire image luminance until the black point estimation succeeds.
    // Although we end up reading four rows twice, it is consistent with our motto of
    // "fail quickly" which is necessary for continuous scanning.
    _localLuminances = source.getMatrix();
    for (var y:int = 0; y < height; y++) 
    {
      var offset:int = y * width;
      for (var x2:int = 0; x2< width; x2++) 
      {
        var pixel2:int = _localLuminances[offset + x2] & 0xff; 
        if (pixel2 < blackPoint) 
        {
          matrix._set(x2, y);
        }
      }
    }

    return matrix;
  }

  public override function createBinarizer(source:LuminanceSource):Binarizer {
    return new GlobalHistogramBinarizer(source);
  }

  private function initArrays(luminanceSize:int):void {
    if (luminances == null || luminances.length < luminanceSize) 
    {
      luminances = new Array(luminanceSize);
    }
    for (var i:int=0;i<luminances.length;i++) { luminances[i]=0;}
    
    if (buckets == null) 
    {
      buckets = new Array(LUMINANCE_BUCKETS);
    }
    for (var j:int=0;j<buckets.length;j++) { buckets[j]=0;}
  }

  private static function estimateBlackPoint(buckets:Array):int {
    // Find the tallest peak in the histogram.
    var numBuckets:int = buckets.length;
    var maxBucketCount:int = 0;
    var firstPeak:int = 0;
    var firstPeakSize:int = 0;
    for (var x:int = 0; x < numBuckets; x++) {
      if (buckets[x] > firstPeakSize) {
        firstPeak = x;
        firstPeakSize = buckets[x];
      }
      if (buckets[x] > maxBucketCount) {
        maxBucketCount = buckets[x];
      }
    }

    // Find the second-tallest peak which is somewhat far from the tallest peak.
    var secondPeak:int = 0;
    var secondPeakScore:int = 0;
    for (var x2:int = 0; x2 < numBuckets; x2++) {
      var distanceToBiggest:int = x2 - firstPeak;
      // Encourage more distant second peaks by multiplying by square of distance.
      var score:int = buckets[x2] * distanceToBiggest * distanceToBiggest;
      if (score > secondPeakScore) {
        secondPeak = x2;
        secondPeakScore = score;
      }
    }

    // Make sure firstPeak corresponds to the black peak.
    if (firstPeak > secondPeak) {
      var temp:int = firstPeak;
      firstPeak = secondPeak;
      secondPeak = temp;
    }

    // If there is too little contrast in the image to pick a meaningful black point, throw rather
    // than waste time trying to decode the image, and risk false positives.
    // TODO: It might be worth comparing the brightest and darkest pixels seen, rather than the
    // two peaks, to determine the contrast.
    if (secondPeak - firstPeak <= numBuckets >> 4) {
      throw new ReaderException("GlobalHistogramBinarizer : estimateBlackPoint");
    }

    // Find a valley between them that is low and closer to the white peak.
    var bestValley:int = secondPeak - 1;
    var bestValleyScore:int = -1;
    for (var x3:int = secondPeak - 1; x3 > firstPeak; x3--) {
      var fromFirst:int = x3 - firstPeak;
      var score2:int = fromFirst * fromFirst * (secondPeak - x3) * (maxBucketCount - buckets[x3]);
      if (score2 > bestValleyScore) {
        bestValley = x3;
        bestValleyScore = score2;
      }
    }

    return bestValley << LUMINANCE_SHIFT;
  }

}

}