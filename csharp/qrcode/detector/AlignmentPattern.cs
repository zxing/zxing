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
using System;
using com.google.zxing;
using com.google.zxing.common;

namespace com.google.zxing.qrcode.detector
{
    public sealed class AlignmentPattern : ResultPoint
    {
          private  float posX;
          private  float posY;
          private  float estimatedModuleSize;

          public AlignmentPattern(float posX, float posY, float estimatedModuleSize) {
            this.posX = posX;
            this.posY = posY;
            this.estimatedModuleSize = estimatedModuleSize;
          }

          public float getX() {
            return posX;
          }

          public float getY() {
            return posY;
          }

          /**
           * <p>Determines if this alignment pattern "about equals" an alignment pattern at the stated
           * position and size -- meaning, it is at nearly the same center with nearly the same size.</p>
           */
          public bool aboutEquals(float moduleSize, float i, float j) {
            return
                Math.Abs(i - posY) <= moduleSize &&
                    Math.Abs(j - posX) <= moduleSize &&
                    (Math.Abs(moduleSize - estimatedModuleSize) <= 1.0f ||
                        Math.Abs(moduleSize - estimatedModuleSize) / estimatedModuleSize <= 0.1f);
          }
    }
    

}