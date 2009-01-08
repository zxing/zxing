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
    using System;
    using System.Collections;
    using com.google.zxing.common;
    using com.google.zxing.qrcode.decoder;
    using com.google.zxing.qrcode.detector;

    public sealed class QRCodeReader: Reader
    { 
          private static  ResultPoint[] NO_POINTS = new ResultPoint[0];
          private  Decoder decoder = new Decoder();

          /**
           * Locates and decodes a QR code in an image.
           *
           * @return a String representing the content encoded by the QR code
           * @throws ReaderException if a QR code cannot be found, or cannot be decoded
           */
          public Result decode(MonochromeBitmapSource image) {
              try{
                return decode(image, null);
              }
              catch(Exception e){
                throw new ReaderException(e.Message);
              }
            
          }

          public Result decode(MonochromeBitmapSource image, Hashtable hints){  
              try{
                DecoderResult decoderResult;
                ResultPoint[] points;
                if (hints != null && hints.ContainsKey(DecodeHintType.PURE_BARCODE)) {
                  BitMatrix bits = extractPureBits(image);
                  decoderResult = decoder.decode(bits);
                  points = NO_POINTS;
                } else {
                  DetectorResult detectorResult = new Detector(image).detect(hints);
                  decoderResult = decoder.decode(detectorResult.getBits());
                  points = detectorResult.getPoints();
                }

                Result result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.QR_CODE);
                if (decoderResult.getByteSegments() != null) {
                  result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, decoderResult.getByteSegments());
                }
                return result;
              }catch(Exception e){             
                throw new ReaderException(e.Message);
              }
            
          }

          /**
           * This method detects a barcode in a "pure" image -- that is, pure monochrome image
           * which contains only an unrotated, unskewed, image of a barcode, with some white border
           * around it. This is a specialized method that works exceptionally fast in this special
           * case.
           */
          private static BitMatrix extractPureBits(MonochromeBitmapSource image){
            // Now need to determine module size in pixels

            int height = image.getHeight();
            int width = image.getWidth();
            int minDimension = Math.Min(height, width);

            // First, skip white border by tracking diagonally from the top left down and to the right:
            int borderWidth = 0;
            while (borderWidth < minDimension && !image.isBlack(borderWidth, borderWidth)) {
              borderWidth++;
            }
            if (borderWidth == minDimension) {
              throw new ReaderException();
            }

            // And then keep tracking across the top-left black module to determine module size
            int moduleEnd = borderWidth;
            while (moduleEnd < minDimension && image.isBlack(moduleEnd, moduleEnd)) {
              moduleEnd++;
            }
            if (moduleEnd == minDimension) {
              throw new ReaderException();
            }

            int moduleSize = moduleEnd - borderWidth;

            // And now find where the rightmost black module on the first row ends
            int rowEndOfSymbol = width - 1;
            while (rowEndOfSymbol >= 0 && !image.isBlack(rowEndOfSymbol, borderWidth)) {
              rowEndOfSymbol--;
            }
            if (rowEndOfSymbol < 0) {
              throw new ReaderException();
            }
            rowEndOfSymbol++;

            // Make sure width of barcode is a multiple of module size
            if ((rowEndOfSymbol - borderWidth) % moduleSize != 0) {
              throw new ReaderException();
            }
            int dimension = (rowEndOfSymbol - borderWidth) / moduleSize;

            // Push in the "border" by half the module width so that we start
            // sampling in the middle of the module. Just in case the image is a
            // little off, this will help recover.
            borderWidth += moduleSize >> 1;

            int sampleDimension = borderWidth + (dimension - 1) * moduleSize;
            if (sampleDimension >= width || sampleDimension >= height) {
              throw new ReaderException();
            }

            // Now just read off the bits
            BitMatrix bits = new BitMatrix(dimension);
            for (int i = 0; i < dimension; i++) {
              int iOffset = borderWidth + i * moduleSize;
              for (int j = 0; j < dimension; j++) {
                if (image.isBlack(borderWidth + j * moduleSize, iOffset)) {
                  bits.set(i, j);
                }
              }
            }
            return bits;
          }

    
    }

}