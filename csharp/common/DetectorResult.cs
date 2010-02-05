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
using ResultPoint = com.google.zxing.ResultPoint;
namespace com.google.zxing.common
{
	
	/// <summary> <p>Encapsulates the result of detecting a barcode in an image. This includes the raw
	/// matrix of black/white pixels corresponding to the barcode, and possibly points of interest
	/// in the image, like the location of finder patterns or corners of the barcode in the image.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class DetectorResult
	{
		public BitMatrix Bits
		{
			get
			{
				return bits;
			}
			
		}
		public ResultPoint[] Points
		{
			get
			{
				return points;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'bits '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private BitMatrix bits;
		//UPGRADE_NOTE: Final was removed from the declaration of 'points '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private ResultPoint[] points;
		
		public DetectorResult(BitMatrix bits, ResultPoint[] points)
		{
			this.bits = bits;
			this.points = points;
		}
	}
}