package com.google.zxing.common.flexdatatypes
{
	// these comparators should reside in the classes but that didn's work for some reason.
	import com.google.zxing.datamatrix.detector.ResultPointsAndTransitionsComparator;
	import com.google.zxing.qrcode.detector.CenterComparator;

	
	public class ArrayList
	{
		//BAS : made public for debugging
		public var _array:Array;
		public function ArrayList(siz:int=0)
		{
			this._array = new Array(siz);
		}
		public function get Capacity():int
		{
			return this._array.length;	
		}
		
		public function getObjectByIndex(index:int):Object
		{
			var obj:Object = this._array[index]; 
			return obj;	
		}

		public function setObjectByIndex(index:int,obj:Object):void
		{
			this._array[index] = obj; 
		}
		
		
		public function Contains(o:Object):Boolean
		{
			if (this._array.indexOf(o) != -1)
			{
				return true;
			}	
			else
			{
				return false;
			}
		}
		
		
		public function set Capacity(cap:int):void
		{
			// not needed;
		}
	
		public function AddRange(itemsToAdd:Array):void
		{
			// add this number of items
			var len:int = this._array.length;
			for (var i:int=0;i<itemsToAdd.length;i++)
			{
				this._array.push(new Object());
			}
		}
		
		public function indexOf(o:Object):int
		{
			return this._array.indexOf(o);	
		}
		
		public function RemoveRange(newSize:int,itemsToRemove:int):void
		{
			// remove the items
			var tmpAr:Object;
			for (var i:int=0;i<itemsToRemove;i++)
			{
				// remove the item with this index
				tmpAr = this._array.pop();
			}
		}
		
		public function get Count():int
		{
			return this._array.length;		
		}
		
		public function Add(item:Object):void
		{
			
			this._array.push(item);
		}
		
		public function addElement(item:Object):void
		{
			this.Add(item);
		}
		
		public function get length():int
		{
			return this._array.length;	
		}
		
		public function sort_ResultPointsAndTransitionsComparator():void
		{
			this._array.sort(ResultPointsAndTransitionsComparator.compare);
			//this._array.sort(args);
		}
		
		public function sort_CenterComparator():void
		{
			this._array.sort(CenterComparator.compare);
		}
		
		public function size():int
		{
			return this._array.length;
		}
		
		public function elementAt(index:int):Object
		{
			return this._array[index];	
		}
		
		public function isEmpty():Boolean
		{
			if (this._array.length == 0)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	
	}
	
}