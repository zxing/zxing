/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.qrcode.detector
{
   public class FinderPatternInfo
    { 
          protected  var bottomLeft:FinderPattern;
          protected  var topLeft:FinderPattern;
          protected  var topRight:FinderPattern;

          public function FinderPatternInfo( patternCenters:Array) {
            this.bottomLeft = patternCenters[0];
            this.topLeft = patternCenters[1];
            this.topRight = patternCenters[2];
          }

          public function getBottomLeft():FinderPattern
          {
            return bottomLeft;
          }

          public function getTopLeft():FinderPattern
          {
            return topLeft;
          }

          public function getTopRight():FinderPattern
          {
            return topRight;
          }
    
    }


}