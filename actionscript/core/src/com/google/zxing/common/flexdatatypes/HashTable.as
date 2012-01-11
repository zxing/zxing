package com.google.zxing.common.flexdatatypes
{
	public class HashTable
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
		
		public function getIndexOf(key:Object):int
		{
			for (var i:String in this._arr)
			{
				if (this._arr[i][0] == key)
				{
					return i;
				}
			}
			return undefined;
		}
		
		public function getValueByIndex(index:int):Object
		{
			return this._arr[index][1];
		}
		
		public function getKeyByIndex(index:int):Object
		{
			return this._arr[index][0];
		}
		
		public function HashTable(siz:int=0)
		{
			this._arr = new Array(siz);
		}
		
		public function Add(key:Object, value:Object):void
		{
			var ta:Array = new Array(2);
			ta[0] = key;
			ta[1] = value;
			this._arr[this._arr.length] = ta;
		}
		
		public function _put(k:Object,v:Object):void
		{
			this.Add(k,v);
		}
		
		public function ContainsKey(key:Object):Boolean
		{
			//for (var i:int=0;i<this._arr.length;i++)
			for (var i:String in this._arr)
			{
				if (this._arr[i][0] == key) { return true; }
			}
			return false;
		}
		
		public function getValuesByKey(key:Object):ArrayList
		{
			var al:ArrayList = new ArrayList();
			//for (var i:int=0;i<this._arr.length;i++)
			for (var i:String in this._arr)
			{
				if (this._arr[i][0] == key)
				{
					al.Add(this._arr[i][1]);
				}
			}
			return al;
		}
		
		public function _get(key:Object):Object
		{
			return this.getValueByKey(key);
		}

		public function getValueByKey(key:Object):Object
		{
			var al:ArrayList = new ArrayList();
			//for (var i:int=0;i<this._arr.length;i++)
			for (var i:String in this._arr) 
			{
				if (this._arr[i][0] == key)
				{
					return this._arr[i][1];	
				}
			}
			return null;
		}

		public function setValue(key:Object,value:Object):void
		{
			//for (var i:int=0;i<this._arr.length;i++)
			for (var i:String in this._arr)
			{
				if (this._arr[i][0] == key)
				{
					this._arr[i][1] = value;
					return;		
				}
			}
		}

		public function getKeyByValue(value:Object):int
		{
			//for (var i:int=0;i<this._arr.length;i++)
			for (var i:String in this._arr)
			{
				if (this._arr[i][1] == value)
				{
					return this._arr[i][0];
				}
			}
			return -1;
		}

		public function containsKey(key:Object):Boolean
		{
			//for (var i:int=0;i<this._arr.length;i++)
			for (var i:String in this._arr)
			{
				if (this._arr[i][0] == key)
				{
					return true;
				}
				
			}
			return false;
		}
		
		public function keys():Array
		{
			var result:Array = new Array(this._arr.length);
			//for (var i:int=0;i<this._arr.length;i++)
			for (var i:String in this._arr)
			{
				result[i] = this._arr[i][0];
			}
			return result;
		}


	}
}