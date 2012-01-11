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

package com.google.zxing.oned.rss
{

/** Adapted from listings in ISO/IEC 24724 Appendix B and Appendix G. */
public class RSSUtils {

  public function RSSUtils() {}

  public static function getRSSwidths(val:int, n:int, elements:int,maxWidth:int, noNarrow:Boolean):Array
  {
    var widths:Array = new Array(elements);
    var bar:int;
    var narrowMask:int = 0;
    for (bar = 0; bar < elements - 1; bar++) {
      narrowMask |= (1 << bar);
      var elmWidth:int = 1;
      var subVal:int;
      while (true) {
        subVal = combins(n - elmWidth - 1, elements - bar - 2);
        if (noNarrow && (narrowMask == 0) &&
            (n - elmWidth - (elements - bar - 1) >= elements - bar - 1)) {
          subVal -= combins(n - elmWidth - (elements - bar), elements - bar - 2);
        }
        if (elements - bar - 1 > 1) {
          var lessVal:int = 0;
          for (var mxwElement:int = n - elmWidth - (elements - bar - 2);
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

  public static function getRSSvalue(widths:Array, maxWidth:int, noNarrow:Boolean):int {
    var elements:int = widths.length;
    var n:int = 0;
    for (var i:int = 0; i < elements; i++) {
      n += widths[i];
    }
    var val:int = 0;
    var narrowMask:int = 0;
    for (var bar:int = 0; bar < elements - 1; bar++) {
      var elmWidth:int;
      for (elmWidth = 1, narrowMask |= (1 << bar);
           elmWidth < widths[bar];
           elmWidth++, narrowMask &= ~(1 << bar)) {
        var subVal:int = combins(n - elmWidth - 1, elements - bar - 2);
        if (noNarrow && (narrowMask == 0) &&
            (n - elmWidth - (elements - bar - 1) >= elements - bar - 1)) {
          subVal -= combins(n - elmWidth - (elements - bar),
                            elements - bar - 2);
        }
        if (elements - bar - 1 > 1) {
          var lessVal:int = 0;
          for (var mxwElement:int = n - elmWidth - (elements - bar - 2);
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

  public static function combins(n:int, r:int):int {
    var maxDenom:int;
    var minDenom:int;
    if (n - r > r) {
      minDenom = r;
      maxDenom = n - r;
    } else {
      minDenom = n - r;
      maxDenom = r;
    }
    var val:int = 1;
    var j:int = 1;
    for (var i:int = n; i > maxDenom; i--) {
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

  public static function elements(eDist:Array, N:int, K:int):Array {
    var widths:Array = new Array(eDist.length + 2);
    var twoK:int = K << 1;
    widths[0] = 1;
    var i:int;
    var minEven:int = 10;
    var barSum:int = 1;
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
}