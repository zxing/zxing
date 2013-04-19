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

package com.google.zxing.pdf417;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.AdjustableBitMatrix;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.multi.MultipleBarcodeReader;
import com.google.zxing.pdf417.decoder.Decoder;
import com.google.zxing.pdf417.decoder.PDF417ScanningDecoder;
import com.google.zxing.pdf417.decoder.SimpleLog;
import com.google.zxing.pdf417.decoder.SimpleLog.LEVEL;
import com.google.zxing.pdf417.detector.Detector;
import com.google.zxing.pdf417.detector.DetectorNew;
import com.google.zxing.pdf417.detector.PDF417DetectorResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This implementation can detect and decode PDF417 codes in an image.
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 */
public final class PDF417Reader implements Reader, MultipleBarcodeReader {

  private static final ResultPoint[] NO_POINTS = new ResultPoint[0];

  private final Decoder decoder = new Decoder();

  /**
   * Locates and decodes a PDF417 code in an image.
   *
   * @return a String representing the content encoded by the PDF417 code
   * @throws NotFoundException if a PDF417 code cannot be found,
   * @throws FormatException if a PDF417 cannot be decoded
   */
  @Override
  public Result decode(BinaryBitmap image) throws NotFoundException, FormatException, ChecksumException {
    return decode(image, null);
  }

  @Override
  public Result decode(BinaryBitmap image, Map<DecodeHintType,?> hints) throws NotFoundException, FormatException,
      ChecksumException {
    Result[] result = decode(image, hints, false);
    if (result == null || result.length == 0 || result[0] == null) {
      throw NotFoundException.getNotFoundInstance();
    }
    return result[0];
  }

  @Override
  public Result[] decodeMultiple(BinaryBitmap image) throws NotFoundException {
    return decodeMultiple(image, null);
  }

  @Override
  public Result[] decodeMultiple(BinaryBitmap image, Map<DecodeHintType,?> hints) throws NotFoundException {
    try {
      return decode(image, hints, true);
    } catch (ReaderException e) {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  private Result[] decode(BinaryBitmap image, Map<DecodeHintType,?> hints, boolean multiple) throws NotFoundException,
      FormatException, ChecksumException {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSSSSS");
    List<Result> results = new ArrayList<Result>();
    boolean old = false;
    if (hints != null && hints.containsKey(DecodeHintType.PURE_BARCODE)) {
      BitMatrix bits = extractPureBits(image.getBlackMatrix());
      DecoderResult decoderResult = decoder.decode(bits);
      results.add(new Result(decoderResult.getText(), decoderResult.getRawBytes(), NO_POINTS, BarcodeFormat.PDF_417));
    } else if (old) {
      SimpleLog.log(LEVEL.DEVEL, "Before detect " + simpleDateFormat.format(new Date()));
      DetectorResult detectorResult = new Detector(image).detect();
      SimpleLog.log(LEVEL.DEVEL, "Before decode " + simpleDateFormat.format(new Date()));
      DecoderResult decoderResult = decoder.decode(detectorResult.getBits());
      SimpleLog.log(LEVEL.DEVEL, "After decode " + simpleDateFormat.format(new Date()));
      results.add(new Result(decoderResult.getText(), decoderResult.getRawBytes(), detectorResult.getPoints(),
          BarcodeFormat.PDF_417));
    } else {
      if (!(image.getBlackMatrix() instanceof AdjustableBitMatrix)) {
        SimpleLog.log(LEVEL.WARNING, "Warning, not using AdjustableBitMatrix");
        SimpleLog.log(LEVEL.ERROR, "Before detect " + simpleDateFormat.format(new Date()));
        PDF417DetectorResult detectorResult = new DetectorNew(image).detect(multiple);
        for (ResultPoint[] points : detectorResult.getPoints()) {
          SimpleLog.log(LEVEL.ERROR, "Before decode " + simpleDateFormat.format(new Date()));
          PDF417DecoderResult decoderResult = PDF417ScanningDecoder.decode(detectorResult.getBits(), points[4],
              points[5], points[6], points[7], getMinCodewordWidth(points), getMaxCodewordWidth(points));
          SimpleLog.log(LEVEL.ERROR, "After decode " + simpleDateFormat.format(new Date()));
          if (decoderResult == null) {
            throw NotFoundException.getNotFoundInstance();
          }
          Result result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points,
              BarcodeFormat.PDF_417);
          result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, decoderResult.getECLevel());
          result.putMetadata(ResultMetadataType.OTHER, decoderResult.getResultMetadata());
          results.add(result);
        }
      } else {
        results = decodeAdjustableBitMatrix(image, multiple);
      }
    }
    return results.toArray(new Result[results.size()]);
  }

