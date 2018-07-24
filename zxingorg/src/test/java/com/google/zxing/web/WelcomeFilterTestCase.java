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
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests {@link WelcomeFilter}.
 */
public final class WelcomeFilterTestCase extends Assert {

  @Test
  public void testRedirect() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    new WelcomeFilter().doFilter(request, response, chain);
    assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
    assertEquals("/w/decode.jspx", response.getHeader(HttpHeaders.LOCATION));
  }

}
