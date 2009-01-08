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

    public sealed class UPCAReader : UPCEANReader
    {
          private UPCEANReader ean13Reader = new EAN13Reader();

          public Result decodeRow(int rowNumber, BitArray row, int[] startGuardRange) {
            return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, startGuardRange));
          }

          public Result decodeRow(int rowNumber, BitArray row, System.Collections.Hashtable hints) {
            return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, hints));
          }

          public Result decode(MonochromeBitmapSource image) {
            return maybeReturnResult(ean13Reader.decode(image));
          }

          public Result decode(MonochromeBitmapSource image, System.Collections.Hashtable hints) {
            return maybeReturnResult(ean13Reader.decode(image, hints));
          }

          private static Result maybeReturnResult(Result result) {
            string text = result.getText();
            if (text[0] == '0') {
              return new Result(text.Substring(1), null, result.getResultPoints(), BarcodeFormat.UPC_A);
            } else {
              throw new ReaderException();
            }
          }
    
    
    }
}