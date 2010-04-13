/*
 * Copyright 2007 ZXing authors
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import junit.framework.TestCase;

/**
 * Tests {@link CalendarParsedResult}.
 *
 * @author Sean Owen
 */
public final class CalendarParsedResultTestCase extends TestCase {

  public void testStartEnd() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "DTEND:20080505T234555Z\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        null, null, null, "20080504T123456Z", "20080505T234555Z", null);
  }

  public void testStart() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        null, null, null, "20080504T123456Z", "20080504T123456Z", null);
  }

  public void testSummary() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "SUMMARY:foo\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        null, "foo", null, "20080504T123456Z", "20080504T123456Z", null);
  }

  public void testLocation() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "LOCATION:Miami\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        null, null, "Miami", "20080504T123456Z", "20080504T123456Z", null);
  }

  public void testDescription() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "DESCRIPTION:This is a test\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        "This is a test", null, null, "20080504T123456Z", "20080504T123456Z", null);
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "DESCRIPTION:This is a test\r\n\t with a continuation\r\n" +        
        "END:VEVENT\r\nEND:VCALENDAR",
        "This is a test with a continuation", null, null, "20080504T123456Z", "20080504T123456Z", null);
  }

  private static void doTest(String contents,
                             String description,
                             String summary,
                             String location,
                             String start,
                             String end,
                             String attendee) {
    Result fakeResult = new Result(contents, null, null, BarcodeFormat.QR_CODE);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertSame(ParsedResultType.CALENDAR, result.getType());
    CalendarParsedResult calResult = (CalendarParsedResult) result;
    assertEquals(description, calResult.getDescription());
    assertEquals(summary, calResult.getSummary());
    assertEquals(location, calResult.getLocation());
    assertEquals(start, calResult.getStart());
    assertEquals(end, calResult.getEnd());
    assertEquals(attendee, calResult.getAttendee());
  }

}