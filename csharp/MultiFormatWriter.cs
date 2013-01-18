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

namespace com.google.zxing
{

	using BitMatrix = com.google.zxing.common.BitMatrix;
	using CodaBarWriter = com.google.zxing.oned.CodaBarWriter;
	using Code128Writer = com.google.zxing.oned.Code128Writer;
	using Code39Writer = com.google.zxing.oned.Code39Writer;
	using EAN13Writer = com.google.zxing.oned.EAN13Writer;
	using EAN8Writer = com.google.zxing.oned.EAN8Writer;
	using ITFWriter = com.google.zxing.oned.ITFWriter;
	using UPCAWriter = com.google.zxing.oned.UPCAWriter;
	using PDF417Writer = com.google.zxing.pdf417.encoder.PDF417Writer;
	using QRCodeWriter = com.google.zxing.qrcode.QRCodeWriter;


	/// <summary>
	/// This is a factory class which finds the appropriate Writer subclass for the BarcodeFormat
	/// requested and encodes the barcode with the supplied contents.
	/// 
	/// @author dswitkin@google.com (Daniel Switkin)
	/// </summary>
	public sealed class MultiFormatWriter : Writer
	{

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.BitMatrix encode(String contents, BarcodeFormat format, int width, int height) throws WriterException
	  public BitMatrix encode(string contents, BarcodeFormat format, int width, int height)
	  {
		return encode(contents, format, width, height, null);
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.BitMatrix encode(String contents, BarcodeFormat format, int width, int height, java.util.Map<EncodeHintType,?> hints) throws WriterException
      public BitMatrix encode(string contents, BarcodeFormat format, int width, int height, IDictionary<EncodeHintType, object> hints)
	  {

		Writer writer;
		switch (format)
		{
		  case com.google.zxing.BarcodeFormat.EAN_8:
			writer = new EAN8Writer();
			break;
		  case com.google.zxing.BarcodeFormat.EAN_13:
			writer = new EAN13Writer();
			break;
		  case com.google.zxing.BarcodeFormat.UPC_A:
			writer = new UPCAWriter();
			break;
		  case com.google.zxing.BarcodeFormat.QR_CODE:
			writer = new QRCodeWriter();
			break;
		  case com.google.zxing.BarcodeFormat.CODE_39:
			writer = new Code39Writer();
			break;
		  case com.google.zxing.BarcodeFormat.CODE_128:
			writer = new Code128Writer();
			break;
		  case com.google.zxing.BarcodeFormat.ITF:
			writer = new ITFWriter();
			break;
		  case com.google.zxing.BarcodeFormat.PDF_417:
			writer = new PDF417Writer();
			break;
		  case com.google.zxing.BarcodeFormat.CODABAR:
			writer = new CodaBarWriter();
			break;
		  default:
			throw new System.ArgumentException("No encoder available for format " + format);
		}
		return writer.encode(contents, format, width, height, hints);
	  }

	}

}