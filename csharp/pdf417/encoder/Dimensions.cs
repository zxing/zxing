/*
 * Copyright 2012 ZXing authors
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

namespace ZXing.PDF417.Internal
{
   /// <summary>
   /// Data object to specify the minimum and maximum number of rows and columns for a PDF417 barcode.
   /// @author qwandor@google.com (Andrew Walbran)
   /// </summary>
   public sealed class Dimensions
   {
      private readonly int minCols;
      private readonly int maxCols;
      private readonly int minRows;
      private readonly int maxRows;

      /// <summary>
      /// Initializes a new instance of the <see cref="Dimensions"/> class.
      /// </summary>
      /// <param name="minCols">The min cols.</param>
      /// <param name="maxCols">The max cols.</param>
      /// <param name="minRows">The min rows.</param>
      /// <param name="maxRows">The max rows.</param>
      public Dimensions(int minCols, int maxCols, int minRows, int maxRows)
      {
         this.minCols = minCols;
         this.maxCols = maxCols;
         this.minRows = minRows;
         this.maxRows = maxRows;
      }

      /// <summary>
      /// Gets the min cols.
      /// </summary>
      public int MinCols
      {
         get { return minCols; }
      }

      /// <summary>
      /// Gets the max cols.
      /// </summary>
      public int MaxCols
      {
         get { return maxCols; }
      }

      /// <summary>
      /// Gets the min rows.
      /// </summary>
      public int MinRows
      {
         get { return minRows; }
      }

      /// <summary>
      /// Gets the max rows.
      /// </summary>
      public int MaxRows
      {
         get { return maxRows; }
      }
   }
}
