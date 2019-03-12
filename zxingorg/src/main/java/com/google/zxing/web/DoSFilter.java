/*
 * Copyright 2015 ZXing authors
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

/**
 * A simplistic {@link Filter} that rejects requests from hosts that are sending too many
 * requests in too short a time.
 *
 * @author Sean Owen
 */
public abstract class DoSFilter implements Filter {

  private Timer timer;
  private DoSTracker sourceAddrTracker;

  @Override
  public void init(FilterConfig filterConfig) {
    int maxAccessPerTime = Integer.parseInt(filterConfig.getInitParameter("maxAccessPerTime"));
    Preconditions.checkArgument(maxAccessPerTime > 0);
    int accessTimeSec = Integer.parseInt(filterConfig.getInitParameter("accessTimeSec"));
    Preconditions.checkArgument(accessTimeSec > 0);
    long accessTimeMS = TimeUnit.MILLISECONDS.convert(accessTimeSec, TimeUnit.SECONDS);
    int maxEntries = Integer.parseInt(filterConfig.getInitParameter("maxEntries"));
    Preconditions.checkArgument(maxEntries > 0);

    String name = getClass().getSimpleName();
    timer = new Timer(name);
    sourceAddrTracker = new DoSTracker(timer, name, maxAccessPerTime, accessTimeMS, maxEntries);
  }

  @Override
  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain chain) throws IOException, ServletException {
    if (isBanned((HttpServletRequest) request)) {
      HttpServletResponse servletResponse = (HttpServletResponse) response;
      // Send very short response as requests may be very frequent
      servletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
      servletResponse.getWriter().write("Forbidden");
    } else {
      chain.doFilter(request, response);
    }
  }

  private boolean isBanned(HttpServletRequest request) {
    String remoteHost = request.getHeader("x-forwarded-for");
    if (remoteHost != null) {
      int comma = remoteHost.indexOf(',');
      if (comma >= 0) {
        remoteHost = remoteHost.substring(0, comma);
      }
      remoteHost = remoteHost.trim();
    }
    // Non-short-circuit "|" below is on purpose
    return
      (remoteHost != null && sourceAddrTracker.isBanned(remoteHost)) |
      sourceAddrTracker.isBanned(request.getRemoteAddr());
  }

  @Override
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }

}
