// /*
//  * Copyright 2009 ZXing authors
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
using System;
using System.Collections.Generic;
using System.Linq;

using ZXing.Common;
using System.Text;


namespace ZXing.PDF417.Internal
{
    /// <summary>
    /// Represents a Column in the Detection Result
    /// </summary>
    /// <author>Guenther Grau (Java Core)</author>
    /// <author>Stephen Furlani (C# Port)</author>
    public class DetectionResultColumn
    {
        /// <summary>
        /// The maximum distance to search in the codeword array in both the positive and negative directions
        /// </summary>
        private static readonly int MAX_NEARBY_DISTANCE = 5;

        /// <summary>
        /// The Bounding Box around the column (in the BitMatrix)
        /// </summary>
        /// <value>The box.</value>
        public BoundingBox Box { get; private set; }

        /// <summary>
        /// The Codewords the Box encodes for, offset by the Box minY.
        /// Remember to Access this ONLY through GetCodeword(imageRow) if you're accessing it in that manner.
        /// </summary>
        /// <value>The codewords.</value>
        public Codeword[] Codewords { get; set; } // TODO convert this to a dictionary? Dictionary<imageRow, Codeword> ??

        /// <summary>
        /// Initializes a new instance of the <see cref="ZXing.PDF417.Internal.DetectionResultColumn"/> class.
        /// </summary>
        /// <param name="box">The Bounding Box around the column (in the BitMatrix)</param>
        public DetectionResultColumn(BoundingBox box)
        {
            this.Box = new BoundingBox(box);
            this.Codewords = new Codeword[Box.MaxY - Box.MinY + 1];
        }

        /// <summary>
        /// Converts the Image's Row to the index in the Codewords array
        /// </summary>
        /// <returns>The Codeword Index.</returns>
        /// <param name="imageRow">Image row.</param>
        public int IndexForRow(int imageRow)
        {
            return imageRow - Box.MinY;
        }

        /// <summary>
        /// Converts the Codeword array index into a Row in the Image (BitMatrix)
        /// </summary>
        /// <returns>The Image Row.</returns>
        /// <param name="codewordIndex">Codeword index.</param>
        public int RowForIndex(int codewordIndex)
        {
            return Box.MinY + codewordIndex;
        }

        /// <summary>
        /// Gets the codeword for a given row
        /// </summary>
        /// <returns>The codeword.</returns>
        /// <param name="imageRow">Image row.</param>
        public Codeword GetCodeword(int imageRow)
        {
            return Codewords[IndexForRow(imageRow)];
        }

        /// <summary>
        /// Gets the codeword closest to the specified row in the image
        /// </summary>
        /// <param name="imageRow">Image row.</param>
        public Codeword GetCodewordNearby(int imageRow)
        {
            Codeword codeword = GetCodeword(imageRow);
            if (codeword == null)
            {
                int index = IndexForRow(imageRow);

                // TODO verify that this LINQ works the same?
                // Codeword nearestCW = Codewords[(from n in Codewords.Select((cw, n) => n) where Codewords[n] != null select n).Aggregate((x, y) => Math.Abs(x - index) > Math.Abs(y - index) ? x : y)];

                int nearby;
                for (int i = 1; i < MAX_NEARBY_DISTANCE; i++)
                {
                    nearby = index - i;
                    if (nearby >= 0)
                    {
                        codeword = Codewords[nearby];
                        if (codeword != null)
                            break;
                    }

                    nearby = index + i;
                    if (nearby < Codewords.Length)
                    {
                        codeword = Codewords[nearby];
                        break;
                    }
                }
            }
            return codeword;
        }

        /// <summary>
        /// Sets the codeword for an image row
        /// </summary>
        /// <param name="imageRow">Image row.</param>
        /// <param name="codeword">Codeword.</param>
        public void SetCodeword(int imageRow, Codeword codeword)
        {
            Codewords[IndexForRow(imageRow)] = codeword;
        }


        /// <summary>
        /// Returns a <see cref="System.String"/> that represents the current <see cref="ZXing.PDF417.Internal.DetectionResultColumn"/>.
        /// </summary>
        /// <returns>A <see cref="System.String"/> that represents the current <see cref="ZXing.PDF417.Internal.DetectionResultColumn"/>.</returns>
        public override string ToString()
        {
            StringBuilder builder = new StringBuilder();
            int row = 0;
            foreach (var cw in Codewords)
            {
                if (cw == null)
                {
                    builder.AppendFormat("{0,3}:    |   \n", row++);
                } else
                {
                    builder.AppendFormat("{0,3}: {1,3}|{2,3}\n", row++, cw.RowNumber, cw.Value);
                }
            }
            return builder.ToString();
            // return "Valid Codewords: " + (from cw in Codewords where cw != null select cw).Count().ToString();
        }
    }
}

