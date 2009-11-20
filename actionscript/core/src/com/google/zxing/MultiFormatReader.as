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
package com.google.zxing
{
  import com.google.zxing.common.flexdatatypes.HashTable;
  import com.google.zxing.common.flexdatatypes.ArrayList;
  import com.google.zxing.oned.MultiFormatOneDReader;
  import com.google.zxing.pdf417.PDF417Reader;
  import com.google.zxing.qrcode.QRCodeReader;
  import com.google.zxing.datamatrix.DataMatrixReader;
  import com.google.zxing.Reader;
	/**
 * MultiFormatReader is a convenience class and the main entry point into the library for most uses.
 * By default it attempts to decode all barcode formats that the library supports. Optionally, you
 * can provide a hints object to request different behavior, for example only decoding QR codes.
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
	public class MultiFormatReader implements Reader
	{
	
	      protected var hints:HashTable;
          protected var readers:ArrayList;


          /**
           * Decode an image using the hints provided. Does not honor existing state.
           *
           * @param image The pixel data to decode
           * @param hints The hints to use, clearing the previous state.
           * @return The contents of the image
           * @throws ReaderException Any errors which occurred
           */
          public function decode( image:BinaryBitmap,  hints:HashTable=null):Result
          {
              setHints(hints);
              return  decodeInternal(image); 
          }

          /**
           * Decode an image using the state set up by calling setHints() previously. Continuous scan
           * clients will get a <b>large</b> speed increase by using this instead of decode().
           *
           * @param image The pixel data to decode
           * @return The contents of the image
           * @throws ReaderException Any errors which occurred
           */
          public function decodeWithState(image:BinaryBitmap):Result{
                // Make sure to set up the default state so we don't crash
                if (readers == null) {
                  setHints(null);
                }
                return decodeInternal(image);
          }

          /**
           * This method adds state to the MultiFormatReader. By setting the hints once, subsequent calls
           * to decodeWithState(image) can reuse the same set of readers without reallocating memory. This
           * is important for performance in continuous scan clients.
           *
           * @param hints The set of hints to use for subsequent calls to decode(image)
           */
          public function setHints(hints:HashTable):void 
          {
              this.hints = hints;

              var tryHarder:Boolean = hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER);
              var formats:ArrayList = ((hints == null) ? null : hints.getValuesByKey(DecodeHintType.POSSIBLE_FORMATS));
              readers = new ArrayList();
              if (formats != null)
              {
                  var addOneDReader:Boolean =
                      (formats.indexOf(BarcodeFormat.UPC_A) != -1)||
                      (formats.indexOf(BarcodeFormat.UPC_E) != -1)||
                      (formats.indexOf(BarcodeFormat.ITF) != -1)||
                      (formats.indexOf(BarcodeFormat.EAN_13) != -1)||
                      (formats.indexOf(BarcodeFormat.EAN_8) != -1)||
                      (formats.indexOf(BarcodeFormat.CODE_39) != -1)||
                      (formats.indexOf(BarcodeFormat.CODE_128) != -1);
                  // Put 1D readers upfront in "normal" mode

                  if (addOneDReader && !tryHarder)
                  {
                      readers.Add(new MultiFormatOneDReader(hints));
                  }

                  if (formats.indexOf(BarcodeFormat.QR_CODE) != -1)
                  {
                      readers.Add(new QRCodeReader());
                  }
			      if (formats.indexOf(BarcodeFormat.PDF417) != -1) {
			         readers.addElement(new PDF417Reader());
			       }

                  // TODO re-enable once Data Matrix is ready
                  if (formats.indexOf(BarcodeFormat.DATAMATRIX) != -1) {
                    readers.Add(new DataMatrixReader());
                  }
                  // At end in "try harder" mode
                  if (addOneDReader && tryHarder)
                  {
                      readers.Add(new MultiFormatOneDReader(hints));
                  }
              }

              if (readers.Count == 0)
              {
                  if (!tryHarder)
                  {
                  		var reader:MultiFormatOneDReader = new MultiFormatOneDReader(hints); 
                      	readers.Add(reader);
                  }
                  readers.Add(new QRCodeReader());
                  // TODO re-enable once Data Matrix is ready
                  readers.Add(new DataMatrixReader());

		      	  // TODO: Enable once PDF417 has passed QA
      			  readers.addElement(new PDF417Reader());

                  if (tryHarder)
                  {
                      readers.Add(new MultiFormatOneDReader(hints));
                  }
              }

          }

          private function decodeInternal( image:BinaryBitmap):Result 
          {
                  var size:int = readers.Count;
                  for (var i:int = 0; i < size; i++)
                  {
                      var reader:Reader = (readers.getObjectByIndex(i)) as Reader;
                      try
                      {
                      	  var res:Result = reader.decode(image, hints); 
                          return res; 
                      }
                      catch ( re:ReaderException)
                      {
                          // continue
                           var a:int=0;
                      }
                  }

              // no decoder could decode the barcode
              return null;
              //throw new ReaderException("MultiFormatReader : decodeInternal :could not decode");
          }
    
    
 	
	}
}