  private List<Result> decodeAdjustableBitMatrix(BinaryBitmap image, boolean multiple) throws NotFoundException {
    List<Result> result = new ArrayList<Result>();
    AdjustableBitMatrix bitMatrix = (AdjustableBitMatrix) image.getBlackMatrix();
    DecoderResult decoderResult = null;
    int estimatedBlackPoint = bitMatrix.getBlackpoint();
    int maxRange = Math.min(estimatedBlackPoint, 255 - estimatedBlackPoint);
    if (bitMatrix.isBlackWhite()) {
      maxRange = 1;
    }
    int range = 0;
    boolean firstTime = true;
    while (range < maxRange) {
      int blackPoint;
      if (firstTime) {
        blackPoint = estimatedBlackPoint + range;
      } else {
        blackPoint = estimatedBlackPoint - range;
      }
      bitMatrix.setBlackpoint(blackPoint);
      try {
        SimpleLog.log(LEVEL.DEVEL, "Blackpoint: " + blackPoint);
        PDF417DetectorResult detectorResult = new DetectorNew(image).detect(multiple);
        for (ResultPoint[] points : detectorResult.getPoints()) {
          decoderResult = PDF417ScanningDecoder.decode(image.getBlackMatrix(), points[4], points[5], points[6],
              points[7], getMinCodewordWidth(points), getMaxCodewordWidth(points));
          result.add(new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.PDF_417));
        }
        break;
      } catch (FormatException e) {
        //System.out.println("Format Exception");
      } catch (ChecksumException e) {
        SimpleLog.log(LEVEL.DEVEL, "Checksum Exception");
      } catch (NotFoundException e) {
        //System.out.println("NotFound Exception");
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
      if (range == 0 || !firstTime) {
        range++;
        firstTime = true;
      } else if (firstTime) {
        firstTime = false;
      }
    }
    return result;
  }

  private void printPoints(ResultPoint[] points) {
    if (points == null) {
      System.out.println("Points is null");
      return;
    }
    int i = 0;
    for (ResultPoint point : points) {
      if (point == null) {
        System.out.println("Point[" + i + "] is null");
        i++;
        continue;
      }
      System.out.println("Point[" + i + "]: " + (int) point.getX() + ";" + (int) point.getY());
      i++;
    }
  }

  private static int getMaxWidth(ResultPoint p1, ResultPoint p2) {
    if (p1 == null || p2 == null) {
      return 0;
    }
    return (int) Math.abs(p1.getX() - p2.getX());
  }

  private static int getMinWidth(ResultPoint p1, ResultPoint p2) {
    if (p1 == null || p2 == null) {
      return Integer.MAX_VALUE;
    }
    return (int) Math.abs(p1.getX() - p2.getX());
  }

  // note, this is not 100% correct. The width of the stop bar on the right side is wider than the normal codeword width (18 instead
  // of 17 modules)
  private static int getMaxCodewordWidth(ResultPoint[] p) {
    return Math.max(Math.max(getMaxWidth(p[0], p[4]), getMaxWidth(p[6], p[2]) * 17 / 18),
        Math.max(getMaxWidth(p[1], p[5]), getMaxWidth(p[7], p[3]) * 17 / 18));
  }

