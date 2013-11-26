/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.client.glass;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.mirror.model.Notification;

/**
 * @author Sean Owen
 * @link https://developers.google.com/glass/develop/mirror/timeline
 */
public final class NotificationServlet extends HttpServlet {

  private ExecutorService executor;
  private final JsonObjectParser jsonParser = new JsonObjectParser(new JacksonFactory());
  private AuthUtil authUtil;
  private String subscriptionVerifyToken;
  
  @Override
  public void init(ServletConfig servletConfig) {
    ServletContext context = servletConfig.getServletContext();
    authUtil = new AuthUtil(context.getInitParameter("CLIENT_ID"), context.getInitParameter("CLIENT_SECRET"));
    executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    subscriptionVerifyToken = context.getInitParameter("SUBSCRIPTION_VERIFY_TOKEN");
  }
  
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    Notification notification = jsonParser.parseAndClose(request.getReader(), Notification.class);
    
    if (!subscriptionVerifyToken.equals(notification.getVerifyToken())) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    String userId = notification.getUserToken();
    Credential credential = authUtil.newAuthorizationCodeFlow().loadCredential(userId);
    String timelineItemID = notification.getItemId();
    
    ResourceBundle resources = ResourceBundle.getBundle("Strings", request.getLocale());
    
    executor.submit(new NotificationCallable(credential, timelineItemID, resources));
  }
  
  @Override
  public void destroy() {
    executor.shutdownNow();
  }

}
