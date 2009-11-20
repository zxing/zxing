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
	public class MultiFormatOneDReader extends AbstractOneDReader
    { 
    	import com.google.zxing.common.BitArray;
    	import com.google.zxing.common.flexdatatypes.ArrayList;
    	import com.google.zxing.common.flexdatatypes.HashTable;
    	import com.google.zxing.DecodeHintType;
		import com.google.zxing.ReaderException;
		import com.google.zxing.Result;
		import com.google.zxing.BarcodeFormat;
    	
          private var readers:ArrayList;
          
          public function MultiFormatOneDReader(hints:HashTable)
          {
            var possibleFormats:ArrayList = (hints == null ? null : hints.getValuesByKey(DecodeHintType.POSSIBLE_FORMATS));
            
            readers = new ArrayList();
            if (possibleFormats != null) {
              if (possibleFormats.Contains(BarcodeFormat.EAN_13) ||
                  possibleFormats.Contains(BarcodeFormat.UPC_A) ||
                  possibleFormats.Contains(BarcodeFormat.EAN_8) ||
                  possibleFormats.Contains(BarcodeFormat.UPC_E))
              {
                readers.Add(new MultiFormatUPCEANReader(hints));
              }
              if (possibleFormats.Contains(BarcodeFormat.CODE_39)) 
              {
                  readers.Add(new Code39Reader());
              }
              if (possibleFormats.Contains(BarcodeFormat.CODE_128))
              {
                  readers.Add(new Code128Reader());
              }
              if (possibleFormats.Contains(BarcodeFormat.ITF))
              {
                  readers.Add(new ITFReader());
              }
            }
            if (readers.Count==0) 
            {
              readers.Add(new MultiFormatUPCEANReader(hints));
              readers.Add(new Code39Reader());
              readers.Add(new Code128Reader());
              readers.Add(new ITFReader());
              
            }
          }
          

          public override function  decodeRow(rowNumber:int,  row:BitArray, hints:Object):Result
          {
            var size:int = readers.Count;
            for (var i:int = 0; i < size; i++) {
              var reader:Object = readers.getObjectByIndex(i);
              try {
              	var res:Result = reader.decodeRow(rowNumber, row, hints);
                return res;
              } catch (re:ReaderException) {
                // continue
                var a:int=0;//BAS :needed for debugging
              }
            }

            throw new ReaderException("MultiFormatOneDReader : decodeRow : could not decode row");
          }
    
    }

}