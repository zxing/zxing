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

package com.google.zxing.oned.rss;

/** Adapted from listings in ISO/IEC 24724 Appendix B and Appendix G. */
public final class RSSUtils {

  private RSSUtils() {}

  static int[] getRSSwidths(int val, int n, int elements, int maxWidth, boolean noNarrow) {
    int[] widths = new int[elements];
    int bar;
    int narrowMask = 0;
    for (bar = 0; bar < elements - 1; bar++) {
      narrowMask |= (1 << bar);
      int elmWidth = 1;
      int subVal;
      while (true) {
        subVal = combins(n - elmWidth - 1, elements - bar - 2);
        if (noNarrow && (narrowMask == 0) &&
            (n - elmWidth - (elements - bar - 1) >= elements - bar - 1)) {
          subVal -= combins(n - elmWidth - (elements - bar), elements - bar - 2);
        }
        if (elements - bar - 1 > 1) {
          int lessVal = 0;
          for (int mxwElement = n - elmWidth - (elements - bar - 2);
               mxwElement > maxWidth;
               mxwElement--) {
            lessVal += combins(n - elmWidth - mxwElement - 1, elements - bar - 3);
          }
          subVal -= lessVal * (elements - 1 - bar);
        } else if (n - elmWidth > maxWidth) {
          subVal--;
        }
        val -= subVal;
        if (val < 0) {
          break;
        }
        elmWidth++;
        narrowMask &= ~(1 << bar);
      }
      val += subVal;
      n -= elmWidth;
      widths[bar] = elmWidth;
    }
    widths[bar] = n;
    return widths;
  }

  public static int getRSSvalue(int[] widths, int maxWidth, boolean noNarrow) {
    int elements = widths.length;
    int n = 0;
    for (int i = 0; i < elements; i++) {
      n += widths[i];
    }
    int val = 0;
    int narrowMask = 0;
    for (int bar = 0; bar < elements - 1; bar++) {
      int elmWidth;
      for (elmWidth = 1, narrowMask |= (1 << bar);
           elmWidth < widths[bar];
           elmWidth++, narrowMask &= ~(1 << bar)) {
        int subVal = combins(n - elmWidth - 1, elements - bar - 2);
        if (noNarrow && (narrowMask == 0) &&
            (n - elmWidth - (elements - bar - 1) >= elements - bar - 1)) {
          subVal -= combins(n - elmWidth - (elements - bar),
                            elements - bar - 2);
        }
        if (elements - bar - 1 > 1) {
          int lessVal = 0;
          for (int mxwElement = n - elmWidth - (elements - bar - 2);
               mxwElement > maxWidth; mxwElement--) {
            lessVal += combins(n - elmWidth - mxwElement - 1,
                               elements - bar - 3);
          }
          subVal -= lessVal * (elements - 1 - bar);
        } else if (n - elmWidth > maxWidth) {
          subVal--;
        }
        val += subVal;
      }
      n -= elmWidth;
    }
    return (val);
  }

  static int combins(int n, int r) {
    int maxDenom, minDenom;
    if (n - r > r) {
      minDenom = r;
      maxDenom = n - r;
    } else {
      minDenom = n - r;
      maxDenom = r;
    }
    int val = 1;
    int j = 1;
    for (int i = n; i > maxDenom; i--) {
      val *= i;
      if (j <= minDenom) {
        val /= j;
        j++;
      }
    }
    while (j <= minDenom) {
      val /= j;
      j++;
    }
    return (val);
  }

  static int[] elements(int[] eDist, int N, int K) {
    int[] widths = new int[eDist.length + 2];
    int twoK = K << 1;
    widths[0] = 1;
    int i;
    int minEven = 10;
    int barSum = 1;
    for (i = 1; i < twoK - 2; i += 2) {
      widths[i] = eDist[i - 1] - widths[i - 1];
      widths[i + 1] = eDist[i] - widths[i];
      barSum += widths[i] + widths[i + 1];
      if (widths[i] < minEven) {
        minEven = widths[i];
      }
    }
    widths[twoK - 1] = N - barSum;
    if (widths[twoK - 1] < minEven) {
      minEven = widths[twoK - 1];
    }
    if (minEven > 1) {
      for (i = 0; i < twoK; i += 2) {
        widths[i] += minEven - 1;
        widths[i + 1] -= minEven - 1;
      }
    }
    return widths;
  }


}
