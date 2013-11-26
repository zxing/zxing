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

package com.google.zxing.client.glass.mirror;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.mirror.model.Contact;
import com.google.api.services.mirror.model.Subscription;
import com.google.api.services.mirror.model.TimelineItem;

public abstract class MirrorClient {
  
  private static MirrorClient instance = null;
  
  public static synchronized MirrorClient get() {
    if (instance == null) {
      instance = new GoogleAPIMirrorClient();
    }
    return instance;
  }
  
  public abstract void insertContact(Credential credential, Contact contact) throws IOException;
  
  public abstract Collection<Contact> listContacts(Credential credential) throws IOException;
  
  public abstract void insertTimelineItem(Credential credential, TimelineItem item) throws IOException;
  
  public abstract void insertSubscription(Credential credential, Subscription subscription) throws IOException;
  
  public abstract Collection<Subscription> listSubscriptions(Credential credential) throws IOException;
  
  public abstract TimelineItem getTimelineItem(Credential credential, String id) throws IOException;
  
  public abstract InputStream getAttachmentInputStream(Credential credential, 
                                                       String timelineItemId, 
                                                       String attachmentId) throws IOException;
  
}
