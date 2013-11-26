/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.client.glass;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

import java.awt.color.CMMException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * @author Sean Owen
 */
final class DecodeHelper {

  // No real reason to deal with more than maybe 8.3 megapixels
  private static final int MAX_PIXELS = 1 << 23;
  private static final Map<DecodeHintType,Object> HINTS;

  static {
    HINTS = new EnumMap<>(DecodeHintType.class);
    HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    HINTS.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
  }

  private DecodeHelper() {
  }

  static Collection<Result> processStream(InputStream is) throws IOException {
    BufferedImage image;
    try {
      image = ImageIO.read(is);
    } catch (CMMException | IllegalArgumentException e) {
      throw new IOException(e);
    }
    if (image == null) {
      throw new IOException("No image");
    }
    if (image.getHeight() <= 1 || image.getWidth() <= 1 ||
        image.getHeight() * image.getWidth() > MAX_PIXELS) {
      throw new IOException("Dimensions out of bounds: " + image.getWidth() + 'x' + image.getHeight());
    }
    
    return processImage(image);
  }
  
  private static Collection<Result> processImage(BufferedImage image) {

    Reader reader = new MultiFormatReader();
    LuminanceSource source = new BufferedImageLuminanceSource(image);
    BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
    Collection<Result> results = new ArrayList<>(1);

    if (results.isEmpty()) {
      try {
        // Look for normal barcode in photo
        Result theResult = reader.decode(bitmap, HINTS);
        if (theResult != null) {
          results.add(theResult);
        }
      } catch (ReaderException re) {
        // continue
      }
    }

    if (results.isEmpty()) {
      try {
        // Try again with other binarizer
        BinaryBitmap hybridBitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result theResult = reader.decode(hybridBitmap, HINTS);
        if (theResult != null) {
          results.add(theResult);
        }
      } catch (ReaderException re) {
        // continue
      }
    }

    return results;
  }

}
