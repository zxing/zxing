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
	/// A simple result type encapsulating a string that has no further
	/// interpretation.
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class TextParsedResult : ParsedResult
	{

	  private readonly string text;
	  private readonly string language;

	  public TextParsedResult(string text, string language) : base(ParsedResultType.TEXT)
	  {
		this.text = text;
		this.language = language;
	  }

	  public string Text
	  {
		  get
		  {
			return text;
		  }
	  }

	  public string Language
	  {
		  get
		  {
			return language;
		  }
	  }

	  public override string DisplayResult
	  {
		  get
		  {
			return text;
		  }
	  }

	}

}