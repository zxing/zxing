/*
 * Copyright 2018 ZXing authors
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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * Tests {@link DoSFilter} implementations.
 */
public final class DoSFilterTestCase extends Assert {

  private static final int MAX_ACCESS_PER_TIME = 10;

  @Test
  public void testRedirect() throws Exception {
    for (DoSFilter filter : Arrays.asList(new ChartDoSFilter(), new DecodeDoSFilter())) {
      initFilter(filter);
      try {
        for (int i = 0; i < MAX_ACCESS_PER_TIME; i++) {
          testRequest(filter, "1.2.3.4", null, HttpServletResponse.SC_OK);
        }
        testRequest(filter, "1.2.3.4", null, HttpServletResponse.SC_FORBIDDEN);
      } finally {
        filter.destroy();
      }
    }
  }

  @Test
  public void testNoRemoteHost() throws Exception {
    Filter filter = new DecodeDoSFilter();
    initFilter(filter);
    try {
      testRequest(filter, null, null, HttpServletResponse.SC_FORBIDDEN);
      testRequest(filter, null, "1.1.1.1", HttpServletResponse.SC_FORBIDDEN);
    } finally {
      filter.destroy();
    }
  }

  @Test
  public void testProxy() throws Exception {
    Filter filter = new DecodeDoSFilter();
    initFilter(filter);
    try {
      for (int i = 0; i < MAX_ACCESS_PER_TIME; i++) {
        testRequest(filter, "1.2.3.4", "1.1.1." + i + ", proxy1", HttpServletResponse.SC_OK);
      }
      testRequest(filter, "1.2.3.4", "1.1.1.0", HttpServletResponse.SC_FORBIDDEN);
    } finally {
      filter.destroy();
    }
  }

  private void initFilter(Filter filter) throws ServletException {
    MockFilterConfig config = new MockFilterConfig();
    config.addInitParameter("maxAccessPerTime", Integer.toString(MAX_ACCESS_PER_TIME));
    config.addInitParameter("accessTimeSec", "60");
    config.addInitParameter("maxEntries", "100");
    filter.init(config);
  }

  private void testRequest(Filter filter, String host, String proxy, int expectedStatus)
      throws IOException, ServletException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/");
    request.setRemoteAddr(host);
    if (proxy != null) {
      request.addHeader("X-Forwarded-For", proxy);
    }
    HttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    assertEquals(expectedStatus, response.getStatus());
  }

}
