package com.google.zxing.client.result
{
	/*
 * Copyright 2007 ZXing authors
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

import com.google.zxing.Result;
import com.google.zxing.common.flexdatatypes.Utils;

/**
 * Implements the "MATMSG" email message entry format.
 *
 * Supported keys: TO, SUB, BODY
 *
 * @author Sean Owen
 */
public final class EmailDoCoMoResultParser extends AbstractDoCoMoResultParser {

  private static var ATEXT_SYMBOLS:Array = ['@','.','!','#','$','%','&','\'','*','+','-','/','=','?','^','_','`','{','|','}','~'];

  public static function parse(result:Result):EmailAddressParsedResult {
    var rawText:String = result.getText();
    if (rawText == null || !Utils.startsWith(rawText,"MATMSG:")) {
      return null;
    }
    var rawTo:Array = matchDoCoMoPrefixedField("TO:", rawText, true);
    if (rawTo == null) {
      return null;
    }
    var _to:String = rawTo[0];
    if (!isBasicallyValidEmailAddress(_to)) {
      return null;
    }
    var subject:String = matchSingleDoCoMoPrefixedField("SUB:", rawText, false);
    var body:String = matchSingleDoCoMoPrefixedField("BODY:", rawText, false);
    return new EmailAddressParsedResult(_to, subject, body, "mailto:" + _to);
  }

  /**
   * This implements only the most basic checking for an email address's validity -- that it contains
   * an '@' contains no characters disallowed by RFC 2822. This is an overly lenient definition of
   * validity. We want to generally be lenient here since this class is only intended to encapsulate what's
   * in a barcode, not "judge" it.
   */
  public static function isBasicallyValidEmailAddress(email:String):Boolean {
    if (email == null) {
      return false;
    }
    var atFound:Boolean = false;
    for (var i:int = 0; i < email.length; i++) {
      var c:String = email.charAt(i);
      if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') &&
          !isAtextSymbol(c)) {
        return false;
      }
      if (c == '@') {
        if (atFound) {
          return false;
        }
        atFound = true;
      }
    }
    return atFound;
  }

  private static function isAtextSymbol(c:String):Boolean {
    for (var i:int = 0; i < ATEXT_SYMBOLS.length; i++) {
      if (c == ATEXT_SYMBOLS[i]) {
        return true;
      }
    }
    return false;
  }

}
}