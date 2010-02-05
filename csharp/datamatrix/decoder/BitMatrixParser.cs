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
using System;
using ReaderException = com.google.zxing.ReaderException;
using BitMatrix = com.google.zxing.common.BitMatrix;
namespace com.google.zxing.datamatrix.decoder
{
	
	/// <author>  bbrown@google.com (Brian Brown)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class BitMatrixParser
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'mappingBitMatrix '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private BitMatrix mappingBitMatrix;
		//UPGRADE_NOTE: Final was removed from the declaration of 'readMappingMatrix '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private BitMatrix readMappingMatrix;
		//UPGRADE_NOTE: Final was removed from the declaration of 'version '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private Version version;
		
		/// <param name="bitMatrix">{@link BitMatrix} to parse
		/// </param>
		/// <throws>  ReaderException if dimension is < 10 or > 144 or not 0 mod 2 </throws>
		internal BitMatrixParser(BitMatrix bitMatrix)
		{
			int dimension = bitMatrix.Dimension;
			if (dimension < 10 || dimension > 144 || (dimension & 0x01) != 0)
			{
				throw ReaderException.Instance;
			}
			
			version = readVersion(bitMatrix);
			this.mappingBitMatrix = extractDataRegion(bitMatrix);
			// TODO(bbrown): Make this work for rectangular symbols
			this.readMappingMatrix = new BitMatrix(this.mappingBitMatrix.Dimension);
		}
		
		/// <summary> <p>Creates the version object based on the dimension of the original bit matrix from 
		/// the datamatrix code.</p>
		/// 
		/// <p>See ISO 16022:2006 Table 7 - ECC 200 symbol attributes</p>
		/// 
		/// </summary>
		/// <param name="bitMatrix">Original {@link BitMatrix} including alignment patterns
		/// </param>
		/// <returns> {@link Version} encapsulating the Data Matrix Code's "version"
		/// </returns>
		/// <throws>  ReaderException if the dimensions of the mapping matrix are not valid </throws>
		/// <summary> Data Matrix dimensions.
		/// </summary>
		internal Version readVersion(BitMatrix bitMatrix)
		{
			
			if (version != null)
			{
				return version;
			}
			
			// TODO(bbrown): make this work for rectangular dimensions as well.
			int numRows = bitMatrix.Dimension;
			int numColumns = numRows;
			
			return Version.getVersionForDimensions(numRows, numColumns);
		}
		
		/// <summary> <p>Reads the bits in the {@link BitMatrix} representing the mapping matrix (No alignment patterns)
		/// in the correct order in order to reconstitute the codewords bytes contained within the
		/// Data Matrix Code.</p>
		/// 
		/// </summary>
		/// <returns> bytes encoded within the Data Matrix Code
		/// </returns>
		/// <throws>  ReaderException if the exact number of bytes expected is not read </throws>
		internal sbyte[] readCodewords()
		{
			
			sbyte[] result = new sbyte[version.TotalCodewords];
			int resultOffset = 0;
			
			int row = 4;
			int column = 0;
			// TODO(bbrown): Data Matrix can be rectangular, assuming square for now
			int numRows = mappingBitMatrix.Dimension;
			int numColumns = numRows;
			
			bool corner1Read = false;
			bool corner2Read = false;
			bool corner3Read = false;
			bool corner4Read = false;
			
			// Read all of the codewords
			do 
			{
				// Check the four corner cases
				if ((row == numRows) && (column == 0) && !corner1Read)
				{
					result[resultOffset++] = (sbyte) readCorner1(numRows, numColumns);
					row -= 2;
					column += 2;
					corner1Read = true;
				}
				else if ((row == numRows - 2) && (column == 0) && ((numColumns & 0x03) != 0) && !corner2Read)
				{
					result[resultOffset++] = (sbyte) readCorner2(numRows, numColumns);
					row -= 2;
					column += 2;
					corner2Read = true;
				}
				else if ((row == numRows + 4) && (column == 2) && ((numColumns & 0x07) == 0) && !corner3Read)
				{
					result[resultOffset++] = (sbyte) readCorner3(numRows, numColumns);
					row -= 2;
					column += 2;
					corner3Read = true;
				}
				else if ((row == numRows - 2) && (column == 0) && ((numColumns & 0x07) == 4) && !corner4Read)
				{
					result[resultOffset++] = (sbyte) readCorner4(numRows, numColumns);
					row -= 2;
					column += 2;
					corner4Read = true;
				}
				else
				{
					// Sweep upward diagonally to the right
					do 
					{
						if ((row < numRows) && (column >= 0) && !readMappingMatrix.get_Renamed(column, row))
						{
							result[resultOffset++] = (sbyte) readUtah(row, column, numRows, numColumns);
						}
						row -= 2;
						column += 2;
					}
					while ((row >= 0) && (column < numColumns));
					row += 1;
					column += 3;
					
					// Sweep downward diagonally to the left
					do 
					{
						if ((row >= 0) && (column < numColumns) && !readMappingMatrix.get_Renamed(column, row))
						{
							result[resultOffset++] = (sbyte) readUtah(row, column, numRows, numColumns);
						}
						row += 2;
						column -= 2;
					}
					while ((row < numRows) && (column >= 0));
					row += 3;
					column += 1;
				}
			}
			while ((row < numRows) || (column < numColumns));
			
			if (resultOffset != version.TotalCodewords)
			{
				throw ReaderException.Instance;
			}
			return result;
		}
		
