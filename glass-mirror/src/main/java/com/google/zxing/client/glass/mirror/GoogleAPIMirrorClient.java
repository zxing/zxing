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

package com.google.zxing.client.glass.mirror;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.Attachment;
import com.google.api.services.mirror.model.Contact;
import com.google.api.services.mirror.model.Subscription;
import com.google.api.services.mirror.model.TimelineItem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

final class GoogleAPIMirrorClient extends MirrorClient {

  @Override
  public void insertContact(Credential credential, Contact contact) throws IOException {
    getMirror(credential).contacts().insert(contact).execute();
  }

  @Override
  public Collection<Contact> listContacts(Credential credential) throws IOException {
    return getMirror(credential).contacts().list().execute().getItems();
  }  

  @Override
  public void insertTimelineItem(Credential credential, TimelineItem item) throws IOException {
    getMirror(credential).timeline().insert(item).execute();
  }
  
  @Override
  public void insertSubscription(Credential credential, Subscription subscription) throws IOException {
    getMirror(credential).subscriptions().insert(subscription).execute();
  }  
  
  @Override
  public Collection<Subscription> listSubscriptions(Credential credential) throws IOException {
    return getMirror(credential).subscriptions().list().execute().getItems();
  }
  
  @Override
  public TimelineItem getTimelineItem(Credential credential, String id) throws IOException {
    return getMirror(credential).timeline().get(id).execute();
  }
  
  @Override
  public InputStream getAttachmentInputStream(Credential credential, String timelineItemId, String attachmentId) 
      throws IOException {
    Mirror mirrorService = getMirror(credential);
    Mirror.Timeline.Attachments attachments = mirrorService.timeline().attachments();
    Attachment attachmentMetadata = attachments.get(timelineItemId, attachmentId).execute();
    HttpResponse resp =
        mirrorService.getRequestFactory()
            .buildGetRequest(new GenericUrl(attachmentMetadata.getContentUrl())).execute();
    return resp.getContent();
  }
  
  private static Mirror getMirror(Credential credential) {
    return new Mirror.Builder(new NetHttpTransport(), new JacksonFactory(), credential).build();
  }

}
