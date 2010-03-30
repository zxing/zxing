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

package com.google.zxing.client.result;

import com.google.zxing.Result;

/**
 * <p>Parses an "smsto:" URI result, whose format is not standardized but appears to be like:
 * <code>smsto:number(:body)</code>.</p>
 *
 * <p>This actually also parses URIs starting with "smsto:", "mmsto:", "SMSTO:", and
 * "MMSTO:", and treats them all the same way, and effectively converts them to an "sms:" URI
 * for purposes of forwarding to the platform.</p>
 *
 * @author Sean Owen
 */
final class SMSTOMMSTOResultParser extends ResultParser {

  private SMSTOMMSTOResultParser() {
  }

  public static SMSParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null) {
      return null;
    }
    if (!(rawText.startsWith("smsto:") || rawText.startsWith("SMSTO:") ||
          rawText.startsWith("mmsto:") || rawText.startsWith("MMSTO:"))) {
      return null;
    }
    // Thanks to dominik.wild for suggesting this enhancement to support
    // smsto:number:body URIs
    String number = rawText.substring(6);
    String body = null;
    int bodyStart = number.indexOf(':');
    if (bodyStart >= 0) {
      body = number.substring(bodyStart + 1);
      number = number.substring(0, bodyStart);
    }
    return new SMSParsedResult(number, null, null, body);
  }

}