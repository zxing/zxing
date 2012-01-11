package com.google.zxing.common.flexdatatypes
{
	// these comparators should reside in the classes but that didn's work for some reason.
	import com.google.zxing.datamatrix.detector.ResultPointsAndTransitionsComparator;
	import com.google.zxing.qrcode.detector.CenterComparator;
	import com.google.zxing.qrcode.detector.FurthestFromAverageComparator;

	
	public class ArrayList
	{
		private var _array:Array;
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
		
		public function removeElementAt(index:int):void
		{
			var newArray:Array = new Array();
			for(var i:int=0;i<this._array.length;i++)
			{
				if (i != index) { newArray.push(this._array[i]); }
			}
			this._array = newArray;
		}
		
		public function setElementAt(elem:Object, index:int):void
		{
			this._array[index] = elem;
		}
		
		// limit size of array
		public function setSize(size:int):void
		{
			var newArray:Array = new Array();
			if (this._array.length > size)
			{
				for (var i:int=0;i<size;i++)
				{
					newArray[i] = this._array[i];	// bas : fixed .push
				}
				this._array = newArray;
			}
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
		
		public function sort_CenterComparator(average:Number):void
		{
			CenterComparator.setAverage(average);
			this._array.sort(CenterComparator.compare);
		}
		
		public function sort_FurthestFromAverageComparator(average:Number):void
		{
			FurthestFromAverageComparator.setAverage(average);
			this._array.sort(FurthestFromAverageComparator.compare);
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
		
		public function clearAll():void
		{
			this._array = new Array();
		}
		
		public function elements():Array
		{
			return this._array;
		}
		
		public function lastElement():Object
		{
			return this._array[this._array.length-1]; // bas : fixed this
		}
	
	}
	
}