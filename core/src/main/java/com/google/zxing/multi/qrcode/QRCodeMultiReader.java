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

package com.google.zxing.multi.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.multi.MultipleBarcodeReader;
import com.google.zxing.multi.qrcode.detector.MultiDetector;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.QRCodeDecoderMetaData;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

/**
 * This implementation can detect and decode multiple QR Codes in an image.
 *
 * @author Sean Owen
 * @author Hannes Erven
 */
public final class QRCodeMultiReader extends QRCodeReader implements MultipleBarcodeReader {

  private static final Result[] EMPTY_RESULT_ARRAY = new Result[0];
  private static final ResultPoint[] NO_POINTS = new ResultPoint[0];

  @Override
  public Result[] decodeMultiple(BinaryBitmap image) throws NotFoundException {
    return decodeMultiple(image, null);
  }

  @Override
  public Result[] decodeMultiple(BinaryBitmap image, Map<DecodeHintType,?> hints) throws NotFoundException {
    List<Result> results = new ArrayList<>();
    DetectorResult[] detectorResults = new MultiDetector(image.getBlackMatrix()).detectMulti(hints);
    for (DetectorResult detectorResult : detectorResults) {
      try {
        DecoderResult decoderResult = getDecoder().decode(detectorResult.getBits(), hints);
        ResultPoint[] points = detectorResult.getPoints();
        // If the code was mirrored: swap the bottom-left and the top-right points.
        if (decoderResult.getOther() instanceof QRCodeDecoderMetaData) {
          ((QRCodeDecoderMetaData) decoderResult.getOther()).applyMirroredCorrection(points);
        }
        Result result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points,
                                   BarcodeFormat.QR_CODE);
        List<byte[]> byteSegments = decoderResult.getByteSegments();
        if (byteSegments != null) {
          result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, byteSegments);
        }
        String ecLevel = decoderResult.getECLevel();
        if (ecLevel != null) {
          result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
        }
        if (decoderResult.hasStructuredAppend()) {
          result.putMetadata(ResultMetadataType.STRUCTURED_APPEND_SEQUENCE,
                             decoderResult.getStructuredAppendSequenceNumber());
          result.putMetadata(ResultMetadataType.STRUCTURED_APPEND_PARITY,
                             decoderResult.getStructuredAppendParity());
        }
        results.add(result);
      } catch (ReaderException re) {
        // ignore and continue
      }
    }
    if (results.isEmpty()) {
      return EMPTY_RESULT_ARRAY;
    } else {
      results = processStructuredAppend(results);
      return results.toArray(EMPTY_RESULT_ARRAY);
    }
  }

  static List<Result> processStructuredAppend(List<Result> results) {
    List<Result> newResults = new ArrayList<>();
    List<Result> saResults = new ArrayList<>();
    for (Result result : results) {
      if (result.getResultMetadata().containsKey(ResultMetadataType.STRUCTURED_APPEND_SEQUENCE)) {
        saResults.add(result);
      } else {
        newResults.add(result);
      }
    }
    if (saResults.isEmpty()) {
      return results;
    }

    // sort and concatenate the SA list items
    Collections.sort(saResults, new SAComparator());
    StringBuilder newText = new StringBuilder();
    ByteArrayOutputStream newRawBytes = new ByteArrayOutputStream();
    ByteArrayOutputStream newByteSegment = new ByteArrayOutputStream();
    for (Result saResult : saResults) {
      newText.append(saResult.getText());
      byte[] saBytes = saResult.getRawBytes();
      newRawBytes.write(saBytes, 0, saBytes.length);
      @SuppressWarnings("unchecked")
      Iterable<byte[]> byteSegments =
          (Iterable<byte[]>) saResult.getResultMetadata().get(ResultMetadataType.BYTE_SEGMENTS);
      if (byteSegments != null) {
        for (byte[] segment : byteSegments) {
          newByteSegment.write(segment, 0, segment.length);
        }
      }
    }

    Result newResult = new Result(newText.toString(), newRawBytes.toByteArray(), NO_POINTS, BarcodeFormat.QR_CODE);
    if (newByteSegment.size() > 0) {
      newResult.putMetadata(ResultMetadataType.BYTE_SEGMENTS, Collections.singletonList(newByteSegment.toByteArray()));
    }
    newResults.add(newResult);
    return newResults;
  }

  private static final class SAComparator implements Comparator<Result>, Serializable {
    @Override
    public int compare(Result a, Result b) {
      int aNumber = (int) a.getResultMetadata().get(ResultMetadataType.STRUCTURED_APPEND_SEQUENCE);
      int bNumber = (int) b.getResultMetadata().get(ResultMetadataType.STRUCTURED_APPEND_SEQUENCE);
      return Integer.compare(aNumber, bNumber);
    }
  }

}
