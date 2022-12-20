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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Simple class which tracks a number of actions that happen per time and can flag when an action has
 * happened too frequently recently. This can be used for example to track and temporarily block access
 * from certain IPs or to certain hosts.
 */
final class DoSTracker {

  private static final Logger log = Logger.getLogger(DoSTracker.class.getName());

  private volatile int maxAccessesPerTime;
  private final Map<String,AtomicInteger> numRecentAccesses;

  /**
   * @param timer {@link Timer} to use for scheduling update tasks
   * @param name identifier for this tracker
   * @param maxAccessesPerTime maximum number of accesses allowed from one source per {@code accessTimeMS}
   * @param accessTimeMS interval in milliseconds over which up to {@code maxAccessesPerTime} accesses are allowed
   * @param maxEntries maximum number of source entries to track before forgetting least recent ones
   * @param maxLoad if set, dynamically adjust {@code maxAccessesPerTime} downwards when average load per core
   *                exceeds this value, and upwards when below this value
   */
  DoSTracker(Timer timer,
             String name,
             int maxAccessesPerTime,
             long accessTimeMS,
             int maxEntries,
             Double maxLoad) {
    this.maxAccessesPerTime = maxAccessesPerTime;
    this.numRecentAccesses = new LRUMap<>(maxEntries);
    timer.schedule(new TrackerTask(name, maxLoad), accessTimeMS, accessTimeMS);
  }

  boolean isBanned(String event) {
    if (event == null) {
      return true;
    }
    AtomicInteger count;
    synchronized (numRecentAccesses) {
      count = numRecentAccesses.get(event);
      if (count == null) {
        numRecentAccesses.put(event, new AtomicInteger(1));
        return false;
      }
    }
    return count.incrementAndGet() > maxAccessesPerTime;
  }

  private final class TrackerTask extends TimerTask {

    private final String name;
    private final Double maxLoad;

    private TrackerTask(String name, Double maxLoad) {
      this.name = name;
      this.maxLoad = maxLoad;
    }

    @Override
    public void run() {
      // largest count <= maxAccessesPerTime
      int maxAllowedCount = 1;
      // smallest count > maxAccessesPerTime
      int minDisallowedCount = Integer.MAX_VALUE;
      int localMAPT = maxAccessesPerTime;
      int totalEntries;
      int clearedEntries = 0;
      synchronized (numRecentAccesses) {
        totalEntries = numRecentAccesses.size();
        Iterator<Map.Entry<String,AtomicInteger>> accessIt = numRecentAccesses.entrySet().iterator();
        while (accessIt.hasNext()) {
          Map.Entry<String,AtomicInteger> entry = accessIt.next();
          AtomicInteger atomicCount = entry.getValue();
          int count = atomicCount.get();
          // If number of accesses is below the threshold, remove it entirely
          if (count <= localMAPT) {
            accessIt.remove();
            maxAllowedCount = Math.max(maxAllowedCount, count);
            clearedEntries++;
          } else {
            // Reduce count of accesses held against the host
            atomicCount.getAndAdd(-localMAPT);
            minDisallowedCount = Math.min(minDisallowedCount, count);
          }
        }
      }
      log.info(name + ": " + clearedEntries + " of " + totalEntries + " cleared");

      if (maxLoad != null) {
        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
        if (mxBean == null) {
          log.warning("Could not obtain OperatingSystemMXBean; ignoring load");
        } else {
          double loadAvg = mxBean.getSystemLoadAverage();
          if (loadAvg >= 0.0) {
            int cores = mxBean.getAvailableProcessors();
            double loadRatio = loadAvg / cores;
            int newMaxAccessesPerTime = loadRatio > maxLoad ?
                Math.min(maxAllowedCount, Math.max(1, maxAccessesPerTime - 1)) :
                Math.max(minDisallowedCount, maxAccessesPerTime);
            log.info(name + ": Load ratio: " + loadRatio +
                " (" + loadAvg + '/' + cores + ") vs " + maxLoad +
                " ; new maxAccessesPerTime: " + newMaxAccessesPerTime);
            maxAccessesPerTime = newMaxAccessesPerTime;
          }
        }
      }
    }

  }

}
