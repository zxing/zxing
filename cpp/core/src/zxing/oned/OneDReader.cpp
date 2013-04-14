// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Copyright 2010 ZXing authors All rights reserved.
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

#include <zxing/ZXing.h>
#include <zxing/oned/OneDReader.h>
#include <zxing/ReaderException.h>
#include <zxing/oned/OneDResultPoint.h>
#include <zxing/NotFoundException.h>
#include <math.h>
#include <limits.h>

using std::vector;
using zxing::Ref;
using zxing::Result;
using zxing::NotFoundException;
using zxing::oned::OneDReader;

// VC++
using zxing::BinaryBitmap;
using zxing::BitArray;
using zxing::DecodeHints;

OneDReader::OneDReader() {}

Ref<Result> OneDReader::decode(Ref<BinaryBitmap> image, DecodeHints hints) {
  try {
    return doDecode(image, hints);
  } catch (NotFoundException const& nfe) {
    // std::cerr << "trying harder" << std::endl;
    bool tryHarder = hints.getTryHarder();
    if (tryHarder && image->isRotateSupported()) {
      // std::cerr << "v rotate" << std::endl;
      Ref<BinaryBitmap> rotatedImage(image->rotateCounterClockwise());
      // std::cerr << "^ rotate" << std::endl;
      Ref<Result> result = doDecode(rotatedImage, hints);
      // Doesn't have java metadata stuff
      ArrayRef< Ref<ResultPoint> >& points (result->getResultPoints());
      if (points && !points->empty()) {
        int height = rotatedImage->getHeight();
        for (int i = 0; i < points->size(); i++) {
          points[i].reset(new OneDResultPoint(height - points[i]->getY() - 1, points[i]->getX()));
        }
      }
      // std::cerr << "tried harder" << std::endl;
      return result;
    } else {
      // std::cerr << "tried harder nfe" << std::endl;
      throw nfe;
    }
  }
}

#include <typeinfo>

Ref<Result> OneDReader::doDecode(Ref<BinaryBitmap> image, DecodeHints hints) {
  int width = image->getWidth();
  int height = image->getHeight();
  Ref<BitArray> row(new BitArray(width));

  int middle = height >> 1;
  bool tryHarder = hints.getTryHarder();
  int rowStep = std::max(1, height >> (tryHarder ? 8 : 5));
  using namespace std;
  // cerr << "rS " << rowStep << " " << height << " " << tryHarder << endl;
  int maxLines;
  if (tryHarder) {
    maxLines = height; // Look at the whole image, not just the center
  } else {
    maxLines = 15; // 15 rows spaced 1/32 apart is roughly the middle half of the image
  }

  for (int x = 0; x < maxLines; x++) {

    // Scanning from the middle out. Determine which row we're looking at next:
    int rowStepsAboveOrBelow = (x + 1) >> 1;
    bool isAbove = (x & 0x01) == 0; // i.e. is x even?
    int rowNumber = middle + rowStep * (isAbove ? rowStepsAboveOrBelow : -rowStepsAboveOrBelow);
    if (false) {
      std::cerr << "rN "
                << rowNumber << " "
                << height << " "
                << middle << " "
                << rowStep << " "
                << isAbove << " "
                << rowStepsAboveOrBelow
                << std::endl;
    }
    if (rowNumber < 0 || rowNumber >= height) {
      // Oops, if we run off the top or bottom, stop
      break;
    }

    // Estimate black point for this row and load it:
    try {
      row = image->getBlackRow(rowNumber, row);
    } catch (NotFoundException const& ignored) {
      (void)ignored;
      continue;
    }

    // While we have the image data in a BitArray, it's fairly cheap to reverse it in place to
    // handle decoding upside down barcodes.
    for (int attempt = 0; attempt < 2; attempt++) {
      if (attempt == 1) {
        row->reverse(); // reverse the row and continue
      }

      // Java hints stuff missing

      try {
        // Look for a barcode
        // std::cerr << "rn " << rowNumber << " " << typeid(*this).name() << std::endl;
        Ref<Result> result = decodeRow(rowNumber, row);
        // We found our barcode
        if (attempt == 1) {
          // But it was upside down, so note that
          // result.putMetadata(ResultMetadataType.ORIENTATION, new Integer(180));
          // And remember to flip the result points horizontally.
          ArrayRef< Ref<ResultPoint> > points(result->getResultPoints());
          if (points) {
            points[0] = Ref<ResultPoint>(new OneDResultPoint(width - points[0]->getX() - 1,
                                                             points[0]->getY()));
            points[1] = Ref<ResultPoint>(new OneDResultPoint(width - points[1]->getX() - 1,
                                                             points[1]->getY()));
            
          }
        }
        return result;
      } catch (ReaderException const& re) {
        (void)re;
        continue;
      }
    }
  }
  throw NotFoundException();
}

int OneDReader::patternMatchVariance(vector<int>& counters,
                                     vector<int> const& pattern,
                                     int maxIndividualVariance) {
  return patternMatchVariance(counters, &pattern[0], maxIndividualVariance);
}

int OneDReader::patternMatchVariance(vector<int>& counters,
                                     int const pattern[],
                                     int maxIndividualVariance) {
  int numCounters = counters.size();
  unsigned int total = 0;
  unsigned int patternLength = 0;
  for (int i = 0; i < numCounters; i++) {
    total += counters[i];
    patternLength += pattern[i];
  }
  if (total < patternLength) {
    // If we don't even have one pixel per unit of bar width, assume this is too small
    // to reliably match, so fail:
    return INT_MAX;
  }
  // We're going to fake floating-point math in integers. We just need to use more bits.
  // Scale up patternLength so that intermediate values below like scaledCounter will have
  // more "significant digits"
  int unitBarWidth = (total << INTEGER_MATH_SHIFT) / patternLength;
  maxIndividualVariance = (maxIndividualVariance * unitBarWidth) >> INTEGER_MATH_SHIFT;

  int totalVariance = 0;
  for (int x = 0; x < numCounters; x++) {
    int counter = counters[x] << INTEGER_MATH_SHIFT;
    int scaledPattern = pattern[x] * unitBarWidth;
    int variance = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
    if (variance > maxIndividualVariance) {
      return INT_MAX;
    }
    totalVariance += variance;
  }
  return totalVariance / total;
}

void OneDReader::recordPattern(Ref<BitArray> row,
                               int start,
                               vector<int>& counters) {
  int numCounters = counters.size();
  for (int i = 0; i < numCounters; i++) {
    counters[i] = 0;
  }
  int end = row->getSize();
  if (start >= end) {
    throw NotFoundException();
  }
  bool isWhite = !row->get(start);
  int counterPosition = 0;
  int i = start;
  while (i < end) {
    if (row->get(i) ^ isWhite) { // that is, exactly one is true
      counters[counterPosition]++;
    } else {
      counterPosition++;
      if (counterPosition == numCounters) {
        break;
      } else {
        counters[counterPosition] = 1;
        isWhite = !isWhite;
      }
    }
    i++;
  }
  // If we read fully the last section of pixels and filled up our counters -- or filled
  // the last counter but ran off the side of the image, OK. Otherwise, a problem.
  if (!(counterPosition == numCounters || (counterPosition == numCounters - 1 && i == end))) {
    throw NotFoundException();
  }
}

OneDReader::~OneDReader() {}
