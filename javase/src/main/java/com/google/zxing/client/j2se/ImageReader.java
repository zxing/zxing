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

package com.google.zxing.client.j2se;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;

/**
 * Encapsulates reading URIs as images.
 * 
 * @author Sean Owen
 */
public final class ImageReader {

  private static final String BASE64TOKEN = "base64,";
  
  private ImageReader() {
  }

  public static BufferedImage readImage(URI uri) throws IOException {
    if ("data".equals(uri.getScheme())) {
      return readDataURIImage(uri);
    }
    BufferedImage result;
    try {
      result = ImageIO.read(uri.toURL());
    } catch (IllegalArgumentException iae) {
      throw new IOException("Resource not found: " + uri, iae);
    }
    if (result == null) {
      throw new IOException("Could not load " + uri);
    }
    return result;
  }
  
  public static BufferedImage readDataURIImage(URI uri) throws IOException {
    String uriString = uri.toString();
    if (!uriString.startsWith("data:image/")) {
      throw new IOException("Unsupported data URI MIME type");
    }
    int base64Start = uriString.indexOf(BASE64TOKEN);
    if (base64Start < 0) {
      throw new IOException("Unsupported data URI encoding");
    }
    String base64DataEncoded = uriString.substring(base64Start + BASE64TOKEN.length());
    String base64Data = URLDecoder.decode(base64DataEncoded, "UTF-8");
    byte[] imageBytes = DatatypeConverter.parseBase64Binary(base64Data);
    return ImageIO.read(new ByteArrayInputStream(imageBytes));
  }

}
