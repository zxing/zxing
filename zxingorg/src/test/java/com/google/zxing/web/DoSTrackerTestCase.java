/*
 * Copyright 2018 ZXing authors
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

package com.google.zxing.web;

import org.junit.Assert;
import org.junit.Test;

import java.util.Timer;

/**
 * Tests {@link DoSTracker}.
 */
public final class DoSTrackerTestCase extends Assert {

  @Test
  public void testDoS() throws Exception {
    Timer timer = new Timer();
    long timerTimeMS = 200;
    DoSTracker tracker = new DoSTracker(timer, 2, timerTimeMS, 3);
    assertFalse(tracker.isBanned("A"));
    assertFalse(tracker.isBanned("A"));
    assertTrue(tracker.isBanned("A"));

    assertFalse(tracker.isBanned("B"));
    assertFalse(tracker.isBanned("C"));
    assertFalse(tracker.isBanned("D"));
    assertFalse(tracker.isBanned("A"));

    assertFalse(tracker.isBanned("A"));
    assertTrue(tracker.isBanned("A"));
    Thread.sleep(timerTimeMS * 3);
    assertFalse(tracker.isBanned("A"));

    assertFalse(tracker.isBanned("A"));
    assertTrue(tracker.isBanned("A"));
    assertTrue(tracker.isBanned("A"));
    assertTrue(tracker.isBanned("A"));
    assertTrue(tracker.isBanned("A"));
    assertTrue(tracker.isBanned("A"));
    Thread.sleep(timerTimeMS * 2);
    assertTrue(tracker.isBanned("A"));
  }

}
