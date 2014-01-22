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

package com.google.zxing.multi.qrcode.detector
{
	import com.google.zxing.common.Comparator;
	import com.google.zxing.qrcode.detector.FinderPattern;
	
	public class ModuleSizeComparator implements Comparator
	{
		  /**
   * A comparator that orders FinderPatterns by their estimated module size.
   */
    public function compare(center1:Object,center2:Object):int {
      var value:Number = ( center2 as FinderPattern).getEstimatedModuleSize() -
                    ( center1 as FinderPattern).getEstimatedModuleSize();
      return value < 0.0 ? -1 : value > 0.0 ? 1 : 0;
  		}


	}
}