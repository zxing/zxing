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

  /*
  static int[] getRSSwidths(int val, int n, int elements, int maxWidth, boolean noNarrow) {
    int[] widths = new int[elements];
    int bar;
    int narrowMask = 0;
    for (bar = 0; bar < elements - 1; bar++) {
      narrowMask |= 1 << bar;
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
   */

  public static int getRSSvalue(int[] widths, int maxWidth, boolean noNarrow) {
    int n = 0;
    for (int width : widths) {
      n += width;
    }
    int val = 0;
    int narrowMask = 0;
    int elements = widths.length;
    for (int bar = 0; bar < elements - 1; bar++) {
      int elmWidth;
      for (elmWidth = 1, narrowMask |= 1 << bar;
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
    return val;
  }

  private static int combins(int n, int r) {
    int maxDenom;
    int minDenom;
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
    return val;
  }

}