  // note, this is not 100% correct. The width of the stop bar on the right side is wider than the normal codeword width (18 instead
  // of 17 modules)
  private static int getMinCodewordWidth(ResultPoint[] p) {
    return Math.min(Math.min(getMinWidth(p[0], p[4]), getMinWidth(p[6], p[2]) * 17 / 18),
        Math.min(getMinWidth(p[1], p[5]), getMinWidth(p[7], p[3]) * 17 / 18));
  }

  @Override
  public void reset() {
    // do nothing
  }

  /**
   * This method detects a code in a "pure" image -- that is, pure monochrome image
   * which contains only an unrotated, unskewed, image of a code, with some white border
   * around it. This is a specialized method that works exceptionally fast in this special
   * case.
   *
   * @see com.google.zxing.qrcode.QRCodeReader#extractPureBits(BitMatrix)
   * @see com.google.zxing.datamatrix.DataMatrixReader#extractPureBits(BitMatrix)
   */
  private static BitMatrix extractPureBits(BitMatrix image) throws NotFoundException {

    int[] leftTopBlack = image.getTopLeftOnBit();
    int[] rightBottomBlack = image.getBottomRightOnBit();
    if (leftTopBlack == null || rightBottomBlack == null) {
      throw NotFoundException.getNotFoundInstance();
    }

    int moduleSize = moduleSize(leftTopBlack, image);

    int top = leftTopBlack[1];
    int bottom = rightBottomBlack[1];
    int left = findPatternStart(leftTopBlack[0], top, image);
    int right = findPatternEnd(leftTopBlack[0], top, image);

    int matrixWidth = (right - left + 1) / moduleSize;
    int matrixHeight = (bottom - top + 1) / moduleSize;
    if (matrixWidth <= 0 || matrixHeight <= 0) {
      throw NotFoundException.getNotFoundInstance();
    }

    // Push in the "border" by half the module width so that we start
    // sampling in the middle of the module. Just in case the image is a
    // little off, this will help recover.
    int nudge = moduleSize >> 1;
    top += nudge;
    left += nudge;

    // Now just read off the bits
    BitMatrix bits = new BitMatrix(matrixWidth, matrixHeight);
    for (int y = 0; y < matrixHeight; y++) {
      int iOffset = top + y * moduleSize;
      for (int x = 0; x < matrixWidth; x++) {
        if (image.get(left + x * moduleSize, iOffset)) {
          bits.set(x, y);
        }
      }
    }
    return bits;
  }

  private static int moduleSize(int[] leftTopBlack, BitMatrix image) throws NotFoundException {
    int x = leftTopBlack[0];
    int y = leftTopBlack[1];
    int width = image.getWidth();
    while (x < width && image.get(x, y)) {
      x++;
    }
    if (x == width) {
      throw NotFoundException.getNotFoundInstance();
    }

    int moduleSize = (x - leftTopBlack[0]) >>> 3; // We've crossed left first bar, which is 8x
    if (moduleSize == 0) {
      throw NotFoundException.getNotFoundInstance();
    }

    return moduleSize;
  }

  private static int findPatternStart(int x, int y, BitMatrix image) throws NotFoundException {
    int width = image.getWidth();
    int start = x;
    // start should be on black
    int transitions = 0;
    boolean black = true;
    while (start < width - 1 && transitions < 8) {
      start++;
      boolean newBlack = image.get(start, y);
      if (black != newBlack) {
        transitions++;
      }
      black = newBlack;
    }
    if (start == width - 1) {
      throw NotFoundException.getNotFoundInstance();
    }
    return start;
  }

  private static int findPatternEnd(int x, int y, BitMatrix image) throws NotFoundException {
    int width = image.getWidth();
    int end = width - 1;
    // end should be on black
    while (end > x && !image.get(end, y)) {
      end--;
    }
    int transitions = 0;
    boolean black = true;
    while (end > x && transitions < 9) {
      end--;
      boolean newBlack = image.get(end, y);
      if (black != newBlack) {
        transitions++;
      }
      black = newBlack;
    }
    if (end == x) {
      throw NotFoundException.getNotFoundInstance();
    }
    return end;
  }
}
