#ifndef __LINESSAMPLER_H__
#define __LINESSAMPLER_H__

/*
 * Copyright 2010 ZXing authors All rights reserved.
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

#include <map>
#include <zxing/common/BitMatrix.h>
#include <zxing/ResultPoint.h>
#include <zxing/common/Point.h>

namespace zxing {
namespace pdf417 {
namespace detector {

class LinesSampler {
private:
  static const int MODULES_IN_SYMBOL = 17;
  static const int BARS_IN_SYMBOL = 8;
  static const int POSSIBLE_SYMBOLS = 2787;
  static const std::vector<float> RATIOS_TABLE;
  static std::vector<float> init_ratios_table();
  static const int BARCODE_START_OFFSET = 2;

  Ref<BitMatrix> linesMatrix_;
  int symbolsPerLine_;
  int dimension_;
  
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

   Ref<BitMatrix> sampleLines(std::vector<Ref<ResultPoint> > const &vertices,
                              int dimensionY,
                              int dimension);

  static void codewordsToBitMatrix(std::vector<std::vector<int> > &codewords,
                                   Ref<BitMatrix> &matrix);
  static int calculateClusterNumber(int codeword);
  static Ref<BitMatrix> sampleGrid(Ref<BitMatrix> image,
                                   int dimension);
  static void computeSymbolWidths(std::vector<float>& symbolWidths,
                                  const int symbolsPerLine, Ref<BitMatrix> linesMatrix);
  static void linesMatrixToCodewords(std::vector<std::vector<int> > &clusterNumbers,
                                     const int symbolsPerLine,
                                     const std::vector<float> &symbolWidths,
                                     Ref<BitMatrix> linesMatrix,
                                     std::vector<std::vector<int> > &codewords);
  static std::vector<std::vector<std::map<int, int> > >
      distributeVotes(const int symbolsPerLine,
                      const std::vector<std::vector<int> >& codewords,
                      const std::vector<std::vector<int> >& clusterNumbers);
  static std::vector<int>
      findMissingLines(const int symbolsPerLine,
                       std::vector<std::vector<int> > &detectedCodeWords);
  static int decodeRowCount(const int symbolsPerLine,
                            std::vector<std::vector<int> > &detectedCodeWords,
                            std::vector<int> &insertLinesAt);

  static int round(float d);
  static Point intersection(Line a, Line b);

public:
  LinesSampler(Ref<BitMatrix> linesMatrix, int dimension);
  Ref<BitMatrix> sample();
};

}
}
}

#endif // __LINESSAMPLER_H__
