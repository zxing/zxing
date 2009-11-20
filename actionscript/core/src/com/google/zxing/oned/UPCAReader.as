/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.oned
{
	
    public class UPCAReader extends UPCEANReader
    {
		import com.google.zxing.common.BitArray;
		import com.google.zxing.common.flexdatatypes.HashTable;
		import com.google.zxing.BarcodeFormat;
		import com.google.zxing.ReaderException;
		import com.google.zxing.Result;
		import com.google.zxing.BinaryBitmap;

          private var ean13Reader:EAN13Reader = new EAN13Reader();

		  public function decodeRow(rowNumber:Object, row:BitArray, o:Object):Result
		  {
		  	if (rowNumber is int)
		  	{
		  	if (o is Array)       { return decodeRow_Array(rowNumber as int,row,o as Array);}
		  	else if (o is HashTable) { return decodeRow_Hashtable(rowNumber as int, row, o as HashTable);}
		  	}
		  	else if (rowNumber is BinaryBitmap)
		  	{
		  		return this.decode_BinaryBitmap(rowNumber as BinaryBitmap);	
		  	}
		  	else { throw new Error('UPCAReader : decodeRow : unknown type of o');}
		  	return null;
		  } 
		  
          public override function decodeRow_Array(rowNumber:int, row:BitArray, startGuardRange:Array):Result {
            return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, startGuardRange));
          }

          public function decodeRow_Hashtable(rowNumber:int, row:BitArray, hints:Object):Result {
            return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, hints));
          }

          
  		  public function  decode_BinaryBitmap(image:BinaryBitmap, hints:HashTable=null):Result {
          	return maybeReturnResult(ean13Reader.decode(image, hints)); 
          }

          private static function maybeReturnResult(result:Result):Result {
            var text:String = result.getText();
            if (text.substr(0,1) == '0') {
              return new Result(text.substring(1), null, result.getResultPoints(), BarcodeFormat.UPC_A);
            } else {
              throw new ReaderException("UPCAReader : maybeReturnResult : first character ("+text[0]+") is not zero");
            }
          }
    }
}