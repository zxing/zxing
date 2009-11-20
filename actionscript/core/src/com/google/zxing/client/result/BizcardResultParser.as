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
 * Implements the "BIZCARD" address book entry format, though this has been
 * largely reverse-engineered from examples observed in the wild -- still
 * looking for a definitive reference.
 *
 * @author Sean Owen
 */
public final class BizcardResultParser extends AbstractDoCoMoResultParser 
{

  // Yes, we extend AbstractDoCoMoResultParser since the format is very much
  // like the DoCoMo MECARD format, but this is not technically one of 
  // DoCoMo's proposed formats

  public static function  parse(result:Result):AddressBookParsedResult {
    var rawText:String = result.getText();
    if (rawText == null || !(rawText.substr(0,8) == "BIZCARD:")) {
      return null;
    }
    var firstName:String  = matchSingleDoCoMoPrefixedField("N:", rawText, true);
    var lastName:String = matchSingleDoCoMoPrefixedField("X:", rawText, true);
    var fullName:String = buildName(firstName, lastName);
    var title:String = matchSingleDoCoMoPrefixedField("T:", rawText, true);
    var org:String = matchSingleDoCoMoPrefixedField("C:", rawText, true);
    var address:String = matchSingleDoCoMoPrefixedField("A:", rawText, true);
    var phoneNumber1:String = matchSingleDoCoMoPrefixedField("B:", rawText, true);
    var phoneNumber2:String = matchSingleDoCoMoPrefixedField("M:", rawText, true);
    var phoneNumber3:String  = matchSingleDoCoMoPrefixedField("F:", rawText, true);
    var email:String = matchSingleDoCoMoPrefixedField("E:", rawText, true);

    return new AddressBookParsedResult(maybeWrap(fullName),
                                       null,
                                       buildPhoneNumbers(phoneNumber1, phoneNumber2, phoneNumber3),
                                       maybeWrap(email),
                                       null,
                                       address,
                                       org,
                                       null,
                                       title,
                                       null);
  }

  private static function buildPhoneNumbers(number1:String, number2:String, number3:String ):Array {
    var numbers:ArrayList = new ArrayList(3);
    if (number1 != null) {
      numbers.addElement(number1);
    }
    if (number2 != null) {
      numbers.addElement(number2);
    }
    if (number3 != null) {
      numbers.addElement(number3);
    }
    var size:int = numbers.size();
    if (size == 0) {
      return null;
    }
    var result:Array = new Array(size);
    for (var i:int = 0; i < size; i++) {
      result[i] = String(numbers.elementAt(i));
    }
    return result;
  }

  private static function buildName(firstName:String, lastName:String):String 
  {
    if (firstName == null) {
      return lastName;
    } else {
      return lastName == null ? firstName : firstName + ' ' + lastName;
    }
  }

}

}