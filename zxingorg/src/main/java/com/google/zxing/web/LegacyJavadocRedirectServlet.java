/*
 * Copyright 2014 ZXing authors
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

package com.google.zxing.web;

import com.google.common.base.Preconditions;
import com.google.common.net.HttpHeaders;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class LegacyJavadocRedirectServlet extends HttpServlet {

  private static final String PREFIX = "/w/docs/javadoc";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    String requestURI = request.getRequestURI();
    Preconditions.checkArgument(requestURI.startsWith(PREFIX));
    String requestWithoutPrefix = requestURI.substring(PREFIX.length());
    if (requestWithoutPrefix.isEmpty()) {
      requestWithoutPrefix = "/";
    }
    response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    response.setHeader(HttpHeaders.LOCATION, "http://zxing.github.io/zxing/apidocs" + requestWithoutPrefix);
  }

}
