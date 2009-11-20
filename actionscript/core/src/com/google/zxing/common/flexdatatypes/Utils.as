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