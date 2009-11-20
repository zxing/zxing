package com.google.zxing.client.result
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

	import com.google.zxing.common.flexdatatypes.ArrayList;
	import com.google.zxing.Result;

	/**
	 * Implements KDDI AU's address book format. See
	 * <a href="http://www.au.kddi.com/ezfactory/tec/two_dimensions/index.html">
	 * http://www.au.kddi.com/ezfactory/tec/two_dimensions/index.html</a>.
	 * (Thanks to Yuzo for translating!)
	 *
	 * @author Sean Owen
	 */
	public final class AddressBookDocomoResultParser extends ResultParser {

  public static function parse(result:Result ):AddressBookParsedResult {
    var rawText:String  = result.getText();
    // MEMORY is mandatory; seems like a decent indicator, as does end-of-record separator CR/LF
    if (rawText == null || rawText.indexOf("MEMORY") < 0 || rawText.indexOf("\r\n") < 0) {
      return null;
    }

    // NAME1 and NAME2 have specific uses, namely written name and pronunciation, respectively.
    // Therefore we treat them specially instead of as an array of names.
    var name:String  = matchSinglePrefixedField("NAME1:", rawText, '\r', true);
    var pronunciation:String = matchSinglePrefixedField("NAME2:", rawText, '\r', true);

    var phoneNumbers:Array  = matchMultipleValuePrefix("TEL", 3, rawText, true);
    var emails:Array = matchMultipleValuePrefix("MAIL", 3, rawText, true);
    var note:String = matchSinglePrefixedField("MEMORY:", rawText, '\r', false);
    var address:String = matchSinglePrefixedField("ADD:", rawText, '\r', true);
    return new AddressBookParsedResult(maybeWrap(name), pronunciation, phoneNumbers, emails, note,
        address, null, null, null, null);
  }

  public static function matchMultipleValuePrefix(prefix:String , max:int , rawText:String , trim:Boolean ):Array 
  {
    var values:ArrayList = null;
    for (var i:int = 1; i <= max; i++) 
    {
      var value:String = matchSinglePrefixedField(prefix + i + ':', rawText, '\r', trim);
      if (value == null) {
        break;
      }
      if (values == null) {
        values = new ArrayList(max); // lazy init
      }
      values.addElement(value);
    }
    if (values == null) {
      return null;
    }
    return toStringArray(values);
  }

}

}