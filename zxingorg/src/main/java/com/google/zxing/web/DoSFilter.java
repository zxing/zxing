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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * A simplistic {@link Filter} that rejects requests from hosts that are sending too many
 * requests in too short a time.
 *
 * @author Sean Owen
 */
@WebFilter("/w/decode")
public final class DoSFilter implements Filter {

  private static final Logger log = Logger.getLogger(DoSFilter.class.getName());

  private static final int MAX_ACCESSES_PER_IP_PER_TIME = 10;
  private static final int MAX_RECENT_ACCESS_MAP_SIZE = 100_000;

  private Map<String,AtomicInteger> numRecentAccesses;
  private Set<String> bannedIPAddresses;
  private Timer timer;

  @Override
  public void init(FilterConfig filterConfig) {
    numRecentAccesses = Collections.synchronizedMap(new LinkedHashMap<String,AtomicInteger>() {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String,AtomicInteger> eldest) {
        return size() > MAX_RECENT_ACCESS_MAP_SIZE;
      }
    });
    bannedIPAddresses = Collections.synchronizedSet(new HashSet<String>());
    timer = new Timer("DoSFilter reset timer");
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            numRecentAccesses.clear();
          }
        }, 0L, TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES));
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            bannedIPAddresses.clear();
          }
        }, 0L, TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES));
  }

  @Override
  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain chain) throws IOException, ServletException {
    if (isBanned((HttpServletRequest) request)) {
      HttpServletResponse servletResponse = (HttpServletResponse) response;
      servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
    } else {
      chain.doFilter(request, response);
    }
  }

  private boolean isBanned(HttpServletRequest request) {
    String remoteIPAddress = request.getHeader("x-forwarded-for");
    if (remoteIPAddress == null) {
      remoteIPAddress = request.getRemoteAddr();
    }
    if (remoteIPAddress == null || bannedIPAddresses.contains(remoteIPAddress)) {
      return true;
    }
    if (getCount(remoteIPAddress) > MAX_ACCESSES_PER_IP_PER_TIME) {
      log.warning("Possible DoS attack from " + remoteIPAddress);
      bannedIPAddresses.add(remoteIPAddress);
      return true;
    }
    return false;
  }

  private int getCount(String remoteIPAddress) {
    synchronized (numRecentAccesses) {
      AtomicInteger count = numRecentAccesses.get(remoteIPAddress);
      if (count == null) {
        numRecentAccesses.put(remoteIPAddress, new AtomicInteger(1));
        return 1;
      } else {
        return count.incrementAndGet();
      }
    }
  }

  @Override
  public void destroy() {
    timer.cancel();
  }

}