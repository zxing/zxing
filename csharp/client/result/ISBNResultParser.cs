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
using BarcodeFormat = com.google.zxing.BarcodeFormat;
using Result = com.google.zxing.Result;
namespace com.google.zxing.client.result
{
	
	/// <summary> Parses strings of digits that represent a ISBN.
	/// 
	/// </summary>
	/// <author>  jbreiden@google.com (Jeff Breidenbach)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public class ISBNResultParser:ResultParser
	{
		
		private ISBNResultParser()
		{
		}
		
		// ISBN-13 For Dummies 
		// http://www.bisg.org/isbn-13/for.dummies.html
		public static ISBNParsedResult parse(Result result)
		{
			BarcodeFormat format = result.BarcodeFormat;
			if (!BarcodeFormat.EAN_13.Equals(format))
			{
				return null;
			}
			System.String rawText = result.Text;
			if (rawText == null)
			{
				return null;
			}
			int length = rawText.Length;
			if (length != 13)
			{
				return null;
			}
			if (!rawText.StartsWith("978") && !rawText.StartsWith("979"))
			{
				return null;
			}
			
			return new ISBNParsedResult(rawText);
		}
	}
}