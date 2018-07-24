/*
 * Copyright 2017 ZXing authors
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

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Simple class which tracks a number of actions that happen per time and can flag when an action has
 * happened too frequently recently. This can be used for example to track and temporarily block access
 * from certain IPs or to certain hosts.
 */
final class DoSTracker {

  private static final Logger log = Logger.getLogger(DoSTracker.class.getName());

  private final long maxAccessesPerTime;
  private final Map<String,AtomicLong> numRecentAccesses;

  DoSTracker(Timer timer, final int maxAccessesPerTime, long accessTimeMS, int maxEntries) {
    this.maxAccessesPerTime = maxAccessesPerTime;
    this.numRecentAccesses = new LRUMap<>(maxEntries);
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        synchronized (numRecentAccesses) {
          Iterator<Map.Entry<String,AtomicLong>> accessIt = numRecentAccesses.entrySet().iterator();
          while (accessIt.hasNext()) {
            Map.Entry<String,AtomicLong> entry = accessIt.next();
            AtomicLong count = entry.getValue();
            // If number of accesses is below the threshold, remove it entirely
            if (count.get() <= maxAccessesPerTime) {
              accessIt.remove();
            } else {
              // Else it exceeded the max, so log it (again)
              log.warning("Blocking " + entry.getKey() + " (" + count + " outstanding)");
              // Reduce count of accesses held against the IP
              count.getAndAdd(-maxAccessesPerTime);
            }
          }
        }
      }
    }, accessTimeMS, accessTimeMS);

  }

  boolean isBanned(String event) {
    if (event == null) {
      return true;
    }
    AtomicLong count;
    synchronized (numRecentAccesses) {
      count = numRecentAccesses.get(event);
      if (count == null) {
        count = new AtomicLong();
        numRecentAccesses.put(event, count);
      }
    }
    return count.incrementAndGet() > maxAccessesPerTime;
  }

}
