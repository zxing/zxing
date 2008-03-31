/*
 * Copyright 2007 Google Inc.
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

package com.google.zxing.client.result;

import com.google.zxing.Result;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class URIParsedResult extends ParsedReaderResult {

  private final String uri;

  private URIParsedResult(String uri) {
    super(ParsedReaderResultType.URI);
    this.uri = uri;
  }

  public static URIParsedResult parse(Result result) {
    String rawText = result.getText();
    if (!isBasicallyValidURI(rawText)) {
      return null;
    }
    String uri = massagePossibleURI(rawText);
    return new URIParsedResult(uri);
  }

  public String getURI() {
    return uri;
  }

  public String getDisplayResult() {
    return uri;
  }

  /**
   * Transforms a string that possibly represents a URI into something more proper, by adding or canonicalizing
   * the protocol.
   */
  private static String massagePossibleURI(String uri) {
    // Take off leading "URL:" if present
    if (uri.startsWith("URL:")) {
      uri = uri.substring(4);
    }
    int protocolEnd = uri.indexOf(':');
    if (protocolEnd < 0) {
      // No protocol, assume http
      uri = "http://" + uri;
    } else {
      // Lowercase protocol to avoid problems
      uri = uri.substring(0, protocolEnd).toLowerCase() + uri.substring(protocolEnd);
      // TODO this logic isn't quite right for URIs like "example.org:443/foo"
    }
    return uri;
  }

  /**
   * Determines whether a string is not obviously not a URI. This implements crude checks; this class does not
   * intend to strictly check URIs as its only function is to represent what is in a barcode, but, it does
   * need to know when a string is obviously not a URI.
   */
  static boolean isBasicallyValidURI(String uri) {
    return uri != null && uri.indexOf(' ') < 0 && (uri.indexOf(':') >= 0 || uri.indexOf('.') >= 0);
  }

}