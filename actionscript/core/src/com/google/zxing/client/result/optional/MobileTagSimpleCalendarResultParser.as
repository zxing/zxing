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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.result.CalendarParsedResult;
import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
import com.google.zxing.common.flexdatatypes.Utils;

/**
 * <p>Represents a "simple calendar" result encoded according to section 4.9 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author Sean Owen
 */
public final class MobileTagSimpleCalendarResultParser extends AbstractMobileTagResultParser {

  public static var SERVICE_TYPE:String = "07";

  public static function parse(result:Result):CalendarParsedResult {
    if (result.getBarcodeFormat() != BarcodeFormat.DATAMATRIX) {
      return null;
    }
    var rawText:String = result.getText();
    if (!Utils.startsWith(rawText,SERVICE_TYPE)) {
      return null;
    }

    var matches:Array = matchDelimitedFields(rawText.substring(2), 6);
    if (matches == null || !isDigits(matches[1], 10) || !isDigits(matches[2], 10)) {
      return null;
    }
    var summary:String = matches[0];
    var start:String = expandDateString(matches[1]);
    var end:String = expandDateString(matches[2]);
    var location:String = matches[3];
    var attendee:String = matches[4];
    var title:String = matches[5];

    try {
      return new CalendarParsedResult(summary, start, end, location, attendee, title);
    } catch ( iae:IllegalArgumentException) {
      
    }
    return null;
  }

  private static function expandDateString(date:String):String {
    if (date == null) {
      return null;
    }
    // Input is of form YYMMddHHmmss, and needs to be YYYYMMdd'T'HHmmss'Z'
    return "20" + date.substring(0, 6) + 'T' + date.substring(6) + "00Z";
  }

}}