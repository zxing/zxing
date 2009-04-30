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

package com.google.zxing.multi;

import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.ResultPoint;
import com.google.zxing.CroppedMonochromeBitmapSource;

import java.util.Hashtable;
import java.util.Vector;

/**
 * <p>Attempts to locate multiple barcodes in an image by repeatedly decoding portion of the image.
 * After one barcode is found, the areas left, above, right and below the barcode's
 * {@link com.google.zxing.ResultPoint}s are scanned, recursively.</p>
 *
 * <p>A caller may want to also employ {@link ByQuadrantReader} when attempting to
 * find multiple 2D barcodes, like QR Codes, in an image, where the presence of multiple barcodes might
 * prevent detecting any one of them.</p>
 *
 * <p>That is, instead of passing a {@link Reader} a caller might pass
 * <code>new ByQuadrantReader(reader)</code>.</p>
 *
 * @author Sean Owen
 */
public final class MultipleBarcodeReader {

  private final Reader delegate;

  public MultipleBarcodeReader(Reader delegate) {
    this.delegate = delegate;
  }

  public Result[] decodeMultiple(MonochromeBitmapSource image) throws ReaderException {
    return decodeMultiple(image, null);
  }

  public Result[] decodeMultiple(MonochromeBitmapSource image, Hashtable hints) throws ReaderException {
    Vector results = new Vector();
    doDecodeMultiple(image, hints, results, 0, 0);
    if (results.isEmpty()) {
      throw ReaderException.getInstance();
    }
    int numResults = results.size();
    Result[] resultArray = new Result[numResults];
    for (int i = 0; i < numResults; i++) {
      resultArray[i] = (Result) results.elementAt(i);
    }
    return resultArray;
  }

  private void doDecodeMultiple(MonochromeBitmapSource image, Hashtable hints, Vector results, int xOffset, int yOffset) {
    Result result;
    try {
      result = delegate.decode(image, hints);
    } catch (ReaderException re) {
      return;
    }
    results.addElement(translateResultPoints(result, xOffset, yOffset));
    ResultPoint[] resultPoints = result.getResultPoints();
    if (resultPoints == null || resultPoints.length == 0) {
      return;
    }
    int width = image.getWidth();
    int height = image.getHeight();
    float minX = width;
    float minY = height;
    float maxX = 0.0f;
    float maxY = 0.0f;
    for (int i = 0; i < resultPoints.length; i++) {
      ResultPoint point = resultPoints[i];
      float x = point.getX();
      float y = point.getY();
      if (x < minX) {
        minX = x;
      }
      if (y < minY) {
        minY = y;
      }
      if (x > maxX) {
        maxX = x;
      }
      if (y > maxY) {
        maxY = y;
      }
    }

    doDecodeMultiple(new CroppedMonochromeBitmapSource(image, 0,          0,          (int) minX, height),
                     hints, results, 0,          0);
    doDecodeMultiple(new CroppedMonochromeBitmapSource(image, 0,          0,          width,      (int) minY),
                     hints, results, 0,          0);
    doDecodeMultiple(new CroppedMonochromeBitmapSource(image, (int) maxX, 0,          width,      height),
                     hints, results, (int) maxX, 0);
    doDecodeMultiple(new CroppedMonochromeBitmapSource(image, 0,          (int) maxY, width,      height),
                     hints, results, 0,          (int) maxY);
  }

  private static Result translateResultPoints(Result result, int xOffset, int yOffset) {
    ResultPoint[] oldResultPoints = result.getResultPoints();
    ResultPoint[] newResultPoints = new ResultPoint[oldResultPoints.length];
    for (int i = 0; i < oldResultPoints.length; i++) {
      ResultPoint oldPoint = oldResultPoints[i];
      newResultPoints[i] = new ResultPoint(oldPoint.getX() + xOffset, oldPoint.getY() + yOffset);
    }
    return new Result(result.getText(), result.getRawBytes(), newResultPoints, result.getBarcodeFormat());
  }

}
