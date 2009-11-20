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

package com.google.zxing.multi.qrcode.detector
{

import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.flexdatatypes.ArrayList;
import com.google.zxing.common.flexdatatypes.HashTable;
import com.google.zxing.qrcode.detector.Detector;
import com.google.zxing.ReaderException;

/**
 * <p>Encapsulates logic that can detect one or more QR Codes in an image, even if the QR Code
 * is rotated or skewed, or partially obscured.</p>
 *
 * @author Sean Owen
 * @author Hannes Erven
 */
public final class MultiDetector extends Detector {

  private static var EMPTY_DETECTOR_RESULTS:Array = new Array(0);

  public function MultiDetector(image:BitMatrix) {
    super(image);
  }

  public function detectMulti(hints:HashTable):Array {
    var image:BitMatrix  = getImage();
    var finder:MultiFinderPatternFinder = new MultiFinderPatternFinder(image);
    var info:Array = finder.findMulti(hints);

    if (info == null || info.length == 0) {
      throw new ReaderException("multi : qrcode : detector : MultiDetector : info contains no elements");
    }

    var result:ArrayList = new ArrayList();
    for (var i:int = 0; i < info.length; i++) {
      try {
        result.addElement(processFinderPatternInfo(info[i]));
      } catch (e:ReaderException) {
        // ignore
      }
    }
    if (result.isEmpty()) {
      return EMPTY_DETECTOR_RESULTS;
    } else {
      var resultArray:Array = new Array(result.size());
      for (var i3:int = 0; i3 < result.size(); i3++) {
         resultArray[i3] = ( result.elementAt(i3) as DetectorResult);
      }
      return resultArray;
    }
  }
}
}