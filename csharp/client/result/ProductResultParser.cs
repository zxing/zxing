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
using System;
using BarcodeFormat = com.google.zxing.BarcodeFormat;
using Result = com.google.zxing.Result;
using UPCEReader = com.google.zxing.oned.UPCEReader;
namespace com.google.zxing.client.result
{
	
	/// <summary> Parses strings of digits that represent a UPC code.
	/// 
	/// </summary>
	/// <author>  dswitkin@google.com (Daniel Switkin)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class ProductResultParser:ResultParser
	{
		
		private ProductResultParser()
		{
		}
		
		// Treat all UPC and EAN variants as UPCs, in the sense that they are all product barcodes.
		public static ProductParsedResult parse(Result result)
		{
			BarcodeFormat format = result.BarcodeFormat;
			if (!(BarcodeFormat.UPC_A.Equals(format) || BarcodeFormat.UPC_E.Equals(format) || BarcodeFormat.EAN_8.Equals(format) || BarcodeFormat.EAN_13.Equals(format)))
			{
				return null;
			}
			// Really neither of these should happen:
			System.String rawText = result.Text;
			if (rawText == null)
			{
				return null;
			}
			
			int length = rawText.Length;
			for (int x = 0; x < length; x++)
			{
				char c = rawText[x];
				if (c < '0' || c > '9')
				{
					return null;
				}
			}
			// Not actually checking the checksum again here    
			
			System.String normalizedProductID;
			// Expand UPC-E for purposes of searching
			if (BarcodeFormat.UPC_E.Equals(format))
			{
				normalizedProductID = UPCEReader.convertUPCEtoUPCA(rawText);
			}
			else
			{
				normalizedProductID = rawText;
			}
			
			return new ProductParsedResult(rawText, normalizedProductID);
		}
	}
}