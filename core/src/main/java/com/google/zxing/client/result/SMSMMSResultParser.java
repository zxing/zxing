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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>Parses an "sms:" URI result, which specifies a number to SMS.
 * See <a href="http://tools.ietf.org/html/rfc5724"> RFC 5724</a> on this.</p>
 *
 * <p>This class supports "via" syntax for numbers, which is not part of the spec.
 * For example "+12125551212;via=+12124440101" may appear as a number.
 * It also supports a "subject" query parameter, which is not mentioned in the spec.
 * These are included since they were mentioned in earlier IETF drafts and might be
 * used.</p>
 *
 * <p>This actually also parses URIs starting with "mms:" and treats them all the same way,
 * and effectively converts them to an "sms:" URI for purposes of forwarding to the platform.</p>
 *
 * @author Sean Owen
 */
public final class SMSMMSResultParser extends ResultParser {

  @Override
  public SMSParsedResult parse(Result result) {
    String rawText = getMassagedText(result);
    if (!(rawText.startsWith("sms:") || rawText.startsWith("SMS:") ||
          rawText.startsWith("mms:") || rawText.startsWith("MMS:"))) {
      return null;
    }

    // Check up front if this is a URI syntax string with query arguments
    Map<String,String> nameValuePairs = parseNameValuePairs(rawText);
    String subject = null;
    String body = null;
    boolean querySyntax = false;
    if (nameValuePairs != null && !nameValuePairs.isEmpty()) {
      subject = nameValuePairs.get("subject");
      body = nameValuePairs.get("body");
      querySyntax = true;
    }

    // Drop sms, query portion
    int queryStart = rawText.indexOf('?', 4);
    String smsURIWithoutQuery;
    // If it's not query syntax, the question mark is part of the subject or message
    if (queryStart < 0 || !querySyntax) {
      smsURIWithoutQuery = rawText.substring(4);
    } else {
      smsURIWithoutQuery = rawText.substring(4, queryStart);
    }

    int lastComma = -1;
    int comma;
    List<String> numbers = new ArrayList<>(1);
    List<String> vias = new ArrayList<>(1);
    while ((comma = smsURIWithoutQuery.indexOf(',', lastComma + 1)) > lastComma) {
      String numberPart = smsURIWithoutQuery.substring(lastComma + 1, comma);
      addNumberVia(numbers, vias, numberPart);
      lastComma = comma;
    }
    addNumberVia(numbers, vias, smsURIWithoutQuery.substring(lastComma + 1));    

    return new SMSParsedResult(numbers.toArray(new String[numbers.size()]),
                               vias.toArray(new String[vias.size()]),
                               subject,
                               body);
  }

  private static void addNumberVia(Collection<String> numbers,
                                   Collection<String> vias,
                                   String numberPart) {
    int numberEnd = numberPart.indexOf(';');
    if (numberEnd < 0) {
      numbers.add(numberPart);
      vias.add(null);
    } else {
      numbers.add(numberPart.substring(0, numberEnd));
      String maybeVia = numberPart.substring(numberEnd + 1);
      String via;
      if (maybeVia.startsWith("via=")) {
        via = maybeVia.substring(4);
      } else {
        via = null;
      }
      vias.add(via);
    }
  }

}