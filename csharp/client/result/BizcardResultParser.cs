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
using System;
using Result = com.google.zxing.Result;
namespace com.google.zxing.client.result
{
	
	/// <summary> Implements the "BIZCARD" address book entry format, though this has been
	/// largely reverse-engineered from examples observed in the wild -- still
	/// looking for a definitive reference.
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class BizcardResultParser:AbstractDoCoMoResultParser
	{
		
		// Yes, we extend AbstractDoCoMoResultParser since the format is very much
		// like the DoCoMo MECARD format, but this is not technically one of 
		// DoCoMo's proposed formats
		
		public static AddressBookParsedResult parse(Result result)
		{
			System.String rawText = result.Text;
			if (rawText == null || !rawText.StartsWith("BIZCARD:"))
			{
				return null;
			}
			System.String firstName = matchSingleDoCoMoPrefixedField("N:", rawText, true);
			System.String lastName = matchSingleDoCoMoPrefixedField("X:", rawText, true);
			System.String fullName = buildName(firstName, lastName);
			System.String title = matchSingleDoCoMoPrefixedField("T:", rawText, true);
			System.String org = matchSingleDoCoMoPrefixedField("C:", rawText, true);
			System.String[] addresses = matchDoCoMoPrefixedField("A:", rawText, true);
			System.String phoneNumber1 = matchSingleDoCoMoPrefixedField("B:", rawText, true);
			System.String phoneNumber2 = matchSingleDoCoMoPrefixedField("M:", rawText, true);
			System.String phoneNumber3 = matchSingleDoCoMoPrefixedField("F:", rawText, true);
			System.String email = matchSingleDoCoMoPrefixedField("E:", rawText, true);
			
			return new AddressBookParsedResult(maybeWrap(fullName), null, buildPhoneNumbers(phoneNumber1, phoneNumber2, phoneNumber3), maybeWrap(email), null, addresses, org, null, title, null);
		}
		
		private static System.String[] buildPhoneNumbers(System.String number1, System.String number2, System.String number3)
		{
			System.Collections.ArrayList numbers = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(3));
			if (number1 != null)
			{
				numbers.Add(number1);
			}
			if (number2 != null)
			{
				numbers.Add(number2);
			}
			if (number3 != null)
			{
				numbers.Add(number3);
			}
			int size = numbers.Count;
			if (size == 0)
			{
				return null;
			}
			System.String[] result = new System.String[size];
			for (int i = 0; i < size; i++)
			{
				result[i] = ((System.String) numbers[i]);
			}
			return result;
		}
		
		private static System.String buildName(System.String firstName, System.String lastName)
		{
			if (firstName == null)
			{
				return lastName;
			}
			else
			{
				return lastName == null?firstName:firstName + ' ' + lastName;
			}
		}
	}
}