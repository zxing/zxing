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
package com.google.zxing.common
{
/**
 * <p>Represents a 2D matrix of bits. In function arguments below, and throughout the common
 * module, x is the column position, and y is the row position. The ordering is always x, y.
 * The origin is at the top-left.</p>
 *
 * <p>Internally the bits are represented in a 1-D array of 32-bit ints. However, each row begins
 * with a new int. This is done intentionally so that we can copy out a row into a BitArray very
 * efficiently.</p>
 *
 * <p>The ordering of bits is row-major. Within each int, the least significant bits are used first,
 * meaning they represent lower x values. This is compatible with BitArray's implementation.</p>
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
    public class BitMatrix
    {
    	import com.google.zxing.common.flexdatatypes.StringBuilder;
    	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    	
  		private var width:int;
  		private var height:int;
  		private var rowSize:int;
        public var bits:Array;
 

  		public function BitMatrix(width:int, o:Object= null) 
  		{
  			var height:int;
  			if (o == null)
  			{
  				height = width;
  			}
  			else if (o is int)
  			{
  				height = (o as int);
  			}
  			
    		if (width < 1 || height < 1) 
    		{
      			throw new IllegalArgumentException("common : BitMatrix : Both dimensions must be greater than 0");
    		}
    		this.width = width;
    		this.height = height;
    		var rowSize:int = width >> 5;
    		if ((width & 0x1f) != 0) 
    		{
      			rowSize++;
    		}
    		this.rowSize = rowSize;
    		bits = new Array(rowSize * height);
    		// BAS : initialize the array
    		for (var i:int=0;i<bits.length;i++) { bits[i] = 0; }
    		
  		}
  /**
   * <p>Gets the requested bit, where true means black.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   * @return value of given bit in matrix
   */
  public function _get(x:int,  y:int):Boolean {
    var offset:int = y * rowSize + (x >> 5);
    return ((bits[offset] >>> (x & 0x1f)) & 1) != 0;
  }

  /**
   * <p>Sets the given bit to true.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public function _set(x:int, y:int):void {
    var offset:int = y * rowSize + (x >> 5);
    bits[offset] |= 1 << (x & 0x1f);
  }

  /**
   * <p>Flips the given bit.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public function flip(x:int, y:int):void {
    var offset:int = y * rowSize + (x >> 5);
    bits[offset] ^= 1 << (x & 0x1f);
  }

  /**
   * Clears all bits (sets to false).
   */
  public function clear():void {
    var max:int = bits.length;
    for (var i:int = 0; i < max; i++) {
      bits[i] = 0;
    }
  }

  /**
   * <p>Sets a square region of the bit matrix to true.</p>
   *
   * @param left The horizontal position to begin at (inclusive)
   * @param top The vertical position to begin at (inclusive)
   * @param width The width of the region
   * @param height The height of the region
   */
  public function setRegion(left:int,top:int, width:int, height:int):void {
    if (top < 0 || left < 0) {
      throw new IllegalArgumentException("Common : BitMatrix : setRegion : Left and top must be nonnegative");
    }
    if (height < 1 || width < 1) {
      throw new IllegalArgumentException("Common : BitMatrix : setRegion : Height and width must be at least 1");
    }
    var right:int = left + width;
    var bottom:int = top + height;
    if (bottom > this.height || right > this.width) {
      throw new IllegalArgumentException("Common : BitMatrix : setRegion : The region must fit inside the matrix");
    }
    for (var y:int = top; y < bottom; y++) {
      var offset:int = y * rowSize;
      for (var x:int = left; x < right; x++) {
        bits[offset + (x >> 5)] |= 1 << (x & 0x1f);
      }
    }
  }

  /**
   * A fast method to retrieve one row of data from the matrix as a BitArray.
   *
   * @param y The row to retrieve
   * @param row An optional caller-allocated BitArray, will be allocated if null or too small
   * @return The resulting BitArray - this reference should always be used even when passing
   *         your own row
   */
  public function getRow(y:int, row:BitArray ):BitArray {
    if (row == null || row.getSize() < width) {
      row = new BitArray(width);
    }
    var offset:int = y * rowSize;
    for (var x:int = 0; x < rowSize; x++) {
      row.setBulk(x << 5, bits[offset + x]);
    }
    return row;
  }

  /**
   * @return The width of the matrix
   */
  public function getWidth():int {
    return width;
  }

  /**
   * @return The height of the matrix
   */
  public function getHeight():int {
    return height;
  }

  /**
   * This method is for compatibility with older code. It's only logical to call if the matrix
   * is square, so I'm throwing if that's not the case.
   *
   * @return row/column dimension of this matrix
   */
  public function getDimension():int {
    if (width != height) {
      throw new Error("Common : BitMatrix : getDimension :Can't call getDimension() on a non-square matrix");
    }
    return width;
  }

  public function toString():String {
    var result:StringBuilder  = new StringBuilder(height * (width + 1));
    for (var y:int = 0; y < height; y++) {
      for (var x:int = 0; x < width; x++) {
        result.Append(_get(x, y) ? "X " : "  ");
      }
      result.Append('\n');
    }
    return result.toString();
  }
}
}