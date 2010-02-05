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
namespace com.google.zxing.common
{
	
	/// <summary> <p>This is basically a substitute for <code>java.util.Collections</code>, which is not
	/// present in MIDP 2.0 / CLDC 1.1.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class Collections
	{
		
		private Collections()
		{
		}
		
		/// <summary> Sorts its argument (destructively) using insert sort; in the context of this package
		/// insertion sort is simple and efficient given its relatively small inputs.
		/// 
		/// </summary>
		/// <param name="vector">vector to sort
		/// </param>
		/// <param name="comparator">comparator to define sort ordering
		/// </param>
		public static void  insertionSort(System.Collections.ArrayList vector, Comparator comparator)
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