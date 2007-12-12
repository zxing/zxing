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
 * Represents the type of data encoded by a barcode -- from plain text, to a
 * URI, to an e-mail address, etc.
 * 
 * @author srowen@google.com (Sean Owen)
 */
public final class ParsedReaderResultType {

  public static final ParsedReaderResultType BOOKMARK = new ParsedReaderResultType();
  public static final ParsedReaderResultType ADDRESSBOOK = new ParsedReaderResultType();
  public static final ParsedReaderResultType EMAIL = new ParsedReaderResultType();
  public static final ParsedReaderResultType EMAIL_ADDRESS = new ParsedReaderResultType();  
  public static final ParsedReaderResultType UPC = new ParsedReaderResultType();
  public static final ParsedReaderResultType URI = new ParsedReaderResultType();
  public static final ParsedReaderResultType TEXT = new ParsedReaderResultType();

  private ParsedReaderResultType() {
    // do nothing
  }

}
