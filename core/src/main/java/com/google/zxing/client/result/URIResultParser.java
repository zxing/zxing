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

import com.google.zxing.Result;

import java.util.regex.Pattern;

/**
 * Tries to parse results that are a URI of some kind.
 * 
 * @author Sean Owen
 */
public final class URIResultParser extends ResultParser {

  private static final Pattern ALLOWED_URI_CHARS_PATTERN =
      Pattern.compile("[-._~:/?#\\[\\]@!$&'()*+,;=%A-Za-z0-9]+");
  // See http://www.ietf.org/rfc/rfc2396.txt
  private static final Pattern URL_WITH_PROTOCOL_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9+-.]+:");
  private static final Pattern URL_WITHOUT_PROTOCOL_PATTERN = Pattern.compile(
      "([a-zA-Z0-9\\-]+\\.){1,6}[a-zA-Z]{2,}" + // host name elements; allow up to say 6 domain elements
      "(:\\d{1,5})?" + // maybe port
      "(/|\\?|$)"); // query, path or nothing

  @Override
  public URIParsedResult parse(Result result) {
    String rawText = getMassagedText(result);
    // We specifically handle the odd "URL" scheme here for simplicity and add "URI" for fun
    // Assume anything starting this way really means to be a URI
    if (rawText.startsWith("URL:") || rawText.startsWith("URI:")) {
      return new URIParsedResult(rawText.substring(4).trim(), null);
    }
    rawText = rawText.trim();
    if (!isBasicallyValidURI(rawText) || isPossiblyMaliciousURI(rawText)) {
      return null;
    }
    return new URIParsedResult(rawText, null);
  }

  /**
   * @return true if the URI contains suspicious patterns that may suggest it intends to
   *  mislead the user about its true nature. At the moment this looks for the presence
   *  of user/password syntax in the host/authority portion of a URI which may be used
   *  in attempts to make the URI's host appear to be other than it is. Example:
   *  http://yourbank.com@phisher.com  This URI connects to phisher.com but may appear
   *  to connect to yourbank.com at first glance.
   */
  static boolean isPossiblyMaliciousURI(String uri) {
    return !ALLOWED_URI_CHARS_PATTERN.matcher(uri).matches() || containsUserInHost(uri);
  }

  /**
   * Linear equivalent of finding {@code :/*([^/@]+)@[^/]+} anywhere in the URI, i.e. user/password
   * syntax in the authority. A regex with {@link java.util.regex.Matcher#find()} backtracks
   * quadratically here because ':' is itself a member of the userinfo class {@code [^/@]}, so a
   * scheme followed by a long run of ':' restarts the greedy scan at every colon. This scans from
   * each '@' instead, examining each character a constant number of times.
   */
  private static boolean containsUserInHost(String uri) {
    int length = uri.length();
    for (int at = uri.indexOf('@'); at >= 0; at = uri.indexOf('@', at + 1)) {
      // Host part "[^/]+": at least one non-'/' character must follow '@'.
      if (at + 1 >= length || uri.charAt(at + 1) == '/') {
        continue;
      }
      // Userinfo "[^/@]+" ends just before '@'. A ':' inside the run (with a character after it)
      // can serve as the scheme colon.
      boolean schemeColon = false;
      int i = at - 1;
      while (i >= 0 && uri.charAt(i) != '/' && uri.charAt(i) != '@') {
        if (uri.charAt(i) == ':' && i <= at - 2) {
          schemeColon = true;
        }
        i--;
      }
      if (i + 1 == at) {
        // No userinfo character immediately before '@'.
        continue;
      }
      if (schemeColon) {
        return true;
      }
      // Otherwise the scheme colon may sit before the run, separated by "/*".
      while (i >= 0 && uri.charAt(i) == '/') {
        i--;
      }
      if (i >= 0 && uri.charAt(i) == ':') {
        return true;
      }
    }
    return false;
  }

  static boolean isBasicallyValidURI(String uri) {
    if (uri.contains(" ")) {
      // Quick hack check for a common case
      return false;
    }
    // Anchor at the start. find() rescans from every position, which is quadratic on long
    // input that has no match at the start; lookingAt() matches only a prefix.
    if (URL_WITH_PROTOCOL_PATTERN.matcher(uri).lookingAt()) {
      return true;
    }
    return URL_WITHOUT_PROTOCOL_PATTERN.matcher(uri).lookingAt();
  }

}