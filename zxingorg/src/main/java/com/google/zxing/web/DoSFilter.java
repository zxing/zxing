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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * A simplistic {@link Filter} that rejects requests from hosts that are sending too many
 * requests in too short a time.
 *
 * @author Sean Owen
 */
@WebFilter({"/w/decode", "/w/chart"})
public final class DoSFilter implements Filter {

  private static final Logger log = Logger.getLogger(DoSFilter.class.getName());

  private static final int MAX_ACCESSES_PER_IP_PER_TIME = 50;
  private static final long MAX_ACCESSES_TIME_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
  private static final int MAX_RECENT_ACCESS_MAP_SIZE = 10_000;

  private final Map<String,AtomicLong> numRecentAccesses;
  private Timer timer;

  public DoSFilter() {
    numRecentAccesses = new LinkedHashMap<String,AtomicLong>() {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String,AtomicLong> eldest) {
        return size() > MAX_RECENT_ACCESS_MAP_SIZE;
      }
    };
  }

  @Override
  public void init(FilterConfig filterConfig) {
    timer = new Timer("DoSFilter");
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            synchronized (numRecentAccesses) {
              // Periodically reduce allowed accesses per IP
              Iterator<Map.Entry<String,AtomicLong>> accessIt = numRecentAccesses.entrySet().iterator();
              while (accessIt.hasNext()) {
                Map.Entry<String,AtomicLong> entry = accessIt.next();
                AtomicLong count = entry.getValue();
                // If number of accesses is below the threshold, remove it entirely
                if (count.get() <= MAX_ACCESSES_PER_IP_PER_TIME) {
                  accessIt.remove();
                } else {
                  // Else it exceeded the max, so log it (again)
                  log.warning("Possible DoS attack from " + entry.getKey() + " (" + count + " outstanding)");
                  // Reduce count of accesses held against the IP
                  count.getAndAdd(-MAX_ACCESSES_PER_IP_PER_TIME);
                }
              }
              log.info("Tracking accesses from " + numRecentAccesses.size() + " IPs");
            }
          }
        }, MAX_ACCESSES_TIME_MS, MAX_ACCESSES_TIME_MS);
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            System.gc();
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
    if (remoteIPAddress == null) {
      return true;
    }
    AtomicLong count;
    synchronized (numRecentAccesses) {
      count = numRecentAccesses.get(remoteIPAddress);
      if (count == null) {
        count = new AtomicLong();
        numRecentAccesses.put(remoteIPAddress, count);
      }
    }
    return count.incrementAndGet() > MAX_ACCESSES_PER_IP_PER_TIME;
  }

  @Override
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }

}