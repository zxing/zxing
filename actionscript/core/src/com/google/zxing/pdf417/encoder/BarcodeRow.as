/*
 * Copyright 2011 ZXing authors
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

package com.google.zxing.pdf417.encoder
{

/**
 * @author Jacob Haynes
 */
public class BarcodeRow {

  private var row:Array;
  //A tacker for position in the bar
  private var currentLocation:int;

  /**
   * Creates a Barcode row of the width
   *
   * @param width
   */
  public function BarcodeRow(width:int ) {
    this.row = new Array(width);
    for (var k:int=0;k<width;k++) {this.row[k] = 0; } 
    currentLocation = 0;
  }

  /**
   * Sets a specific location in the bar
   *
   * @param x The location in the bar
   * @param black Black if true, white if false;
   */
  public function set_value(x:int, value:*):void 
  {
  	if (value is Boolean)
  	{
    	row[x] = value ? 1 : 0;
   }
   else
   {
   	row[x] = value;
   }
   
  }

  /**
   * @param black A boolean which is true if the bar black false if it is white
   * @param width How many spots wide the bar is.
   */
  public function addBar(black:Boolean, width:int):void {
    for (var ii:int = 0; ii < width; ii++) {
      set_value(currentLocation++, black);
    }
  }

  public function getRow():Array {
    return row;
  }

  /**
   * This function scales the row
   *
   * @param scale How much you want the image to be scaled, must be greater than or equal to 1.
   * @return the scaled row
   */
  public function getScaledRow(scale:int):Array {
    var output:Array = new Array(row.length * scale);
    for (var ii:int = 0; ii < row.length * scale; ii++) 
    {
      output[ii] = row[int(ii / scale)];
    }
    return output;
  }
  
  public function toString():String
  {
 	return this.row.toString(); 	 	
  }
}
}
