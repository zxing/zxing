package com.google.zxing.client.result
{

import com.google.zxing.common.flexdatatypes.StringBuilder;
import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
import com.google.zxing.common.flexdatatypes.Utils;
	
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

/**
 * @author Sean Owen
 */
public final class CalendarParsedResult extends ParsedResult 
{

  private var summary:String;
  private var start:String;
  private var end:String;
  private var location:String;
  private var attendee:String;
  private var title:String;

  public function CalendarParsedResult(summary:String,
                              start:String,
                              end:String,
                              location:String,
                              attendee:String,
                              title:String) 
  {
    super(ParsedResultType.CALENDAR);
    // Start is required, end is not
    if (start == null) {
      throw new IllegalArgumentException();
    }
    validateDate(start);
    validateDate(end);
    this.summary = summary;
    this.start = start;
    this.end = end;
    this.location = location;
    this.attendee = attendee;
    this.title = title;
  }

  public function getSummary():String 
  {
    return summary;
  }

  /**
   * <p>We would return the start and end date as a {@link java.util.Date} except that this code
   * needs to work under JavaME / MIDP and there is no date parsing library available there, such
   * as <code>java.text.SimpleDateFormat</code>.</p> See validateDate() for the return format.
   *
   * @return start time formatted as a RFC 2445 DATE or DATE-TIME.</p>
   */
  public function getStart():String 
  {
    return start;
  }

  /**
   * @see #getStart(). May return null if the event has no duration.
   */
  public function getEnd():String 
  {
    return end;
  }

  public function getLocation():String 
  {
    return location;
  }

  public function getAttendee():String {
    return attendee;
  }

  public function getTitle():String {
    return title;
  }

  public override function getDisplayResult():String {
    var result:StringBuilder = new StringBuilder();
    maybeAppend(summary, result);
    maybeAppend(start, result);
    maybeAppend(end, result);
    maybeAppend(location, result);
    maybeAppend(attendee, result);
    maybeAppend(title, result);
    return result.toString();
  }

  /**
   * RFC 2445 allows the start and end fields to be of type DATE (e.g. 20081021) or DATE-TIME
   * (e.g. 20081021T123000 for local time, or 20081021T123000Z for UTC).
   *
   * @param date The string to validate
   */
  private static function validateDate( date:String):void {
    if (date != null) {
      var length:int = date.length;
      if (length != 8 && length != 15 && length != 16) {
        throw new IllegalArgumentException();
      }
      for (var i:int = 0; i < 8; i++) {
        if (!Utils.isDigit(date.charAt(i))) {
          throw new IllegalArgumentException();
        }
      }
      if (length > 8) {
        if (date.charAt(8) != 'T') {
          throw new IllegalArgumentException();
        }
        for (var ii:int = 9; ii < 15; i++) {
          if (!Utils.isDigit(date.charAt(ii))) {
            throw new IllegalArgumentException();
          }
        }
        if (length == 16 && date.charAt(15) != 'Z') {
          throw new IllegalArgumentException();
        }
      }
    }
  }

}

}