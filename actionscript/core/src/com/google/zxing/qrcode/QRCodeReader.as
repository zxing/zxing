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
package com.google.zxing.qrcode
{
	import com.google.zxing.BarcodeFormat;
	import com.google.zxing.BinaryBitmap;
	import com.google.zxing.DecodeHintType;
	import com.google.zxing.NotFoundException;
	import com.google.zxing.Reader;
	import com.google.zxing.Result;
	import com.google.zxing.ResultMetadataType;
	import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.DecoderResult;
	import com.google.zxing.common.DetectorResult;
	import com.google.zxing.common.flexdatatypes.HashTable;
	import com.google.zxing.qrcode.decoder.Decoder;
	import com.google.zxing.qrcode.detector.Detector;

    public class QRCodeReader implements Reader
    { 

          private static var  NO_POINTS:Array = new Array(0);
          private var decoder:Decoder = new Decoder();

  public function reset():void {
    // do nothing
  }
  
		  protected function getDecoder():Decoder 
		  {
		    return decoder;
		  }

          /**
           * Locates and decodes a QR code in an image.
           *
           * @return a String representing the content encoded by the QR code
           * @throws ReaderException if a QR code cannot be found, or cannot be decoded
           */
           
          //public function decode(image:MonochromeBitmapSource):Result 
          //{
          //    try
          //    {
          //      return decode(image, null);
          //    }
          //    catch(e:Exception)
          //    {
          //      throw new ReaderException(e.message);
          //    }
          //  
          //}

          public function decode(image:BinaryBitmap,  hints:HashTable=null):Result{  
                var decoderResult:DecoderResult;
                var points:Array;
                if (hints != null && hints.ContainsKey(DecodeHintType.PURE_BARCODE)) 
                {
                  var bits:BitMatrix = extractPureBits(image.getBlackMatrix());
                  decoderResult = decoder.decode(bits);
                  points = NO_POINTS;
                } 
                else 
                {
                  var imageres:BitMatrix = image.getBlackMatrix();
                  //imageres.fromByteArray(imageres.width,imageres.height,[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -119422972, 524287, 0, 0, 0, 0, -262144, -470285825, -118374385, 524287, 0, 0, 0, 0, -262144, -470285825, -118374385, 524287, 0, 0, 0, 0, -131072, -470285825, -127811569, 491520, 0, 0, 0, 0, 1835008, -470285856, 939525104, 491520, 0, 0, 0, 0, 1835008, -470285856, 939525104, 491520, 0, 0, 0, 0, 786432, -470285888, 939525104, 491520, 0, 0, 0, 0, 1835008, -1635835422, 954205127, 495612, 0, 0, 0, 0, -15990784, 473952707, 955253647, 495612, 0, 0, 0, 0, -15990784, 473952707, 955253647, 495612, 0, 0, 0, 0, -14942208, 473952739, 955253711, 495612, 0, 0, 0, 0, -14942208, -67046973, 955253744, 495614, 0, 0, 0, 0, -15990784, -67079709, 955253744, 495612, 0, 0, 0, 0, -15990784, -67079709, 955253752, 495614, 0, 0, 0, 0, -15990784, -470257181, 955382799, 495612, 0, 0, 0, 0, -15990784, -470257181, 955382799, 495614, 0, 0, 0, 0, -15990784, -470257213, 955382799, 495612, 0, 0, 0, 0, -31719424, -536088125, 955254777, 491520, 0, 0, 0, 0, 1835008, -536350272, 955253752, 491520, 0, 0, 0, 0, 1835008, -536350272, 955253744, 491520, 0, 0, 0, 0, 1835008, -536350240, -118488072, 524287, 0, 0, 0, 0, -262144, 473494015, -118373489, 524287, 0, 0, 0, 0, -262144, 473461247, -118373497, 524287, 0, 0, 0, 0, -262144, 473461247, -118373489, 262143, 0, 0, 0, 0, 0, 473952256, 1033103, 0, 0, 0, 0, 0, 0, 473952256, 1033095, 0, 0, 0, 0, 0, 0, 473952256, 1033103, 0, 0, 0, 0, 0, 0, 507502592, -266345594, 2047, 0, 0, 0, 0, -14942208, 532677119, -117440528, 4095, 0, 0, 0, 0, -14942208, 532677119, -117440528, 4095, 0, 0, 0, 0, -14942208, 532677119, -117440528, 4095, 0, 0, 0, 0, 253493248, -533200836, -15844352, 28703, 0, 0, 0, 0, 119275520, -533200868, -15852544, 28703, 0, 0, 0, 0, 119275520, -533200836, -15852544, 28703, 0, 0, 0, 0, 251658240, -524160, -1073611713, 227, 0, 0, 0, 0, 2145386496, -523840, -1073611649, 227, 0, 0, 0, 0, 2145386496, -523840, -1073611649, 227, 0, 0, 0, 0, 2145386496, -261952, -805183361, 65763, 0, 0, 0, 0, 14680064, -532709376, -118366088, 491747, 0, 0, 0, 0, 14680064, -532709376, -118374288, 491747, 0, 0, 0, 0, 14680064, -532742144, -126754696, 229601, 0, 0, 0, 0, -253755392, 3735551, 1048575, 28896, 0, 0, 0, 0, -119537664, 3702783, 1048575, 28896, 0, 0, 0, 0, -119537664, 3702783, 1048575, 28896, 0, 0, 0, 0, -101711872, 29389119, -15859712, 28703, 0, 0, 0, 0, -262144, 29388863, -15859712, 28703, 0, 0, 0, 0, -262144, 29388863, -15859712, 28703, 0, 0, 0, 0, -262144, 29421599, -15859712, 14367, 0, 0, 0, 0, -2147483648, 470286275, -118373392, 3843, 0, 0, 0, 0, -2147483648, 470286275, -118365192, 3843, 0, 0, 0, 0, -2147483648, 503840707, -118373384, 3843, 0, 0, 0, 0, -2147483648, 3993663, -956301185, 227, 0, 0, 0, 0, -2147483648, 3698751, -956301185, 227, 0, 0, 0, 0, -2147483648, 3698751, -956301185, 227, 0, 0, 0, 0, -2147483648, 3993661, 52431098, 193, 0, 0, 0, 0, 133955584, 473956348, 15859696, 0, 0, 0, 0, 0, 133955584, 507510780, 15859704, 0, 0, 0, 0, 0, 133955584, 473956348, 15859704, 0, 0, 0, 0, 0, -119537664, -33062909, -15852529, 28927, 0, 0, 0, 0, -119537664, -33062909, -15852529, 28927, 0, 0, 0, 0, -119537664, -66617341, -15852529, 28927, 0, 0, 0, 0, 132120576, -532709184, -1059004353, 3, 0, 0, 0, 0, 132120576, -532708928, -1057890177, 3, 0, 0, 0, 0, 132120576, -532708928, -1057890177, 3, 0, 0, 0, 0, 132120576, -532709184, -1071521665, 7, 0, 0, 0, 0, -2015363072, -532680673, -1073618824, 31, 0, 0, 0, 0, -2015363072, -532680641, -1073618832, 31, 0, 0, 0, 0, -2015363072, -532680641, -1073618832, 31, 0, 0, 0, 0, -2015363072, 536842239, -917625, 233471, 0, 0, 0, 0, -2015363072, 536842239, -917625, 495615, 0, 0, 0, 0, -2015363072, 536809471, -917617, 495615, 0, 0, 0, 0, 0, 507502592, 16646144, 245566, 0, 0, 0, 0, 0, 507506688, 16646144, 32540, 0, 0, 0, 0, 0, 507506688, 16646144, 32540, 0, 0, 0, 0, 0, 205512704, 16646144, 32540, 0, 0, 0, 0, -262144, 29360639, 956177520, 3868, 0, 0, 0, 0, -262144, 29360639, 956177528, 3868, 0, 0, 0, 0, -262144, 29360639, 956177528, 3868, 0, 0, 0, 0, 1835008, 507507136, 15851647, 260348, 0, 0, 0, 0, 1835008, 507507136, 15851647, 520444, 0, 0, 0, 0, 1835008, 507507136, 15851647, 520444, 0, 0, 0, 0, -31719424, 101183939, 15851522, 258302, 0, 0, 0, 0, -14942208, 29880771, -917632, 31, 0, 0, 0, 0, -14942208, 29880771, -917632, 31, 0, 0, 0, 0, -14942208, 63435203, -983168, 31, 0, 0, 0, 0, -14942208, 533197251, 117440519, 262142, 0, 0, 0, 0, -14942208, 533197251, 117440519, 524284, 0, 0, 0, 0, -14942208, 533197251, 117440519, 524286, 0, 0, 0, 0, -14942208, 3699139, -15851536, 196383, 0, 0, 0, 0, -14942208, 3699139, -15851528, 32543, 0, 0, 0, 0, -14942208, 3699139, -15851528, 32543, 0, 0, 0, 0, 1835008, -536378944, 262084603, 32287, 0, 0, 0, 0, 1835008, -536378944, 134210559, 28672, 0, 0, 0, 0, 1835008, -536378944, 134210559, 28672, 0, 0, 0, 0, -262144, -536673824, 134210559, 28672, 0, 0, 0, 0, -262144, -63409665, 117571583, 4064, 0, 0, 0, 0, -262144, -63409665, 117571583, 4064, 0, 0, 0, 0, -262144, -63409665, 117571583, 4064, 0, 0, 0, 0, 0, 0, 0, 960, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]);
                  var wop:String = imageres.toString2();
                  var detector:Detector = new Detector(imageres);
                  var result2:DetectorResult = detector.detect(hints);
                  var detectRes:BitMatrix = result2.getBits();
                  var a:String = detectRes.toString2(); 
                  decoderResult = decoder.decode(detectRes);
                  points = result2.getPoints();
                }

                var result:Result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.QR_CODE);
                if (decoderResult.getByteSegments() != null) {
                  result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, decoderResult.getByteSegments());
                }
                return result;
          }

          /**
           * This method detects a barcode in a "pure" image -- that is, pure monochrome image
           * which contains only an unrotated, unskewed, image of a barcode, with some white border
           * around it. This is a specialized method that works exceptionally fast in this special
           * case.
           */
          private static function extractPureBits(image:BitMatrix):BitMatrix
		  {
			var leftTopBlack:Array = image.getTopLeftOnBit();
			var rightBottomBlack:Array = image.getBottomRightOnBit();
			if (leftTopBlack == null || rightBottomBlack == null) 
			{
			  throw NotFoundException.getNotFoundInstance();
			}

			var moduleSize:int = moduleSize(leftTopBlack, image);
			var top:int = leftTopBlack[1];
			var bottom:int = rightBottomBlack[1];
			var left:int = leftTopBlack[0];
			var right:int = rightBottomBlack[0];

			var matrixWidth:int = int((right - left + 1) / moduleSize);
			var matrixHeight:int = int((bottom - top + 1) / moduleSize);
			if (matrixWidth <= 0 || matrixHeight <= 0) {
			  throw NotFoundException.getNotFoundInstance();
			}
			if (matrixHeight != matrixWidth) {
			  // Only possibly decode square regions
			  throw NotFoundException.getNotFoundInstance();
			}

			// Push in the "border" by half the module width so that we start
			// sampling in the middle of the module. Just in case the image is a
			// little off, this will help recover.
			var nudge:int = moduleSize >> 1;
			top += nudge;
			left += nudge;

			// Now just read off the bits
			var bits:BitMatrix = new BitMatrix(matrixWidth, matrixHeight);
			for (var y:int = 0; y < matrixHeight; y++) {
			  var iOffset:int = top + y * moduleSize;
			  for (var x:int = 0; x < matrixWidth; x++) {
				if (image._get(left + x * moduleSize, iOffset)) {
				  bits._set(x, y);
				}
			  }
			}
			return bits;
				 
		}
		
		  private static function moduleSize(leftTopBlack:Array, image:BitMatrix):int 
		  {
			var height:int = image.getHeight();
			var width:int = image.getWidth();
			var x:int = leftTopBlack[0];
			var y:int = leftTopBlack[1];
			while (x < width && y < height && image._get(x, y)) {
			  x++;
			  y++;
			}
			if (x == width || y == height) {
			  throw NotFoundException.getNotFoundInstance();
			}

			var moduleSize:int = x - leftTopBlack[0];
			if (moduleSize == 0) {
			  throw NotFoundException.getNotFoundInstance();
			}
			return moduleSize;
		}
  
  
    }
}