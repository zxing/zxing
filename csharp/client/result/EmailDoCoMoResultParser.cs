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

namespace com.google.zxing.client.result
{

	using Result = com.google.zxing.Result;
    using System.Text.RegularExpressions;


	/// <summary>
	/// Implements the "MATMSG" email message entry format.
	/// 
	/// Supported keys: TO, SUB, BODY
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class EmailDoCoMoResultParser : AbstractDoCoMoResultParser
	{

	  private static readonly string ATEXT_ALPHANUMERIC = "[a-zA-Z0-9@.!#$%&'*+\\-/=?^_`{|}~]+";

	  public override ParsedResult parse(Result result)
	  {
		string rawText = getMassagedText(result);
		if (!rawText.StartsWith("MATMSG:"))
		{
		  return null;
		}
		string[] rawTo = matchDoCoMoPrefixedField("TO:", rawText, true);
		if (rawTo == null)
		{
		  return null;
		}
		string to = rawTo[0];
		if (!isBasicallyValidEmailAddress(to))
		{
		  return null;
		}
		string subject = matchSingleDoCoMoPrefixedField("SUB:", rawText, false);
		string body = matchSingleDoCoMoPrefixedField("BODY:", rawText, false);
		return new EmailAddressParsedResult(to, subject, body, "mailto:" + to);
	  }

	  /// <summary>
	  /// This implements only the most basic checking for an email address's validity -- that it contains
	  /// an '@' and contains no characters disallowed by RFC 2822. This is an overly lenient definition of
	  /// validity. We want to generally be lenient here since this class is only intended to encapsulate what's
	  /// in a barcode, not "judge" it.
	  /// </summary>
	  internal static bool isBasicallyValidEmailAddress(string email)
	  {
          //return email != null && ATEXT_ALPHANUMERIC.matcher(email).matches() && email.IndexOf('@') >= 0;
          return email != null && new Regex( ATEXT_ALPHANUMERIC).IsMatch(email) && email.IndexOf('@') >= 0;
      }

	}
}