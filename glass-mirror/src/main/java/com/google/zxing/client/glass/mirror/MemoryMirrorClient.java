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

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.mirror.model.Attachment;
import com.google.api.services.mirror.model.Contact;
import com.google.api.services.mirror.model.Subscription;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MemoryMirrorClient extends MirrorClient {
  
  private static final Logger log = LoggerFactory.getLogger(MemoryMirrorClient.class);
  
  private static final String FAKE_ATTACHMENT_ID = "0123";
  
  private final Collection<Contact> contacts = 
      Collections.synchronizedCollection(Sets.<Contact>newHashSet());
  private final Collection<Subscription> subscriptions = 
      Collections.synchronizedCollection(Sets.<Subscription>newHashSet());

  @Override
  public void insertContact(Credential credential, Contact contact) {
    log.info("Insert Contact: {}", contact);    
    contacts.add(contact);
  }
  
  @Override
  public Collection<Contact> listContacts(Credential credential) {
    log.info("Contacts: {}", contacts);
    return contacts;
  }  

  @Override
  public void insertTimelineItem(Credential credential, TimelineItem item) {
    log.info("Insert Timeline: {}", item);
  }
  
  @Override
  public void insertSubscription(Credential credential, Subscription subscription) {
    log.info("Insert Subscription: {}", subscription);
    subscriptions.add(subscription);
  }  
  
  @Override
  public Collection<Subscription> listSubscriptions(Credential credential) {
    log.info("Subscriptions: {}", subscriptions);
    return subscriptions;
  }  
  
  @Override
  public TimelineItem getTimelineItem(Credential credential, String id) {
    log.info("Get Timeline {}", id);
    TimelineItem timelineItem = new TimelineItem();
    timelineItem.setId(id);
    Attachment fakeAttachment = new Attachment();
    fakeAttachment.setId(FAKE_ATTACHMENT_ID);
    timelineItem.setAttachments(Collections.singletonList(fakeAttachment));
    return timelineItem;
  }
  
  @Override
  public InputStream getAttachmentInputStream(Credential credential, String timelineItemId, String attachmentId) {
    log.info("Get Attachment {} {}", timelineItemId, attachmentId);
    if (!FAKE_ATTACHMENT_ID.equals(attachmentId)) {
      return null;
    }
    return MemoryMirrorClient.class.getResourceAsStream("/test.jpg");
  }

}
