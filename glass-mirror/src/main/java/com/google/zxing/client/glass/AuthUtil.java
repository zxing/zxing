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

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * A collection of utility functions that simplify common authentication and
 * user identity tasks
 * 
 * @author Sean Owen
 * @author Google
 */
final class AuthUtil {

  private static final Collection<String> SCOPES = 
      Arrays.asList("https://www.googleapis.com/auth/glass.timeline",
                    "https://www.googleapis.com/auth/userinfo.profile");

  private final DataStoreFactory dataStoreFactory;
  private final String clientID;
  private final String clientSecret;
  
  AuthUtil(String clientID, String clientSecret) {
    dataStoreFactory = new MemoryDataStoreFactory();
    this.clientID = clientID;
    this.clientSecret = clientSecret;
  }

  /**
   * Creates and returns a new {@link AuthorizationCodeFlow} for this app.
   */
  public AuthorizationCodeFlow newAuthorizationCodeFlow() throws IOException {
    return new GoogleAuthorizationCodeFlow.Builder(new NetHttpTransport(), 
                                                   new JacksonFactory(),
                                                   clientID,
                                                   clientSecret,
                                                   SCOPES)
        .setAccessType("offline").setDataStoreFactory(dataStoreFactory).build();
  }
  
  public Credential getCredential(String userId) throws IOException {
    return userId == null ? null : newAuthorizationCodeFlow().loadCredential(userId);
  }

}
