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

namespace com.google.zxing.multi.qrcode
{

	using BarcodeFormat = com.google.zxing.BarcodeFormat;
	using BinaryBitmap = com.google.zxing.BinaryBitmap;
	using DecodeHintType = com.google.zxing.DecodeHintType;
	using NotFoundException = com.google.zxing.NotFoundException;
	using ReaderException = com.google.zxing.ReaderException;
	using Result = com.google.zxing.Result;
	using ResultMetadataType = com.google.zxing.ResultMetadataType;
	using ResultPoint = com.google.zxing.ResultPoint;
	using DecoderResult = com.google.zxing.common.DecoderResult;
	using DetectorResult = com.google.zxing.common.DetectorResult;
	using MultipleBarcodeReader = com.google.zxing.multi.MultipleBarcodeReader;
	using MultiDetector = com.google.zxing.multi.qrcode.detector.MultiDetector;
	using QRCodeReader = com.google.zxing.qrcode.QRCodeReader;


	/// <summary>
	/// This implementation can detect and decode multiple QR Codes in an image.
	/// 
	/// @author Sean Owen
	/// @author Hannes Erven
	/// </summary>
	public sealed class QRCodeMultiReader : com.google.zxing.qrcode.QRCodeReader, com.google.zxing.multi.MultipleBarcodeReader
	{

	  private static readonly Result[] EMPTY_RESULT_ARRAY = new Result[0];

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
		DetectorResult[] detectorResults = (new MultiDetector(image.BlackMatrix)).detectMulti(hints);
		foreach (DetectorResult detectorResult in detectorResults)
		{
		  try
		  {
			DecoderResult decoderResult = Decoder.decode(detectorResult.Bits, hints);
			ResultPoint[] points = detectorResult.Points;
			Result result = new Result(decoderResult.Text, decoderResult.RawBytes, points, BarcodeFormat.QR_CODE);
			IList<sbyte[]> byteSegments = decoderResult.ByteSegments;
			if (byteSegments != null)
			{
			  result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, byteSegments);
			}
			string ecLevel = decoderResult.ECLevel;
			if (ecLevel != null)
			{
			  result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
			}
			results.Add(result);
		  }
		  catch (ReaderException re)
		  {
			// ignore and continue 
		  }
		}
		if (results.Count == 0)
		{
		  return EMPTY_RESULT_ARRAY;
		}
		else
		{
		  return results.ToArray();
		}
	  }

	}

}