		/// <summary> <p>Reads a bit of the mapping matrix accounting for boundary wrapping.</p>
		/// 
		/// </summary>
		/// <param name="row">Row to read in the mapping matrix
		/// </param>
		/// <param name="column">Column to read in the mapping matrix
		/// </param>
		/// <param name="numRows">Number of rows in the mapping matrix
		/// </param>
		/// <param name="numColumns">Number of columns in the mapping matrix
		/// </param>
		/// <returns> value of the given bit in the mapping matrix
		/// </returns>
		internal bool readModule(int row, int column, int numRows, int numColumns)
		{
			// Adjust the row and column indices based on boundary wrapping
			if (row < 0)
			{
				row += numRows;
				column += 4 - ((numRows + 4) & 0x07);
			}
			if (column < 0)
			{
				column += numColumns;
				row += 4 - ((numColumns + 4) & 0x07);
			}
			readMappingMatrix.set_Renamed(column, row);
			return mappingBitMatrix.get_Renamed(column, row);
		}
		
		/// <summary> <p>Reads the 8 bits of the standard Utah-shaped pattern.</p>
		/// 
		/// <p>See ISO 16022:2006, 5.8.1 Figure 6</p>
		/// 
		/// </summary>
		/// <param name="row">Current row in the mapping matrix, anchored at the 8th bit (LSB) of the pattern
		/// </param>
		/// <param name="column">Current column in the mapping matrix, anchored at the 8th bit (LSB) of the pattern
		/// </param>
		/// <param name="numRows">Number of rows in the mapping matrix
		/// </param>
		/// <param name="numColumns">Number of columns in the mapping matrix
		/// </param>
		/// <returns> byte from the utah shape
		/// </returns>
		internal int readUtah(int row, int column, int numRows, int numColumns)
		{
			int currentByte = 0;
			if (readModule(row - 2, column - 2, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(row - 2, column - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(row - 1, column - 2, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(row - 1, column - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(row - 1, column, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(row, column - 2, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(row, column - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(row, column, numRows, numColumns))
			{
				currentByte |= 1;
			}
			return currentByte;
		}
		
		/// <summary> <p>Reads the 8 bits of the special corner condition 1.</p>
		/// 
		/// <p>See ISO 16022:2006, Figure F.3</p>
		/// 
		/// </summary>
		/// <param name="numRows">Number of rows in the mapping matrix
		/// </param>
		/// <param name="numColumns">Number of columns in the mapping matrix
		/// </param>
		/// <returns> byte from the Corner condition 1
		/// </returns>
		internal int readCorner1(int numRows, int numColumns)
		{
			int currentByte = 0;
			if (readModule(numRows - 1, 0, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(numRows - 1, 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(numRows - 1, 2, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 2, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(1, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(2, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(3, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			return currentByte;
		}
		
		/// <summary> <p>Reads the 8 bits of the special corner condition 2.</p>
		/// 
		/// <p>See ISO 16022:2006, Figure F.4</p>
		/// 
		/// </summary>
		/// <param name="numRows">Number of rows in the mapping matrix
		/// </param>
		/// <param name="numColumns">Number of columns in the mapping matrix
		/// </param>
		/// <returns> byte from the Corner condition 2
		/// </returns>
		internal int readCorner2(int numRows, int numColumns)
		{
			int currentByte = 0;
			if (readModule(numRows - 3, 0, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(numRows - 2, 0, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(numRows - 1, 0, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 4, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 3, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 2, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(1, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			return currentByte;
		}
		
		/// <summary> <p>Reads the 8 bits of the special corner condition 3.</p>
		/// 
		/// <p>See ISO 16022:2006, Figure F.5</p>
		/// 
		/// </summary>
		/// <param name="numRows">Number of rows in the mapping matrix
		/// </param>
		/// <param name="numColumns">Number of columns in the mapping matrix
		/// </param>
		/// <returns> byte from the Corner condition 3
		/// </returns>
		internal int readCorner3(int numRows, int numColumns)
		{
			int currentByte = 0;
			if (readModule(numRows - 1, 0, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(numRows - 1, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 3, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 2, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(1, numColumns - 3, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(1, numColumns - 2, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(1, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			return currentByte;
		}
		
		/// <summary> <p>Reads the 8 bits of the special corner condition 4.</p>
		/// 
		/// <p>See ISO 16022:2006, Figure F.6</p>
		/// 
		/// </summary>
		/// <param name="numRows">Number of rows in the mapping matrix
		/// </param>
		/// <param name="numColumns">Number of columns in the mapping matrix
		/// </param>
		/// <returns> byte from the Corner condition 4
		/// </returns>
		internal int readCorner4(int numRows, int numColumns)
		{
			int currentByte = 0;
			if (readModule(numRows - 3, 0, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(numRows - 2, 0, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(numRows - 1, 0, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 2, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(0, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(1, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(2, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			currentByte <<= 1;
			if (readModule(3, numColumns - 1, numRows, numColumns))
			{
				currentByte |= 1;
			}
			return currentByte;
		}
		
		/// <summary> <p>Extracts the data region from a {@link BitMatrix} that contains
		/// alignment patterns.</p>
		/// 
		/// </summary>
		/// <param name="bitMatrix">Original {@link BitMatrix} with alignment patterns
		/// </param>
		/// <returns> BitMatrix that has the alignment patterns removed
		/// </returns>
		internal BitMatrix extractDataRegion(BitMatrix bitMatrix)
		{
			int symbolSizeRows = version.SymbolSizeRows;
			int symbolSizeColumns = version.SymbolSizeColumns;
			
			// TODO(bbrown): Make this work with rectangular codes
			if (bitMatrix.Dimension != symbolSizeRows)
			{
				throw new System.ArgumentException("Dimension of bitMarix must match the version size");
			}
			
			int dataRegionSizeRows = version.DataRegionSizeRows;
			int dataRegionSizeColumns = version.DataRegionSizeColumns;
			
			int numDataRegionsRow = symbolSizeRows / dataRegionSizeRows;
			int numDataRegionsColumn = symbolSizeColumns / dataRegionSizeColumns;
			
			int sizeDataRegionRow = numDataRegionsRow * dataRegionSizeRows;
			//int sizeDataRegionColumn = numDataRegionsColumn * dataRegionSizeColumns;
			
			// TODO(bbrown): Make this work with rectangular codes
			BitMatrix bitMatrixWithoutAlignment = new BitMatrix(sizeDataRegionRow);
			for (int dataRegionRow = 0; dataRegionRow < numDataRegionsRow; ++dataRegionRow)
			{
				int dataRegionRowOffset = dataRegionRow * dataRegionSizeRows;
				for (int dataRegionColumn = 0; dataRegionColumn < numDataRegionsColumn; ++dataRegionColumn)
				{
					int dataRegionColumnOffset = dataRegionColumn * dataRegionSizeColumns;
					for (int i = 0; i < dataRegionSizeRows; ++i)
					{
						int readRowOffset = dataRegionRow * (dataRegionSizeRows + 2) + 1 + i;
						int writeRowOffset = dataRegionRowOffset + i;
						for (int j = 0; j < dataRegionSizeColumns; ++j)
						{
							int readColumnOffset = dataRegionColumn * (dataRegionSizeColumns + 2) + 1 + j;
							if (bitMatrix.get_Renamed(readColumnOffset, readRowOffset))
							{
								int writeColumnOffset = dataRegionColumnOffset + j;
								bitMatrixWithoutAlignment.set_Renamed(writeColumnOffset, writeRowOffset);
							}
						}
					}
				}
			}
			return bitMatrixWithoutAlignment;
		}
	}
}