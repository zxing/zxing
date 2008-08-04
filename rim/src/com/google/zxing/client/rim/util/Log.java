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

package com.google.zxing.client.rim.util;

import net.rim.device.api.system.EventLogger;

/**
 * Used to write logging messages. When debugging, System.out is used to write to the simulator.
 * When running on a real device, the EventLogger is used. To access the event log on a real device,
 * go to the home screen, hold down ALT and type lglg.
 *
 * This code was contributed by LifeMarks.
 *
 * @author Matt York (matt@lifemarks.mobi)
 */
public final class Log {

  private static final String LOG_ID_STRING = "zxing";
  private static final long LOG_ID_LONG = 0x351e9b79fd52317L;

  /** Used to determine if the log message should be set to System.out */
  private static final boolean logToSystemOut;

  static {
   // Initializes the logger. Currently set to not log to System.out and log
   // at the INFO level.
    EventLogger.register(LOG_ID_LONG, LOG_ID_STRING, EventLogger.VIEWER_STRING);
    EventLogger.setMinimumLevel(EventLogger.DEBUG_INFO); // set this to change logging level message.
    logToSystemOut = false; // needs to be false for deployment to blackberry device and true for debuging on simulator.
  }

  private Log() {
  }

  /**
   * Logs the given message at the debug level.
   */
  public static void debug(String message) {
    EventLogger.logEvent(LOG_ID_LONG, message.getBytes(), EventLogger.DEBUG_INFO);
    logToSystemOut(message);
  }

  /**
   * Logs the given message at the info level.
   */
  public static void info(String message) {
    EventLogger.logEvent(LOG_ID_LONG, message.getBytes(), EventLogger.INFORMATION);
    logToSystemOut(message);
  }

  /**
   * Logs the given message at the error level.
   */
  public static void error(String message) {
    EventLogger.logEvent(LOG_ID_LONG, message.getBytes(), EventLogger.ERROR);
    logToSystemOut(message);
  }

  /**
   * Logs the given message to system.out.
   * This is useful when debugging on the simulator.
   */
  private static void logToSystemOut(String message) {
    if (logToSystemOut) {
      System.out.println(message);
    }
  }

}
