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

import com.google.common.net.HttpHeaders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests {@link HTTPSFilter}.
 */
public final class HTTPSFilterTestCase extends Assert {

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain chain;

  @Before
  public void setUp() {
    request = new MockHttpServletRequest();
    request.setServerName("example.org");
    request.setRequestURI("/path");
    response = new MockHttpServletResponse();
    chain = new MockFilterChain();

  }

  @Test
  public void testNoRedirect() throws Exception {
    request.setSecure(true);
    request.setScheme("https");
    request.setServerPort(443);
    new HTTPSFilter().doFilter(request, response, chain);
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
  }

  @Test
  public void testRedirect() throws Exception {
    request.setScheme("http");
    request.setServerPort(80);
    new HTTPSFilter().doFilter(request, response, chain);
    assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
    assertEquals("https://example.org/path", response.getHeader(HttpHeaders.LOCATION));
  }

}
