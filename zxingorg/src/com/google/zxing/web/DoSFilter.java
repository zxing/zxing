/*
 * Copyright 2008 ZXing authors
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
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A {@link Filter} that rejects requests from hosts that are sending too many
 * requests in too short a time.
 * 
 * @author Sean Owen
 */
public final class DoSFilter implements Filter {

  private static final int MAX_ACCESSES_PER_IP_PER_TIME = 10;
  private static final long MAX_ACCESS_INTERVAL_MSEC = 10L * 1000L;
  private static final long UNBAN_INTERVAL_MSEC = 60L * 60L * 1000L;

  private final IPTrie numRecentAccesses;
  private final Timer timer;
  private final Set<String> bannedIPAddresses;
  private ServletContext context;

  public DoSFilter() {
    numRecentAccesses = new IPTrie();
    timer = new Timer("DosFilter reset timer");
    bannedIPAddresses = Collections.synchronizedSet(new HashSet<String>());
  }

  public void init(FilterConfig filterConfig) {
    context = filterConfig.getServletContext();
    timer.scheduleAtFixedRate(new ResetTask(), 0L, MAX_ACCESS_INTERVAL_MSEC);
    timer.scheduleAtFixedRate(new UnbanTask(), 0L, UNBAN_INTERVAL_MSEC);
  }

  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain chain) throws IOException, ServletException {
    if (isBanned(request)) {
      HttpServletResponse servletResponse = (HttpServletResponse) response;
      servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
    } else {
      chain.doFilter(request, response);
    }
  }

  private boolean isBanned(ServletRequest request) {
    String remoteIPAddressString = request.getRemoteAddr();
    if (bannedIPAddresses.contains(remoteIPAddressString)) {
      return true;
    }
    InetAddress remoteIPAddress;
    try {
      remoteIPAddress = InetAddress.getByName(remoteIPAddressString);
    } catch (UnknownHostException uhe) {
      context.log("Can't determine host from: " + remoteIPAddressString + "; assuming banned");
      return true;
    }
    if (numRecentAccesses.incrementAndGet(remoteIPAddress) > MAX_ACCESSES_PER_IP_PER_TIME) {
      context.log("Possible DoS attack from " + remoteIPAddressString);
      bannedIPAddresses.add(remoteIPAddressString);
      return true;
    }
    return false;
  }

  public void destroy() {
    timer.cancel();
    numRecentAccesses.clear();
    bannedIPAddresses.clear();
  }

  private final class ResetTask extends TimerTask {
    @Override
    public void run() {
      numRecentAccesses.clear();
    }
  }

  private final class UnbanTask extends TimerTask {
    @Override
    public void run() {
      bannedIPAddresses.clear();
    }
  }

}