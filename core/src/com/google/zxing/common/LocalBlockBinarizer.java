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

/**
 * This class implements a local thresholding algorithm, which while slower than the
 * GlobalHistogramBinarizer, is fairly efficient for what it does. It is designed for
 * high frequency images of barcodes with black data on white backgrounds. For this application,
 * it does a much better job than a global blackpoint with severe shadows and gradients.
 * However it tends to produce artifacts on lower frequency images and is therefore not
 * a good general purpose binarizer for uses outside ZXing.
 *
 * NOTE: This class is still experimental and may not be ready for prime time yet.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class LocalBlockBinarizer extends Binarizer {

  private BitMatrix matrix = null;

  public LocalBlockBinarizer(LuminanceSource source) {
    super(source);
  }

  // TODO: Consider a different strategy for 1D Readers.
  public BitArray getBlackRow(int y, BitArray row) {
    binarizeEntireImage();
    return matrix.getRow(y, row);
  }

  // TODO: If getBlackRow() calculates its own values, removing sharpening here.
  public BitMatrix getBlackMatrix() {
    binarizeEntireImage();
    return matrix;
  }

  public Binarizer createBinarizer(LuminanceSource source) {
    return new LocalBlockBinarizer(source);
  }

  // Calculates the final BitMatrix once for all requests. This could be called once from the
  // constructor instead, but there are some advantages to doing it lazily, such as making
  // profiling easier, and not doing heavy lifting when callers don't expect it.
  private void binarizeEntireImage() {
    if (matrix == null) {
      LuminanceSource source = getLuminanceSource();
      byte[] luminances = source.getMatrix();
      int width = source.getWidth();
      int height = source.getHeight();
      sharpenRow(luminances, width, height);

      int subWidth = width >> 3;
      int subHeight = height >> 3;
      int[][] blackPoints = calculateBlackPoints(luminances, subWidth, subHeight, width);

      matrix = new BitMatrix(width, height);
      calculateThresholdForBlock(luminances, subWidth, subHeight, width, blackPoints, matrix);
    }
  }

  // For each 8x8 block in the image, calculate the average black point using a 5x5 grid
  // of the blocks around it. Also handles the corner cases, but will ignore up to 7 pixels
  // on the right edge and 7 pixels at the bottom of the image if the overall dimensions are not
  // multiples of eight. In practice, leaving those pixels white does not seem to be a problem.
  private static void calculateThresholdForBlock(byte[] luminances, int subWidth, int subHeight,
      int stride, int[][] blackPoints, BitMatrix matrix) {
    for (int y = 0; y < subHeight; y++) {
      for (int x = 0; x < subWidth; x++) {
        int left = (x > 1) ? x : 2;
        left = (left < subWidth - 2) ? left : subWidth - 3;
        int top = (y > 1) ? y : 2;
        top = (top < subHeight - 2) ? top : subHeight - 3;
        int sum = 0;
        for (int z = -2; z <= 2; z++) {
          sum += blackPoints[top + z][left - 2];
          sum += blackPoints[top + z][left - 1];
          sum += blackPoints[top + z][left];
          sum += blackPoints[top + z][left + 1];
          sum += blackPoints[top + z][left + 2];
        }
        int average = sum / 25;
        threshold8x8Block(luminances, x << 3, y << 3, average, stride, matrix);
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
      int stride) {
    int[][] blackPoints = new int[subHeight][subWidth];
    for (int y = 0; y < subHeight; y++) {
      for (int x = 0; x < subWidth; x++) {
        int sum = 0;
        int min = 255;
        int max = 0;
        for (int yy = 0; yy < 8; yy++) {
          int offset = ((y << 3) + yy) * stride + (x << 3);
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
        int average = (max - min > 24) ? (sum >> 6) : (min >> 1);
        blackPoints[y][x] = average;
      }
    }
    return blackPoints;
  }

  // Applies a simple -1 4 -1 box filter with a weight of 2 to each row.
  private static void sharpenRow(byte[] luminances, int width, int height) {
    for (int y = 0; y < height; y++) {
      int offset = y * width;
      int left = luminances[offset] & 0xff;
      int center = luminances[offset + 1] & 0xff;
      for (int x = 1; x < width - 1; x++) {
        int right = luminances[offset + x + 1] & 0xff;
        int pixel = ((center << 2) - left - right) >> 1;
        // Must clamp values to 0..255 so they will fit in a byte.
        if (pixel > 255) {
          pixel = 255;
        } else if (pixel < 0) {
          pixel = 0;
        }
        luminances[offset + x] = (byte)pixel;
        left = center;
        center = right;
      }
    }
  }

}
