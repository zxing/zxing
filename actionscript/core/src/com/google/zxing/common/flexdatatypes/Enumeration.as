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

package com.google.zxing.common.flexdatatypes
{
	public class Enumeration 
	{
		private var _arr:Array;
		
		public function isEmpty():Boolean
		{
			return this.getSize()==0?true:false;
		}

		public function getSize():int
		{
			return this._arr.length;
		}
		
		public function Enumeration(arr:Array)
		{
			this._arr = arr;
		}
		
		public function hasMoreElement():Boolean
		{
			return (!this.isEmpty());
		}
		
		public function nextElement():Object
		{
			return this._arr.shift();
		}
		
		
	}
}