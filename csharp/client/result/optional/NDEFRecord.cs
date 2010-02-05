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
namespace com.google.zxing.client.result.optional
{
	
	/// <summary> <p>Represents a record in an NDEF message. This class only supports certain types
	/// of records -- namely, non-chunked records, where ID length is omitted, and only
	/// "short records".</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class NDEFRecord
	{
		internal bool MessageBegin
		{
			get
			{
				return (header & 0x80) != 0;
			}
			
		}
		internal bool MessageEnd
		{
			get
			{
				return (header & 0x40) != 0;
			}
			
		}
		internal System.String Type
		{
			get
			{
				return type;
			}
			
		}
		internal sbyte[] Payload
		{
			get
			{
				return payload;
			}
			
		}
		internal int TotalRecordLength
		{
			get
			{
				return totalRecordLength;
			}
			
		}
		
		private const int SUPPORTED_HEADER_MASK = 0x3F; // 0 0 1 1 1 111 (the bottom 6 bits matter)
		private const int SUPPORTED_HEADER = 0x11; // 0 0 0 1 0 001
		
		public const System.String TEXT_WELL_KNOWN_TYPE = "T";
		public const System.String URI_WELL_KNOWN_TYPE = "U";
		public const System.String SMART_POSTER_WELL_KNOWN_TYPE = "Sp";
		public const System.String ACTION_WELL_KNOWN_TYPE = "act";
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'header '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int header;
		//UPGRADE_NOTE: Final was removed from the declaration of 'type '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String type;
		//UPGRADE_NOTE: Final was removed from the declaration of 'payload '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private sbyte[] payload;
		//UPGRADE_NOTE: Final was removed from the declaration of 'totalRecordLength '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int totalRecordLength;
		
		private NDEFRecord(int header, System.String type, sbyte[] payload, int totalRecordLength)
		{
			this.header = header;
			this.type = type;
			this.payload = payload;
			this.totalRecordLength = totalRecordLength;
		}
		
		internal static NDEFRecord readRecord(sbyte[] bytes, int offset)
		{
			int header = bytes[offset] & 0xFF;
			// Does header match what we support in the bits we care about?
			// XOR figures out where we differ, and if any of those are in the mask, fail
			if (((header ^ SUPPORTED_HEADER) & SUPPORTED_HEADER_MASK) != 0)
			{
				return null;
			}
			int typeLength = bytes[offset + 1] & 0xFF;
			
			int payloadLength = bytes[offset + 2] & 0xFF;
			
			System.String type = AbstractNDEFResultParser.bytesToString(bytes, offset + 3, typeLength, "US-ASCII");
			
			sbyte[] payload = new sbyte[payloadLength];
			Array.Copy(bytes, offset + 3 + typeLength, payload, 0, payloadLength);
			
			return new NDEFRecord(header, type, payload, 3 + typeLength + payloadLength);
		}
	}
}