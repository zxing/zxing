/*
 * Copyright 2007 ZXing authors
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

import java.util.regex.Pattern;

/**
 * @author Sean Owen
 */
public final class URIParsedResult extends ParsedResult {

  private static final Pattern USER_IN_HOST = Pattern.compile(":/*([^/@]+)@[^/]+");

  private final String uri;
  private final String title;

  public URIParsedResult(String uri, String title) {
    super(ParsedResultType.URI);
    this.uri = massageURI(uri);
    this.title = title;
  }

  public String getURI() {
    return uri;
  }

  public String getTitle() {
    return title;
  }

  /**
   * @return true if the URI contains suspicious patterns that may suggest it intends to
   *  mislead the user about its true nature. At the moment this looks for the presence
   *  of user/password syntax in the host/authority portion of a URI which may be used
   *  in attempts to make the URI's host appear to be other than it is. Example:
   *  http://yourbank.com@phisher.com  This URI connects to phisher.com but may appear
   *  to connect to yourbank.com at first glance.
   */
  public boolean isPossiblyMaliciousURI() {
    return USER_IN_HOST.matcher(uri).find();
  }

  @Override
  public String getDisplayResult() {
    StringBuilder result = new StringBuilder(30);
    maybeAppend(title, result);
    maybeAppend(uri, result);
    return result.toString();
  }

  /**
   * Transforms a string that represents a URI into something more proper, by adding or canonicalizing
   * the protocol.
   */
  private static String massageURI(String uri) {
    uri = uri.trim();
    int protocolEnd = uri.indexOf(':');
    if (protocolEnd < 0) {
      // No protocol, assume http
      uri = "http://" + uri;
    } else if (isColonFollowedByPortNumber(uri, protocolEnd)) {
      // Found a colon, but it looks like it is after the host, so the protocol is still missing
      uri = "http://" + uri;
    }
    return uri;
  }

  private static boolean isColonFollowedByPortNumber(String uri, int protocolEnd) {
    int nextSlash = uri.indexOf('/', protocolEnd + 1);
    if (nextSlash < 0) {
      nextSlash = uri.length();
    }
    if (nextSlash <= protocolEnd + 1) {
      return false;
    }
    for (int x = protocolEnd + 1; x < nextSlash; x++) {
      if (uri.charAt(x) < '0' || uri.charAt(x) > '9') {
        return false;
      }
    }
    return true;
  }


}