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
	import com.google.zxing.client.result.TextParsedResult;
	import com.google.zxing.Result;

/**
 * Recognizes an NDEF message that encodes text according to the
 * "Text Record Type Definition" specification.
 *
 * @author Sean Owen
 */
public final class NDEFTextResultParser extends AbstractNDEFResultParser {

  public static function parse(result:Result ):TextParsedResult {
    var bytes:Array = result.getRawBytes();
    if (bytes == null) {
      return null;
    }
    var ndefRecord:NDEFRecord = NDEFRecord.readRecord(bytes, 0);
    if (ndefRecord == null || !ndefRecord.isMessageBegin() || !ndefRecord.isMessageEnd()) {
      return null;
    }
    if (ndefRecord.getType() != NDEFRecord.TEXT_WELL_KNOWN_TYPE) {
      return null;
    }
    var languageText:Array = decodeTextPayload(ndefRecord.getPayload());
    return new TextParsedResult(languageText[0], languageText[1]);
  }

  public static function decodeTextPayload(payload:Array):Array {
    var statusByte:int = payload[0];
    var isUTF16:Boolean = (statusByte & 0x80) != 0;
    var languageLength:int = statusByte & 0x1F;
    // language is always ASCII-encoded:
    var language:String = bytesToString(payload, 1, languageLength, "US-ASCII");
    var encoding:String = isUTF16 ? "UTF-16" : "UTF8";
    var text:String = bytesToString(payload, 1 + languageLength, payload.length - languageLength - 1, encoding);
    return new [language, text ];
  }

}
}