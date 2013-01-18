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

namespace com.google.zxing.multi.qrcode.detector
{

	using DecodeHintType = com.google.zxing.DecodeHintType;
	using NotFoundException = com.google.zxing.NotFoundException;
	using ReaderException = com.google.zxing.ReaderException;
	using ResultPointCallback = com.google.zxing.ResultPointCallback;
	using BitMatrix = com.google.zxing.common.BitMatrix;
	using DetectorResult = com.google.zxing.common.DetectorResult;
	using Detector = com.google.zxing.qrcode.detector.Detector;
	using FinderPatternInfo = com.google.zxing.qrcode.detector.FinderPatternInfo;


	/// <summary>
	/// <p>Encapsulates logic that can detect one or more QR Codes in an image, even if the QR Code
	/// is rotated or skewed, or partially obscured.</p>
	/// 
	/// @author Sean Owen
	/// @author Hannes Erven
	/// </summary>
	public sealed class MultiDetector : com.google.zxing.qrcode.detector.Detector
	{

	  private static readonly DetectorResult[] EMPTY_DETECTOR_RESULTS = new DetectorResult[0];

	  public MultiDetector(BitMatrix image) : base(image)
	  {
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.DetectorResult[] detectMulti(java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException
      public DetectorResult[] detectMulti(IDictionary<DecodeHintType, object> hints)
	  {
		BitMatrix image = Image;
        //ResultPointCallback resultPointCallback = hints == null ? null : (ResultPointCallback) hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK];
        ResultPointCallback resultPointCallback = null;
        if (hints !=null && hints.ContainsKey(DecodeHintType.NEED_RESULT_POINT_CALLBACK))
        {
            resultPointCallback = (ResultPointCallback) hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK];
        }

		MultiFinderPatternFinder finder = new MultiFinderPatternFinder(image, resultPointCallback);
		FinderPatternInfo[] infos = finder.findMulti(hints);

		if (infos.Length == 0)
		{
		  throw NotFoundException.NotFoundInstance;
		}

		List<DetectorResult> result = new List<DetectorResult>();
		foreach (FinderPatternInfo info in infos)
		{
		  try
		  {
			result.Add(processFinderPatternInfo(info));
		  }
		  catch (ReaderException e)
		  {
			// ignore
		  }
		}
		if (result.Count == 0)
		{
		  return EMPTY_DETECTOR_RESULTS;
		}
		else
		{
		  return result.ToArray();
		}
	  }

	}

}