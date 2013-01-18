using System;
using System.Collections.Generic;
using com.google.zxing.common;

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

namespace com.google.zxing.pdf417.detector
{

	using BinaryBitmap = com.google.zxing.BinaryBitmap;
	using DecodeHintType = com.google.zxing.DecodeHintType;
	using NotFoundException = com.google.zxing.NotFoundException;
	using ResultPoint = com.google.zxing.ResultPoint;
	using BitMatrix = com.google.zxing.common.BitMatrix;
	using DetectorResult = com.google.zxing.common.DetectorResult;
	using GridSampler = com.google.zxing.common.GridSampler;
	using MathUtils = com.google.zxing.common.detector.MathUtils;


	/// <summary>
	/// <p>Encapsulates logic that can detect a PDF417 Code in an image, even if the
	/// PDF417 Code is rotated or skewed, or partially obscured.</p>
	/// 
	/// @author SITA Lab (kevin.osullivan@sita.aero)
	/// @author dswitkin@google.com (Daniel Switkin)
	/// </summary>
	public sealed class Detector
	{

	  private const int INTEGER_MATH_SHIFT = 8;
	  private static readonly int PATTERN_MATCH_RESULT_SCALE_FACTOR = 1 << INTEGER_MATH_SHIFT;
	  private static readonly int MAX_AVG_VARIANCE = (int)(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42f);
	  private static readonly int MAX_INDIVIDUAL_VARIANCE = (int)(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.8f);
	  private const int SKEW_THRESHOLD = 3;

	  // B S B S B S B S Bar/Space pattern
	  // 11111111 0 1 0 1 0 1 000
	  private static readonly int[] START_PATTERN = {8, 1, 1, 1, 1, 1, 1, 3};

	  // 11111111 0 1 0 1 0 1 000
	  private static readonly int[] START_PATTERN_REVERSE = {3, 1, 1, 1, 1, 1, 1, 8};

	  // 1111111 0 1 000 1 0 1 00 1
	  private static readonly int[] STOP_PATTERN = {7, 1, 1, 3, 1, 1, 1, 2, 1};

	  // B S B S B S B S B Bar/Space pattern
	  // 1111111 0 1 000 1 0 1 00 1
	  private static readonly int[] STOP_PATTERN_REVERSE = {1, 2, 1, 1, 1, 3, 1, 1, 7};

	  private readonly BinaryBitmap image;

	  public Detector(BinaryBitmap image)
	  {
		this.image = image;
	  }

	  /// <summary>
	  /// <p>Detects a PDF417 Code in an image, simply.</p>
	  /// </summary>
	  /// <returns> <seealso cref="DetectorResult"/> encapsulating results of detecting a PDF417 Code </returns>
	  /// <exception cref="NotFoundException"> if no QR Code can be found </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.DetectorResult detect() throws com.google.zxing.NotFoundException
	  public DetectorResult detect()
	  {
		return detect(null);
	  }

	  /// <summary>
	  /// <p>Detects a PDF417 Code in an image. Only checks 0 and 180 degree rotations.</p>
	  /// </summary>
	  /// <param name="hints"> optional hints to detector </param>
	  /// <returns> <seealso cref="DetectorResult"/> encapsulating results of detecting a PDF417 Code </returns>
	  /// <exception cref="NotFoundException"> if no PDF417 Code can be found </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.DetectorResult detect(java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException
      public DetectorResult detect(IDictionary<DecodeHintType, object> hints)
	  {
		// Fetch the 1 bit matrix once up front.
		BitMatrix matrix = image.BlackMatrix;

		bool tryHarder = hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER);

		// Try to find the vertices assuming the image is upright.
		ResultPoint[] vertices = findVertices(matrix, tryHarder);
		if (vertices == null)
		{
		  // Maybe the image is rotated 180 degrees?
		  vertices = findVertices180(matrix, tryHarder);
		  if (vertices != null)
		  {
			correctCodeWordVertices(vertices, true);
		  }
		}
		else
		{
		  correctCodeWordVertices(vertices, false);
		}

		if (vertices == null)
		{
		  throw NotFoundException.NotFoundInstance;
		}

		float moduleWidth = computeModuleWidth(vertices);
		if (moduleWidth < 1.0f)
		{
		  throw NotFoundException.NotFoundInstance;
		}

		int dimension = computeDimension(vertices[4], vertices[6], vertices[5], vertices[7], moduleWidth);
		if (dimension < 1)
		{
		  throw NotFoundException.NotFoundInstance;
		}

