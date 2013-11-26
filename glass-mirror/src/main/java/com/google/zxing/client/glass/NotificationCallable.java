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
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.mirror.model.Attachment;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.client.result.URIParsedResult;
import com.google.zxing.client.glass.mirror.MirrorClient;

/**
 * @author Sean Owen
 */
final class NotificationCallable implements Callable<Void> {
  
  private final Credential credential;
  private final String timelineItemID;
  private final ResourceBundle resources;

  NotificationCallable(Credential credential, String timelineItemID, ResourceBundle resources) {
    this.credential = credential;
    this.timelineItemID = timelineItemID;
    this.resources = resources;
  }

  @Override
  public Void call() throws Exception {
    MirrorClient mirrorClient = MirrorClient.get();
    TimelineItem timelineItem = mirrorClient.getTimelineItem(credential, timelineItemID);
    if (timelineItem == null) {
      return null;
    }
    List<Attachment> attachments = timelineItem.getAttachments();
    if (attachments == null || attachments.isEmpty()) {
      return null;
    }
    Attachment attachment = attachments.get(0);
    String attachmentId = attachment.getId();
    
    String errorMessage = null;
    Collection<Result> results = null;
    try (InputStream in = mirrorClient.getAttachmentInputStream(credential, timelineItemID, attachmentId)) {
      results = DecodeHelper.processStream(in);
    } catch (IOException | RuntimeException e) {
      errorMessage = e.toString();
    }
    
    if (results == null || results.isEmpty()) {
      
      TimelineItem resultTimelineItem = new TimelineItem();
      if (errorMessage == null) {
        resultTimelineItem.setTitle(resources.getString("scan.result.notfound.title"));
        resultTimelineItem.setText(resources.getString("scan.result.notfound.text"));
      } else {
        resultTimelineItem.setTitle(resources.getString("scan.result.error.title"));
        resultTimelineItem.setText(MessageFormat.format(resources.getString("scan.result.error.text"), errorMessage));        
      }
      mirrorClient.insertTimelineItem(credential, resultTimelineItem);
      
    } else {    
      
      for (Result result : results) {
        ParsedResult parsedResult = ResultParser.parseResult(result);
        
        TimelineItem resultTimelineItem = new TimelineItem();
        resultTimelineItem.setText(parsedResult.getDisplayResult());        
        resultTimelineItem.setTitle(
            MessageFormat.format(resources.getString("scan.result.success.title"), parsedResult.getType()));
        
        addResultDetailToTimelineItem(parsedResult, resultTimelineItem);
        
        mirrorClient.insertTimelineItem(credential, resultTimelineItem);        
      }
      
    }
    
    return null;
  }
  
  private static void addResultDetailToTimelineItem(ParsedResult parsedResult, TimelineItem item) {
    if (parsedResult instanceof URIParsedResult) {
      addURIResultDetailToTimelineItem((URIParsedResult) parsedResult, item);
    }
  }
  
  private static void addURIResultDetailToTimelineItem(URIParsedResult parsedResult, TimelineItem item) {
    String uri = parsedResult.getURI();
    item.setHtml("<a href=\"" + uri + "\">" + parsedResult.getDisplayResult() + "</a>");
    item.setCanonicalUrl(uri);
  }
  
}
