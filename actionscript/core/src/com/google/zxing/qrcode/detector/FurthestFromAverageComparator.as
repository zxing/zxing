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
	  import com.google.zxing.common.Comparator;
      public class FurthestFromAverageComparator
      {
      	public static var average:Number = 0;
          
          public static function setAverage(average:Number):void
          {
          		FurthestFromAverageComparator.average = average;
          }
          
          public static function compare(center1:Object, center2:Object):int
		  {
				var dA:Number = Math.abs((center2 as FinderPattern).getEstimatedModuleSize() - FurthestFromAverageComparator.average);
      			var dB:Number = Math.abs((center1 as FinderPattern).getEstimatedModuleSize() - FurthestFromAverageComparator.average);
      			return dA < dB ? -1 : dA == dB ? 0 : 1;
    		}
      }
}