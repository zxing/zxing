package com.google.zxing.client.result.optional
{
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
public final class NDEFSmartPosterResultParser extends AbstractNDEFResultParser {

  public static function parse(result:Result):NDEFSmartPosterParsedResult {
    var bytes:Array = result.getRawBytes();
    if (bytes == null) {
      return null;
    }
    var headerRecord:NDEFRecord = NDEFRecord.readRecord(bytes, 0);
    // Yes, header record starts and ends a message
    if (headerRecord == null || !headerRecord.isMessageBegin() || !headerRecord.isMessageEnd()) {
      return null;
    }
    if (headerRecord.getType() != NDEFRecord.SMART_POSTER_WELL_KNOWN_TYPE) {
      return null;
    }

    var offset:int = 0;
    var recordNumber:int = 0;
    var ndefRecord:NDEFRecord  = null;
    var payload:Array = headerRecord.getPayload();
    var action:int = NDEFSmartPosterParsedResult.ACTION_UNSPECIFIED;
    var title:String = null;
    var uri:String = null;

    while (offset < payload.length && (ndefRecord = NDEFRecord.readRecord(payload, offset)) != null) {
      if (recordNumber == 0 && !ndefRecord.isMessageBegin()) {
        return null;
      }

      var type:String = ndefRecord.getType();
      if (NDEFRecord.TEXT_WELL_KNOWN_TYPE == type) {
        var languageText:Array = NDEFTextResultParser.decodeTextPayload(ndefRecord.getPayload());
        title = languageText[1];
      } else if (NDEFRecord.URI_WELL_KNOWN_TYPE == type) {
        uri = NDEFURIResultParser.decodeURIPayload(ndefRecord.getPayload());
      } else if (NDEFRecord.ACTION_WELL_KNOWN_TYPE == type) {
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
}