		int ydimension = computeYDimension(vertices[4], vertices[6], vertices[5], vertices[7], moduleWidth);
		ydimension = ydimension > dimension ? ydimension : dimension;

		// Deskew and sample image.
		BitMatrix bits = sampleGrid(matrix, vertices[4], vertices[5], vertices[6], vertices[7], dimension, ydimension);
		return new DetectorResult(bits, new ResultPoint[]{vertices[5], vertices[4], vertices[6], vertices[7]});
	  }

	  /// <summary>
	  /// Locate the vertices and the codewords area of a black blob using the Start
	  /// and Stop patterns as locators.
	  /// </summary>
	  /// <param name="matrix"> the scanned barcode image. </param>
	  /// <returns> an array containing the vertices:
	  ///           vertices[0] x, y top left barcode
	  ///           vertices[1] x, y bottom left barcode
	  ///           vertices[2] x, y top right barcode
	  ///           vertices[3] x, y bottom right barcode
	  ///           vertices[4] x, y top left codeword area
	  ///           vertices[5] x, y bottom left codeword area
	  ///           vertices[6] x, y top right codeword area
	  ///           vertices[7] x, y bottom right codeword area </returns>
	  private static ResultPoint[] findVertices(BitMatrix matrix, bool tryHarder)
	  {
		int height = matrix.Height;
		int width = matrix.Width;

		ResultPoint[] result = new ResultPoint[8];
		bool found = false;

		int[] counters = new int[START_PATTERN.Length];

		int rowStep = Math.Max(1, height >> (tryHarder ? 9 : 7));

		// Top Left
		for (int i = 0; i < height; i += rowStep)
		{
		  int[] loc = findGuardPattern(matrix, 0, i, width, false, START_PATTERN, counters);
		  if (loc != null)
		  {
			result[0] = new ResultPoint(loc[0], i);
			result[4] = new ResultPoint(loc[1], i);
			found = true;
			break;
		  }
		}
		// Bottom left
		if (found) // Found the Top Left vertex
		{
		  found = false;
		  for (int i = height - 1; i > 0; i -= rowStep)
		  {
			int[] loc = findGuardPattern(matrix, 0, i, width, false, START_PATTERN, counters);
			if (loc != null)
			{
			  result[1] = new ResultPoint(loc[0], i);
			  result[5] = new ResultPoint(loc[1], i);
			  found = true;
			  break;
			}
		  }
		}

		counters = new int[STOP_PATTERN.Length];

		// Top right
		if (found) // Found the Bottom Left vertex
		{
		  found = false;
		  for (int i = 0; i < height; i += rowStep)
		  {
			int[] loc = findGuardPattern(matrix, 0, i, width, false, STOP_PATTERN, counters);
			if (loc != null)
			{
			  result[2] = new ResultPoint(loc[1], i);
			  result[6] = new ResultPoint(loc[0], i);
			  found = true;
			  break;
			}
		  }
		}
		// Bottom right
		if (found) // Found the Top right vertex
		{
		  found = false;
		  for (int i = height - 1; i > 0; i -= rowStep)
		  {
			int[] loc = findGuardPattern(matrix, 0, i, width, false, STOP_PATTERN, counters);
			if (loc != null)
			{
			  result[3] = new ResultPoint(loc[1], i);
			  result[7] = new ResultPoint(loc[0], i);
			  found = true;
			  break;
			}
		  }
		}
		return found ? result : null;
	  }

