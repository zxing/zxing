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
	/// <p>Abstract class representing the result of decoding a barcode, as more than
	/// a String -- as some type of structured data. This might be a subclass which represents
	/// a URL, or an e-mail address. <seealso cref="ResultParser#parseResult(com.google.zxing.Result)"/> will turn a raw
	/// decoded string into the most appropriate type of structured representation.</p>
	/// 
	/// <p>Thanks to Jeff Griffin for proposing rewrite of these classes that relies less
	/// on exception-based mechanisms during parsing.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public abstract class ParsedResult
	{

	  private readonly ParsedResultType type;

	  protected internal ParsedResult(ParsedResultType type)
	  {
		this.type = type;
	  }

	  public ParsedResultType Type
	  {
		  get
		  {
			return type;
		  }
	  }

	  public abstract string DisplayResult {get;}

	  public override string ToString()
	  {
		return DisplayResult;
	  }

	  public static void maybeAppend(string value, StringBuilder result)
	  {
		if (value != null && value.Length > 0)
		{
		  // Don't add a newline before the first value
		  if (result.Length > 0)
		  {
			result.Append('\n');
		  }
		  result.Append(value);
		}
	  }

	  public static void maybeAppend(string[] value, StringBuilder result)
	  {
		if (value != null)
		{
		  foreach (string s in value)
		  {
			if (s != null && s.Length > 0)
			{
			  if (result.Length > 0)
			  {
				result.Append('\n');
			  }
			  result.Append(s);
			}
		  }
		}
	  }

	}

}