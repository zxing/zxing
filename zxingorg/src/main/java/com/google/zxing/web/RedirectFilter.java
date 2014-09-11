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

import com.google.common.net.HttpHeaders;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public final class RedirectFilter implements Filter {

  private static final String OLD_JAVADOC_PREFIX = "/w/docs/javadoc";

  @Override
  public void init(FilterConfig filterConfig) {
    // do nothing
  }

  @Override
  public void doFilter(ServletRequest servletRequest,
                       ServletResponse servletResponse,
                       FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    String requestURI = request.getRequestURI();
    if ("/".equals(requestURI) || "/index.jspx".equals(requestURI) ||
        "/w/".equals(requestURI) || "/w/index.jspx".equals(requestURI)) {
      redirect(servletResponse, "/w/decode.jspx");
    } else if (requestURI.startsWith(OLD_JAVADOC_PREFIX)) {
      String withoutPrefix = requestURI.substring(OLD_JAVADOC_PREFIX.length());
      redirect(servletResponse, "http://zxing.github.io/zxing/apidocs" + withoutPrefix);
    } else {
      filterChain.doFilter(servletRequest, servletResponse);
    }
  }

  private static void redirect(ServletResponse servletResponse, String location) {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    response.setHeader(HttpHeaders.LOCATION, location);
  }

  @Override
  public void destroy() {
    // do nothing
  }

}
