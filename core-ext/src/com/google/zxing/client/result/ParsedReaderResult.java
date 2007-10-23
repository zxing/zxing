/*
 * Copyright 2007 Google Inc.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author srowen@google.com (Sean Owen)
 */
public abstract class ParsedReaderResult {

  private final ParsedReaderResultType type;

  ParsedReaderResult(ParsedReaderResultType type) {
    this.type = type;
  }

  public ParsedReaderResultType getType() {
    return type;
  }

  public abstract String getDisplayResult();

  public static ParsedReaderResult parseReaderResult(String rawText) {
    for (ParsedReaderResultType type : ParsedReaderResultType.values()) {
      Class<? extends ParsedReaderResult> resultClass = type.getResultClass();
      try {
        Constructor<? extends ParsedReaderResult> constructor =
            resultClass.getConstructor(String.class);
        return constructor.newInstance(rawText);
      } catch (InvocationTargetException ite) {
        Throwable cause = ite.getCause();
        if (cause instanceof IllegalArgumentException) {
          continue;
        }
        throw new RuntimeException(cause);
      } catch (NoSuchMethodException nsme) {
        throw new RuntimeException(nsme);
      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae);
      } catch (InstantiationException ie) {
        throw new RuntimeException(ie);
      }
    }
    throw new IllegalStateException("TextResult should always work");
  }

  @Override
  public String toString() {
    return getDisplayResult();
  }

}
