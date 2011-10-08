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
using DecodeHintType = com.google.zxing.DecodeHintType;
using ReaderException = com.google.zxing.ReaderException;
using Result = com.google.zxing.Result;
using BitArray = com.google.zxing.common.BitArray;
namespace com.google.zxing.oned
{
	
	/// <summary> <p>A reader that can read all available UPC/EAN formats. If a caller wants to try to
	/// read all such formats, it is most efficient to use this implementation rather than invoke
	/// individual readers.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class MultiFormatUPCEANReader:OneDReader
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'readers '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.Collections.ArrayList readers;
		
		public MultiFormatUPCEANReader(System.Collections.Hashtable hints)
		{
			System.Collections.ArrayList possibleFormats = hints == null?null:(System.Collections.ArrayList) hints[DecodeHintType.POSSIBLE_FORMATS];
			readers = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(10));
			if (possibleFormats != null)
			{
				if (possibleFormats.Contains(BarcodeFormat.EAN_13))
				{
					readers.Add(new EAN13Reader());
				}
				else if (possibleFormats.Contains(BarcodeFormat.UPC_A))
				{
					readers.Add(new UPCAReader());
				}
				if (possibleFormats.Contains(BarcodeFormat.EAN_8))
				{
					readers.Add(new EAN8Reader());
				}
				if (possibleFormats.Contains(BarcodeFormat.UPC_E))
				{
					readers.Add(new UPCEReader());
				}
			}
			if ((readers.Count == 0))
			{
				readers.Add(new EAN13Reader());
				// UPC-A is covered by EAN-13
				readers.Add(new EAN8Reader());
				readers.Add(new UPCEReader());
			}
		}
		
		public override Result decodeRow(int rowNumber, BitArray row, System.Collections.Hashtable hints)
		{
			// Compute this location once and reuse it on multiple implementations
			int[] startGuardPattern = UPCEANReader.findStartGuardPattern(row);
			int size = readers.Count;
			for (int i = 0; i < size; i++)
			{
				UPCEANReader reader = (UPCEANReader) readers[i];
				Result result;
				try
				{
					result = reader.decodeRow(rowNumber, row, startGuardPattern, hints);
				}
				catch (ReaderException)
				{
					continue;
				}
				// Special case: a 12-digit code encoded in UPC-A is identical to a "0"
				// followed by those 12 digits encoded as EAN-13. Each will recognize such a code,
				// UPC-A as a 12-digit string and EAN-13 as a 13-digit string starting with "0".
				// Individually these are correct and their readers will both read such a code
				// and correctly call it EAN-13, or UPC-A, respectively.
				//
				// In this case, if we've been looking for both types, we'd like to call it
				// a UPC-A code. But for efficiency we only run the EAN-13 decoder to also read
				// UPC-A. So we special case it here, and convert an EAN-13 result to a UPC-A
				// result if appropriate.
				if (result.BarcodeFormat.Equals(BarcodeFormat.EAN_13) && result.Text[0] == '0')
				{
					return new Result(result.Text.Substring(1), null, result.ResultPoints, BarcodeFormat.UPC_A);
				}
				return result;
			}
			
			throw ReaderException.Instance;
		}
	}
}
