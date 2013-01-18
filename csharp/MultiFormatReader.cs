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

namespace com.google.zxing
{

	using AztecReader = com.google.zxing.aztec.AztecReader;
	using DataMatrixReader = com.google.zxing.datamatrix.DataMatrixReader;
	using MaxiCodeReader = com.google.zxing.maxicode.MaxiCodeReader;
	using MultiFormatOneDReader = com.google.zxing.oned.MultiFormatOneDReader;
	using PDF417Reader = com.google.zxing.pdf417.PDF417Reader;
	using QRCodeReader = com.google.zxing.qrcode.QRCodeReader;


	/// <summary>
	/// MultiFormatReader is a convenience class and the main entry point into the library for most uses.
	/// By default it attempts to decode all barcode formats that the library supports. Optionally, you
	/// can provide a hints object to request different behavior, for example only decoding QR codes.
	/// 
	/// @author Sean Owen
	/// @author dswitkin@google.com (Daniel Switkin)
	/// </summary>
	public sealed class MultiFormatReader : Reader
	{

//JAVA TO C# CONVERTER TODO TASK: Java wildcard generics are not converted to .NET:
//ORIGINAL LINE: private java.util.Map<DecodeHintType,?> hints;
	  private IDictionary<DecodeHintType, object> hints;
	  private Reader[] readers;

	  /// <summary>
	  /// This version of decode honors the intent of Reader.decode(BinaryBitmap) in that it
	  /// passes null as a hint to the decoders. However, that makes it inefficient to call repeatedly.
	  /// Use setHints() followed by decodeWithState() for continuous scan applications.
	  /// </summary>
	  /// <param name="image"> The pixel data to decode </param>
	  /// <returns> The contents of the image </returns>
	  /// <exception cref="NotFoundException"> Any errors which occurred </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public Result decode(BinaryBitmap image) throws NotFoundException
	  public Result decode(BinaryBitmap image)
	  {
		Hints = null;
		return decodeInternal(image);
	  }

	  /// <summary>
	  /// Decode an image using the hints provided. Does not honor existing state.
	  /// </summary>
	  /// <param name="image"> The pixel data to decode </param>
	  /// <param name="hints"> The hints to use, clearing the previous state. </param>
	  /// <returns> The contents of the image </returns>
	  /// <exception cref="NotFoundException"> Any errors which occurred </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public Result decode(BinaryBitmap image, java.util.Map<DecodeHintType,?> hints) throws NotFoundException
      public Result decode(BinaryBitmap image, IDictionary<DecodeHintType, object> hints)
	  {
		Hints = hints;
		return decodeInternal(image);
	  }

	  /// <summary>
	  /// Decode an image using the state set up by calling setHints() previously. Continuous scan
	  /// clients will get a <b>large</b> speed increase by using this instead of decode().
	  /// </summary>
	  /// <param name="image"> The pixel data to decode </param>
	  /// <returns> The contents of the image </returns>
	  /// <exception cref="NotFoundException"> Any errors which occurred </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public Result decodeWithState(BinaryBitmap image) throws NotFoundException
	  public Result decodeWithState(BinaryBitmap image)
	  {
		// Make sure to set up the default state so we don't crash
		if (readers == null)
		{
		  Hints = null;
		}
		return decodeInternal(image);
	  }

	  /// <summary>
	  /// This method adds state to the MultiFormatReader. By setting the hints once, subsequent calls
	  /// to decodeWithState(image) can reuse the same set of readers without reallocating memory. This
	  /// is important for performance in continuous scan clients.
	  /// </summary>
	  /// <param name="hints"> The set of hints to use for subsequent calls to decode(image) </param>
	  public IDictionary<DecodeHintType, object> Hints
	  {
		  set
		  {
			this.hints = value;
    
			bool tryHarder = value != null && value.ContainsKey(DecodeHintType.TRY_HARDER);

            //ICollection<BarcodeFormat> formats = value == null ? null : (ICollection<BarcodeFormat>) value[DecodeHintType.POSSIBLE_FORMATS];
		    ICollection<BarcodeFormat> formats = null;
            if (value  != null && value.ContainsKey(DecodeHintType.POSSIBLE_FORMATS))
		    {
                formats = (ICollection<BarcodeFormat>)value[DecodeHintType.POSSIBLE_FORMATS];
		    }
			List<Reader> readers = new List<Reader>();
			if (formats != null)
			{
			  bool addOneDReader = formats.Contains(BarcodeFormat.UPC_A) || formats.Contains(BarcodeFormat.UPC_E) || formats.Contains(BarcodeFormat.EAN_13) || formats.Contains(BarcodeFormat.EAN_8) || formats.Contains(BarcodeFormat.CODABAR) || formats.Contains(BarcodeFormat.CODE_39) || formats.Contains(BarcodeFormat.CODE_93) || formats.Contains(BarcodeFormat.CODE_128) || formats.Contains(BarcodeFormat.ITF) || formats.Contains(BarcodeFormat.RSS_14) || formats.Contains(BarcodeFormat.RSS_EXPANDED);
			  // Put 1D readers upfront in "normal" mode
			  if (addOneDReader && !tryHarder)
			  {
				readers.Add(new MultiFormatOneDReader(value));
			  }
			  if (formats.Contains(BarcodeFormat.QR_CODE))
			  {
				readers.Add(new QRCodeReader());
			  }
			  if (formats.Contains(BarcodeFormat.DATA_MATRIX))
			  {
				readers.Add(new DataMatrixReader());
			  }
			  if (formats.Contains(BarcodeFormat.AZTEC))
			  {
				readers.Add(new AztecReader());
			  }
			  if (formats.Contains(BarcodeFormat.PDF_417))
			  {
				 readers.Add(new PDF417Reader());
			  }
			  if (formats.Contains(BarcodeFormat.MAXICODE))
			  {
				 readers.Add(new MaxiCodeReader());
			  }
			  // At end in "try harder" mode
			  if (addOneDReader && tryHarder)
			  {
				readers.Add(new MultiFormatOneDReader(value));
			  }
			}
			if (readers.Count == 0)
			{
			  if (!tryHarder)
			  {
				readers.Add(new MultiFormatOneDReader(value));
			  }
    
			  readers.Add(new QRCodeReader());
			  readers.Add(new DataMatrixReader());
			  readers.Add(new AztecReader());
			  readers.Add(new PDF417Reader());
			  readers.Add(new MaxiCodeReader());
    
			  if (tryHarder)
			  {
				readers.Add(new MultiFormatOneDReader(value));
			  }
			}
			this.readers = readers.ToArray(/*new Reader[readers.Count]*/);
		  }
	  }

	  public void reset()
	  {
		if (readers != null)
		{
		  foreach (Reader reader in readers)
		  {
			reader.reset();
		  }
		}
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private Result decodeInternal(BinaryBitmap image) throws NotFoundException
	  private Result decodeInternal(BinaryBitmap image)
	  {
		if (readers != null)
		{
		  foreach (Reader reader in readers)
		  {
			try
			{
			  return reader.decode(image, hints);
			}
			catch (ReaderException re)
			{
			  // continue
			}
		  }
		}
		throw NotFoundException.NotFoundInstance;
	  }

	}

}