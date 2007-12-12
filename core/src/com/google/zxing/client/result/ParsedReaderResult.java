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
    // This is a bit messy, but given limited options in MIDP / CLDC, this may well be the simplest
    // way to go about this. For example, we have no reflection available, really.
    // Order is important here.
    try {
      return new BookmarkDoCoMoResult(rawText);
    } catch (IllegalArgumentException iae) {
      // continue
    }
    try {
      return new AddressBookDoCoMoResult(rawText);
    } catch (IllegalArgumentException iae) {
      // continue
    }
    try {
      return new EmailDoCoMoResult(rawText);
    } catch (IllegalArgumentException iae) {
      // continue
    }
    try {
      return new EmailAddressResult(rawText);
    } catch (IllegalArgumentException iae) {
      // continue
    }
    try {
      return new URIParsedResult(rawText);
    } catch (IllegalArgumentException iae) {
      // continue
    }
    try {
      return new UPCParsedResult(rawText);
    } catch (IllegalArgumentException iae) {
      // continue
    }
    return new TextParsedResult(rawText);
  }

  public String toString() {
    return getDisplayResult();
  }

}
