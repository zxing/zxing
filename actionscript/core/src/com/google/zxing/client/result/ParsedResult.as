package com.google.zxing.client.result
{/*
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

import com.google.zxing.common.flexdatatypes.StringBuilder;

/**
 * <p>Abstract class representing the result of decoding a barcode, as more than
 * a String -- as some type of structured data. This might be a subclass which represents
 * a URL, or an e-mail address. {@link ResultParser#parseResult(Result)} will turn a raw
 * decoded string into the most appropriate type of structured representation.</p>
 *
 * <p>Thanks to Jeff Griffin for proposing rewrite of these classes that relies less
 * on exception-based mechanisms during parsing.</p>
 *
 * @author Sean Owen
 */
public class ParsedResult {

  private var type:ParsedResultType;

  public function ParsedResult(type:ParsedResultType ) {
    this.type = type;
  }

  public function getType():ParsedResultType {
    return type;
  }

  public function getDisplayResult():String{return '';}

  public function toString():String 
  {
    return getDisplayResult();
  }

  public static function maybeAppend(value1:Object, result:StringBuilder ):void 
  {
  	var value:Array;
  	if (value1 is Array) { value = value1 as Array; }
  	else if (value1 is String) { value = [value1]; }
    if (value != null) {
      for (var i:int = 0; i < value.length; i++) {
        if (value[i] != null && value[i].length > 0) {
          if (result.length > 0) {
            result.Append('\n');
          }
          result.Append(value[i]);
        }
      }
    }
  }

}

}