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

package com.google.zxing.client.result.optional;

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
 * @author Sean Owen
 */
final class NDEFSmartPosterResultParser extends AbstractNDEFResultParser {

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
    int action = NDEFSmartPosterParsedResult.ACTION_UNSPECIFIED;
    String title = null;
    String uri = null;

    while (offset < payload.length && (ndefRecord = NDEFRecord.readRecord(payload, offset)) != null) {
      if (recordNumber == 0 && !ndefRecord.isMessageBegin()) {
        return null;
      }

      String type = ndefRecord.getType();
      if (NDEFRecord.TEXT_WELL_KNOWN_TYPE.equals(type)) {
        String[] languageText = NDEFTextResultParser.decodeTextPayload(ndefRecord.getPayload());
        title = languageText[1];
      } else if (NDEFRecord.URI_WELL_KNOWN_TYPE.equals(type)) {
        uri = NDEFURIResultParser.decodeURIPayload(ndefRecord.getPayload());
      } else if (NDEFRecord.ACTION_WELL_KNOWN_TYPE.equals(type)) {
        action = ndefRecord.getPayload()[0];
      }
      recordNumber++;
      offset += ndefRecord.getTotalRecordLength();
    }

    if (recordNumber == 0 || (ndefRecord != null && !ndefRecord.isMessageEnd())) {
      return null;
    }

    return new NDEFSmartPosterParsedResult(action, uri, title);
  }

}