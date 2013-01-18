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
	public sealed class TelParsedResult : ParsedResult
	{

	  private readonly string number;
	  private readonly string telURI;
	  private readonly string title;

	  public TelParsedResult(string number, string telURI, string title) : base(ParsedResultType.TEL)
	  {
		this.number = number;
		this.telURI = telURI;
		this.title = title;
	  }

	  public string Number
	  {
		  get
		  {
			return number;
		  }
	  }

	  public string TelURI
	  {
		  get
		  {
			return telURI;
		  }
	  }

	  public string Title
	  {
		  get
		  {
			return title;
		  }
	  }

	  public override string DisplayResult
	  {
		  get
		  {
			StringBuilder result = new StringBuilder(20);
			maybeAppend(number, result);
			maybeAppend(title, result);
			return result.ToString();
		  }
	  }

	}
}