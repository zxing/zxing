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
namespace com.google.zxing.client.result
{
	
	/// <author>  jbreiden@google.com (Jeff Breidenbach)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class ISBNParsedResult:ParsedResult
	{
		public System.String ISBN
		{
			get
			{
				return isbn;
			}
			
		}
		override public System.String DisplayResult
		{
			get
			{
				return isbn;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'isbn '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String isbn;
		
		internal ISBNParsedResult(System.String isbn):base(ParsedResultType.ISBN)
		{
			this.isbn = isbn;
		}
	}
}