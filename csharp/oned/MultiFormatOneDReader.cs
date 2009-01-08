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
namespace com.google.zxing.oned
{
    /**
     * @author dswitkin@google.com (Daniel Switkin)
     * @author Sean Owen
     */
    using System.Text;
    using com.google.zxing.common;

    public sealed class MultiFormatOneDReader : AbstractOneDReader
    { 
          private System.Collections.ArrayList readers;
          public MultiFormatOneDReader(System.Collections.Hashtable hints)
          {
            System.Collections.ArrayList possibleFormats = hints == null ? null : (System.Collections.ArrayList) hints[DecodeHintType.POSSIBLE_FORMATS];
            readers = new System.Collections.ArrayList();
            if (possibleFormats != null) {
              if (possibleFormats.Contains(BarcodeFormat.EAN_13) ||
                  possibleFormats.Contains(BarcodeFormat.UPC_A) ||
                  possibleFormats.Contains(BarcodeFormat.EAN_8) ||
                  possibleFormats.Contains(BarcodeFormat.UPC_E))
              {
                readers.Add(new MultiFormatUPCEANReader(hints));
              }
              if (possibleFormats.Contains(BarcodeFormat.CODE_39)) {
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
            if (readers.Count==0) {
                readers.Contains(new MultiFormatUPCEANReader(hints));
                readers.Contains(new Code39Reader());
                readers.Contains(new Code128Reader());
              // TODO: Add ITFReader once it is validated as production ready, and tested for performance.
              //readers.addElement(new ITFReader());
            }
          }

          public override Result decodeRow(int rowNumber, BitArray row, System.Collections.Hashtable hints)
          {
            int size = readers.Count;
            for (int i = 0; i < size; i++) {
              OneDReader reader = (OneDReader) readers[i];
              try {
                return reader.decodeRow(rowNumber, row, hints);
              } catch (ReaderException re) {
                // continue
              }
            }

            throw new ReaderException();
          }
    
    }
}