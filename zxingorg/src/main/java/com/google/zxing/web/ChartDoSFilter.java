/*
 * Copyright 2019 ZXing authors
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

import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

/**
 * Protect the /chart endpoint from too many requests.
 */
@WebFilter(urlPatterns = {"/w/chart"}, initParams = {
  @WebInitParam(name = "maxAccessPerTime", value = "250"),
  @WebInitParam(name = "accessTimeSec", value = "500"),
  @WebInitParam(name = "maxEntries", value = "10000")
})
public final class ChartDoSFilter extends DoSFilter {
  // no additional implementation
}
