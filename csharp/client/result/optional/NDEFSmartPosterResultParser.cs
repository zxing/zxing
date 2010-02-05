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
using Result = com.google.zxing.Result;
namespace com.google.zxing.client.result.optional
{
	
	/// <summary> <p>Recognizes an NDEF message that encodes information according to the
	/// "Smart Poster Record Type Definition" specification.</p>
	/// 
	/// <p>This actually only supports some parts of the Smart Poster format: title,
	/// URI, and action records. Icon records are not supported because the size
	/// of these records are infeasibly large for barcodes. Size and type records
	/// are not supported. Multiple titles are not supported.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class NDEFSmartPosterResultParser:AbstractNDEFResultParser
	{
		
		public static NDEFSmartPosterParsedResult parse(Result result)
		{
			sbyte[] bytes = result.RawBytes;
			if (bytes == null)
			{
				return null;
			}
			NDEFRecord headerRecord = NDEFRecord.readRecord(bytes, 0);
			// Yes, header record starts and ends a message
			if (headerRecord == null || !headerRecord.MessageBegin || !headerRecord.MessageEnd)
			{
				return null;
			}
			if (!headerRecord.Type.Equals(NDEFRecord.SMART_POSTER_WELL_KNOWN_TYPE))
			{
				return null;
			}
			
			int offset = 0;
			int recordNumber = 0;
			NDEFRecord ndefRecord = null;
			sbyte[] payload = headerRecord.Payload;
			int action = NDEFSmartPosterParsedResult.ACTION_UNSPECIFIED;
			System.String title = null;
			System.String uri = null;
			
			while (offset < payload.Length && (ndefRecord = NDEFRecord.readRecord(payload, offset)) != null)
			{
				if (recordNumber == 0 && !ndefRecord.MessageBegin)
				{
					return null;
				}
				
				System.String type = ndefRecord.Type;
				if (NDEFRecord.TEXT_WELL_KNOWN_TYPE.Equals(type))
				{
					System.String[] languageText = NDEFTextResultParser.decodeTextPayload(ndefRecord.Payload);
					title = languageText[1];
				}
				else if (NDEFRecord.URI_WELL_KNOWN_TYPE.Equals(type))
				{
					uri = NDEFURIResultParser.decodeURIPayload(ndefRecord.Payload);
				}
				else if (NDEFRecord.ACTION_WELL_KNOWN_TYPE.Equals(type))
				{
					action = ndefRecord.Payload[0];
				}
				recordNumber++;
				offset += ndefRecord.TotalRecordLength;
			}
			
			if (recordNumber == 0 || (ndefRecord != null && !ndefRecord.MessageEnd))
			{
				return null;
			}
			
			return new NDEFSmartPosterParsedResult(action, uri, title);
		}
	}
}