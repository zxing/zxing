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

/**
 * @author srowen@google.com (Sean Owen)
 */
public enum ParsedReaderResultType {

  // Order is important: if a string could be construed as multiple types,
  // put the most specific one first
  BOOKMARK(BookmarkDoCoMoResult.class),
  ADDRESSBOOK(AddressBookDoCoMoResult.class),
  EMAIL(EmailDoCoMoResult.class),
  URI(URIParsedResult.class),
  TEXT(TextParsedResult.class);

  private Class<? extends ParsedReaderResult> resultClass;

  ParsedReaderResultType(Class<? extends ParsedReaderResult> resultClass) {
    this.resultClass = resultClass;
  }

  Class<? extends ParsedReaderResult> getResultClass() {
    return resultClass;
  }

}
