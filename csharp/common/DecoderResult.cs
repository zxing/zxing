using System.Collections.Generic;

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

namespace com.google.zxing.common
{


	/// <summary>
	/// <p>Encapsulates the result of decoding a matrix of bits. This typically
	/// applies to 2D barcode formats. For now it contains the raw bytes obtained,
	/// as well as a String interpretation of those bytes, if applicable.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class DecoderResult
	{

	  private readonly sbyte[] rawBytes;
	  private readonly string text;
	  private readonly IList<sbyte[]> byteSegments;
	  private readonly string ecLevel;

	  public DecoderResult(sbyte[] rawBytes, string text, IList<sbyte[]> byteSegments, string ecLevel)
	  {
		this.rawBytes = rawBytes;
		this.text = text;
		this.byteSegments = byteSegments;
		this.ecLevel = ecLevel;
	  }

	  public sbyte[] RawBytes
	  {
		  get
		  {
			return rawBytes;
		  }
	  }

	  public string Text
	  {
		  get
		  {
			return text;
		  }
	  }

	  public IList<sbyte[]> ByteSegments
	  {
		  get
		  {
			return byteSegments;
		  }
	  }

	  public string ECLevel
	  {
		  get
		  {
			return ecLevel;
		  }
	  }

	}
}