	  /// <summary>
	  /// Locate the vertices and the codewords area of a black blob using the Start
	  /// and Stop patterns as locators. This assumes that the image is rotated 180
	  /// degrees and if it locates the start and stop patterns at it will re-map
	  /// the vertices for a 0 degree rotation.
	  /// TODO: Change assumption about barcode location.
	  /// </summary>
	  /// <param name="matrix"> the scanned barcode image. </param>
	  /// <returns> an array containing the vertices:
	  ///           vertices[0] x, y top left barcode
	  ///           vertices[1] x, y bottom left barcode
	  ///           vertices[2] x, y top right barcode
	  ///           vertices[3] x, y bottom right barcode
	  ///           vertices[4] x, y top left codeword area
	  ///           vertices[5] x, y bottom left codeword area
	  ///           vertices[6] x, y top right codeword area
	  ///           vertices[7] x, y bottom right codeword area </returns>
	  private static ResultPoint[] findVertices180(BitMatrix matrix, bool tryHarder)
	  {
		int height = matrix.Height;
		int width = matrix.Width;
		int halfWidth = width >> 1;

		ResultPoint[] result = new ResultPoint[8];
		bool found = false;

		int[] counters = new int[START_PATTERN_REVERSE.Length];

		int rowStep = Math.Max(1, height >> (tryHarder ? 9 : 7));

		// Top Left
		for (int i = height - 1; i > 0; i -= rowStep)
		{
		  int[] loc = findGuardPattern(matrix, halfWidth, i, halfWidth, true, START_PATTERN_REVERSE, counters);
		  if (loc != null)
		  {
			result[0] = new ResultPoint(loc[1], i);
			result[4] = new ResultPoint(loc[0], i);
			found = true;
			break;
		  }
		}
		// Bottom Left
		if (found) // Found the Top Left vertex
		{
		  found = false;
		  for (int i = 0; i < height; i += rowStep)
		  {
			int[] loc = findGuardPattern(matrix, halfWidth, i, halfWidth, true, START_PATTERN_REVERSE, counters);
			if (loc != null)
			{
			  result[1] = new ResultPoint(loc[1], i);
			  result[5] = new ResultPoint(loc[0], i);
			  found = true;
			  break;
			}
		  }
		}

		counters = new int[STOP_PATTERN_REVERSE.Length];

		// Top Right
		if (found) // Found the Bottom Left vertex
		{
		  found = false;
		  for (int i = height - 1; i > 0; i -= rowStep)
		  {
			int[] loc = findGuardPattern(matrix, 0, i, halfWidth, false, STOP_PATTERN_REVERSE, counters);
			if (loc != null)
			{
			  result[2] = new ResultPoint(loc[0], i);
			  result[6] = new ResultPoint(loc[1], i);
			  found = true;
			  break;
			}
		  }
		}
		// Bottom Right
		if (found) // Found the Top Right vertex
		{
		  found = false;
		  for (int i = 0; i < height; i += rowStep)
		  {
			int[] loc = findGuardPattern(matrix, 0, i, halfWidth, false, STOP_PATTERN_REVERSE, counters);
			if (loc != null)
			{
			  result[3] = new ResultPoint(loc[0], i);
			  result[7] = new ResultPoint(loc[1], i);
			  found = true;
			  break;
			}
		  }
		}
		return found ? result : null;
	  }

	  /// <summary>
	  /// Because we scan horizontally to detect the start and stop patterns, the vertical component of
	  /// the codeword coordinates will be slightly wrong if there is any skew or rotation in the image.
	  /// This method moves those points back onto the edges of the theoretically perfect bounding
	  /// quadrilateral if needed.
	  /// </summary>
	  /// <param name="vertices"> The eight vertices located by findVertices(). </param>
	  private static void correctCodeWordVertices(ResultPoint[] vertices, bool upsideDown)
	  {

		float v0x = vertices[0].X;
		float v0y = vertices[0].Y;
		float v2x = vertices[2].X;
		float v2y = vertices[2].Y;
		float v4x = vertices[4].X;
		float v4y = vertices[4].Y;
		float v6x = vertices[6].X;
		float v6y = vertices[6].Y;

		float skew = v4y - v6y;
		if (upsideDown)
		{
		  skew = -skew;
		}
		if (skew > SKEW_THRESHOLD)
		{
		  // Fix v4
		  float deltax = v6x - v0x;
		  float deltay = v6y - v0y;
		  float delta2 = deltax * deltax + deltay * deltay;
		  float correction = (v4x - v0x) * deltax / delta2;
		  vertices[4] = new ResultPoint(v0x + correction * deltax, v0y + correction * deltay);
		}
		else if (-skew > SKEW_THRESHOLD)
		{
		  // Fix v6
		  float deltax = v2x - v4x;
		  float deltay = v2y - v4y;
		  float delta2 = deltax * deltax + deltay * deltay;
		  float correction = (v2x - v6x) * deltax / delta2;
		  vertices[6] = new ResultPoint(v2x - correction * deltax, v2y - correction * deltay);
		}

		float v1x = vertices[1].X;
		float v1y = vertices[1].Y;
		float v3x = vertices[3].X;
		float v3y = vertices[3].Y;
		float v5x = vertices[5].X;
		float v5y = vertices[5].Y;
		float v7x = vertices[7].X;
		float v7y = vertices[7].Y;

		skew = v7y - v5y;
		if (upsideDown)
		{
		  skew = -skew;
		}
		if (skew > SKEW_THRESHOLD)
		{
		  // Fix v5
		  float deltax = v7x - v1x;
		  float deltay = v7y - v1y;
		  float delta2 = deltax * deltax + deltay * deltay;
		  float correction = (v5x - v1x) * deltax / delta2;
		  vertices[5] = new ResultPoint(v1x + correction * deltax, v1y + correction * deltay);
		}
		else if (-skew > SKEW_THRESHOLD)
		{
		  // Fix v7
		  float deltax = v3x - v5x;
		  float deltay = v3y - v5y;
		  float delta2 = deltax * deltax + deltay * deltay;
		  float correction = (v3x - v7x) * deltax / delta2;
		  vertices[7] = new ResultPoint(v3x - correction * deltax, v3y - correction * deltay);
		}
	  }

