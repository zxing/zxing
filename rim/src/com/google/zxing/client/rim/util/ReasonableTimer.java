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

/**
 * Used to determine if something happend within a specified amount of time.
 * For example, if a QR code was decoded in a resonable amount of time.
 * If not, perhaps the user should lower their camera resolution.
 *
 * This code was contributed by LifeMarks.
 *
 * @author Matt York (matt@lifemarks.mobi)
 */
public final class ReasonableTimer {

  // 2000 too low for qr decoding
  private static final long DEF_RESONABLE_TIME = 2500; // in ms

  private long reasonableTime;
  private final long startTime;
  private long finishTime;

  public ReasonableTimer() {
    startTime = System.currentTimeMillis();
    reasonableTime = DEF_RESONABLE_TIME;
  }

  public ReasonableTimer(long reasonableTime) {
    startTime = System.currentTimeMillis();
    this.reasonableTime = reasonableTime;
  }

  /**
   * Stops the timing.
   */
  public void finished() {
    finishTime = System.currentTimeMillis();
  }

  /**
   * Returns true if the timer finished in a reasonable amount of time.
   */
  public boolean wasResonableTime() {
    return finishTime - startTime <= reasonableTime;
  }

  /**
   * Sets the reasonable time to the given time
   */
  public void setResonableTime(long reasonableTime) {
    this.reasonableTime = reasonableTime;
  }

  /**
   * Returns the reasonable time.
   */
  public long getResonableTime() {
    return reasonableTime;
  }

}
