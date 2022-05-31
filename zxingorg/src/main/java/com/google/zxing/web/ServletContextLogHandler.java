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

import javax.servlet.ServletContext;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * A {@link Handler} that redirects log messages to the servlet container log.
 * 
 * @author Sean Owen
 */
final class ServletContextLogHandler extends Handler {

  private final ServletContext context;

  ServletContextLogHandler(ServletContext context) {
    this.context = context;
  }

  @Override
  public void publish(LogRecord record) {
    Formatter formatter = getFormatter();
    String message;
    if (formatter == null) {
      message = record.getMessage();
    } else {
      message = formatter.format(record);
    }
    Throwable throwable = record.getThrown();
    if (throwable == null) {
      context.log(message);
    } else {
      context.log(message, throwable);
    }
  }

  @Override
  public void flush() {
    // do nothing
  }

  @Override
  public void close() {
    // do nothing
  }

}