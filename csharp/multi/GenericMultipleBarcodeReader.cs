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
	using DecodeHintType = com.google.zxing.DecodeHintType;
	using NotFoundException = com.google.zxing.NotFoundException;
	using Reader = com.google.zxing.Reader;
	using ReaderException = com.google.zxing.ReaderException;
	using Result = com.google.zxing.Result;
	using ResultPoint = com.google.zxing.ResultPoint;


	/// <summary>
	/// <p>Attempts to locate multiple barcodes in an image by repeatedly decoding portion of the image.
	/// After one barcode is found, the areas left, above, right and below the barcode's
	/// <seealso cref="ResultPoint"/>s are scanned, recursively.</p>
	/// 
	/// <p>A caller may want to also employ <seealso cref="ByQuadrantReader"/> when attempting to find multiple
	/// 2D barcodes, like QR Codes, in an image, where the presence of multiple barcodes might prevent
	/// detecting any one of them.</p>
	/// 
	/// <p>That is, instead of passing a <seealso cref="Reader"/> a caller might pass
	/// {@code new ByQuadrantReader(reader)}.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class GenericMultipleBarcodeReader : MultipleBarcodeReader
	{

	  private const int MIN_DIMENSION_TO_RECUR = 100;

	  private readonly Reader @delegate;

	  public GenericMultipleBarcodeReader(Reader @delegate)
	  {
		this.@delegate = @delegate;
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result[] decodeMultiple(com.google.zxing.BinaryBitmap image) throws com.google.zxing.NotFoundException
	  public Result[] decodeMultiple(BinaryBitmap image)
	  {
		return decodeMultiple(image, null);
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result[] decodeMultiple(com.google.zxing.BinaryBitmap image, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException
      public Result[] decodeMultiple(BinaryBitmap image, IDictionary<DecodeHintType, object> hints)
	  {
		List<Result> results = new List<Result>();
		doDecodeMultiple(image, hints, results, 0, 0);
		if (results.Count == 0)
		{
		  throw NotFoundException.NotFoundInstance;
		}
		return results.ToArray();
	  }

      private void doDecodeMultiple(BinaryBitmap image, IDictionary<DecodeHintType, object> hints, IList<Result> results, int xOffset, int yOffset)
	  {
		Result result;
		try
		{
		  result = @delegate.decode(image, hints);
		}
		catch (ReaderException re)
		{
		  return;
		}
		bool alreadyFound = false;
		foreach (Result existingResult in results)
		{
		  if (existingResult.Text.Equals(result.Text))
		  {
			alreadyFound = true;
			break;
		  }
		}
		if (!alreadyFound)
		{
		  results.Add(translateResultPoints(result, xOffset, yOffset));
		}
		ResultPoint[] resultPoints = result.ResultPoints;
		if (resultPoints == null || resultPoints.Length == 0)
		{
		  return;
		}
		int width = image.Width;
		int height = image.Height;
		float minX = width;
		float minY = height;
		float maxX = 0.0f;
		float maxY = 0.0f;
		foreach (ResultPoint point in resultPoints)
		{
		  float x = point.X;
		  float y = point.Y;
		  if (x < minX)
		  {
			minX = x;
		  }
		  if (y < minY)
		  {
			minY = y;
		  }
		  if (x > maxX)
		  {
			maxX = x;
		  }
		  if (y > maxY)
		  {
			maxY = y;
		  }
		}

		// Decode left of barcode
		if (minX > MIN_DIMENSION_TO_RECUR)
		{
		  doDecodeMultiple(image.crop(0, 0, (int) minX, height), hints, results, xOffset, yOffset);
		}
		// Decode above barcode
		if (minY > MIN_DIMENSION_TO_RECUR)
		{
		  doDecodeMultiple(image.crop(0, 0, width, (int) minY), hints, results, xOffset, yOffset);
		}
		// Decode right of barcode
		if (maxX < width - MIN_DIMENSION_TO_RECUR)
		{
		  doDecodeMultiple(image.crop((int) maxX, 0, width - (int) maxX, height), hints, results, xOffset + (int) maxX, yOffset);
		}
		// Decode below barcode
		if (maxY < height - MIN_DIMENSION_TO_RECUR)
		{
		  doDecodeMultiple(image.crop(0, (int) maxY, width, height - (int) maxY), hints, results, xOffset, yOffset + (int) maxY);
		}
	  }

	  private static Result translateResultPoints(Result result, int xOffset, int yOffset)
	  {
		ResultPoint[] oldResultPoints = result.ResultPoints;
		if (oldResultPoints == null)
		{
		  return result;
		}
		ResultPoint[] newResultPoints = new ResultPoint[oldResultPoints.Length];
		for (int i = 0; i < oldResultPoints.Length; i++)
		{
		  ResultPoint oldPoint = oldResultPoints[i];
		  newResultPoints[i] = new ResultPoint(oldPoint.X + xOffset, oldPoint.Y + yOffset);
		}
		return new Result(result.Text, result.RawBytes, newResultPoints, result.BarcodeFormat);
	  }

	}

}