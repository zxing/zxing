/*
 *  Copyright 2011 ZXing authors
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

#include <algorithm>
#include <math.h>
#include <stdlib.h>
#include <zxing/multi/qrcode/detector/MultiFinderPatternFinder.h>
#include <zxing/DecodeHints.h>
#include <zxing/ReaderException.h>

namespace zxing{
namespace multi {
using namespace zxing::qrcode;

const float MultiFinderPatternFinder::MAX_MODULE_COUNT_PER_EDGE = 180;
const float MultiFinderPatternFinder::MIN_MODULE_COUNT_PER_EDGE = 9;
const float MultiFinderPatternFinder::DIFF_MODSIZE_CUTOFF_PERCENT = 0.05f;
const float MultiFinderPatternFinder::DIFF_MODSIZE_CUTOFF = 0.5f;

bool compareModuleSize(Ref<FinderPattern> a, Ref<FinderPattern> b){
    float value = a->getEstimatedModuleSize() - b->getEstimatedModuleSize();
    return value < 0.0;
}


MultiFinderPatternFinder::MultiFinderPatternFinder(Ref<BitMatrix> image, 
  Ref<ResultPointCallback> resultPointCallback) : 
    FinderPatternFinder(image, resultPointCallback)
{
}

MultiFinderPatternFinder::~MultiFinderPatternFinder(){}

std::vector<Ref<FinderPatternInfo> > MultiFinderPatternFinder::findMulti(DecodeHints const& hints){
  bool tryHarder = hints.getTryHarder();
  Ref<BitMatrix> image = image_; // Protected member
  int maxI = image->getHeight();
  int maxJ = image->getWidth();
  // We are looking for black/white/black/white/black modules in
  // 1:1:3:1:1 ratio; this tracks the number of such modules seen so far

  // Let's assume that the maximum version QR Code we support takes up 1/4 the height of the
  // image, and then account for the center being 3 modules in size. This gives the smallest
  // number of pixels the center could be, so skip this often. When trying harder, look for all
  // QR versions regardless of how dense they are.
  int iSkip = (int) (maxI / (MAX_MODULES * 4.0f) * 3);
  if (iSkip < MIN_SKIP || tryHarder) {
    iSkip = MIN_SKIP;
  }

  int stateCount[5];
  for (int i = iSkip - 1; i < maxI; i += iSkip) {
    // Get a row of black/white values
    stateCount[0] = 0;
    stateCount[1] = 0;
    stateCount[2] = 0;
    stateCount[3] = 0;
    stateCount[4] = 0;
    int currentState = 0;
    for (int j = 0; j < maxJ; j++) {
      if (image->get(j, i)) {
        // Black pixel
        if ((currentState & 1) == 1) { // Counting white pixels
          currentState++;
        }
        stateCount[currentState]++;
      } else { // White pixel
        if ((currentState & 1) == 0) { // Counting black pixels
          if (currentState == 4) { // A winner?
            if (foundPatternCross(stateCount)) { // Yes
              bool confirmed = handlePossibleCenter(stateCount, i, j);
              if (!confirmed) {
                do { // Advance to next black pixel
                  j++;
                } while (j < maxJ && !image->get(j, i));
                  j--; // back up to that last white pixel
              }
              // Clear state to start looking again
              currentState = 0;
              stateCount[0] = 0;
              stateCount[1] = 0;
              stateCount[2] = 0;
              stateCount[3] = 0;
              stateCount[4] = 0;
            } else { // No, shift counts back by two
              stateCount[0] = stateCount[2];
              stateCount[1] = stateCount[3];
              stateCount[2] = stateCount[4];
              stateCount[3] = 1;
              stateCount[4] = 0;
              currentState = 3;
            }
          } else {
            stateCount[++currentState]++;
          }
        } else { // Counting white pixels
            stateCount[currentState]++;
        }
      }
    } // for j=...

    if (foundPatternCross(stateCount)) {
      handlePossibleCenter(stateCount, i, maxJ);
    } // end if foundPatternCross
  } // for i=iSkip-1 ...
  std::vector<std::vector<Ref<FinderPattern> > > patternInfo = selectBestPatterns();
  std::vector<Ref<FinderPatternInfo> > result;
  for (unsigned int i = 0; i < patternInfo.size(); i++) {
    std::vector<Ref<FinderPattern> > pattern = patternInfo[i];
    FinderPatternFinder::orderBestPatterns(pattern);
    result.push_back(Ref<FinderPatternInfo>(new FinderPatternInfo(pattern)));
  }
  return result;
}

std::vector<std::vector<Ref<FinderPattern> > > MultiFinderPatternFinder::selectBestPatterns(){
  std::vector<Ref<FinderPattern> > possibleCenters = possibleCenters_;
  
  int size = possibleCenters.size();

  if (size < 3) {
    // Couldn't find enough finder patterns
    throw ReaderException("No code detected");
  }
  
  std::vector<std::vector<Ref<FinderPattern> > > results;

  /*
  * Begin HE modifications to safely detect multiple codes of equal size
  */
  if (size == 3) {
    results.push_back(possibleCenters_);
    return results;
  }

  // Sort by estimated module size to speed up the upcoming checks
  //TODO do a sort based on module size
  std::sort(possibleCenters.begin(), possibleCenters.end(), compareModuleSize);

  /*
  * Now lets start: build a list of tuples of three finder locations that
  *  - feature similar module sizes
  *  - are placed in a distance so the estimated module count is within the QR specification
  *  - have similar distance between upper left/right and left top/bottom finder patterns
  *  - form a triangle with 90° angle (checked by comparing top right/bottom left distance
  *    with pythagoras)
  *
  * Note: we allow each point to be used for more than one code region: this might seem
  * counterintuitive at first, but the performance penalty is not that big. At this point,
  * we cannot make a good quality decision whether the three finders actually represent
  * a QR code, or are just by chance layouted so it looks like there might be a QR code there.
  * So, if the layout seems right, lets have the decoder try to decode.     
  */

  for (int i1 = 0; i1 < (size - 2); i1++) {
    Ref<FinderPattern> p1 = possibleCenters[i1];
    for (int i2 = i1 + 1; i2 < (size - 1); i2++) {
      Ref<FinderPattern> p2 = possibleCenters[i2];
      // Compare the expected module sizes; if they are really off, skip
      float vModSize12 = (p1->getEstimatedModuleSize() - p2->getEstimatedModuleSize()) / std::min(p1->getEstimatedModuleSize(), p2->getEstimatedModuleSize());
      float vModSize12A = abs(p1->getEstimatedModuleSize() - p2->getEstimatedModuleSize());
      if (vModSize12A > DIFF_MODSIZE_CUTOFF && vModSize12 >= DIFF_MODSIZE_CUTOFF_PERCENT) {
        // break, since elements are ordered by the module size deviation there cannot be
        // any more interesting elements for the given p1.
        break;
      }
      for (int i3 = i2 + 1; i3 < size; i3++) {
        Ref<FinderPattern> p3 = possibleCenters[i3];
        // Compare the expected module sizes; if they are really off, skip
        float vModSize23 = (p2->getEstimatedModuleSize() - p3->getEstimatedModuleSize()) / std::min(p2->getEstimatedModuleSize(), p3->getEstimatedModuleSize());
        float vModSize23A = abs(p2->getEstimatedModuleSize() - p3->getEstimatedModuleSize());
        if (vModSize23A > DIFF_MODSIZE_CUTOFF && vModSize23 >= DIFF_MODSIZE_CUTOFF_PERCENT) {
          // break, since elements are ordered by the module size deviation there cannot be
          // any more interesting elements for the given p1.
          break;
        }
        std::vector<Ref<FinderPattern> > test;
        test.push_back(p1);
        test.push_back(p2);
        test.push_back(p3);
        FinderPatternFinder::orderBestPatterns(test);
        // Calculate the distances: a = topleft-bottomleft, b=topleft-topright, c = diagonal
        Ref<FinderPatternInfo> info = Ref<FinderPatternInfo>(new FinderPatternInfo(test));
        float dA = FinderPatternFinder::distance(info->getTopLeft(), info->getBottomLeft());
        float dC = FinderPatternFinder::distance(info->getTopRight(), info->getBottomLeft());
        float dB = FinderPatternFinder::distance(info->getTopLeft(), info->getTopRight());
        // Check the sizes
        float estimatedModuleCount = (dA + dB) / (p1->getEstimatedModuleSize() * 2.0f);
        if (estimatedModuleCount > MAX_MODULE_COUNT_PER_EDGE || estimatedModuleCount < MIN_MODULE_COUNT_PER_EDGE) {
          continue;
        }
        // Calculate the difference of the edge lengths in percent
        float vABBC = abs((dA - dB) / std::min(dA, dB));
        if (vABBC >= 0.1f) {
          continue;
        }
        // Calculate the diagonal length by assuming a 90° angle at topleft
        float dCpy = (float) sqrt(dA * dA + dB * dB);
        // Compare to the real distance in %
        float vPyC = abs((dC - dCpy) / std::min(dC, dCpy));
        if (vPyC >= 0.1f) {
          continue;
        }
        // All tests passed!
        results.push_back(test);
      } // end iterate p3
    } // end iterate p2
  } // end iterate p1
  if (results.empty()){
    // Nothing found!
    throw ReaderException("No code detected");    
  }
  return results;
}

} // End zxing::multi namespace
} // End zxing namespace
