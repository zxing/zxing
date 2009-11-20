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
import com.google.zxing.client.result.URIParsedResult;

/**
 * Recognizes an NDEF message that encodes a URI according to the
 * "URI Record Type Definition" specification.
 *
 * @author Sean Owen
 */
public final class NDEFURIResultParser extends AbstractNDEFResultParser {

  private static var URI_PREFIXES:Array = [
      null,
      "http://www.",
      "https://www.",
      "http://",
      "https://",
      "tel:",
      "mailto:",
      "ftp://anonymous:anonymous@",
      "ftp://ftp.",
      "ftps://",
      "sftp://",
      "smb://",
      "nfs://",
      "ftp://",
      "dav://",
      "news:",
      "telnet://",
      "imap:",
      "rtsp://",
      "urn:",
      "pop:",
      "sip:",
      "sips:",
      "tftp:",
      "btspp://",
      "btl2cap://",
      "btgoep://",
      "tcpobex://",
      "irdaobex://",
      "file://",
      "urn:epc:id:",
      "urn:epc:tag:",
      "urn:epc:pat:",
      "urn:epc:raw:",
      "urn:epc:",
      "urn:nfc:",
  ];

  public static function parse(result:Result ):URIParsedResult {
    var bytes:Array = result.getRawBytes();
    if (bytes == null) {
      return null;
    }
    var ndefRecord:NDEFRecord = NDEFRecord.readRecord(bytes, 0);
    if (ndefRecord == null || !ndefRecord.isMessageBegin() || !ndefRecord.isMessageEnd()) {
      return null;
    }
    if (ndefRecord.getType() != NDEFRecord.URI_WELL_KNOWN_TYPE) {
      return null;
    }
    var fullURI:String = decodeURIPayload(ndefRecord.getPayload());
    return new URIParsedResult(fullURI, null);
  }

  public static function decodeURIPayload(payload:Array):String {
    var identifierCode:int = payload[0] & 0xFF;
    var prefix:String = null;
    if (identifierCode < URI_PREFIXES.length) {
      prefix = URI_PREFIXES[identifierCode];
    }
    var restOfURI:String = bytesToString(payload, 1, payload.length - 1, "UTF8");
    return prefix == null ? restOfURI : prefix + restOfURI;
  }

}
}