package com.google.zxing.client.result
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
import com.google.zxing.common.flexdatatypes.IllegalArgumentException;

/**
 * Partially implements the iCalendar format's "VEVENT" format for specifying a
 * calendar event. See RFC 2445. This supports SUMMARY, DTSTART and DTEND fields.
 *
 * @author Sean Owen
 */
public final class VEventResultParser extends ResultParser {

  public function VEventResultParser() {
  }

  public static function parse(result:Result):CalendarParsedResult {
    var rawText:String = result.getText();
    if (rawText == null) {
      return null;
    }
    var vEventStart:int = rawText.indexOf("BEGIN:VEVENT");
    if (vEventStart < 0) {
      return null;
    }
    var vEventEnd:int = rawText.indexOf("END:VEVENT");
    if (vEventEnd < 0) {
      return null;
    }

    var summary:String = VCardResultParser.matchSingleVCardPrefixedField("SUMMARY", rawText, true);
    var start:String = VCardResultParser.matchSingleVCardPrefixedField("DTSTART", rawText, true);
    var end:String = VCardResultParser.matchSingleVCardPrefixedField("DTEND", rawText, true);
    try {
      return new CalendarParsedResult(summary, start, end, null, null, null);
    } catch (iae:IllegalArgumentException) {
      
    }
    return null;
  }

}
}