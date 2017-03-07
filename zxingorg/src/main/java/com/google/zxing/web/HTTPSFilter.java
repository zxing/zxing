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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Redirects things to HTTPS, like the main decode page, which should prefer HTTPS.
 */
@WebFilter("/w/decode.jspx")
public final class HTTPSFilter extends AbstractFilter {

  @Override
  public void doFilter(ServletRequest servletRequest, 
                       ServletResponse servletResponse, 
                       FilterChain chain) throws IOException, ServletException {
    if (servletRequest.isSecure()) {
      chain.doFilter(servletRequest, servletResponse);
    } else {
      HttpServletRequest request = (HttpServletRequest) servletRequest; 
      String target = request.getRequestURL().toString().replaceFirst("http://", "https://");
      redirect(servletResponse, target);
    }
  }

}
