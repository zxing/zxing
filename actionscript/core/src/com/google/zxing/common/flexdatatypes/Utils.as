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
	public class Utils
	{
		public function Utils()
		{
		}
		
		public static function startsWith(text:String, subtext:String):Boolean
		{
			if (text.substr(0,subtext.length) == subtext) { return true; }
			return false;
		}
		public static function endsWith(text:String, subtext:String):Boolean
		{
			if (text.substr(text.length-subtext.length) == subtext) { return true; }
			return false;
		}
		
		public static function isDigit(s:String):Boolean
		{
			return !isNaN(Number(s));
		}
		
		public static function arraycopy(source:Array, sourceoffset:int, target:Array, targetoffset:int, length:int):void
		{
			for (var i:int=sourceoffset;i<(sourceoffset+length);i++)
			{
				target[targetoffset++] = source[i];			
			}
		}
		

	}
}