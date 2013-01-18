using System;
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

namespace com.google.zxing.qrcode
{

	using BarcodeFormat = com.google.zxing.BarcodeFormat;
	using EncodeHintType = com.google.zxing.EncodeHintType;
	using Writer = com.google.zxing.Writer;
	using WriterException = com.google.zxing.WriterException;
	using BitMatrix = com.google.zxing.common.BitMatrix;
	using ByteMatrix = com.google.zxing.qrcode.encoder.ByteMatrix;
	using ErrorCorrectionLevel = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
	using Encoder = com.google.zxing.qrcode.encoder.Encoder;
	using QRCode = com.google.zxing.qrcode.encoder.QRCode;


	/// <summary>
	/// This object renders a QR Code as a BitMatrix 2D array of greyscale values.
	/// 
	/// @author dswitkin@google.com (Daniel Switkin)
	/// </summary>
	public sealed class QRCodeWriter : com.google.zxing.Writer
	{

	  private const int QUIET_ZONE_SIZE = 4;

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.BitMatrix encode(String contents, com.google.zxing.BarcodeFormat format, int width, int height) throws com.google.zxing.WriterException
	  public BitMatrix encode(string contents, BarcodeFormat format, int width, int height)
	  {

		return encode(contents, format, width, height, null);
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.BitMatrix encode(String contents, com.google.zxing.BarcodeFormat format, int width, int height, java.util.Map<com.google.zxing.EncodeHintType,?> hints) throws com.google.zxing.WriterException
      public BitMatrix encode(string contents, BarcodeFormat format, int width, int height, IDictionary<EncodeHintType, object> hints)
	  {

		if (contents.Length == 0)
		{
		  throw new System.ArgumentException("Found empty contents");
		}

		if (format != BarcodeFormat.QR_CODE)
		{
		  throw new System.ArgumentException("Can only encode QR_CODE, but got " + format);
		}

		if (width < 0 || height < 0)
		{
		  throw new System.ArgumentException("Requested dimensions are too small: " + width + 'x' + height);
		}

		ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
		int quietZone = QUIET_ZONE_SIZE;
		if (hints != null)
		{
            //ErrorCorrectionLevel requestedECLevel = (ErrorCorrectionLevel) hints[EncodeHintType.ERROR_CORRECTION];
		    ErrorCorrectionLevel requestedECLevel = null;
            if (hints.ContainsKey(EncodeHintType.ERROR_CORRECTION))
            {
                requestedECLevel = (ErrorCorrectionLevel)hints[EncodeHintType.ERROR_CORRECTION];
            }
		    if (requestedECLevel != null)
		    {
			    errorCorrectionLevel = requestedECLevel;
		    }

            //int? quietZoneInt = (int?) hints[EncodeHintType.MARGIN];
		    int? quietZoneInt = null;
            if (hints.ContainsKey(EncodeHintType.MARGIN))
            {
                quietZoneInt = (int?) hints[EncodeHintType.MARGIN];
            }

		    if (quietZoneInt != null)
		    {
			    quietZone = (int)quietZoneInt;
		    }
		}

		QRCode code = Encoder.encode(contents, errorCorrectionLevel, hints);
		return renderResult(code, width, height, quietZone);
	  }

	  // Note that the input matrix uses 0 == white, 1 == black, while the output matrix uses
	  // 0 == black, 255 == white (i.e. an 8 bit greyscale bitmap).
	  private static BitMatrix renderResult(QRCode code, int width, int height, int quietZone)
	  {
		ByteMatrix input = code.Matrix;
		if (input == null)
		{
		  throw new InvalidOperationException();
		}
		int inputWidth = input.Width;
		int inputHeight = input.Height;
		int qrWidth = inputWidth + (quietZone << 1);
		int qrHeight = inputHeight + (quietZone << 1);
		int outputWidth = Math.Max(width, qrWidth);
		int outputHeight = Math.Max(height, qrHeight);

		int multiple = Math.Min(outputWidth / qrWidth, outputHeight / qrHeight);
		// Padding includes both the quiet zone and the extra white pixels to accommodate the requested
		// dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
		// If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
		// handle all the padding from 100x100 (the actual QR) up to 200x160.
		int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
		int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

		BitMatrix output = new BitMatrix(outputWidth, outputHeight);

		for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple)
		{
		  // Write the contents of this row of the barcode
		  for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple)
		  {
			if (input.get(inputX, inputY) == 1)
			{
			  output.setRegion(outputX, outputY, multiple, multiple);
			}
		  }
		}

		return output;
	  }

	}

}