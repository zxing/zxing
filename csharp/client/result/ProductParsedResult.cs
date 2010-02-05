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
namespace com.google.zxing.client.result
{
	
	/// <author>  dswitkin@google.com (Daniel Switkin)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class ProductParsedResult:ParsedResult
	{
		public System.String ProductID
		{
			get
			{
				return productID;
			}
			
		}
		public System.String NormalizedProductID
		{
			get
			{
				return normalizedProductID;
			}
			
		}
		override public System.String DisplayResult
		{
			get
			{
				return productID;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'productID '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String productID;
		//UPGRADE_NOTE: Final was removed from the declaration of 'normalizedProductID '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String normalizedProductID;
		
		internal ProductParsedResult(System.String productID):this(productID, productID)
		{
		}
		
		internal ProductParsedResult(System.String productID, System.String normalizedProductID):base(ParsedResultType.PRODUCT)
		{
			this.productID = productID;
			this.normalizedProductID = normalizedProductID;
		}
	}
}