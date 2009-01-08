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
    using com.google.zxing.common;  
    /**
     * <p>This interfaces captures addtional functionality that readers of
     * UPC/EAN family of barcodes should expose.</p>
     *
     * @author Sean Owen
     */

    public interface UPCEANReader : OneDReader 
    {
           /**
           * <p>Like {@link #decodeRow(int, BitArray, java.util.Hashtable)}, but
           * allows caller to inform method about where the UPC/EAN start pattern is
           * found. This allows this to be computed once and reused across many implementations.</p>
           */
          Result decodeRow(int rowNumber, BitArray row, int[] startGuardRange);
    }

}