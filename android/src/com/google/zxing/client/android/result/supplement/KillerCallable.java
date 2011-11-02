/*
 * Copyright (C) 2010 ZXing authors
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

package com.google.zxing.client.android.result.supplement;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class KillerCallable implements Callable<Void> {

  private final Future<?> future;
  private final long timeout;
  private final TimeUnit unit;

  KillerCallable(Future<?> future, long timeout, TimeUnit unit) {
    this.future = future;
    this.timeout = timeout;
    this.unit = unit;
  }

  @Override
  public Void call() throws ExecutionException, InterruptedException {
    try {
      future.get(timeout, unit);
    } catch (TimeoutException te) {
      future.cancel(true);
    }
    return null;
  }

}
