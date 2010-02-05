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
namespace com.google.zxing.qrcode.detector
{
	
	/// <summary> <p>Encapsulates an alignment pattern, which are the smaller square patterns found in
	/// all but the simplest QR Codes.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class AlignmentPattern:ResultPoint
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'estimatedModuleSize '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private float estimatedModuleSize;
		
		internal AlignmentPattern(float posX, float posY, float estimatedModuleSize):base(posX, posY)
		{
			this.estimatedModuleSize = estimatedModuleSize;
		}
		
		/// <summary> <p>Determines if this alignment pattern "about equals" an alignment pattern at the stated
		/// position and size -- meaning, it is at nearly the same center with nearly the same size.</p>
		/// </summary>
		internal bool aboutEquals(float moduleSize, float i, float j)
		{
			if (System.Math.Abs(i - Y) <= moduleSize && System.Math.Abs(j - X) <= moduleSize)
			{
				float moduleSizeDiff = System.Math.Abs(moduleSize - estimatedModuleSize);
				return moduleSizeDiff <= 1.0f || moduleSizeDiff / estimatedModuleSize <= 1.0f;
			}
			return false;
		}
	}
}