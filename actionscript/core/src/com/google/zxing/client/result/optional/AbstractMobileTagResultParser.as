package com.google.zxing.client.result.optional
{
/*
 * Copyright 2008 ZXing authors
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

import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.flexdatatypes.Utils;
/**
 * <p>Superclass for classes encapsulating reader results encoded according
 * to the MobileTag Reader International Specification.</p>
 * 
 * @author Sean Owen
 */
public class AbstractMobileTagResultParser extends ResultParser {

  public static var ACTION_DO:int = 1;
  public static var ACTION_EDIT:int = 2;
  public static var  ACTION_SAVE:int = 4;

  public static function matchDelimitedFields(rawText:String, maxItems:int):Array {
    var result:Array = new Array(maxItems);
    var item:int = 0;
    var i:int = 0;
    var max:int = rawText.length;
    while (item < maxItems && i < max) {
      var start:int = i; // Found the start of a match here
      var done:Boolean = false;
      while (!done) {
        i = rawText.indexOf('|', i);
        if (i < 0) {
          // No terminating end character? done. Set i such that loop terminates and break
          i = rawText.length;
          done = true;
        } else if (rawText.charAt(i - 1) == '\\') {
          // semicolon was escaped so continue
          i++;
        } else {
          // found a match
          if (start != i) {
            result[item] = unescapeBackslash(rawText.substring(start, i));
          }
          item++;
          i++;
          done = true;
        }
      }
    }
    if (item < maxItems) {
      return null;
    }
    return result;
  }

  public static function isDigits(s:String, expectedLength:int):Boolean 
  {
    if (s == null) {
      return true;
    }
    if (s.length != expectedLength) {
      return false;
    }
    for (var i:int = 0; i < expectedLength; i++) {
      if (!Utils.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

}}