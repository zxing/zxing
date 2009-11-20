package com.google.zxing.common.flexdatatypes
{
	import com.google.zxing.ReaderException;
	
	public class StringBuilder
	{
		public var _string:String = "";
		
		public function StringBuilder(ignore:int=0)
		{
				
		}

		public function charAt(index:int):String
		{
			return this._string.charAt(index);
		}

		
		public function setCharAt(index:int, char:String):void
		{
			var temp:Array = this._string.split("");
			temp[index] = char.charAt(0);
			this._string = temp.join(""); 
		}
		
		public function setLength(l:int):void
		{
			if (l == 0)
			{
				this._string = "";
			}
			else
			{
				throw new ReaderException("StringBuilder : setLength : only 0 supported");
			}
		}

		public function Append(o:Object,startIndex:int=-1,count:int=-1):void
		{
			if (startIndex == -1)
			{
				if (o is Array)
				{
					this._string = this._string + (o as Array).join("");
				}
				else
				{
					this._string = this._string + o.toString();
				}
			}
			else if (count == -1)
			{
				this._string = this._string + (o.toString()).substr(startIndex);
			}
			else
			{
				this._string = this._string + (o.toString()).substr(startIndex,count);
			}
		}
		
		public function ToString():String
		{
			return this._string;
		}
		
		public function get length():int
		{
			return this._string.length;
		}
		
		public function set length(size:int):void
		{
			if (size==0) { this._string = "";}
			else
			{
				throw new ReaderException("size can ony be set to 0");
			}
		}
		public function Insert (pos:int,o:Object):void
		{
			if (pos == 0)
			{
				this._string = o.toString() + this._string;
			}
			else
			{
				throw new ReaderException('pos not supported yet');
			}
		}
		
		public function Remove(startIndex:int,length:int):void
		{
			
			var leftPart:String = "";
			var rightPart:String = "";
			if (startIndex > 0) { leftPart = this._string.substring(0,startIndex); }
			if ((startIndex+length) < this._string.length) 
			{ rightPart = this._string.substr(startIndex+length); }
			this._string = leftPart + rightPart;
		}
		
		public function toString():String
		{
			return this._string;
		}
		
		public function deleteCharAt(index:int):void
		{
			var temp:Array = this._string.split("");
			var result:String = "";
			for(var i:int=0;i<temp.length;i++)
			{
				if (i!=index){result = result + (temp[i] as String); }
			}
			this._string = result;
		}
	}
}