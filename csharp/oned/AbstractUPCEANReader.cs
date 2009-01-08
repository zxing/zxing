/*
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
namespace com.google.zxing.oned
{
    using System;
    using System.Text;
    using com.google.zxing.common;

    /**
     * <p>Encapsulates functionality and implementation that is common to UPC and EAN families
     * of one-dimensional barcodes.</p>
     *
     * @author dswitkin@google.com (Daniel Switkin)
     * @author Sean Owen
     * @author alasdair@google.com (Alasdair Mackintosh)
     */

    public abstract class AbstractUPCEANReader : AbstractOneDReader,UPCEANReader
    { 
          private static  int MAX_AVG_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42f);
          private static  int MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7f);

          /**
           * Start/end guard pattern.
           */
          private static  int[] START_END_PATTERN = {1, 1, 1,};

          /**
           * Pattern marking the middle of a UPC/EAN pattern, separating the two halves.
           */
          public static  int[] MIDDLE_PATTERN = {1, 1, 1, 1, 1};

          /**
           * "Odd", or "L" patterns used to encode UPC/EAN digits.
           */
          public static  int[][] L_PATTERNS = new int[][]{
              new int[]{3, 2, 1, 1}, // 0
              new int[]{2, 2, 2, 1}, // 1
              new int[]{2, 1, 2, 2}, // 2
              new int[]{1, 4, 1, 1}, // 3
              new int[]{1, 1, 3, 2}, // 4
              new int[]{1, 2, 3, 1}, // 5
              new int[]{1, 1, 1, 4}, // 6
              new int[]{1, 3, 1, 2}, // 7
              new int[]{1, 2, 1, 3}, // 8
              new int[]{3, 1, 1, 2}  // 9
          };

          /**
           * As above but also including the "even", or "G" patterns used to encode UPC/EAN digits.
           */
          public static  int[][] L_AND_G_PATTERNS=new int[20][];

          //static {
          //  L_AND_G_PATTERNS = new int[20][];
          //  for (int i = 0; i < 10; i++) {
          //    L_AND_G_PATTERNS[i] = L_PATTERNS[i];
          //  }
          //  for (int i = 10; i < 20; i++) {
          //    int[] widths = L_PATTERNS[i - 10];
          //    int[] reversedWidths = new int[widths.length];
          //    for (int j = 0; j < widths.length; j++) {
          //      reversedWidths[j] = widths[widths.length - j - 1];
          //    }
          //    L_AND_G_PATTERNS[i] = reversedWidths;
          //  }
          //}

          private  StringBuilder decodeRowStringBuffer;

          protected AbstractUPCEANReader() {
            for (int i = 0; i < 10; i++) {
              L_AND_G_PATTERNS[i] = L_PATTERNS[i];
            }
            for (int i = 10; i < 20; i++) {
              int[] widths = L_PATTERNS[i - 10];
              int[] reversedWidths = new int[widths.Length];
              for (int j = 0; j < widths.Length; j++) {
                reversedWidths[j] = widths[widths.Length - j - 1];
              }
              L_AND_G_PATTERNS[i] = reversedWidths;
            }
            decodeRowStringBuffer = new StringBuilder(20);
          }

          public static int[] findStartGuardPattern(BitArray row) {
            bool foundStart = false;
            int[] startRange = null;
            int nextStart = 0;
            while (!foundStart) {
              startRange = findGuardPattern(row, nextStart, false, START_END_PATTERN);
              int start = startRange[0];
              nextStart = startRange[1];
              // Make sure there is a quiet zone at least as big as the start pattern before the barcode. If
              // this check would run off the left edge of the image, do not accept this barcode, as it is
              // very likely to be a false positive.
              int quietStart = start - (nextStart - start);
              if (quietStart >= 0) {
                foundStart = row.isRange(quietStart, start, false);
              }
            }
            return startRange;
          }

          public override Result decodeRow(int rowNumber, BitArray row, System.Collections.Hashtable hints) {
            return decodeRow(rowNumber, row, findStartGuardPattern(row));
          }

          public  Result decodeRow(int rowNumber, BitArray row, int[] startGuardRange) {
            StringBuilder result = decodeRowStringBuffer;
            result.Length = 0;
            int endStart = decodeMiddle(row, startGuardRange, result);
            int[] endRange = decodeEnd(row, endStart);

            // Make sure there is a quiet zone at least as big as the end pattern after the barcode. The
            // spec might want more whitespace, but in practice this is the maximum we can count on.
            int end = endRange[1];
            int quietEnd = end + (end - endRange[0]);
            if (quietEnd >= row.getSize() || !row.isRange(end, quietEnd, false)) {
              throw new ReaderException();
            }

            String resultString = result.ToString();
            if (!checkChecksum(resultString)) {
              throw new ReaderException();
            }

            float left = (float) (startGuardRange[1] + startGuardRange[0]) / 2.0f;
            float right = (float) (endRange[1] + endRange[0]) / 2.0f;
            return new Result(resultString,
                null, // no natural byte representation for these barcodes
                new ResultPoint[]{
                    new GenericResultPoint(left, (float) rowNumber),
                    new GenericResultPoint(right, (float) rowNumber)},
                getBarcodeFormat());
          }

          public abstract BarcodeFormat getBarcodeFormat();

          /**
           * @return {@link #checkStandardUPCEANChecksum(String)} 
           */
          public bool checkChecksum(String s) {
            return checkStandardUPCEANChecksum(s);
          }

          /**
           * Computes the UPC/EAN checksum on a string of digits, and reports
           * whether the checksum is correct or not.
           *
           * @param s string of digits to check
           * @return true iff string of digits passes the UPC/EAN checksum algorithm
           * @throws ReaderException if the string does not contain only digits
           */
          public static bool checkStandardUPCEANChecksum(String s) {
            int length = s.Length;
            if (length == 0) {
              return false;
            }

            int sum = 0;
            for (int i = length - 2; i >= 0; i -= 2) {
              int digit = (int) s[i] - (int) '0';
              if (digit < 0 || digit > 9) {
                throw new ReaderException();
              }
              sum += digit;
            }
            sum *= 3;
            for (int i = length - 1; i >= 0; i -= 2) {
              int digit = (int) s[i] - (int) '0';
              if (digit < 0 || digit > 9) {
                throw new ReaderException();
              }
              sum += digit;
            }
            return sum % 10 == 0;
          }

          /**
           * Subclasses override this to decode the portion of a barcode between the start and end guard patterns.
           *
           * @param row row of black/white values to search
           * @param startRange start/end offset of start guard pattern
           * @param resultString {@link StringBuffer} to append decoded chars to
           * @return horizontal offset of first pixel after the "middle" that was decoded
           * @throws ReaderException if decoding could not complete successfully
           */
          protected abstract int decodeMiddle(BitArray row, int[] startRange, StringBuilder resultString);

          int[] decodeEnd(BitArray row, int endStart) {
            return findGuardPattern(row, endStart, false, START_END_PATTERN);
          }

          /**
           * @param row row of black/white values to search
           * @param rowOffset position to start search
           * @param whiteFirst if true, indicates that the pattern specifies white/black/white/...
           * pixel counts, otherwise, it is interpreted as black/white/black/...
           * @param pattern pattern of counts of number of black and white pixels that are being
           * searched for as a pattern
           * @return start/end horizontal offset of guard pattern, as an array of two ints
           * @throws ReaderException if pattern is not found
           */
          public static int[] findGuardPattern(BitArray row, int rowOffset, bool whiteFirst, int[] pattern)
              {
            int patternLength = pattern.Length;
            int[] counters = new int[patternLength];
            int width = row.getSize();
            bool isWhite = false;
            while (rowOffset < width) {
              isWhite = !row.get(rowOffset);
              if (whiteFirst == isWhite) {
                break;
              }
              rowOffset++;
            }

            int counterPosition = 0;
            int patternStart = rowOffset;
            for (int x = rowOffset; x < width; x++) {
              bool pixel = row.get(x);
              if ((!pixel && isWhite) || (pixel && !isWhite)) {
                counters[counterPosition]++;
              } else {
                if (counterPosition == patternLength - 1) {
                  if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
                    return new int[]{patternStart, x};
                  }
                  patternStart += counters[0] + counters[1];
                  for (int y = 2; y < patternLength; y++) {
                    counters[y - 2] = counters[y];
                  }
                  counters[patternLength - 2] = 0;
                  counters[patternLength - 1] = 0;
                  counterPosition--;
                } else {
                  counterPosition++;
                }
                counters[counterPosition] = 1;
                isWhite = !isWhite;
              }
            }
            throw new ReaderException();
          }

          /**
           * Attempts to decode a single UPC/EAN-encoded digit.
           *
           * @param row row of black/white values to decode
           * @param counters the counts of runs of observed black/white/black/... values
           * @param rowOffset horizontal offset to start decoding from
           * @param patterns the set of patterns to use to decode -- sometimes different encodings
           * for the digits 0-9 are used, and this indicates the encodings for 0 to 9 that should
           * be used
           * @return horizontal offset of first pixel beyond the decoded digit
           * @throws ReaderException if digit cannot be decoded
           */
          public static int decodeDigit(BitArray row, int[] counters, int rowOffset, int[][] patterns)
              {
            recordPattern(row, rowOffset, counters);
            int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
            int bestMatch = -1;
            int max = patterns.Length;
            for (int i = 0; i < max; i++) {
              int[] pattern = patterns[i];
              int variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
              if (variance < bestVariance) {
                bestVariance = variance;
                bestMatch = i;
              }
            }
            if (bestMatch >= 0) {
              return bestMatch;
            } else {
              throw new ReaderException();
            }
          }

    
    }


}