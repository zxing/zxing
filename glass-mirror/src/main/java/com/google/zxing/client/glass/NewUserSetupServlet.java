/*
 * Copyright (C) 2013 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.services.mirror.model.Contact;
import com.google.api.services.mirror.model.Subscription;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.zxing.client.glass.mirror.MirrorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet manages the OAuth 2.0 dance.
 * 
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 * @author Sean Owen
 * @link https://developers.google.com/glass/develop/mirror/authorization
 */
public final class NewUserSetupServlet extends HttpServlet {
  
  private static final Logger log = LoggerFactory.getLogger(NewUserSetupServlet.class);
  
  private static final String BS_ID = "barcode-scanner";

  private AuthUtil authUtil;
  private String baseURL;
  private String notificationCallbackURL;
  private String subscriptionVerifyToken;

  @Override
  public void init(ServletConfig servletConfig) {
    ServletContext context = servletConfig.getServletContext();
    authUtil = new AuthUtil(context.getInitParameter("CLIENT_ID"), context.getInitParameter("CLIENT_SECRET"));
    baseURL = context.getInitParameter("BASE_URL");
    notificationCallbackURL = baseURL + "/notification";
    subscriptionVerifyToken = context.getInitParameter("SUBSCRIPTION_VERIFY_TOKEN");
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    String redirect;    
    
    if (request.getParameter("error") != null) {
      
      log.warn("OAuth error: {}", Arrays.toString(request.getParameterValues("error")));
      redirect = baseURL;
      
    } else {

      GenericUrl url1 = new GenericUrl(request.getRequestURL().toString());
      url1.setRawPath("/oauth2callback");
      String callbackURL = url1.build();
      String code = request.getParameter("code");
      
      if (code == null) {
        
        log.info("New OAuth flow");
    
        AuthorizationCodeFlow flow = authUtil.newAuthorizationCodeFlow();
        GenericUrl url = flow.newAuthorizationUrl().setRedirectUri(callbackURL);
        url.set("approval_prompt", "force");
        redirect = url.build();
        
      } else {    
        // If we have a code, finish the OAuth 2.0 dance
        
        log.info("OAuth response, code: {}", code);
  
        AuthorizationCodeFlow flow = authUtil.newAuthorizationCodeFlow();
        TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(callbackURL).execute();
  
        // Extract the Google User ID from the ID token in the auth response
        String userId = ((GoogleTokenResponse) tokenResponse).parseIdToken().getPayload().getSubject();
  
        // Set it into the session
        //AuthUtil.setUserId(request, userId);
        flow.createAndStoreCredential(tokenResponse, userId);
  
        ResourceBundle resources = ResourceBundle.getBundle("Strings", request.getLocale());
        
        bootstrapNewUser(userId, resources);
  
        redirect = "glass/index.jspx";
      }
    }
    
    response.sendRedirect(redirect);
  }
  
  private void bootstrapNewUser(String userId, ResourceBundle resources) throws IOException {
    Credential credential = authUtil.newAuthorizationCodeFlow().loadCredential(userId);

    String appName = resources.getString("app.title");
    
    MirrorClient mirrorClient = MirrorClient.get();
    
    boolean contactExists = false;
    for (Contact contact : mirrorClient.listContacts(credential)) {
      if (BS_ID.equals(contact.getId())) {
        contactExists = true;
        break;
      }
    }
    
    if (!contactExists) {
      Contact contact = new Contact();
      contact.setId(BS_ID);
      contact.setDisplayName(appName);
      contact.setAcceptTypes(Arrays.asList("image/png", "image/jpeg", "image/gif", "image/bmp"));
      mirrorClient.insertContact(credential, contact);
    }

    boolean subscriptionExists = false;
    for (Subscription subscription : mirrorClient.listSubscriptions(credential)) {
      if (notificationCallbackURL.equals(subscription.getCallbackUrl())) {
        subscriptionExists = true;
        break;
      }
    }

    if (!subscriptionExists) {
      Subscription subscription = new Subscription();
      subscription.setCollection("timeline");
      subscription.setOperation(Collections.singletonList("INSERT"));
      subscription.setCallbackUrl(notificationCallbackURL);
      subscription.setVerifyToken(subscriptionVerifyToken);
      subscription.setUserToken(userId);
      mirrorClient.insertSubscription(credential, subscription);
    }
    
    TimelineItem timelineItem = new TimelineItem();
    timelineItem.setTitle(MessageFormat.format(resources.getString("setup.welcome.title"), appName));
    timelineItem.setText(MessageFormat.format(resources.getString("setup.welcome.text"), appName));
    mirrorClient.insertTimelineItem(credential, timelineItem);
    
  }
}
