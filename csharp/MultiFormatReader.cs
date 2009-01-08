/*
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

using System;
using System.Collections;
using com.google.zxing.qrcode;  

namespace com.google.zxing
{
    public sealed class MultiFormatReader : Reader
    { 
          private Hashtable hints;
          private ArrayList readers;

          /**
           * This version of decode honors the intent of Reader.decode(MonochromeBitmapSource) in that it
           * passes null as a hint to the decoders. However, that makes it inefficient to call repeatedly.
           * Use setHints() followed by decodeWithState() for continuous scan applications.
           *
           * @param image The pixel data to decode
           * @return The contents of the image
           * @throws ReaderException Any errors which occurred
           */
          public Result decode(MonochromeBitmapSource image){
              try{
                setHints(null);
                return decodeInternal(image);
              }
              catch(Exception e){
                throw new ReaderException(e.Message);
              }           
          }

          /**
           * Decode an image using the hints provided. Does not honor existing state.
           *
           * @param image The pixel data to decode
           * @param hints The hints to use, clearing the previous state.
           * @return The contents of the image
           * @throws ReaderException Any errors which occurred
           */
          public Result decode(MonochromeBitmapSource image, Hashtable hints){
              try{
                 setHints(hints);
                 return decodeInternal(image);
              }catch(Exception e){
                throw new ReaderException (e.Message);
              }           
          }

          /**
           * Decode an image using the state set up by calling setHints() previously. Continuous scan
           * clients will get a <b>large</b> speed increase by using this instead of decode().
           *
           * @param image The pixel data to decode
           * @return The contents of the image
           * @throws ReaderException Any errors which occurred
           */
          public Result decodeWithState(MonochromeBitmapSource image){
              try{
                // Make sure to set up the default state so we don't crash
                if (readers == null) {
                  setHints(null);
                }
                return decodeInternal(image);
              }catch(Exception e){
                throw new ReaderException(e.Message);
              }            
          }

          /**
           * This method adds state to the MultiFormatReader. By setting the hints once, subsequent calls
           * to decodeWithState(image) can reuse the same set of readers without reallocating memory. This
           * is important for performance in continuous scan clients.
           *
           * @param hints The set of hints to use for subsequent calls to decode(image)
           */
          public void setHints(Hashtable hints) {
              this.hints = hints;

              bool tryHarder = hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER);

              ArrayList possibleFormats = hints == null ? null : (ArrayList)hints[(DecodeHintType.POSSIBLE_FORMATS)];
              readers = new ArrayList();
              if (possibleFormats != null)
              {
                  bool addOneDReader =
                  possibleFormats.Contains(BarcodeFormat.UPC_A) ||
                      possibleFormats.Contains(BarcodeFormat.UPC_E) ||
                      possibleFormats.Contains(BarcodeFormat.EAN_13) ||
                      possibleFormats.Contains(BarcodeFormat.EAN_8) ||
                      possibleFormats.Contains(BarcodeFormat.CODE_39) ||
                      possibleFormats.Contains(BarcodeFormat.CODE_128);
                  // Put 1D readers upfront in "normal" mode

                  if (addOneDReader && !tryHarder)
                  {
                      //readers.Add(new MultiFormatOneDReader(hints));
                  }

                  if (possibleFormats.Contains(BarcodeFormat.QR_CODE))
                  {
                      readers.Add(new QRCodeReader());
                  }
                  // TODO re-enable once Data Matrix is ready
                  //if (possibleFormats.contains(BarcodeFormat.DATAMATRIX)) {
                  //  readers.addElement(new DataMatrixReader());
                  //}
                  // At end in "try harder" mode
                  if (addOneDReader && tryHarder)
                  {
                      //readers.Add(new MultiFormatOneDReader(hints));
                  }
              }

              if (readers.Count == 0)
              {
                  if (!tryHarder)
                  {
                      //readers.Add(new MultiFormatOneDReader(hints));
                  }
                  readers.Add(new QRCodeReader());
                  // TODO re-enable once Data Matrix is ready
                  // readers.addElement(new DataMatrixReader());
                  if (tryHarder)
                  {
                      //readers.Add(new MultiFormatOneDReader(hints));
                  }
              }
          }

          private Result decodeInternal(MonochromeBitmapSource image) {
              try
              {
                  int size = readers.Count;
                  for (int i = 0; i < size; i++)
                  {
                      Reader reader = (Reader)readers[i];
                      try
                      {
                          return reader.decode(image, hints);
                      }
                      catch (ReaderException re)
                      {
                          // continue
                      }
                  }

                  throw new ReaderException("");
              }
              catch (Exception e) {
                  throw new ReaderException(e.Message);
              }
          }
    
    
    }
}