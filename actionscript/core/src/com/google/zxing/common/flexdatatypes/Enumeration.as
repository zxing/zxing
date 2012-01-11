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