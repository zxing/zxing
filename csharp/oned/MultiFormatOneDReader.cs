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
	using RSS14Reader = com.google.zxing.oned.rss.RSS14Reader;
	using RSSExpandedReader = com.google.zxing.oned.rss.expanded.RSSExpandedReader;


	/// <summary>
	/// @author dswitkin@google.com (Daniel Switkin)
	/// @author Sean Owen
	/// </summary>
	public sealed class MultiFormatOneDReader : OneDReader
	{

	  private readonly OneDReader[] readers;

      public MultiFormatOneDReader(IDictionary<DecodeHintType, object> hints)
	  {
        //ICollection<BarcodeFormat> possibleFormats = hints == null ? null : (ICollection<BarcodeFormat>) hints[DecodeHintType.POSSIBLE_FORMATS];
          ICollection<BarcodeFormat> possibleFormats = null;
          if (hints !=null && hints.ContainsKey(DecodeHintType.POSSIBLE_FORMATS))
          {
              possibleFormats = (ICollection<BarcodeFormat>)hints[DecodeHintType.POSSIBLE_FORMATS];
          }

        //bool useCode39CheckDigit = hints != null && hints[DecodeHintType.ASSUME_CODE_39_CHECK_DIGIT] != null;
		bool useCode39CheckDigit = hints != null && hints.ContainsKey(DecodeHintType.ASSUME_CODE_39_CHECK_DIGIT);
		List<OneDReader> readers = new List<OneDReader>();
		if (possibleFormats != null)
		{
		  if (possibleFormats.Contains(BarcodeFormat.EAN_13) || possibleFormats.Contains(BarcodeFormat.UPC_A) || possibleFormats.Contains(BarcodeFormat.EAN_8) || possibleFormats.Contains(BarcodeFormat.UPC_E))
		  {
			readers.Add(new MultiFormatUPCEANReader(hints));
		  }
		  if (possibleFormats.Contains(BarcodeFormat.CODE_39))
		  {
			readers.Add(new Code39Reader(useCode39CheckDigit));
		  }
		  if (possibleFormats.Contains(BarcodeFormat.CODE_93))
		  {
			readers.Add(new Code93Reader());
		  }
		  if (possibleFormats.Contains(BarcodeFormat.CODE_128))
		  {
			readers.Add(new Code128Reader());
		  }
		  if (possibleFormats.Contains(BarcodeFormat.ITF))
		  {
			 readers.Add(new ITFReader());
		  }
		  if (possibleFormats.Contains(BarcodeFormat.CODABAR))
		  {
			 readers.Add(new CodaBarReader());
		  }
		  if (possibleFormats.Contains(BarcodeFormat.RSS_14))
		  {
			 readers.Add(new RSS14Reader());
		  }
		  if (possibleFormats.Contains(BarcodeFormat.RSS_EXPANDED))
		  {
			readers.Add(new RSSExpandedReader());
		  }
		}
		if (readers.Count == 0)
		{
		  readers.Add(new MultiFormatUPCEANReader(hints));
		  readers.Add(new Code39Reader());
		  readers.Add(new CodaBarReader());
		  readers.Add(new Code93Reader());
		  readers.Add(new Code128Reader());
		  readers.Add(new ITFReader());
		  readers.Add(new RSS14Reader());
		  readers.Add(new RSSExpandedReader());
		}
		this.readers = readers.ToArray(/*new OneDReader[readers.Count]*/);
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decodeRow(int rowNumber, com.google.zxing.common.BitArray row, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException
      public override Result decodeRow(int rowNumber, BitArray row, IDictionary<DecodeHintType, object> hints)
	  {
		foreach (OneDReader reader in readers)
		{
		  try
		  {
			return reader.decodeRow(rowNumber, row, hints);
		  }
		  catch (ReaderException re)
		  {
			// continue
		  }
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