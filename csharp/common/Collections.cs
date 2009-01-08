/*
* Copyright 2008 ZXing authors
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
namespace com.google.zxing.common
{
    using System;
    using System.Text;

    /// <summary> A class which wraps a 2D array of bytes. The default usage is signed. If you want to use it as a
    /// unsigned container, it's up to you to do byteValue & 0xff at each location.
    /// *
    /// JAVAPORT: I'm not happy about the argument ordering throughout the file, as I always like to have
    /// the horizontal component first, but this is for compatibility with the C++ code. The original
    /// code was a 2D array of ints, but since it only ever gets assigned -1, 0, and 1, I'm going to use
    /// less memory and go with bytes.
    /// *
    /// </summary>
    /// <author>  dswitkin@google.com (Daniel Switkin)
    /// 
    /// </author>
    public sealed class Collections
    {

        private Collections()
        {
        }

        /**
         * Sorts its argument (destructively) using insert sort; in the context of this package
         * insertion sort is simple and efficient given its relatively small inputs.
         *
         * @param vector vector to sort
         * @param comparator comparator to define sort ordering
         */
        public static void insertionSort(System.Collections.ArrayList vector, Comparator comparator)
        {
            int max = vector.Count;
			for (int i = 1; i < max; i++)
			{
				System.Object value_Renamed = vector[i];
				int j = i - 1;
				System.Object valueB;
				while (j >= 0 && comparator.compare((valueB = vector[j]), value_Renamed) > 0)
				{
					vector[j + 1] = valueB;
					j--;
				}
				vector[j + 1] = value_Renamed;
			}
        }
    }
}