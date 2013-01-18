using System.Collections;
using System.Collections.Generic;
using System.Text;

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

namespace com.google.zxing.oned
{

	using BarcodeFormat = com.google.zxing.BarcodeFormat;
	using BinaryBitmap = com.google.zxing.BinaryBitmap;
	using ChecksumException = com.google.zxing.ChecksumException;
	using DecodeHintType = com.google.zxing.DecodeHintType;
	using FormatException = com.google.zxing.FormatException;
	using NotFoundException = com.google.zxing.NotFoundException;
	using Result = com.google.zxing.Result;
	using BitArray = com.google.zxing.common.BitArray;


	/// <summary>
	/// <p>Implements decoding of the UPC-A format.</p>
	/// 
	/// @author dswitkin@google.com (Daniel Switkin)
	/// @author Sean Owen
	/// </summary>
	public sealed class UPCAReader : UPCEANReader
	{

	  private readonly UPCEANReader ean13Reader = new EAN13Reader();

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decodeRow(int rowNumber, com.google.zxing.common.BitArray row, int[] startGuardRange, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException, com.google.zxing.FormatException, com.google.zxing.ChecksumException
      public override Result decodeRow(int rowNumber, BitArray row, int[] startGuardRange, IDictionary<DecodeHintType, object> hints)
	  {
		return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, startGuardRange, hints));
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decodeRow(int rowNumber, com.google.zxing.common.BitArray row, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException, com.google.zxing.FormatException, com.google.zxing.ChecksumException
      public override Result decodeRow(int rowNumber, BitArray row, IDictionary<DecodeHintType, object> hints)
	  {
		return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, hints));
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decode(com.google.zxing.BinaryBitmap image) throws com.google.zxing.NotFoundException, com.google.zxing.FormatException
	  public override Result decode(BinaryBitmap image)
	  {
		return maybeReturnResult(ean13Reader.decode(image));
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decode(com.google.zxing.BinaryBitmap image, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException, com.google.zxing.FormatException
      public override Result decode(BinaryBitmap image, IDictionary<DecodeHintType, object> hints)
	  {
		return maybeReturnResult(ean13Reader.decode(image, hints));
	  }

	  internal override BarcodeFormat BarcodeFormat
	  {
		  get
		  {
			return BarcodeFormat.UPC_A;
		  }
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: protected int decodeMiddle(com.google.zxing.common.BitArray row, int[] startRange, StringBuilder resultString) throws com.google.zxing.NotFoundException
	  protected internal override int decodeMiddle(BitArray row, int[] startRange, StringBuilder resultString)
	  {
		return ean13Reader.decodeMiddle(row, startRange, resultString);
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static com.google.zxing.Result maybeReturnResult(com.google.zxing.Result result) throws com.google.zxing.FormatException
	  private static Result maybeReturnResult(Result result)
	  {
		string text = result.Text;
		if (text[0] == '0')
		{
		  return new Result(text.Substring(1), null, result.ResultPoints, BarcodeFormat.UPC_A);
		}
		else
		{
		  throw FormatException.FormatInstance;
		}
	  }

	}

}