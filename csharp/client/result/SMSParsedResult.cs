using System.Text;

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

namespace com.google.zxing.client.result
{

	/// <summary>
	/// @author Sean Owen
	/// </summary>
	public sealed class SMSParsedResult : ParsedResult
	{

	  private readonly string[] numbers;
	  private readonly string[] vias;
	  private readonly string subject;
	  private readonly string body;

	  public SMSParsedResult(string number, string via, string subject, string body) : base(ParsedResultType.SMS)
	  {
		this.numbers = new string[] {number};
		this.vias = new string[] {via};
		this.subject = subject;
		this.body = body;
	  }

	  public SMSParsedResult(string[] numbers, string[] vias, string subject, string body) : base(ParsedResultType.SMS)
	  {
		this.numbers = numbers;
		this.vias = vias;
		this.subject = subject;
		this.body = body;
	  }

	  public string SMSURI
	  {
		  get
		  {
			StringBuilder result = new StringBuilder();
			result.Append("sms:");
			bool first = true;
			for (int i = 0; i < numbers.Length; i++)
			{
			  if (first)
			  {
				first = false;
			  }
			  else
			  {
				result.Append(',');
			  }
			  result.Append(numbers[i]);
			  if (vias != null && vias[i] != null)
			  {
				result.Append(";via=");
				result.Append(vias[i]);
			  }
			}
			bool hasBody = body != null;
			bool hasSubject = subject != null;
			if (hasBody || hasSubject)
			{
			  result.Append('?');
			  if (hasBody)
			  {
				result.Append("body=");
				result.Append(body);
			  }
			  if (hasSubject)
			  {
				if (hasBody)
				{
				  result.Append('&');
				}
				result.Append("subject=");
				result.Append(subject);
			  }
			}
			return result.ToString();
		  }
	  }

	  public string[] Numbers
	  {
		  get
		  {
			return numbers;
		  }
	  }

	  public string[] Vias
	  {
		  get
		  {
			return vias;
		  }
	  }

	  public string Subject
	  {
		  get
		  {
			return subject;
		  }
	  }

	  public string Body
	  {
		  get
		  {
			return body;
		  }
	  }

	  public override string DisplayResult
	  {
		  get
		  {
			StringBuilder result = new StringBuilder(100);
			maybeAppend(numbers, result);
			maybeAppend(subject, result);
			maybeAppend(body, result);
			return result.ToString();
		  }
	  }

	}
}