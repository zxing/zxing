/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.aztec
{
	import com.google.zxing.Reader;
	
	public final class AztecReader implements Reader 
	{

		import com.google.zxing.BarcodeFormat;
		import com.google.zxing.BinaryBitmap;
		import com.google.zxing.ChecksumException;
		import com.google.zxing.DecodeHintType;
		import com.google.zxing.FormatException;
		import com.google.zxing.NotFoundException;
		import com.google.zxing.Reader;
		import com.google.zxing.Result;
		import com.google.zxing.ResultMetadataType;
		import com.google.zxing.ResultPoint;
		import com.google.zxing.ResultPointCallback;
		import com.google.zxing.common.DecoderResult;
		import com.google.zxing.aztec.decoder.Decoder;
		import com.google.zxing.aztec.detector.Detector;

    	import com.google.zxing.common.BitArray;
    	import com.google.zxing.ResultPoint;
    	import com.google.zxing.common.flexdatatypes.StringBuilder;
    	import com.google.zxing.common.flexdatatypes.HashTable;

		/**
		 * This implementation can detect and decode Aztec codes in an image.
		 *
		 * @author David Olivier
		 */


		  /**
		   * Locates and decodes a Data Matrix code in an image.
		   *
		   * @return a String representing the content encoded by the Data Matrix code
		   * @throws NotFoundException if a Data Matrix code cannot be found
		   * @throws FormatException if a Data Matrix code cannot be decoded
		   * @throws ChecksumException if error correction fails
		   */
  			public  function decode(image:BinaryBitmap, hints:HashTable=null ):Result
  			{
  				if (hints == null) {return decode(image, null);}

    			var detectorResult:AztecDetectorResult = new Detector(image.getBlackMatrix()).detect();
     			var points:Array = detectorResult.getPoints();

    			if ((hints != null) && (detectorResult.getPoints() != null)) 
    			{
      				var rpcb:ResultPointCallback = hints._get(DecodeHintType.NEED_RESULT_POINT_CALLBACK) as ResultPointCallback;
      				if (rpcb != null) 
      				{
        				for (var i:int = 0; i < detectorResult.getPoints().length; i++)
        				{
          					rpcb.foundPossibleResultPoint(detectorResult.getPoints()[i]);
        				}
      				}
   				 }

   				var decoderResult:DecoderResult = new Decoder().decode(detectorResult);

    			var result:Result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.AZTEC);
    
    			if (decoderResult.getByteSegments() != null) 
    			{
      				result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, decoderResult.getByteSegments());
   				}
   				
    			if (decoderResult.getECLevel() != null) 
    			{
      				result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, decoderResult.getECLevel().toString());
    			}
    
    			return result;
  			}

  			public function reset():void
  			{
    				// do nothing
  			}

	}	
}