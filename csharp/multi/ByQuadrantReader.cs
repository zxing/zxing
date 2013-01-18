using System.Collections.Generic;

/*
 * Copyright 2009 ZXing authors
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

namespace com.google.zxing.multi
{

	using BinaryBitmap = com.google.zxing.BinaryBitmap;
	using ChecksumException = com.google.zxing.ChecksumException;
	using DecodeHintType = com.google.zxing.DecodeHintType;
	using FormatException = com.google.zxing.FormatException;
	using NotFoundException = com.google.zxing.NotFoundException;
	using Reader = com.google.zxing.Reader;
	using Result = com.google.zxing.Result;


	/// <summary>
	/// This class attempts to decode a barcode from an image, not by scanning the whole image,
	/// but by scanning subsets of the image. This is important when there may be multiple barcodes in
	/// an image, and detecting a barcode may find parts of multiple barcode and fail to decode
	/// (e.g. QR Codes). Instead this scans the four quadrants of the image -- and also the center
	/// 'quadrant' to cover the case where a barcode is found in the center.
	/// </summary>
	/// <seealso cref= GenericMultipleBarcodeReader </seealso>
	public sealed class ByQuadrantReader : com.google.zxing.Reader
	{

	  private readonly Reader @delegate;

	  public ByQuadrantReader(Reader @delegate)
	  {
		this.@delegate = @delegate;
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decode(com.google.zxing.BinaryBitmap image) throws com.google.zxing.NotFoundException, com.google.zxing.ChecksumException, com.google.zxing.FormatException
	  public Result decode(BinaryBitmap image)
	  {
		return decode(image, null);
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decode(com.google.zxing.BinaryBitmap image, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException, com.google.zxing.ChecksumException, com.google.zxing.FormatException
      public Result decode(BinaryBitmap image, IDictionary<DecodeHintType, object> hints)
	  {

		int width = image.Width;
		int height = image.Height;
		int halfWidth = width / 2;
		int halfHeight = height / 2;

		BinaryBitmap topLeft = image.crop(0, 0, halfWidth, halfHeight);
		try
		{
		  return @delegate.decode(topLeft, hints);
		}
		catch (NotFoundException re)
		{
		  // continue
		}

		BinaryBitmap topRight = image.crop(halfWidth, 0, halfWidth, halfHeight);
		try
		{
		  return @delegate.decode(topRight, hints);
		}
		catch (NotFoundException re)
		{
		  // continue
		}

		BinaryBitmap bottomLeft = image.crop(0, halfHeight, halfWidth, halfHeight);
		try
		{
		  return @delegate.decode(bottomLeft, hints);
		}
		catch (NotFoundException re)
		{
		  // continue
		}

		BinaryBitmap bottomRight = image.crop(halfWidth, halfHeight, halfWidth, halfHeight);
		try
		{
		  return @delegate.decode(bottomRight, hints);
		}
		catch (NotFoundException re)
		{
		  // continue
		}

		int quarterWidth = halfWidth / 2;
		int quarterHeight = halfHeight / 2;
		BinaryBitmap center = image.crop(quarterWidth, quarterHeight, halfWidth, halfHeight);
		return @delegate.decode(center, hints);
	  }

	  public void reset()
	  {
		@delegate.reset();
	  }

	}

}