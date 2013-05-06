/*
 * Copyright 2011 ZXing authors
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
   /// <author>Jacob Haynes</author>
   /// </summary>
   internal sealed class BarcodeRow
   {
      private sbyte[] row;
      //A tacker for position in the bar
      private int currentLocation;

      /// <summary>
      /// Creates a Barcode row of the width
      ///
      /// @param width
      /// </summary>
      internal BarcodeRow(int width)
      {
         this.row = new sbyte[width];
         currentLocation = 0;
      }

      /// <summary>
      /// Sets a specific location in the bar
      ///
      /// <param name="x">The location in the bar</param>
      /// <param name="value">Black if true, white if false;</param>
      /// </summary>
      internal sbyte this[int x]
      {
         get { return row[x]; }
         set { row[x] = value; }
      }

      /// <summary>
      /// Sets a specific location in the bar
      ///
      /// <param name="x">The location in the bar</param>
      /// <param name="black">Black if true, white if false;</param>
      /// </summary>
      internal void set(int x, bool black)
      {
         row[x] = (sbyte)(black ? 1 : 0);
      }

      /// <summary>
      /// <param name="black">A boolean which is true if the bar black false if it is white</param>
      /// <param name="width">How many spots wide the bar is.</param>
      /// </summary>
      internal void addBar(bool black, int width)
      {
         for (int ii = 0; ii < width; ii++)
         {
            set(currentLocation++, black);
         }
      }

      internal sbyte[] Row
      {
         get { return row; }
      }

      /// <summary>
      /// This function scales the row
      ///
      /// <param name="scale">How much you want the image to be scaled, must be greater than or equal to 1.</param>
      /// <returns>the scaled row</returns>
      /// </summary>
      internal sbyte[] getScaledRow(int scale)
      {
         sbyte[] output = new sbyte[row.Length * scale];
         for (int i = 0; i < output.Length; i++)
         {
            output[i] = row[i / scale];
         }
         return output;
      }
   }
}
