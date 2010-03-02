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

package com.google.zxing.multi.qrcode.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.qrcode.detector.Detector;
import com.google.zxing.qrcode.detector.FinderPatternInfo;

import java.util.Hashtable;
import java.util.Vector;

/**
 * <p>Encapsulates logic that can detect one or more QR Codes in an image, even if the QR Code
 * is rotated or skewed, or partially obscured.</p>
 *
 * @author Sean Owen
 * @author Hannes Erven
 */
public final class MultiDetector extends Detector {

  private static final DetectorResult[] EMPTY_DETECTOR_RESULTS = new DetectorResult[0];

  public MultiDetector(BitMatrix image) {
    super(image);
  }

  public DetectorResult[] detectMulti(Hashtable hints) throws NotFoundException {
    BitMatrix image = getImage();
    MultiFinderPatternFinder finder = new MultiFinderPatternFinder(image);
    FinderPatternInfo[] info = finder.findMulti(hints);

    if (info == null || info.length == 0) {
      throw NotFoundException.getNotFoundInstance();
    }

    Vector result = new Vector();
    for (int i = 0; i < info.length; i++) {
      try {
        result.addElement(processFinderPatternInfo(info[i]));
      } catch (ReaderException e) {
        // ignore
      }
    }
    if (result.isEmpty()) {
      return EMPTY_DETECTOR_RESULTS;
    } else {
      DetectorResult[] resultArray = new DetectorResult[result.size()];
      for (int i = 0; i < result.size(); i++) {
        resultArray[i] = (DetectorResult) result.elementAt(i);
      }
      return resultArray;
    }
  }

}
