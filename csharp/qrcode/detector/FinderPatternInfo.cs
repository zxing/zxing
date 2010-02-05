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
namespace com.google.zxing.qrcode.detector
{
	
	/// <summary> <p>Encapsulates information about finder patterns in an image, including the location of
	/// the three finder patterns, and their estimated module size.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class FinderPatternInfo
	{
		public FinderPattern BottomLeft
		{
			get
			{
				return bottomLeft;
			}
			
		}
		public FinderPattern TopLeft
		{
			get
			{
				return topLeft;
			}
			
		}
		public FinderPattern TopRight
		{
			get
			{
				return topRight;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'bottomLeft '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private FinderPattern bottomLeft;
		//UPGRADE_NOTE: Final was removed from the declaration of 'topLeft '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private FinderPattern topLeft;
		//UPGRADE_NOTE: Final was removed from the declaration of 'topRight '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private FinderPattern topRight;
		
		public FinderPatternInfo(FinderPattern[] patternCenters)
		{
			this.bottomLeft = patternCenters[0];
			this.topLeft = patternCenters[1];
			this.topRight = patternCenters[2];
		}
	}
}