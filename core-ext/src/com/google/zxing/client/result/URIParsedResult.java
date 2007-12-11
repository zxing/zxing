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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class URIParsedResult extends ParsedReaderResult {

  private final URI uri;

  public URIParsedResult(String rawText) {
    super(ParsedReaderResultType.URI);
    if (rawText.startsWith("URL:")) {
      // Sometimes a URL is prefixed with "URL:" -- support this
      rawText = rawText.substring(4);
    }
    try {
      uri = new URI(rawText);
    } catch (URISyntaxException urise) {
      throw new IllegalArgumentException(urise.toString());
    }
  }

  public URI getURI() {
    return uri;
  }

  @Override
  public String getDisplayResult() {
    return uri.toString();
  }

}