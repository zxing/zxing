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

import javax.mail.Authenticator;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Timer;

/**
 * @author Sean Owen
 */
public final class DecodeEmailListener implements ServletContextListener {

  private static final long EMAIL_CHECK_INTERVAL = 5L * 60 * 1000;

  private Timer emailTimer;

  public void contextInitialized(ServletContextEvent event) {
    ServletContext context = event.getServletContext();
    String emailAddress = context.getInitParameter("emailAddress");
    String emailPassword = context.getInitParameter("emailPassword");
    if (emailAddress == null || emailPassword == null) {
      throw new IllegalArgumentException("emailAddress or emailPassword not specified");
    }
    Authenticator emailAuthenticator = new EmailAuthenticator(emailAddress, emailPassword);
    emailTimer = new Timer("Email decoder timer", true);
    emailTimer.schedule(new DecodeEmailTask(emailAddress, emailAuthenticator), 0L, EMAIL_CHECK_INTERVAL);
  }

  public void contextDestroyed(ServletContextEvent event) {
    emailTimer.cancel();
  }

}
