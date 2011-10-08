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
	
	/// <author>  dswitkin@google.com (Daniel Switkin)
	/// </author>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class MultiFormatOneDReader:OneDReader
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'readers '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.Collections.ArrayList readers;
		
		public MultiFormatOneDReader(System.Collections.Hashtable hints)
		{
			System.Collections.ArrayList possibleFormats = hints == null?null:(System.Collections.ArrayList) hints[DecodeHintType.POSSIBLE_FORMATS];
			bool useCode39CheckDigit = hints != null && hints[DecodeHintType.ASSUME_CODE_39_CHECK_DIGIT] != null;
			readers = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(10));
			if (possibleFormats != null)
			{
				if (possibleFormats.Contains(BarcodeFormat.EAN_13) || possibleFormats.Contains(BarcodeFormat.UPC_A) || possibleFormats.Contains(BarcodeFormat.EAN_8) || possibleFormats.Contains(BarcodeFormat.UPC_E))
				{
					readers.Add(new MultiFormatUPCEANReader(hints));
				}
				if (possibleFormats.Contains(BarcodeFormat.CODE_39))
				{
					readers.Add(new Code39Reader(useCode39CheckDigit));
				}
				if (possibleFormats.Contains(BarcodeFormat.CODE_128))
				{
					readers.Add(new Code128Reader());
				}
				if (possibleFormats.Contains(BarcodeFormat.ITF))
				{
					readers.Add(new ITFReader());
				}
			}
			if ((readers.Count == 0))
			{
				readers.Add(new MultiFormatUPCEANReader(hints));
				readers.Add(new Code39Reader());
				readers.Add(new Code128Reader());
				readers.Add(new ITFReader());
			}
		}
		
		public override Result decodeRow(int rowNumber, BitArray row, System.Collections.Hashtable hints)
		{
			int size = readers.Count;
			for (int i = 0; i < size; i++)
			{
				OneDReader reader = (OneDReader) readers[i];
				try
				{
					return reader.decodeRow(rowNumber, row, hints);
				}
				catch (ReaderException)
				{
					// continue
				}
			}
			
			throw ReaderException.Instance;
		}
	}
}
