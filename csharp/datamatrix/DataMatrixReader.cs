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

namespace com.google.zxing.datamatrix
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
	using Decoder = com.google.zxing.datamatrix.decoder.Decoder;
	using Detector = com.google.zxing.datamatrix.detector.Detector;


	/// <summary>
	/// This implementation can detect and decode Data Matrix codes in an image.
	/// 
	/// @author bbrown@google.com (Brian Brown)
	/// </summary>
	public sealed class DataMatrixReader : com.google.zxing.Reader
	{

	  private static readonly ResultPoint[] NO_POINTS = new ResultPoint[0];

	  private readonly Decoder decoder = new Decoder();

	  /// <summary>
	  /// Locates and decodes a Data Matrix code in an image.
	  /// </summary>
	  /// <returns> a String representing the content encoded by the Data Matrix code </returns>
	  /// <exception cref="NotFoundException"> if a Data Matrix code cannot be found </exception>
	  /// <exception cref="FormatException"> if a Data Matrix code cannot be decoded </exception>
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
		  decoderResult = decoder.decode(bits);
		  points = NO_POINTS;
		}
		else
		{
		  DetectorResult detectorResult = (new Detector(image.BlackMatrix)).detect();
		  decoderResult = decoder.decode(detectorResult.Bits);
		  points = detectorResult.Points;
		}
		Result result = new Result(decoderResult.Text, decoderResult.RawBytes, points, BarcodeFormat.DATA_MATRIX);
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
	  /// <seealso cref= com.google.zxing.qrcode.QRCodeReader#extractPureBits(BitMatrix) </seealso>
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

		int moduleSize = getModuleSize(leftTopBlack, image);

		int top = leftTopBlack[1];
		int bottom = rightBottomBlack[1];
		int left = leftTopBlack[0];
		int right = rightBottomBlack[0];

		int matrixWidth = (right - left + 1) / moduleSize;
		int matrixHeight = (bottom - top + 1) / moduleSize;
		if (matrixWidth <= 0 || matrixHeight <= 0)
		{
		  throw NotFoundException.NotFoundInstance;
		}

		// Push in the "border" by half the module width so that we start
		// sampling in the middle of the module. Just in case the image is a
		// little off, this will help recover.
		int nudge = moduleSize >> 1;
		top += nudge;
		left += nudge;

		// Now just read off the bits
		BitMatrix bits = new BitMatrix(matrixWidth, matrixHeight);
		for (int y = 0; y < matrixHeight; y++)
		{
		  int iOffset = top + y * moduleSize;
		  for (int x = 0; x < matrixWidth; x++)
		  {
			if (image.get(left + x * moduleSize, iOffset))
			{
			  bits.set(x, y);
			}
		  }
		}
		return bits;
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static int moduleSize(int[] leftTopBlack, com.google.zxing.common.BitMatrix image) throws com.google.zxing.NotFoundException
	  private static int getModuleSize(int[] leftTopBlack, BitMatrix image)
	  {
		int width = image.Width;
		int x = leftTopBlack[0];
		int y = leftTopBlack[1];
		while (x < width && image.get(x, y))
		{
		  x++;
		}
		if (x == width)
		{
		  throw NotFoundException.NotFoundInstance;
		}

		int moduleSize = x - leftTopBlack[0];
		if (moduleSize == 0)
		{
		  throw NotFoundException.NotFoundInstance;
		}
		return moduleSize;
	  }

	}
}