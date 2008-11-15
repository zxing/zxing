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
import com.google.zxing.client.result.TextParsedResult;

/**
 * Recognizes an NDEF message that encodes text according to the
 * "Text Record Type Definition" specification.
 *
 * @author Sean Owen
 */
final class NDEFTextResultParser extends AbstractNDEFResultParser {

  public static TextParsedResult parse(Result result) {
    byte[] bytes = result.getRawBytes();
    if (bytes == null) {
      return null;
    }
    NDEFRecord ndefRecord = NDEFRecord.readRecord(bytes, 0);
    if (ndefRecord == null || !ndefRecord.isMessageBegin() || !ndefRecord.isMessageEnd()) {
      return null;
    }
    if (!ndefRecord.getType().equals(NDEFRecord.TEXT_WELL_KNOWN_TYPE)) {
      return null;
    }
    String[] languageText = decodeTextPayload(ndefRecord.getPayload());
    return new TextParsedResult(languageText[0], languageText[1]);
  }

  static String[] decodeTextPayload(byte[] payload) {
    byte statusByte = payload[0];
    boolean isUTF16 = (statusByte & 0x80) != 0;
    int languageLength = statusByte & 0x1F;
    // language is always ASCII-encoded:
    String language = bytesToString(payload, 1, languageLength, "US-ASCII");
    String encoding = isUTF16 ? "UTF-16" : "UTF8";
    String text = bytesToString(payload, 1 + languageLength, payload.length - languageLength - 1, encoding);
    return new String[] { language, text };
  }

}