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

package com.google.zxing.common;

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

  private static final int LUMINANCE_BITS = 5;
  private static final int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
  private static final int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

  private byte[] luminances = null;
  private int[] buckets = null;

  public GlobalHistogramBinarizer(LuminanceSource source) {
    super(source);
  }

  // Applies simple sharpening to the row data to improve performance of the 1D Readers.
  public BitArray getBlackRow(int y, BitArray row) throws ReaderException {
    LuminanceSource source = getLuminanceSource();
    int width = source.getWidth();
    if (row == null || row.getSize() < width) {
      row = new BitArray(width);
    } else {
      row.clear();
    }

    initArrays(width);
    byte[] localLuminances = source.getRow(y, luminances);
    int[] localBuckets = buckets;
    for (int x = 0; x < width; x++) {
      int pixel = localLuminances[x] & 0xff;
      localBuckets[pixel >> LUMINANCE_SHIFT]++;
    }
    int blackPoint = estimateBlackPoint(localBuckets);

    int left = localLuminances[0] & 0xff;
    int center = localLuminances[1] & 0xff;
    for (int x = 1; x < width - 1; x++) {
      int right = localLuminances[x + 1] & 0xff;
      // A simple -1 4 -1 box filter with a weight of 2.
      int luminance = ((center << 2) - left - right) >> 1;
      if (luminance < blackPoint) {
        row.set(x);
      }
      left = center;
      center = right;
    }
    return row;
  }

  // Does not sharpen the data, as this call is intended to only be used by 2D Readers.
  public BitMatrix getBlackMatrix() throws ReaderException {
    LuminanceSource source = getLuminanceSource();
    int width = source.getWidth();
    int height = source.getHeight();
    BitMatrix matrix = new BitMatrix(width, height);

    // Quickly calculates the histogram by sampling four rows from the image. This proved to be
    // more robust on the blackbox tests than sampling a diagonal as we used to do.
    initArrays(width);
    int[] localBuckets = buckets;
    for (int y = 1; y < 5; y++) {
      int row = height * y / 5;
      byte[] localLuminances = source.getRow(row, luminances);
      int right = width * 4 / 5;
      for (int x = width / 5; x < right; x++) {
        int pixel = localLuminances[x] & 0xff;
        localBuckets[pixel >> LUMINANCE_SHIFT]++;
      }
    }
    int blackPoint = estimateBlackPoint(localBuckets);

    // We delay reading the entire image luminance until the black point estimation succeeds.
    // Although we end up reading four rows twice, it is consistent with our motto of
    // "fail quickly" which is necessary for continuous scanning.
    byte[] localLuminances = source.getMatrix();
    for (int y = 0; y < height; y++) {
      int offset = y * width;
      for (int x = 0; x< width; x++) {
        int pixel = localLuminances[offset + x] & 0xff; 
        if (pixel < blackPoint) {
          matrix.set(x, y);
        }
      }
    }

    return matrix;
  }

  public Binarizer createBinarizer(LuminanceSource source) {
    return new GlobalHistogramBinarizer(source);
  }

  private void initArrays(int luminanceSize) {
    if (luminances == null || luminances.length < luminanceSize) {
      luminances = new byte[luminanceSize];
    }
    if (buckets == null) {
      buckets = new int[LUMINANCE_BUCKETS];
    } else {
      for (int x = 0; x < LUMINANCE_BUCKETS; x++) {
        buckets[x] = 0;
      }
    }
  }

  private static int estimateBlackPoint(int[] buckets) throws ReaderException {
    // Find the tallest peak in the histogram.
    int numBuckets = buckets.length;
    int maxBucketCount = 0;
    int firstPeak = 0;
    int firstPeakSize = 0;
    for (int i = 0; i < numBuckets; i++) {
      if (buckets[i] > firstPeakSize) {
        firstPeak = i;
        firstPeakSize = buckets[i];
      }
      if (buckets[i] > maxBucketCount) {
        maxBucketCount = buckets[i];
      }
    }

    // Find the second-tallest peak which is somewhat far from the tallest peak.
    int secondPeak = 0;
    int secondPeakScore = 0;
    for (int i = 0; i < numBuckets; i++) {
      int distanceToBiggest = i - firstPeak;
      // Encourage more distant second peaks by multiplying by square of distance.
      int score = buckets[i] * distanceToBiggest * distanceToBiggest;
      if (score > secondPeakScore) {
        secondPeak = i;
        secondPeakScore = score;
      }
    }

    // Make sure firstPeak corresponds to the black peak.
    if (firstPeak > secondPeak) {
      int temp = firstPeak;
      firstPeak = secondPeak;
      secondPeak = temp;
    }

    // If there is too little contrast in the image to pick a meaningful black point, throw rather
    // than waste time trying to decode the image, and risk false positives.
    // TODO: It might be worth comparing the brightest and darkest pixels seen, rather than the
    // two peaks, to determine the contrast.
    if (secondPeak - firstPeak <= numBuckets >> 4) {
      throw ReaderException.getInstance();
    }

    // Find a valley between them that is low and closer to the white peak.
    int bestValley = secondPeak - 1;
    int bestValleyScore = -1;
    for (int i = secondPeak - 1; i > firstPeak; i--) {
      int fromFirst = i - firstPeak;
      int score = fromFirst * fromFirst * (secondPeak - i) * (maxBucketCount - buckets[i]);
      if (score > bestValleyScore) {
        bestValley = i;
        bestValleyScore = score;
      }
    }

    return bestValley << LUMINANCE_SHIFT;
  }

}
