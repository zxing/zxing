/*
* Copyright 2009 ZXing authors
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
using Result = com.google.zxing.Result;
using BinaryBitmap = com.google.zxing.BinaryBitmap;
using ReaderException = com.google.zxing.ReaderException;
namespace com.google.zxing.multi
{
	
	/// <summary> Implementation of this interface attempt to read several barcodes from one image.
	/// 
	/// </summary>
	/// <seealso cref="com.google.zxing.Reader">
	/// </seealso>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>

	public interface MultipleBarcodeReader
	{
		
		Result[] decodeMultiple(BinaryBitmap image);
		
		Result[] decodeMultiple(BinaryBitmap image, System.Collections.Hashtable hints);
	}
}