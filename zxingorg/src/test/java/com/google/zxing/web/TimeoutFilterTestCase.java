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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tests {@link TimeoutFilter}.
 */
public final class TimeoutFilterTestCase extends Assert {

  @Test
  public void testTimeout() throws Exception {
    MockFilterConfig config = new MockFilterConfig();
    config.addInitParameter("timeoutSec", "1");
    Filter filter = new TimeoutFilter();
    filter.init(config);

    FilterChain chain = new MockFilterChain(new GenericServlet() {
      @Override
      public void service(ServletRequest req, ServletResponse res) {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          // continue
        }
      }
    });
    HttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(new MockHttpServletRequest(), response, chain);
    filter.destroy();
    assertEquals(HttpServletResponse.SC_REQUEST_TIMEOUT, response.getStatus());
  }

}
