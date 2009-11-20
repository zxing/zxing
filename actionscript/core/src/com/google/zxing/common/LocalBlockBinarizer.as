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
import com.google.zxing.Binarizer;
import com.google.zxing.LuminanceSource;

public final class LocalBlockBinarizer extends Binarizer {


  private var matrix:BitMatrix = null;

  public function LocalBlockBinarizer(source:LuminanceSource) {
    super(source);
  }

  // TODO: Consider a different strategy for 1D Readers.
  public override function  getBlackRow(y:int , row:BitArray ):BitArray {
    binarizeEntireImage();
    return matrix.getRow(y, row);
  }

  // TODO: If getBlackRow() calculates its own values, removing sharpening here.
  public override function getBlackMatrix():BitMatrix {
    binarizeEntireImage();
    return matrix;
  }

  public override function createBinarizer(source:LuminanceSource ):Binarizer {
    return new LocalBlockBinarizer(source);
  }

  // Calculates the final BitMatrix once for all requests. This could be called once from the
  // constructor instead, but there are some advantages to doing it lazily, such as making
  // profiling easier, and not doing heavy lifting when callers don't expect it.
  private function binarizeEntireImage():void {
    if (matrix == null) {
      var source:LuminanceSource = getLuminanceSource();
      var luminances:Array = source.getMatrix();
      var width:int = source.getWidth();
      var height:int = source.getHeight();
      sharpenRow(luminances, width, height);

      var subWidth:int = width >> 3;
      var subHeight:int = height >> 3;
      var blackPoints:Array = calculateBlackPoints(luminances, subWidth, subHeight, width);

      matrix = new BitMatrix(width, height);
      calculateThresholdForBlock(luminances, subWidth, subHeight, width, blackPoints, matrix);
    }
  }

  // For each 8x8 block in the image, calculate the average black point using a 5x5 grid
  // of the blocks around it. Also handles the corner cases, but will ignore up to 7 pixels
  // on the right edge and 7 pixels at the bottom of the image if the overall dimsions are not
  // multiples of eight. In practice, leaving those pixels white does not seem to be a problem.
  private static function calculateThresholdForBlock(luminances:Array , subWidth:int , subHeight:int ,
      stride:int , blackPoints:Array , matrix:BitMatrix ):void {
    for (var y:int = 0; y < subHeight; y++) {
      for (var x:int = 0; x < subWidth; x++) {
        var left:int = (x > 1) ? x : 2;
        left = (left < subWidth - 2) ? left : subWidth - 3;
        var top:int = (y > 1) ? y : 2;
        top = (top < subHeight - 2) ? top : subHeight - 3;
        var sum:int = 0;
        for (var z:int = -2; z <= 2; z++) {
          sum += blackPoints[top + z][left - 2];
          sum += blackPoints[top + z][left - 1];
          sum += blackPoints[top + z][left];
          sum += blackPoints[top + z][left + 1];
          sum += blackPoints[top + z][left + 2];
        }
        var average:int = sum / 25;
        threshold8x8Block(luminances, x << 3, y << 3, average, stride, matrix);
      }
    }
  }

  // Applies a single threshold to an 8x8 block of pixels.
  private static function threshold8x8Block(luminances:Array , xoffset:int , yoffset:int , threshold:int ,
      stride:int , matrix:BitMatrix ):void {
    for (var y:int = 0; y < 8; y++) {
      var offset:int = (yoffset + y) * stride + xoffset;
      for (var x:int = 0; x < 8; x++) {
        var pixel:int = luminances[offset + x] & 0xff;
        if (pixel < threshold) {
          matrix._set(xoffset + x, yoffset + y);
        }
      }
    }
  }

  // Calculates a single black point for each 8x8 block of pixels and saves it away.
  private static function calculateBlackPoints(luminances:Array, subWidth:int, subHeight:int,
      stride:int):Array {
    //int[][] blackPoints = new int[subHeight][subWidth];
    var blackPoints:Array = new Array();
    for (var y:int = 0; y < subHeight; y++) {
      for (var x:int = 0; x < subWidth; x++) {
        var sum:int = 0;
        var min:int = 255;
        var max:int = 0;
        for (var yy:int = 0; yy < 8; yy++) {
          var offset:int = ((y << 3) + yy) * stride + (x << 3);
          for (var xx:int = 0; xx < 8; xx++) {
            var pixel:int = luminances[offset + xx] & 0xff;
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
        var average:int = (max - min > 24) ? (sum >> 6) : (min >> 1);
        blackPoints[y][x] = average;
      }
    }
    return blackPoints;
  }

  // Applies a simple -1 4 -1 box filter with a weight of 2 to each row.
  private static function sharpenRow(luminances:Array, width:int, height:int):void {
    for (var y:int = 0; y < height; y++) {
      var offset:int = y * width;
      var left:int = luminances[offset] & 0xff;
      var center:int = luminances[offset + 1] & 0xff;
      for (var x:int = 1; x < width - 1; x++) {
        var right:int = luminances[offset + x + 1] & 0xff;
        var pixel:int = ((center << 2) - left - right) >> 1;
        // Must clamp values to 0..255 so they will fit in a byte.
        if (pixel > 255) {
          pixel = 255;
        } else if (pixel < 0) {
          pixel = 0;
        }
        luminances[offset + x] = pixel;
        left = center;
        center = right;
      }
    }
  }

}

}