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


package com.google.zxing.common
{
/**
 * <p>This is basically a substitute for <code>java.util.Collections</code>, which is not
 * present in MIDP 2.0 / CLDC 1.1.</p>
 *
 * @author Sean Owen
 */
    public class Collections
    {

		import com.google.zxing.common.flexdatatypes.ArrayList;

        public function Collections()
        {
        }

        /**
         * Sorts its argument (destructively) using insert sort; in the context of this package
         * insertion sort is simple and efficient given its relatively small inputs.
         *
         * @param vector vector to sort
         * @param comparator comparator to define sort ordering
         */
        public static function insertionSort( vector:ArrayList, comparator:Comparator ):void
        {
            var max:int = vector.Count;
			for (var i:int = 1; i < max; i++)
			{
				var valueA:Object = vector[i];
				var j:int = i - 1;
				var valueB:Object;
				while (j >= 0 && comparator.compare((valueB = vector[j]), valueA) > 0)
				{
					vector[j + 1] = valueB;
					j--;
				}
				vector[j + 1] = valueA;
			}
        }
    }
}