	  /// <summary>
	  /// <p>Estimates module size (pixels in a module) based on the Start and End
	  /// finder patterns.</p>
	  /// </summary>
	  /// <param name="vertices"> an array of vertices:
	  ///           vertices[0] x, y top left barcode
	  ///           vertices[1] x, y bottom left barcode
	  ///           vertices[2] x, y top right barcode
	  ///           vertices[3] x, y bottom right barcode
	  ///           vertices[4] x, y top left codeword area
	  ///           vertices[5] x, y bottom left codeword area
	  ///           vertices[6] x, y top right codeword area
	  ///           vertices[7] x, y bottom right codeword area </param>
	  /// <returns> the module size. </returns>
	  private static float computeModuleWidth(ResultPoint[] vertices)
	  {
		float pixels1 = ResultPoint.distance(vertices[0], vertices[4]);
		float pixels2 = ResultPoint.distance(vertices[1], vertices[5]);
		float moduleWidth1 = (pixels1 + pixels2) / (17 * 2.0f);
		float pixels3 = ResultPoint.distance(vertices[6], vertices[2]);
		float pixels4 = ResultPoint.distance(vertices[7], vertices[3]);
		float moduleWidth2 = (pixels3 + pixels4) / (18 * 2.0f);
		return (moduleWidth1 + moduleWidth2) / 2.0f;
	  }

	  /// <summary>
	  /// Computes the dimension (number of modules in a row) of the PDF417 Code
	  /// based on vertices of the codeword area and estimated module size.
	  /// </summary>
	  /// <param name="topLeft">     of codeword area </param>
	  /// <param name="topRight">    of codeword area </param>
	  /// <param name="bottomLeft">  of codeword area </param>
	  /// <param name="bottomRight"> of codeword are </param>
	  /// <param name="moduleWidth"> estimated module size </param>
	  /// <returns> the number of modules in a row. </returns>
	  private static int computeDimension(ResultPoint topLeft, ResultPoint topRight, ResultPoint bottomLeft, ResultPoint bottomRight, float moduleWidth)
	  {
		int topRowDimension = MathUtils.round(ResultPoint.distance(topLeft, topRight) / moduleWidth);
		int bottomRowDimension = MathUtils.round(ResultPoint.distance(bottomLeft, bottomRight) / moduleWidth);
		return ((((topRowDimension + bottomRowDimension) >> 1) + 8) / 17) * 17;
	  }

