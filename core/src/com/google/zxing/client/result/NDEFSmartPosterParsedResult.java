/*
 * Copyright 2008 Google Inc.
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

package com.google.zxing.client.result;

import com.google.zxing.Result;

/**
 * <p>Recognizes an NDEF message that encodes information according to the
 * "Smart Poster Record Type Definition" specification.</p>
 *
 * <p>This actually only supports some parts of the Smart Poster format: title,
 * URI, and action records. Icon records are not supported because the size
 * of these records are infeasibly large for barcodes. Size and type records
 * are not supported. Multiple titles are not supported.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class NDEFSmartPosterParsedResult extends AbstractNDEFParsedResult {

  public static final int ACTION_UNSPECIFIED = -1;
  public static final int ACTION_DO = 0;
  public static final int ACTION_SAVE = 1;
  public static final int ACTION_OPEN = 2;

  private String title;
  private String uri;
  private int action;

  private NDEFSmartPosterParsedResult() {
    super(ParsedReaderResultType.NDEF_SMART_POSTER);
    action = ACTION_UNSPECIFIED;
  }

  public static NDEFSmartPosterParsedResult parse(Result result) {
    byte[] bytes = result.getRawBytes();
    if (bytes == null) {
      return null;
    }
    NDEFRecord headerRecord = NDEFRecord.readRecord(bytes, 0);
    // Yes, header record starts and ends a message
    if (headerRecord == null || !headerRecord.isMessageBegin() || !headerRecord.isMessageEnd()) {
      return null;
    }
    if (!headerRecord.getType().equals(NDEFRecord.SMART_POSTER_WELL_KNOWN_TYPE)) {
      return null;
    }

    int offset = 0;
    int recordNumber = 0;
    NDEFRecord ndefRecord = null;
    byte[] payload = headerRecord.getPayload();
    NDEFSmartPosterParsedResult smartPosterParsedResult = new NDEFSmartPosterParsedResult();

    while (offset < payload.length && (ndefRecord = NDEFRecord.readRecord(payload, offset)) != null) {
      if (recordNumber == 0 && !ndefRecord.isMessageBegin()) {
        return null;
      }
      String type = ndefRecord.getType();
      if (NDEFRecord.TEXT_WELL_KNOWN_TYPE.equals(type)) {
        String[] languageText = NDEFTextParsedResult.decodeTextPayload(ndefRecord.getPayload());
        smartPosterParsedResult.title = languageText[1];
      } else if (NDEFRecord.URI_WELL_KNOWN_TYPE.equals(type)) {
        smartPosterParsedResult.uri = NDEFURIParsedResult.decodeURIPayload(ndefRecord.getPayload());
      } else if (NDEFRecord.ACTION_WELL_KNOWN_TYPE.equals(type)) {
        smartPosterParsedResult.action = ndefRecord.getPayload()[0];
      }
      recordNumber++;
      offset += ndefRecord.getTotalRecordLength();
    }
    
    if (recordNumber == 0 || (ndefRecord != null && !ndefRecord.isMessageEnd())) {
      return null;
    }

    return smartPosterParsedResult;
  }

  public String getTitle() {
    return title;
  }

  public String getURI() {
    return uri;
  }

  public int getAction() {
    return action;
  }

  public String getDisplayResult() {
    if (title == null) {
      return uri;
    } else {
      return title + '\n' + uri;
    }
  }

}