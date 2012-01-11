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
	public class MultiFormatOneDReader extends OneDReader
    { 
    	import com.google.zxing.common.BitArray;
    	import com.google.zxing.common.flexdatatypes.ArrayList;
    	import com.google.zxing.common.flexdatatypes.HashTable;
    	import com.google.zxing.DecodeHintType;
		import com.google.zxing.ReaderException;
		import com.google.zxing.Result;
		import com.google.zxing.BarcodeFormat;
		import com.google.zxing.oned.rss.RSS14Reader;
		import com.google.zxing.oned.rss.expanded.RSSExpandedReader;
    	
          private var readers:ArrayList;
          
          public function MultiFormatOneDReader(hints:HashTable)
          {
            var possibleFormats:ArrayList = (hints == null ? null : hints.getValuesByKey(DecodeHintType.POSSIBLE_FORMATS));
            var useCode39CheckDigit:Boolean = hints != null && hints._get(DecodeHintType.ASSUME_CODE_39_CHECK_DIGIT) != null;
            readers = new ArrayList();
            
            if (possibleFormats != null) 
            {
		      if (possibleFormats.Contains(BarcodeFormat.EAN_13) ||
		          possibleFormats.Contains(BarcodeFormat.UPC_A) ||
		          possibleFormats.Contains(BarcodeFormat.EAN_8) ||
		          possibleFormats.Contains(BarcodeFormat.UPC_E)) 
		      {
		        readers.addElement(new MultiFormatUPCEANReader(hints));
		      }
		      if (possibleFormats.Contains(BarcodeFormat.CODE_39)) 
		      {
		        readers.addElement(new Code39Reader(useCode39CheckDigit));
		      }
		      if (possibleFormats.Contains(BarcodeFormat.CODE_93)) 
		      {
		        readers.addElement(new Code93Reader());
		      }
		      if (possibleFormats.Contains(BarcodeFormat.CODE_128)) 
		      {
		        readers.addElement(new Code128Reader());
		      }
		      if (possibleFormats.Contains(BarcodeFormat.ITF)) 
		      {
		         readers.addElement(new ITFReader());
		      }
		      if (possibleFormats.Contains(BarcodeFormat.CODABAR)) 
		      {
		         readers.addElement(new CodaBarReader());
		      }
		      if (possibleFormats.Contains(BarcodeFormat.RSS_14)) 
		      {
		         readers.addElement(new RSS14Reader());
		      }
		      if (possibleFormats.Contains(BarcodeFormat.RSS_EXPANDED))
		      {
		        readers.addElement(new RSSExpandedReader());
		      }
		    }
		    if (readers.isEmpty()) 
		    {
		      readers.addElement(new MultiFormatUPCEANReader(hints));
		      readers.addElement(new Code39Reader());
		      readers.addElement(new CodaBarReader());
		      readers.addElement(new Code93Reader());
		      readers.addElement(new Code128Reader());
		      readers.addElement(new ITFReader());
		      readers.addElement(new RSS14Reader());
		      readers.addElement(new RSSExpandedReader());
		    }

          }
          

          public override function  decodeRow(rowNumber:Object,  row:BitArray, hints:Object):Result
          {
            var size:int = readers.Count;
            for (var i:int = 0; i < size; i++) {
              var reader:Object = readers.getObjectByIndex(i);
              try {
              	var res:Result = reader.decodeRow(rowNumber as Number, row, hints);
                return res;
              } catch (re:Error) {
                // continue
                var a:int=0;//BAS :needed for debugging
              }
            }

            throw new ReaderException("MultiFormatOneDReader : decodeRow : could not decode row");
          }
    
    }

}