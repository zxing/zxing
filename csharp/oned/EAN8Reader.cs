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
using ReaderException = com.google.zxing.ReaderException;
using BitArray = com.google.zxing.common.BitArray;
namespace com.google.zxing.oned
{
	
	/// <summary> <p>Implements decoding of the EAN-8 format.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class EAN8Reader:UPCEANReader
	{
		override internal BarcodeFormat BarcodeFormat
		{
			get
			{
				return BarcodeFormat.EAN_8;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'decodeMiddleCounters '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int[] decodeMiddleCounters;
		
		public EAN8Reader()
		{
			decodeMiddleCounters = new int[4];
		}
		
		protected internal override int decodeMiddle(BitArray row, int[] startRange, System.Text.StringBuilder result)
		{
			int[] counters = decodeMiddleCounters;
			counters[0] = 0;
			counters[1] = 0;
			counters[2] = 0;
			counters[3] = 0;
			int end = row.Size;
			int rowOffset = startRange[1];
			
			for (int x = 0; x < 4 && rowOffset < end; x++)
			{
				int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
				result.Append((char) ('0' + bestMatch));
				for (int i = 0; i < counters.Length; i++)
				{
					rowOffset += counters[i];
				}
			}
			
			int[] middleRange = findGuardPattern(row, rowOffset, true, MIDDLE_PATTERN);
			rowOffset = middleRange[1];
			
			for (int x = 0; x < 4 && rowOffset < end; x++)
			{
				int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
				result.Append((char) ('0' + bestMatch));
				for (int i = 0; i < counters.Length; i++)
				{
					rowOffset += counters[i];
				}
			}
			
			return rowOffset;
		}
	}
}