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
 * This class implements a local thresholding algorithm, which while slower than the
 * GlobalHistogramBinarizer, is fairly efficient for what it does. It is designed for
 * high frequency images of barcodes with black data on white backgrounds. For this application,
 * it does a much better job than a global blackpoint with severe shadows and gradients.
 * However it tends to produce artifacts on lower frequency images and is therefore not
 * a good general purpose binarizer for uses outside ZXing.
 *
 * This class extends GlobalHistogramBinarizer, using the older histogram approach for 1D readers,
 * and the newer local approach for 2D readers. 1D decoding using a per-row histogram is already
 * inherently local, and only fails for horizontal gradients. We can revisit that problem later,
 * but for now it was not a win to use local blocks for 1D.
 *
 * This Binarizer is the default for the unit tests and the recommended class for library users.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class HybridBinarizer extends GlobalHistogramBinarizer {

  // This class uses 5x5 blocks to compute local luminance, where each block is 8x8 pixels.
  // So this is the smallest dimension in each axis we can accept.
  private static final int MINIMUM_DIMENSION = 40;

  private BitMatrix matrix = null;

  public HybridBinarizer(LuminanceSource source) {
    super(source);
  }

  public BitMatrix getBlackMatrix() throws NotFoundException {
    binarizeEntireImage();
    return matrix;
  }

  public Binarizer createBinarizer(LuminanceSource source) {
    return new HybridBinarizer(source);
  }

  // Calculates the final BitMatrix once for all requests. This could be called once from the
  // constructor instead, but there are some advantages to doing it lazily, such as making
  // profiling easier, and not doing heavy lifting when callers don't expect it.
  private void binarizeEntireImage() throws NotFoundException {
    if (matrix == null) {
      LuminanceSource source = getLuminanceSource();
      if (source.getWidth() >= MINIMUM_DIMENSION && source.getHeight() >= MINIMUM_DIMENSION) {
        byte[] luminances = source.getMatrix();
        int width = source.getWidth();
        int height = source.getHeight();
        int subWidth = width >> 3;
        if ((width & 0x07) != 0) {
          subWidth++;
        }
        int subHeight = height >> 3;
        if ((height & 0x07) != 0) {
          subHeight++;
        }
        int[][] blackPoints = calculateBlackPoints(luminances, subWidth, subHeight, width, height);

        matrix = new BitMatrix(width, height);
        calculateThresholdForBlock(luminances, subWidth, subHeight, width, height, blackPoints, matrix);
      } else {
        // If the image is too small, fall back to the global histogram approach.
        matrix = super.getBlackMatrix();
      }
    }
  }

  // For each 8x8 block in the image, calculate the average black point using a 5x5 grid
  // of the blocks around it. Also handles the corner cases (fractional blocks are computed based
  // on the last 8 pixels in the row/column which are also used in the previous block).
  private static void calculateThresholdForBlock(byte[] luminances, int subWidth, int subHeight,
      int width, int height, int[][] blackPoints, BitMatrix matrix) {
    for (int y = 0; y < subHeight; y++) {
      int yoffset = y << 3;
      if ((yoffset + 8) >= height) {
        yoffset = height - 8;
      }
      for (int x = 0; x < subWidth; x++) {
        int xoffset = x << 3;
        if ((xoffset + 8) >= width) {
            xoffset = width - 8;
        }
        int left = (x > 1) ? x : 2;
        left = (left < subWidth - 2) ? left : subWidth - 3;
        int top = (y > 1) ? y : 2;
        top = (top < subHeight - 2) ? top : subHeight - 3;
        int sum = 0;
        for (int z = -2; z <= 2; z++) {
          int[] blackRow = blackPoints[top + z];
          sum += blackRow[left - 2];
          sum += blackRow[left - 1];
          sum += blackRow[left];
          sum += blackRow[left + 1];
          sum += blackRow[left + 2];
        }
        int average = sum / 25;
        threshold8x8Block(luminances, xoffset, yoffset, average, width, matrix);
      }
    }
  }

  // Applies a single threshold to an 8x8 block of pixels.
  private static void threshold8x8Block(byte[] luminances, int xoffset, int yoffset, int threshold,
      int stride, BitMatrix matrix) {
    for (int y = 0; y < 8; y++) {
      int offset = (yoffset + y) * stride + xoffset;
      for (int x = 0; x < 8; x++) {
        int pixel = luminances[offset + x] & 0xff;
        if (pixel < threshold) {
          matrix.set(xoffset + x, yoffset + y);
        }
      }
    }
  }

  // Calculates a single black point for each 8x8 block of pixels and saves it away.
  private static int[][] calculateBlackPoints(byte[] luminances, int subWidth, int subHeight,
      int width, int height) {
    int[][] blackPoints = new int[subHeight][subWidth];
    for (int y = 0; y < subHeight; y++) {
      int yoffset = y << 3;
      if ((yoffset + 8) >= height) {
        yoffset = height - 8;
      }
      for (int x = 0; x < subWidth; x++) {
        int xoffset = x << 3;
        if ((xoffset + 8) >= width) {
            xoffset = width - 8;
        }
        int sum = 0;
        int min = 255;
        int max = 0;
        for (int yy = 0; yy < 8; yy++) {
          int offset = (yoffset + yy) * width + xoffset;
          for (int xx = 0; xx < 8; xx++) {
            int pixel = luminances[offset + xx] & 0xff;
            sum += pixel;
            if (pixel < min) {
              min = pixel;
            }
            if (pixel > max) {
              max = pixel;
            }
          }
        }

        // If the contrast is inadequate, use half the minimum, so that this block will be
        // treated as part of the white background, but won't drag down neighboring blocks
        // too much.
        int average;
        if (max - min > 24) {
          average = sum >> 6;
        } else {
          // When min == max == 0, let average be 1 so all is black
          average = max == 0 ? 1 : min >> 1;
        }
        blackPoints[y][x] = average;
      }
    }
    return blackPoints;
  }

}
