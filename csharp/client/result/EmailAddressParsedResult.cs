using System.Text;

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

	/// <summary>
	/// @author Sean Owen
	/// </summary>
	public sealed class EmailAddressParsedResult : ParsedResult
	{

	  private readonly string emailAddress;
	  private readonly string subject;
	  private readonly string body;
	  private readonly string mailtoURI;

	  internal EmailAddressParsedResult(string emailAddress, string subject, string body, string mailtoURI) : base(ParsedResultType.EMAIL_ADDRESS)
	  {
		this.emailAddress = emailAddress;
		this.subject = subject;
		this.body = body;
		this.mailtoURI = mailtoURI;
	  }

	  public string EmailAddress
	  {
		  get
		  {
			return emailAddress;
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

	  public string MailtoURI
	  {
		  get
		  {
			return mailtoURI;
		  }
	  }

	  public override string DisplayResult
	  {
		  get
		  {
			StringBuilder result = new StringBuilder(30);
			maybeAppend(emailAddress, result);
			maybeAppend(subject, result);
			maybeAppend(body, result);
			return result.ToString();
		  }
	  }

	}
}