/*
 * Copyright 2011 ZXing authors
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

package com.google.zxing.client.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * Utility methods for retrieving content over HTTP using the more-supported {@code java.net} classes
 * in Android.
 */
public final class HttpHelper {

  private static final Collection<String> REDIRECTOR_DOMAINS = new HashSet<String>(Arrays.asList(
    "amzn.to", "bit.ly", "bitly.com", "fb.me", "goo.gl", "is.gd", "j.mp", "lnkd.in", "ow.ly",
    "R.BEETAGG.COM", "r.beetagg.com", "SCN.BY", "su.pr", "t.co", "tinyurl.com", "tr.im"
  ));

  private HttpHelper() {
  }
  
  public enum ContentType {
    /** HTML-like content type, including HTML, XHTML, etc. */
    HTML,
    /** JSON content */
    JSON,
    /** Plain text content */
    TEXT,
  }

  /**
   * @param uri URI to retrieve
   * @param type expected text-like MIME type of that content
   * @return content as a {@code String}
   * @throws IOException if the content can't be retrieved because of a bad URI, network problem, etc.
   */
  public static String downloadViaHttp(String uri, ContentType type) throws IOException {
    String contentTypes;
    switch (type) {
      case HTML:
        contentTypes = "application/xhtml+xml,text/html,text/*,*/*";
        break;
      case JSON:
        contentTypes = "application/json,text/*,*/*";
        break;
      case TEXT:
      default:
        contentTypes = "text/*,*/*";
    }
    return downloadViaHttp(uri, contentTypes);
  }

  private static String downloadViaHttp(String uri, String contentTypes) throws IOException {
    URL url = new URL(uri);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestProperty("Accept", contentTypes);
    connection.setRequestProperty("Accept-Charset", "utf-8,*");
    connection.setRequestProperty("User-Agent", "ZXing (Android)");
    try {
      connection.connect();
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        throw new IOException("Bad HTTP response: " + connection.getResponseCode());
      }
      return consume(connection);
    } finally {
      connection.disconnect();
    }
  }

  private static String getEncoding(URLConnection connection) {
    String contentTypeHeader = connection.getHeaderField("Content-Type");
    if (contentTypeHeader != null) {
      int charsetStart = contentTypeHeader.indexOf("charset=");
      if (charsetStart >= 0) {
        return contentTypeHeader.substring(charsetStart + "charset=".length());
      }
    }
    return "UTF-8";
  }

  private static String consume(URLConnection connection) throws IOException {
    String encoding = getEncoding(connection);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    InputStream in = connection.getInputStream();
    try {
      in = connection.getInputStream();
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) > 0) {
        out.write(buffer, 0, bytesRead);
      }
    } finally {
      try {
        in.close();
      } catch (IOException ioe) {
        // continue
      }
    }
    try {
      return new String(out.toByteArray(), encoding);
    } catch (UnsupportedEncodingException uee) {
      try {
        return new String(out.toByteArray(), "UTF-8");
      } catch (UnsupportedEncodingException uee2) {
        // can't happen
        throw new IllegalStateException(uee2);
      }
    }
  }

  public static URI unredirect(URI uri) throws IOException {
    if (!REDIRECTOR_DOMAINS.contains(uri.getHost())) {
      return uri;
    }
    URL url = uri.toURL();

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setInstanceFollowRedirects(false);
    connection.setDoInput(false);
    connection.setRequestMethod("HEAD");
    connection.setRequestProperty("User-Agent", "ZXing (Android)");
    try {
      connection.connect();
      switch (connection.getResponseCode()) {
        case HttpURLConnection.HTTP_MULT_CHOICE:
        case HttpURLConnection.HTTP_MOVED_PERM:
        case HttpURLConnection.HTTP_MOVED_TEMP:
        case HttpURLConnection.HTTP_SEE_OTHER:
        case 307: // No constant for 307 Temporary Redirect ?
          String location = connection.getHeaderField("Location");
          if (location != null) {
            try {
              return new URI(location);
            } catch (URISyntaxException e) {
              // nevermind
            }
          }
      }
      return uri;
    } finally {
      connection.disconnect();
    }
  }

}