	  /// <summary>
	  /// Computes the y dimension (number of modules in a column) of the PDF417 Code
	  /// based on vertices of the codeword area and estimated module size.
	  /// </summary>
	  /// <param name="topLeft">     of codeword area </param>
	  /// <param name="topRight">    of codeword area </param>
	  /// <param name="bottomLeft">  of codeword area </param>
	  /// <param name="bottomRight"> of codeword are </param>
	  /// <param name="moduleWidth"> estimated module size </param>
	  /// <returns> the number of modules in a row. </returns>
	  private static int computeYDimension(ResultPoint topLeft, ResultPoint topRight, ResultPoint bottomLeft, ResultPoint bottomRight, float moduleWidth)
	  {
		int leftColumnDimension = MathUtils.round(ResultPoint.distance(topLeft, bottomLeft) / moduleWidth);
		int rightColumnDimension = MathUtils.round(ResultPoint.distance(topRight, bottomRight) / moduleWidth);
		return (leftColumnDimension + rightColumnDimension) >> 1;
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static com.google.zxing.common.BitMatrix sampleGrid(com.google.zxing.common.BitMatrix matrix, com.google.zxing.ResultPoint topLeft, com.google.zxing.ResultPoint bottomLeft, com.google.zxing.ResultPoint topRight, com.google.zxing.ResultPoint bottomRight, int xdimension, int ydimension) throws com.google.zxing.NotFoundException
	  private static BitMatrix sampleGrid(BitMatrix matrix, ResultPoint topLeft, ResultPoint bottomLeft, ResultPoint topRight, ResultPoint bottomRight, int xdimension, int ydimension)
	  {

		// Note that unlike the QR Code sampler, we didn't find the center of modules, but the
		// very corners. So there is no 0.5f here; 0.0f is right.
		GridSampler sampler = GridSampler.Instance;

		return sampler.sampleGrid(matrix, xdimension, ydimension, 0.0f, 0.0f, xdimension, 0.0f, xdimension, ydimension, 0.0f, ydimension, topLeft.X, topLeft.Y, topRight.X, topRight.Y, bottomRight.X, bottomRight.Y, bottomLeft.X, bottomLeft.Y); // p4FromY -  p4FromX -  p3FromY -  p3FromX -  p2FromY -  p2FromX -  p1FromY -  p1FromX -  p4ToY -  p4ToX -  p3ToY -  p3ToX -  p2ToY -  p2ToX -  p1ToY -  p1ToX
	  }

	  /// <param name="matrix"> row of black/white values to search </param>
	  /// <param name="column"> x position to start search </param>
	  /// <param name="row"> y position to start search </param>
	  /// <param name="width"> the number of pixels to search on this row </param>
	  /// <param name="pattern"> pattern of counts of number of black and white pixels that are
	  ///                 being searched for as a pattern </param>
	  /// <param name="counters"> array of counters, as long as pattern, to re-use </param>
	  /// <returns> start/end horizontal offset of guard pattern, as an array of two ints. </returns>
	  private static int[] findGuardPattern(BitMatrix matrix, int column, int row, int width, bool whiteFirst, int[] pattern, int[] counters)
	  {
        //Arrays.fill(counters, 0, counters.Length, 0);
	    counters.Fill(0);
		int patternLength = pattern.Length;
		bool isWhite = whiteFirst;

		int counterPosition = 0;
		int patternStart = column;
		for (int x = column; x < column + width; x++)
		{
		  bool pixel = matrix.get(x, row);
		  if (pixel ^ isWhite)
		  {
			counters[counterPosition]++;
		  }
		  else
		  {
			if (counterPosition == patternLength - 1)
			{
			  if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE)
			  {
				return new int[]{patternStart, x};
			  }
			  patternStart += counters[0] + counters[1];
			  Array.Copy(counters, 2, counters, 0, patternLength - 2);
			  counters[patternLength - 2] = 0;
			  counters[patternLength - 1] = 0;
			  counterPosition--;
			}
			else
			{
			  counterPosition++;
			}
			counters[counterPosition] = 1;
			isWhite = !isWhite;
		  }
		}
		return null;
	  }

	  /// <summary>
	  /// Determines how closely a set of observed counts of runs of black/white
	  /// values matches a given target pattern. This is reported as the ratio of
	  /// the total variance from the expected pattern proportions across all
	  /// pattern elements, to the length of the pattern.
	  /// </summary>
	  /// <param name="counters"> observed counters </param>
	  /// <param name="pattern"> expected pattern </param>
	  /// <param name="maxIndividualVariance"> The most any counter can differ before we give up </param>
	  /// <returns> ratio of total variance between counters and pattern compared to
	  ///         total pattern size, where the ratio has been multiplied by 256.
	  ///         So, 0 means no variance (perfect match); 256 means the total
	  ///         variance between counters and patterns equals the pattern length,
	  ///         higher values mean even more variance </returns>
	  private static int patternMatchVariance(int[] counters, int[] pattern, int maxIndividualVariance)
	  {
		int numCounters = counters.Length;
		int total = 0;
		int patternLength = 0;
		for (int i = 0; i < numCounters; i++)
		{
		  total += counters[i];
		  patternLength += pattern[i];
		}
		if (total < patternLength)
		{
		  // If we don't even have one pixel per unit of bar width, assume this
		  // is too small to reliably match, so fail:
		  return int.MaxValue;
		}
		// We're going to fake floating-point math in integers. We just need to use more bits.
		// Scale up patternLength so that intermediate values below like scaledCounter will have
		// more "significant digits".
		int unitBarWidth = (total << INTEGER_MATH_SHIFT) / patternLength;
		maxIndividualVariance = (maxIndividualVariance * unitBarWidth) >> 8;

		int totalVariance = 0;
		for (int x = 0; x < numCounters; x++)
		{
		  int counter = counters[x] << INTEGER_MATH_SHIFT;
		  int scaledPattern = pattern[x] * unitBarWidth;
		  int variance = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
		  if (variance > maxIndividualVariance)
		  {
			return int.MaxValue;
		  }
		  totalVariance += variance;
		}
		return totalVariance / total;
	  }

	}

}