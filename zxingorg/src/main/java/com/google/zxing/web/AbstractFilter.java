/*
 * Copyright 2017 ZXing authors
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

import com.google.common.net.HttpHeaders;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides no-op default implementations for convenience of subclasses.
 */
abstract class AbstractFilter implements Filter {

  @Override
  public final void init(FilterConfig filterConfig) {
    // do nothing
  }

  @Override
  public final void destroy() {
    // do nothing
  }
  
  static void redirect(ServletResponse servletResponse, String location) {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    response.setHeader(HttpHeaders.LOCATION, location);
  }

}
