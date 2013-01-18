using System;
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

namespace com.google.zxing.qrcode
{

	using BarcodeFormat = com.google.zxing.BarcodeFormat;
	using BinaryBitmap = com.google.zxing.BinaryBitmap;
	using ChecksumException = com.google.zxing.ChecksumException;
	using DecodeHintType = com.google.zxing.DecodeHintType;
	using FormatException = com.google.zxing.FormatException;
	using NotFoundException = com.google.zxing.NotFoundException;
	using Reader = com.google.zxing.Reader;
	using Result = com.google.zxing.Result;
	using ResultMetadataType = com.google.zxing.ResultMetadataType;
	using ResultPoint = com.google.zxing.ResultPoint;
	using BitMatrix = com.google.zxing.common.BitMatrix;
	using DecoderResult = com.google.zxing.common.DecoderResult;
	using DetectorResult = com.google.zxing.common.DetectorResult;
	using Decoder = com.google.zxing.qrcode.decoder.Decoder;
	using Detector = com.google.zxing.qrcode.detector.Detector;


	/// <summary>
	/// This implementation can detect and decode QR Codes in an image.
	/// 
	/// @author Sean Owen
	/// </summary>
	public class QRCodeReader : com.google.zxing.Reader
	{

	  private static readonly ResultPoint[] NO_POINTS = new ResultPoint[0];

	  private readonly Decoder decoder = new Decoder();

	  protected internal virtual Decoder Decoder
	  {
		  get
		  {
			return decoder;
		  }
	  }

	  /// <summary>
	  /// Locates and decodes a QR code in an image.
	  /// </summary>
	  /// <returns> a String representing the content encoded by the QR code </returns>
	  /// <exception cref="NotFoundException"> if a QR code cannot be found </exception>
	  /// <exception cref="FormatException"> if a QR code cannot be decoded </exception>
	  /// <exception cref="ChecksumException"> if error correction fails </exception>
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
		DecoderResult decoderResult;
		ResultPoint[] points;
		if (hints != null && hints.ContainsKey(DecodeHintType.PURE_BARCODE))
		{
		  BitMatrix bits = extractPureBits(image.BlackMatrix);
		  decoderResult = decoder.decode(bits, hints);
		  points = NO_POINTS;
		}
		else
		{
		  DetectorResult detectorResult = (new Detector(image.BlackMatrix)).detect(hints);
		  decoderResult = decoder.decode(detectorResult.Bits, hints);
		  points = detectorResult.Points;
		}

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
		return result;
	  }

	  public void reset()
	  {
		// do nothing
	  }

	  /// <summary>
	  /// This method detects a code in a "pure" image -- that is, pure monochrome image
	  /// which contains only an unrotated, unskewed, image of a code, with some white border
	  /// around it. This is a specialized method that works exceptionally fast in this special
	  /// case.
	  /// </summary>
	  /// <seealso cref= com.google.zxing.pdf417.PDF417Reader#extractPureBits(BitMatrix) </seealso>
	  /// <seealso cref= com.google.zxing.datamatrix.DataMatrixReader#extractPureBits(BitMatrix) </seealso>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static com.google.zxing.common.BitMatrix extractPureBits(com.google.zxing.common.BitMatrix image) throws com.google.zxing.NotFoundException
	  private static BitMatrix extractPureBits(BitMatrix image)
	  {

		int[] leftTopBlack = image.TopLeftOnBit;
		int[] rightBottomBlack = image.BottomRightOnBit;
		if (leftTopBlack == null || rightBottomBlack == null)
		{
		  throw NotFoundException.NotFoundInstance;
		}

		float moduleSize = getModuleSize(leftTopBlack, image);

		int top = leftTopBlack[1];
		int bottom = rightBottomBlack[1];
		int left = leftTopBlack[0];
		int right = rightBottomBlack[0];

		if (bottom - top != right - left)
		{
		  // Special case, where bottom-right module wasn't black so we found something else in the last row
		  // Assume it's a square, so use height as the width
		  right = left + (bottom - top);
		}

		int matrixWidth = (int)Math.Round((right - left + 1) / moduleSize);
		int matrixHeight = (int) Math.Round((bottom - top + 1) / moduleSize);
		if (matrixWidth <= 0 || matrixHeight <= 0)
		{
		  throw NotFoundException.NotFoundInstance;
		}
		if (matrixHeight != matrixWidth)
		{
		  // Only possibly decode square regions
		  throw NotFoundException.NotFoundInstance;
		}

		// Push in the "border" by half the module width so that we start
		// sampling in the middle of the module. Just in case the image is a
		// little off, this will help recover.
		int nudge = (int)(moduleSize / 2.0f);
		top += nudge;
		left += nudge;

		// Now just read off the bits
		BitMatrix bits = new BitMatrix(matrixWidth, matrixHeight);
		for (int y = 0; y < matrixHeight; y++)
		{
		  int iOffset = top + (int)(y * moduleSize);
		  for (int x = 0; x < matrixWidth; x++)
		  {
			if (image.get(left + (int)(x * moduleSize), iOffset))
			{
			  bits.set(x, y);
			}
		  }
		}
		return bits;
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static float moduleSize(int[] leftTopBlack, com.google.zxing.common.BitMatrix image) throws com.google.zxing.NotFoundException
	  private static float getModuleSize(int[] leftTopBlack, BitMatrix image)
	  {
		int height = image.Height;
		int width = image.Width;
		int x = leftTopBlack[0];
		int y = leftTopBlack[1];
		bool inBlack = true;
		int transitions = 0;
		while (x < width && y < height)
		{
		  if (inBlack != image.get(x, y))
		  {
			if (++transitions == 5)
			{
			  break;
			}
			inBlack = !inBlack;
		  }
		  x++;
		  y++;
		}
		if (x == width || y == height)
		{
		  throw NotFoundException.NotFoundInstance;
		}
		return (x - leftTopBlack[0]) / 7.0f;
	  }

	}

}