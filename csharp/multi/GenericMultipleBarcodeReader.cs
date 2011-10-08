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
using System;
using Reader = com.google.zxing.Reader;
using Result = com.google.zxing.Result;
using BinaryBitmap = com.google.zxing.BinaryBitmap;
using ReaderException = com.google.zxing.ReaderException;
using ResultPoint = com.google.zxing.ResultPoint;
namespace com.google.zxing.multi
{
	
	/// <summary> <p>Attempts to locate multiple barcodes in an image by repeatedly decoding portion of the image.
	/// After one barcode is found, the areas left, above, right and below the barcode's
	/// {@link com.google.zxing.ResultPoint}s are scanned, recursively.</p>
	/// 
	/// <p>A caller may want to also employ {@link ByQuadrantReader} when attempting to find multiple
	/// 2D barcodes, like QR Codes, in an image, where the presence of multiple barcodes might prevent
	/// detecting any one of them.</p>
	/// 
	/// <p>That is, instead of passing a {@link Reader} a caller might pass
	/// <code>new ByQuadrantReader(reader)</code>.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>

	public sealed class GenericMultipleBarcodeReader : MultipleBarcodeReader
	{
		
		private const int MIN_DIMENSION_TO_RECUR = 30;
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'delegate '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private Reader delegate_Renamed;
		
		public GenericMultipleBarcodeReader(Reader delegate_Renamed)
		{
			this.delegate_Renamed = delegate_Renamed;
		}
		
		public Result[] decodeMultiple(BinaryBitmap image)
		{
			return decodeMultiple(image, null);
		}
		
		public Result[] decodeMultiple(BinaryBitmap image, System.Collections.Hashtable hints)
		{
			System.Collections.ArrayList results = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(10));
			doDecodeMultiple(image, hints, results, 0, 0);
			if ((results.Count == 0))
			{
				throw ReaderException.Instance;
			}
			int numResults = results.Count;
			Result[] resultArray = new Result[numResults];
			for (int i = 0; i < numResults; i++)
			{
				resultArray[i] = (Result) results[i];
			}
			return resultArray;
		}
		
		private void  doDecodeMultiple(BinaryBitmap image, System.Collections.Hashtable hints, System.Collections.ArrayList results, int xOffset, int yOffset)
		{
			Result result;
			try
			{
				result = delegate_Renamed.decode(image, hints);
			}
			catch (ReaderException)
			{
				return ;
			}
			bool alreadyFound = false;
			for (int i = 0; i < results.Count; i++)
			{
				Result existingResult = (Result) results[i];
				if (existingResult.Text.Equals(result.Text))
				{
					alreadyFound = true;
					break;
				}
			}
			if (alreadyFound)
			{
				return ;
			}
			results.Add(translateResultPoints(result, xOffset, yOffset));
			ResultPoint[] resultPoints = result.ResultPoints;
			if (resultPoints == null || resultPoints.Length == 0)
			{
				return ;
			}
			int width = image.Width;
			int height = image.Height;
			float minX = width;
			float minY = height;
			float maxX = 0.0f;
			float maxY = 0.0f;
			for (int i = 0; i < resultPoints.Length; i++)
			{
				ResultPoint point = resultPoints[i];
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
				//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
				doDecodeMultiple(image.crop(0, 0, (int) minX, height), hints, results, xOffset, yOffset);
			}
			// Decode above barcode
			if (minY > MIN_DIMENSION_TO_RECUR)
			{
				//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
				doDecodeMultiple(image.crop(0, 0, width, (int) minY), hints, results, xOffset, yOffset);
			}
			// Decode right of barcode
			if (maxX < width - MIN_DIMENSION_TO_RECUR)
			{
				//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
				doDecodeMultiple(image.crop((int) maxX, 0, width - (int) maxX, height), hints, results, xOffset + (int) maxX, yOffset);
			}
			// Decode below barcode
			if (maxY < height - MIN_DIMENSION_TO_RECUR)
			{
				//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
				doDecodeMultiple(image.crop(0, (int) maxY, width, height - (int) maxY), hints, results, xOffset, yOffset + (int) maxY);
			}
		}
		
		private static Result translateResultPoints(Result result, int xOffset, int yOffset)
		{
			ResultPoint[] oldResultPoints = result.ResultPoints;
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
