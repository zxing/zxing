/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.web.generator.client;

/**
 * A class containing a list of timezones, with their full names, and time
 * offset.
 * 
 * @author Yohann Coppel
 */
final class TimeZoneList {

  private TimeZoneList() {
  }

  private static final long ONE_HOUR = 60L * 60 * 1000;
  private static final long THIRTY_MIN = 30L * 60 * 1000;
  
  static final TimeZoneInfo[] TIMEZONES = {
    new TimeZoneInfo("GMT", "Greenwich Mean Time", "GMT",                          0), // 0
    new TimeZoneInfo("UTC", "Universal Coordinated Time", "GMT",                   0),
    new TimeZoneInfo("ECT", "European Central Time", "GMT+1:00",                       ONE_HOUR),
    new TimeZoneInfo("EET", "Eastern European Time", "GMT+2:00",                   2 * ONE_HOUR),
    new TimeZoneInfo("ART", "(Arabic) Egypt Standard Time", "GMT+2:00",            2 * ONE_HOUR),
    new TimeZoneInfo("EAT", "Eastern African Time", "GMT+3:00",                    3 * ONE_HOUR), // 5
    new TimeZoneInfo("MET", "Middle East Time", "GMT+3:30",                        3 * ONE_HOUR + THIRTY_MIN),
    new TimeZoneInfo("NET", "Near East Time", "GMT+4:00",                          4 * ONE_HOUR),
    new TimeZoneInfo("PLT", "Pakistan Lahore Time", "GMT+5:00",                    5 * ONE_HOUR),
    new TimeZoneInfo("IST", "India Standard Time", "GMT+5:30",                     5 * ONE_HOUR + THIRTY_MIN),
    new TimeZoneInfo("BST", "Bangladesh Standard Time", "GMT+6:00",                6 * ONE_HOUR), // 10
    new TimeZoneInfo("VST", "Vietnam Standard Time", "GMT+7:00",                   7 * ONE_HOUR),
    new TimeZoneInfo("CTT", "China Taiwan Time", "GMT+8:00",                       8 * ONE_HOUR),
    new TimeZoneInfo("JST", "Japan Standard Time", "GMT+9:00",                     9 * ONE_HOUR),
    new TimeZoneInfo("ACT", "Australia Central Time", "GMT+9:30",                  9 * ONE_HOUR + THIRTY_MIN),
    new TimeZoneInfo("AET", "Australia Eastern Time", "GMT+10:00",                10 * ONE_HOUR), // 15
    new TimeZoneInfo("SST", "Solomon Standard Time", "GMT+11:00",                 11 * ONE_HOUR),
    new TimeZoneInfo("NST", "New Zealand Standard Time", "GMT+12:00",             12 * ONE_HOUR),
    new TimeZoneInfo("MIT", "Midway Islands Time", "GMT-11:00",                  -11 * ONE_HOUR),
    new TimeZoneInfo("HST", "Hawaii Standard Time", "GMT-10:00",                 -10 * ONE_HOUR),
    new TimeZoneInfo("AST", "Alaska Standard Time", "GMT-9:00",                   -9 * ONE_HOUR), // 20
    new TimeZoneInfo("PST", "Pacific Standard Time", "GMT-8:00",                  -8 * ONE_HOUR),
    new TimeZoneInfo("PNT", "Phoenix Standard Time", "GMT-7:00",                  -7 * ONE_HOUR),
    new TimeZoneInfo("MST", "Mountain Standard Time", "GMT-7:00",                 -7 * ONE_HOUR),
    new TimeZoneInfo("CST", "Central Standard Time", "GMT-6:00",                  -6 * ONE_HOUR),
    new TimeZoneInfo("EST", "Eastern Standard Time", "GMT-5:00",                  -5 * ONE_HOUR), // 25
    new TimeZoneInfo("IET", "Indiana Eastern Standard Time", "GMT-5:00",          -5 * ONE_HOUR),
    new TimeZoneInfo("PRT", "Puerto Rico and US Virgin Islands Time", "GMT-4:00", -4 * ONE_HOUR),
    new TimeZoneInfo("CNT", "Canada Newfoundland Time", "GMT-3:30",               -3 * ONE_HOUR - THIRTY_MIN),
    new TimeZoneInfo("AGT", "Argentina Standard Time", "GMT-3:00",                -3 * ONE_HOUR),
    new TimeZoneInfo("BET", "Brazil Eastern Time", "GMT-3:00",                    -3 * ONE_HOUR), // 30
    new TimeZoneInfo("CAT", "Central African Time", "GMT-1:00",                   -1 * ONE_HOUR),
  };
  
}
