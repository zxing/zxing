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
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link CalendarParsedResult}.
 *
 * @author Sean Owen
 */
public final class CalendarParsedResultTestCase extends Assert {

  private static final double EPSILON = 0.0000000001;

  @Test
  public void testStartEnd() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "DTEND:20080505T234555Z\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        null, null, null, "20080504T123456Z", "20080505T234555Z");
  }

  @Test
  public void testNoVCalendar() {
    doTest(
        "BEGIN:VEVENT\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "DTEND:20080505T234555Z\r\n" +
        "END:VEVENT",
        null, null, null, "20080504T123456Z", "20080505T234555Z");
  }

  @Test
  public void testStart() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        null, null, null, "20080504T123456Z", "20080504T123456Z");
  }

  @Test
  public void testSummary() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "SUMMARY:foo\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        null, "foo", null, "20080504T123456Z", "20080504T123456Z");
  }

  @Test
  public void testLocation() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "LOCATION:Miami\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        null, null, "Miami", "20080504T123456Z", "20080504T123456Z");
  }

  @Test
  public void testDescription() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "DESCRIPTION:This is a test\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        "This is a test", null, null, "20080504T123456Z", "20080504T123456Z");
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "DESCRIPTION:This is a test\r\n\t with a continuation\r\n" +        
        "END:VEVENT\r\nEND:VCALENDAR",
        "This is a test with a continuation", null, null, "20080504T123456Z", "20080504T123456Z");
  }

  @Test
  public void testGeo() {
    doTest(
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
        "DTSTART:20080504T123456Z\r\n" +
        "GEO:-12.345;-45.678\r\n" +
        "END:VEVENT\r\nEND:VCALENDAR",
        null, null, null, "20080504T123456Z", "20080504T123456Z", null, -12.345, -45.678);
  }

  private static void doTest(String contents,
                             String description,
                             String summary,
                             String location,
                             String start,
                             String end) {
    doTest(contents, description, summary, location, start, end, null, Double.NaN, Double.NaN);
  }

  private static void doTest(String contents,
                             String description,
                             String summary,
                             String location,
                             String start,
                             String end,
                             String attendee,
                             double latitude,
                             double longitude) {
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
    assertEqualOrNaN(latitude, calResult.getLatitude());
    assertEqualOrNaN(longitude, calResult.getLongitude());
  }

  private static void assertEqualOrNaN(double expected, double actual) {
    if (Double.isNaN(expected)) {
      assertTrue(Double.isNaN(actual));
    } else {
      assertEquals(expected, actual, EPSILON);
    }
  }

}