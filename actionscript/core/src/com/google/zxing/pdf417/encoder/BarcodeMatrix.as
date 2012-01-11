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
 * Holds all of the information for a barcode in a format where it can be easily accessable
 *
 * @author Jacob Haynes
 */
public final class BarcodeMatrix {

  private var matrix:Array;
  private var currentRow:int;
  private var height:int;
  private var width:int;

  /**
   * @param height the height of the matrix (Rows)
   * @param width  the width of the matrix (Cols)
   */
  public function BarcodeMatrix(height:int,width:int) 
  {
    matrix = new Array(height + 2);
    //Initializes the array to the correct width
    var matrixLength:int = matrix.length;
    var al:int = (width + 4) * 17 + 1;
    for (var i:int = 0; i < matrixLength; i++) 
    {
      matrix[i] = new BarcodeRow(al);
      
    }
    this.width = width * 17;
    this.height = height + 2;
    this.currentRow = 0;
  }

  public function set_value(x:int, y:int, value:*):void {
    matrix[y].set_value(x, value);
  }

  public function setMatrix(x:int, y:int, black:Boolean):void {
    set_value(x, y, (black ? 1 : 0));
  }

  public function startRow():void {
    ++currentRow;
  }

  public  function getCurrentRow():BarcodeRow {
    return (matrix[currentRow] as BarcodeRow);
  }

  public function getMatrix():Array {
    return getScaledMatrix(1, 1);
  }

  public function getScaledMatrix( xScale:int, yScale:int =-1):Array 
  {
  	if (yScale == -1) { yScale = xScale; } 
    var matrixOut:Array = new Array();//height * yScale)[width * xScale];
    var yMax:int = height * yScale;
    for (var ii:int = 0; ii < yMax; ii++) 
    {
    	var temp:Array = matrix[int(ii / yScale)].getScaledRow(xScale);
      	matrixOut[yMax - ii - 1] = temp
    }
    return matrixOut;
  }
  
  public function toString():String
  {
  	var resultString:String = "";
  	for (var i:int=0;i<matrix.length;i++)
  	{
  		resultString += matrix[i].toString()+"\n";
  	}
  	return resultString;
  }
  
  }
}
