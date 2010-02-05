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
using ByteArray = com.google.zxing.common.ByteArray;
namespace com.google.zxing.qrcode.encoder
{
	
	sealed class BlockPair
	{
		public ByteArray DataBytes
		{
			get
			{
				return dataBytes;
			}
			
		}
		public ByteArray ErrorCorrectionBytes
		{
			get
			{
				return errorCorrectionBytes;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'dataBytes '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private ByteArray dataBytes;
		//UPGRADE_NOTE: Final was removed from the declaration of 'errorCorrectionBytes '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private ByteArray errorCorrectionBytes;
		
		internal BlockPair(ByteArray data, ByteArray errorCorrection)
		{
			dataBytes = data;
			errorCorrectionBytes = errorCorrection;
		}
	}
}