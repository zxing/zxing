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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests {@link DoSFilter}.
 */
public final class DoSFilterTestCase extends Assert {

  @Test
  public void testRedirect() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/");
    request.setRemoteAddr("1.2.3.4");
    HttpServletResponse response = new MockHttpServletResponse();
    DoSFilter filter = new DoSFilter();
    filter.init(null);
    for (int i = 0; i < DoSFilter.MAX_ACCESS_PER_TIME; i++) {
      filter.doFilter(request, response, new MockFilterChain());
      assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }
    filter.doFilter(request, response, new MockFilterChain());
    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

}
