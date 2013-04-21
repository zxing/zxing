#ifndef __DETECTOR_H__
#define __DETECTOR_H__

/*
 *  Detector.h
 *  zxing
 *
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

#include <zxing/common/Point.h>
#include <zxing/common/DetectorResult.h>
#include <zxing/NotFoundException.h>
#include <zxing/BinaryBitmap.h>
#include <zxing/DecodeHints.h>

namespace zxing {
namespace pdf417 {
namespace detector {

class Detector {
private:
  static const int MAX_AVG_VARIANCE;
  static const int MAX_INDIVIDUAL_VARIANCE;
  static const int START_PATTERN[];
  static const int START_PATTERN_REVERSE[];
  static const int STOP_PATTERN[];
  static const int STOP_PATTERN_REVERSE[];
  static const int SIZEOF_START_PATTERN;
  static const int SIZEOF_START_PATTERN_REVERSE;
  static const int SIZEOF_STOP_PATTERN;
  static const int SIZEOF_STOP_PATTERN_REVERSE;
  static const int COUNT_VERTICES;

  Ref<BinaryBitmap> image_;
  
  static std::vector<Ref<ResultPoint> > findVertices(Ref<BitMatrix> matrix, int rowStep);
  static std::vector<Ref<ResultPoint> > findVertices180(Ref<BitMatrix> matrix, int rowStep);

  static ArrayRef<int> findGuardPattern(Ref<BitMatrix> matrix,
                                        int column,
                                        int row,
                                        int width,
                                        bool whiteFirst,
                                        const int pattern[],
                                        int patternSize,
                                        ArrayRef<int> counters);
  static int patternMatchVariance(ArrayRef<int> counters, const int pattern[],
                                  int maxIndividualVariance);

  static void correctVertices(Ref<BitMatrix> matrix,
                              std::vector<Ref<ResultPoint> > &vertices,
                              bool upsideDown);
  static void findWideBarTopBottom(Ref<BitMatrix> matrix,
                                   std::vector<Ref<ResultPoint> > &vertices,
                                   int offsetVertice,
                                   int startWideBar,
                                   int lenWideBar,
                                   int lenPattern,
                                   int nIncrement);
  static void findCrossingPoint(std::vector<Ref<ResultPoint> > &vertices,
                                int idxResult,
                                int idxLineA1,int idxLineA2,
                                int idxLineB1,int idxLineB2,
                                Ref<BitMatrix> matrix);
  static Point intersection(Line a, Line b);
  static float computeModuleWidth(std::vector<Ref<ResultPoint> > &vertices);
  static int computeDimension(Ref<ResultPoint> topLeft,
                              Ref<ResultPoint> topRight,
                              Ref<ResultPoint> bottomLeft,
                              Ref<ResultPoint> bottomRight,
                              float moduleWidth);
  int computeYDimension(Ref<ResultPoint> topLeft,
                        Ref<ResultPoint> topRight,
                        Ref<ResultPoint> bottomLeft,
                        Ref<ResultPoint> bottomRight,
                        float moduleWidth);

  Ref<BitMatrix> sampleLines(const std::vector<Ref<ResultPoint> > &vertices, int dimensionY, int dimension);

  static int round(float d);

public:
  Detector(Ref<BinaryBitmap> image);
  Ref<BinaryBitmap> getImage();
  Ref<DetectorResult> detect();
  Ref<DetectorResult> detect(DecodeHints const& hints);
};

}
}
}

#endif // __DETECTOR_H__
