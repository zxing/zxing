/*
 * Copyright 2022 ZXing authors
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

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * Protect the decode endpoint from long-running requests.
 */
@WebFilter(urlPatterns = {"/w/decode"}, initParams = {
  @WebInitParam(name = "timeoutSec", value = "10"),
})
public final class TimeoutFilter implements Filter {

  private ExecutorService executorService;
  private TimeLimiter timeLimiter;
  private int timeoutSec;

  @Override
  public void init(FilterConfig filterConfig) {
    executorService = Executors.newCachedThreadPool();
    timeLimiter = SimpleTimeLimiter.create(executorService);
    timeoutSec = Integer.parseInt(filterConfig.getInitParameter("timeoutSec"));
  }

  @Override
  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain chain) throws IOException, ServletException {
    try {
      timeLimiter.callWithTimeout(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          chain.doFilter(request, response);
          return null;
        }
      }, timeoutSec, TimeUnit.SECONDS);
    } catch (TimeoutException | InterruptedException e) {
      HttpServletResponse servletResponse = (HttpServletResponse) response;
      servletResponse.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
      servletResponse.getWriter().write("Request took too long");
    } catch (ExecutionException e) {
      if (e.getCause() instanceof ServletException) {
        throw (ServletException) e.getCause();
      }
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw new ServletException(e.getCause());
    }
  }

  @Override
  public void destroy() {
    if (executorService != null) {
      executorService.shutdownNow();
    }
  }

}
