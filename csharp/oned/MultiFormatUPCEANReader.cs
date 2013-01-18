using System.Collections;
using System.Collections.Generic;

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
	using DecodeHintType = com.google.zxing.DecodeHintType;
	using NotFoundException = com.google.zxing.NotFoundException;
	using Reader = com.google.zxing.Reader;
	using ReaderException = com.google.zxing.ReaderException;
	using Result = com.google.zxing.Result;
	using BitArray = com.google.zxing.common.BitArray;


	/// <summary>
	/// <p>A reader that can read all available UPC/EAN formats. If a caller wants to try to
	/// read all such formats, it is most efficient to use this implementation rather than invoke
	/// individual readers.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class MultiFormatUPCEANReader : OneDReader
	{

	  private readonly UPCEANReader[] readers;

      public MultiFormatUPCEANReader(IDictionary<DecodeHintType, object> hints)
	  {
          //ICollection<BarcodeFormat> possibleFormats = hints == null ? null : (ICollection<BarcodeFormat>) hints[DecodeHintType.POSSIBLE_FORMATS];
          ICollection<BarcodeFormat> possibleFormats = null;
          if (hints != null && hints.ContainsKey(DecodeHintType.POSSIBLE_FORMATS))
          {
              possibleFormats = (ICollection<BarcodeFormat>)hints[DecodeHintType.POSSIBLE_FORMATS];
          }
          List<UPCEANReader> readers = new List<UPCEANReader>();
		if (possibleFormats != null)
		{
		  if (possibleFormats.Contains(BarcodeFormat.EAN_13))
		  {
			readers.Add(new EAN13Reader());
		  }
		  else if (possibleFormats.Contains(BarcodeFormat.UPC_A))
		  {
			readers.Add(new UPCAReader());
		  }
		  if (possibleFormats.Contains(BarcodeFormat.EAN_8))
		  {
			readers.Add(new EAN8Reader());
		  }
		  if (possibleFormats.Contains(BarcodeFormat.UPC_E))
		  {
			readers.Add(new UPCEReader());
		  }
		}
		if (readers.Count == 0)
		{
		  readers.Add(new EAN13Reader());
		  // UPC-A is covered by EAN-13
		  readers.Add(new EAN8Reader());
		  readers.Add(new UPCEReader());
		}
		this.readers = readers.ToArray(/*new UPCEANReader[readers.Count]*/);
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decodeRow(int rowNumber, com.google.zxing.common.BitArray row, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException
      public override Result decodeRow(int rowNumber, BitArray row, IDictionary<DecodeHintType, object> hints)
	  {
		// Compute this location once and reuse it on multiple implementations
		int[] startGuardPattern = UPCEANReader.findStartGuardPattern(row);
		foreach (UPCEANReader reader in readers)
		{
		  Result result;
		  try
		  {
			result = reader.decodeRow(rowNumber, row, startGuardPattern, hints);
		  }
		  catch (ReaderException re)
		  {
			continue;
		  }
		  // Special case: a 12-digit code encoded in UPC-A is identical to a "0"
		  // followed by those 12 digits encoded as EAN-13. Each will recognize such a code,
		  // UPC-A as a 12-digit string and EAN-13 as a 13-digit string starting with "0".
		  // Individually these are correct and their readers will both read such a code
		  // and correctly call it EAN-13, or UPC-A, respectively.
		  //
		  // In this case, if we've been looking for both types, we'd like to call it
		  // a UPC-A code. But for efficiency we only run the EAN-13 decoder to also read
		  // UPC-A. So we special case it here, and convert an EAN-13 result to a UPC-A
		  // result if appropriate.
		  //
		  // But, don't return UPC-A if UPC-A was not a requested format!
		  bool ean13MayBeUPCA = result.BarcodeFormat == BarcodeFormat.EAN_13 && result.Text[0] == '0';
          //ICollection<BarcodeFormat> possibleFormats = hints == null ? null : (ICollection<BarcodeFormat>) hints[DecodeHintType.POSSIBLE_FORMATS];
          ICollection<BarcodeFormat> possibleFormats = null;
          if (hints != null && hints.ContainsKey(DecodeHintType.POSSIBLE_FORMATS))
          {
              possibleFormats = (ICollection<BarcodeFormat>)hints[DecodeHintType.POSSIBLE_FORMATS];
          }

		  bool canReturnUPCA = possibleFormats == null || possibleFormats.Contains(BarcodeFormat.UPC_A);

		  if (ean13MayBeUPCA && canReturnUPCA)
		  {
			// Transfer the metdata across
			Result resultUPCA = new Result(result.Text.Substring(1), result.RawBytes, result.ResultPoints, BarcodeFormat.UPC_A);
			resultUPCA.putAllMetadata(result.ResultMetadata);
			return resultUPCA;
		  }
		  return result;
		}

		throw NotFoundException.NotFoundInstance;
	  }

	  public override void reset()
	  {
		foreach (Reader reader in readers)
		{
		  reader.reset();
		}
	  }

	}

}