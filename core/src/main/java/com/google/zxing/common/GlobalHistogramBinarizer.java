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
import com.google.zxing.NotFoundException;

/**
 * This Binarizer implementation uses the old ZXing global histogram approach. It is suitable
 * for low-end mobile devices which don't have enough CPU or memory to use a local thresholding
 * algorithm. However, because it picks a global black point, it cannot handle difficult shadows
 * and gradients.
 *
 * Faster mobile devices and all desktop applications should probably use HybridBinarizer instead.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public class GlobalHistogramBinarizer extends Binarizer {

  private static final int LUMINANCE_BITS = 5;
  private static final int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
  private static final int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;
  private static final byte[] EMPTY = new byte[0];

  private byte[] luminances;
  private final int[] buckets;

  public GlobalHistogramBinarizer(LuminanceSource source) {
    super(source);
    luminances = EMPTY;
    buckets = new int[LUMINANCE_BUCKETS];
  }

  // Applies simple sharpening to the row data to improve performance of the 1D Readers.
  @Override
  public BitArray getBlackRow(int y, BitArray row) throws NotFoundException {
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
      localBuckets[(localLuminances[x] & 0xff) >> LUMINANCE_SHIFT]++;
    }
    int blackPoint = estimateBlackPoint(localBuckets);

    if (width < 3) {
      // Special case for very small images
      for (int x = 0; x < width; x++) {
        if ((localLuminances[x] & 0xff) < blackPoint) {
          row.set(x);
        }
      }
    } else {
      int[] rowBits = row.getBitArray();
      int threshold2 = blackPoint << 1;
      int left = localLuminances[0] & 0xFF;
      int center = localLuminances[1] & 0xFF;
      int word = 0;
      for (int x = 1; x < width - 1; x++) {
        int right = localLuminances[x + 1] & 0xFF;
        // A simple -1 4 -1 box filter with a weight of 2: ((4 * center - left - right) / 2) < blackPoint
        // is equivalent to (4 * center - left - right) < (2 * blackPoint) because blackPoint >= 8.
        word |= (((center << 2) - left - right - threshold2) >>> 31) << x;
        if ((x & 31) == 31) {
          rowBits[x >>> 5] = word;
          word = 0;
        }
        left = center;
        center = right;
      }
      if (word != 0) {
        rowBits[(width - 2) >>> 5] = word;
      }
    }
    return row;
  }

  // Does not sharpen the data, as this call is intended to only be used by 2D Readers.
  @Override
  public BitMatrix getBlackMatrix() throws NotFoundException {
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
      int right = (width * 4) / 5;
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
    int[] bits = matrix.getBits();
    int rowSize = matrix.getRowSize();
    int fullWords = width >>> 5;
    int tailBits = width & 31;
    for (int y = 0; y < height; y++) {
      int offset = y * width;
      int rowOffset = y * rowSize;
      for (int w = 0; w < fullWords; w++, offset += 32) {
        bits[rowOffset + w] = pack8(localLuminances, offset, blackPoint)
            | (pack8(localLuminances, offset + 8, blackPoint) << 8)
            | (pack8(localLuminances, offset + 16, blackPoint) << 16)
            | (pack8(localLuminances, offset + 24, blackPoint) << 24);
      }
      if (tailBits != 0) {
        int word = 0;
        for (int b = 0; b < tailBits; b++) {
          word |= (((localLuminances[offset + b] & 0xFF) - blackPoint) >>> 31) << b;
        }
        bits[rowOffset + fullWords] = word;
      }
    }

    return matrix;
  }

  @Override
  public Binarizer createBinarizer(LuminanceSource source) {
    return new GlobalHistogramBinarizer(source);
  }

  /**
   * Packs 8 consecutive pixels into the low 8 bits of an {@code int}, setting bit {@code i} iff
   * {@code (luminances[offset + i] & 0xFF) < limit}. Both pixel values and {@code limit} must be
   * non-negative and fit safely without int overflow (e.g. {@code [0, 256]}).
   */
  static int pack8(byte[] luminances, int offset, int limit) {
    return (((luminances[offset] & 0xFF) - limit) >>> 31)
        | ((((luminances[offset + 1] & 0xFF) - limit) >>> 31) << 1)
        | ((((luminances[offset + 2] & 0xFF) - limit) >>> 31) << 2)
        | ((((luminances[offset + 3] & 0xFF) - limit) >>> 31) << 3)
        | ((((luminances[offset + 4] & 0xFF) - limit) >>> 31) << 4)
        | ((((luminances[offset + 5] & 0xFF) - limit) >>> 31) << 5)
        | ((((luminances[offset + 6] & 0xFF) - limit) >>> 31) << 6)
        | ((((luminances[offset + 7] & 0xFF) - limit) >>> 31) << 7);
  }

  private void initArrays(int luminanceSize) {
    if (luminances.length < luminanceSize) {
      luminances = new byte[luminanceSize];
    }
    for (int x = 0; x < LUMINANCE_BUCKETS; x++) {
      buckets[x] = 0;
    }
  }

  private static int estimateBlackPoint(int[] buckets) throws NotFoundException {
    // Find the tallest peak in the histogram.
    int numBuckets = buckets.length;
    int maxBucketCount = 0;
    int firstPeak = 0;
    int firstPeakSize = 0;
    for (int x = 0; x < numBuckets; x++) {
      if (buckets[x] > firstPeakSize) {
        firstPeak = x;
        firstPeakSize = buckets[x];
      }
      if (buckets[x] > maxBucketCount) {
        maxBucketCount = buckets[x];
      }
    }

    // Find the second-tallest peak which is somewhat far from the tallest peak.
    int secondPeak = 0;
    int secondPeakScore = 0;
    for (int x = 0; x < numBuckets; x++) {
      int distanceToBiggest = x - firstPeak;
      // Encourage more distant second peaks by multiplying by square of distance.
      int score = buckets[x] * distanceToBiggest * distanceToBiggest;
      if (score > secondPeakScore) {
        secondPeak = x;
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
    if (secondPeak - firstPeak <= numBuckets / 16) {
      throw NotFoundException.getNotFoundInstance();
    }

    // Find a valley between them that is low and closer to the white peak.
    int bestValley = secondPeak - 1;
    int bestValleyScore = -1;
    for (int x = secondPeak - 1; x > firstPeak; x--) {
      int fromFirst = x - firstPeak;
      int score = fromFirst * fromFirst * (secondPeak - x) * (maxBucketCount - buckets[x]);
      if (score > bestValleyScore) {
        bestValley = x;
        bestValleyScore = score;
      }
    }

    return bestValley << LUMINANCE_SHIFT;
  }

}
