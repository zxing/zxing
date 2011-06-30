/*
 * Copyright 2011 ZXing authors
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
package com.google.zxing {

  import com.adobe.images.PNGEncoder;
  import com.google.zxing.client.result.ParsedResult;
  import com.google.zxing.client.result.ResultParser;
  import com.google.zxing.common.ByteMatrix;
  import com.google.zxing.common.GlobalHistogramBinarizer;
  import com.google.zxing.common.flexdatatypes.HashTable;

  import flash.display.Bitmap;
  import flash.display.BitmapData;
  import flash.events.Event;
  import flash.net.FileReference;
  import flash.utils.ByteArray;

  import mx.core.BitmapAsset;

  import org.flexunit.asserts.assertEquals;
  import org.flexunit.asserts.assertTrue;
  import org.flexunit.asserts.fail;

  public class EncodeDecodeEAN8Test {

    [Embed(source="/blackbox/ean8-1/1.gif")]
    private var Ean8_48512343:Class;

    [Embed(source="/data/ean8/ean8_48512343.png")]
    private var ZXingGeneratedEan8_48512343:Class;

    private var imageWidth:int = 338;
    private var imageHeight:int = 323;

    [Test(description="Read EAN8 and validate decoded number.")]
    public function testDecodedEAN8ShouldBeEqualToEncodedNumber():void {
      var ean8image:BitmapAsset = new Ean8_48512343() as BitmapAsset;
      var decodedNumber:Number = decodeEAN8BarcodeImage(ean8image.bitmapData);
      assertEquals(48512343, decodedNumber);
    }

    [Test(async,
        description="Ensure an encoded EAN8 image equals to expected.")]
    public function testEncodedEAN8ImageShouldBeEqualToTarget():void {
      var testCode:Number = 48512343;
      var expectedEan8image:BitmapAsset =
          new ZXingGeneratedEan8_48512343() as BitmapAsset;
      var bitmap:Bitmap = generateEANBarcodeImage(testCode,
                                                  expectedEan8image.width,
                                                  expectedEan8image.height);
      var diffObject:Object =
          expectedEan8image.bitmapData.compare(bitmap.bitmapData);
      assertTrue("diffObject should be int",
          diffObject is int);
      assertEquals(0, diffObject as int);
    }

    [Test(description="Encode a number, decode it, and compare.")]
    public function testEncodedEAN8shouldBeDecodedSuccessfully():void {
      var testCode:Number = 48512343;
      var decodedNumber:Number = encodeAndDecode(testCode);
      assertEquals(testCode, decodedNumber);
    }

    [Test(description="Encodes EAN8 from 0 to 9999999.")]
    public function testEncodeAndDecodeAnyValidEAN8Number():void {
      for (var testCode:Number = 9999999; testCode >= 0; testCode -= 1111111) {
        var checkSum:Number = calculateEANChecksumDigit(testCode);
        var testCodeWithCheckSum:Number = testCode * 10 + checkSum;
        var decodedNumber:Number = encodeAndDecode(testCodeWithCheckSum);
        assertEquals(testCodeWithCheckSum, decodedNumber);
      }
    }

    private function encodeAndDecode(testCode:Number):Number {
      var bitmap:Bitmap = generateEANBarcodeImage(testCode, imageWidth,
                    imageHeight);
      var decodedNumber:Number = decodeEAN8BarcodeImage(bitmap.bitmapData);
      return decodedNumber;
    }

    public function saveBitmapToImage(bitmapData:BitmapData, fileName:String,
        onFileSaveComplete:Function):void {
      var imageBytes:ByteArray = PNGEncoder.encode(bitmapData);
      var file:FileReference = new FileReference();
      file.addEventListener(Event.COMPLETE, onFileSaveComplete);
      file.save(imageBytes, fileName);
    }

    public function generateEANBarcodeImage(number:Number, imageWidth:Number,
        imageHeight:Number):Bitmap {
      var contents:String = formatEAN8String(number);
      var resultBits:Array = encodeEAN8(contents);
      var bitmap:Bitmap = generateImage(resultBits, imageWidth, imageHeight);
      return bitmap;
    }

    public function decodeEAN8BarcodeImage(imageBitmapData:BitmapData):Number {
      var resultNumber:Number;
      var source:BufferedImageLuminanceSource =
          new BufferedImageLuminanceSource(imageBitmapData);
      var luminance:BinaryBitmap =
          new BinaryBitmap(new GlobalHistogramBinarizer(source));
      var hints:HashTable = new HashTable();
      hints.Add(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.EAN_8);
      var reader:MultiFormatReader = new MultiFormatReader();
      var result:Result = reader.decode(luminance, hints);
      if (result != null) {
        var parsedResult:ParsedResult = ResultParser.parseResult(result);
        var resultText:String = parsedResult.getDisplayResult();
        resultNumber = Number(resultText);
      }
      return resultNumber;
    }

    private function generateImage(resultBits:Array, imageWidth:Number,
        imageHeight:Number):Bitmap {
      var bitmapData:BitmapData = new BitmapData(imageWidth, imageHeight, false,
          0xFFFFFF);
      var bitmap:Bitmap = new Bitmap(bitmapData);
      for (var h:int = 0; h < bitmapData.height; h++) {
        var bmpdwidth:int = bitmapData.width;
        var padding:int = bmpdwidth * 0.12;
        var barsWidth:int = bmpdwidth - padding;
        for (var w:int = 0; w < barsWidth; w++) {
          if (resultBits[Math.round(w * (resultBits.length / barsWidth))]
              == 0) {
            bitmapData.setPixel(w + padding / 2, h, 0);
          }
          else {
            bitmapData.setPixel(w + padding / 2, h, 0xFFFFFF);
          }
        }
      }
      return bitmap;
    }

    private function encodeEAN8(contents:String):Array {
      var barcodeType:BarcodeFormat = BarcodeFormat.EAN_8;
      var myWriter:MultiFormatWriter = new MultiFormatWriter();
      try {
        var result:ByteMatrix = myWriter.encode(contents, barcodeType)
            as ByteMatrix;
      } catch (e:Error) {
        fail("An error occured when making the barcode: " + e);
      }
      var resultBits:Array = new Array(result.height());
      for (var i:int = 0; i < result.height(); i++) {
        resultBits[i] = result._get(i, 0);
      }
      return resultBits;
    }

    private function formatEAN8String(number:Number):String {
      var inputlength:Number = 8;
      var barCodeFormat:String = "EAN8";

      var contents:String = String(number);
      if (contents.length > inputlength) {
        fail(barCodeFormat + ' can only contain ' + inputlength + ' digits');
        return null;
      }
      while (contents.length < inputlength) {
        contents = "0" + contents;
      }
      return contents;
    }

    public function calculateEANChecksumDigit(number:Number):Number {
      var checksum:Number = 0;
      var weightedSumOfDigits:Number = 0;
      var weight:Number = 3;
      while (number > 0) {
        var lastDigit:int = number % 10;
        weightedSumOfDigits = weightedSumOfDigits + lastDigit * weight;
        number = (number - lastDigit) / 10;
        weight = weight == 3 ? 1 : 3;
      }
      var additionToTen:Number = weightedSumOfDigits % 10;
      if (additionToTen > 0) {
        checksum = 10 - additionToTen;
      }
      return checksum;
    }
  }
}
