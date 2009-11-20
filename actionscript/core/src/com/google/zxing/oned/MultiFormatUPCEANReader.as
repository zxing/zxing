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
	public class MultiFormatUPCEANReader extends AbstractOneDReader
    { 
    	import com.google.zxing.common.BitArray;
    	import com.google.zxing.common.flexdatatypes.ArrayList;
    	import com.google.zxing.common.flexdatatypes.HashTable;
    	import com.google.zxing.BarcodeFormat;
		import com.google.zxing.DecodeHintType;
		import com.google.zxing.ReaderException;
		import com.google.zxing.Result;

    	
          private var readers:ArrayList;
          public function MultiFormatUPCEANReader(hints:HashTable) 
          {
            var possibleFormats:ArrayList = (hints == null ? null : hints.getValuesByKey(DecodeHintType.POSSIBLE_FORMATS));
            readers = new ArrayList();
            if (possibleFormats != null) {
              if (possibleFormats.Contains(BarcodeFormat.EAN_13)) {
                readers.Add(new EAN13Reader());
              } else if (possibleFormats.Contains(BarcodeFormat.UPC_A)) {
                readers.Add(new UPCAReader());
              }
              if (possibleFormats.Contains(BarcodeFormat.EAN_8)) {
                readers.Add(new EAN8Reader());
              }
              if (possibleFormats.Contains(BarcodeFormat.UPC_E)) {
                readers.Add(new UPCEReader());
              }
            }
            if (readers.Count==0) {
              readers.Add(new EAN13Reader());
              // UPC-A is covered by EAN-13
              readers.Add(new EAN8Reader());
              readers.Add(new UPCEReader());
            }
          }

          public override function decodeRow(rowNumber:int, row:BitArray,hints:Object):Result 
          {
            // Compute this location once and reuse it on multiple implementations

            var startGuardPattern:Array = AbstractUPCEANReader.findStartGuardPattern(row);
            var size:int = readers.Count;
            for (var i:int = 0; i < size; i++) {
            	
              var reader:Object = readers.getObjectByIndex(i);
              
              var result:Result;
              try {
                result = reader.decodeRow(rowNumber, row, startGuardPattern);
              } 
              catch (re:ReaderException) 
              {
                continue;//
              }
              // Special case: a 12-digit code encoded in UPC-A is identical to a "0"
              // followed by those 12 digits encoded as EAN-13. Each will recognize such a code,
              // UPC-A as a 12-digit string and EAN-13 as a 13-digit string starting with "0".
              // Individually these are correct and their readers will both read such a code
              // and correctly call it EAN-13, or UPC-A, respectively.
              //
              // In this case, if we've been looking for both types, we'd like to call it
              // a UPC-A code. But for efficiency we only run the EAN-13 decoder to also read
              // UPC-A. So we special case it here, and convert an EAN-13 result to a UPC-A
              // result if appropriate.
              if ((result.getBarcodeFormat() == BarcodeFormat.EAN_13) && 
                (result.getText()).charAt(0) == '0') {
                return new Result(result.getText().substring(1), null, result.getResultPoints(), BarcodeFormat.UPC_A);
              }
              return result;
            }

            throw new ReaderException("MultiFormatUPCEANReader : decodeRow : could not decode row");
          }
    
	} 
}