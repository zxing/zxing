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

import com.google.zxing.Result;

/**
 * <p>Abstract class representing the result of decoding a barcode, as more than
 * a String -- as some type of structured data. This might be a subclass which represents
 * a URL, or an e-mail address. {@link #parseReaderResult(Result)} will turn a raw
 * decoded string into the most appropriate type of structured representation.</p>
 *
 * <p>Thanks to Jeff Griffin for proposing rewrite of these classes that relies less
 * on exception-based mechanisms during parsing.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public abstract class ParsedReaderResult {

  private final ParsedReaderResultType type;

  public ParsedReaderResult(ParsedReaderResultType type) {
    this.type = type;
  }

  public ParsedReaderResultType getType() {
    return type;
  }

  public abstract String getDisplayResult();

  public static ParsedReaderResult parseReaderResult(Result theResult) {
    // This is a bit messy, but given limited options in MIDP / CLDC, this may well be the simplest
    // way to go about this. For example, we have no reflection available, really.
    // Order is important here.
    ParsedReaderResult result;
    if ((result = BookmarkDoCoMoParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = AddressBookDoCoMoParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = EmailDoCoMoParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = EmailAddressParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = AddressBookAUParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = TelParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = GeoParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = URLTOParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = URIParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = UPCParsedResult.parse(theResult)) != null) {
      return result;
    }
    return TextParsedResult.parse(theResult);
  }

  public String toString() {
    return getDisplayResult();
  }

}
