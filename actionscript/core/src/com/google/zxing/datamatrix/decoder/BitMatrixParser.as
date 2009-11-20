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
package com.google.zxing.datamatrix.decoder
{
    import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
	import com.google.zxing.ReaderException;
    /**
     * @author bbrown@google.com (Brian Brown)
     */
    public  class BitMatrixParser
    {

          private  var mappingBitMatrix:BitMatrix;
          private  var readMappingMatrix:BitMatrix;
          private  var version:Version;

          /**
           * @param bitMatrix {@link BitMatrix} to parse
           * @throws ReaderException if dimension is < 10 or > 144 or not 0 mod 2
           */
          public function BitMatrixParser(bitMatrix:BitMatrix) {
            var dimension:int = bitMatrix.getDimension();
            if (dimension < 10 || dimension > 144 || (dimension & 0x01) != 0) {
              throw new ReaderException("BitMatrixParser : Dimension out of range :"+dimension+" range 11~143 or uneven number");
            }
            
            version = readVersion(bitMatrix);
            this.mappingBitMatrix = extractDataRegion(bitMatrix);
            // TODO(bbrown): Make this work for rectangular symbols
            this.readMappingMatrix = new BitMatrix(this.mappingBitMatrix.getDimension());
          }

          /**
           * <p>Creates the version object based on the dimension of the original bit matrix from 
           * the datamatrix code.</p>
           *
           * <p>See ISO 16022:2006 Table 7 - ECC 200 symbol attributes</p>
           * 
           * @param bitMatrix Original {@link BitMatrix} including alignment patterns
           * @return {@link Version} encapsulating the Data Matrix Code's "version"
           * @throws ReaderException if the dimensions of the mapping matrix are not valid
           * Data Matrix dimensions.
           */
          public function readVersion(bitMatrix:BitMatrix ):Version {

            if (version != null) {
              return version;
            }

            // TODO(bbrown): make this work for rectangular dimensions as well.
            var numRows:int = bitMatrix.getDimension();
            var numColumns:int = numRows;
            return Version.getVersionForDimensions(numRows, numColumns);;
          }

          /**
           * <p>Reads the bits in the {@link BitMatrix} representing the mapping matrix (No alignment patterns)
           * in the correct order in order to reconstitute the codewords bytes contained within the
           * Data Matrix Code.</p>
           *
           * @return bytes encoded within the Data Matrix Code
           * @throws ReaderException if the exact number of bytes expected is not read
           */
          public function readCodewords():Array {

            var result:Array = new Array(version.getTotalCodewords());
            var resultOffset:int = 0;
            
            var row:int = 4;
            var column:int = 0;
            // TODO(bbrown): Data Matrix can be rectangular, assuming square for now
            var numRows:int = mappingBitMatrix.getDimension();
            var numColumns:int = numRows;
            
            var corner1Read:Boolean = false;
            var corner2Read:Boolean = false;
            var corner3Read:Boolean = false;
            var corner4Read:Boolean = false;
            
            // Read all of the codewords
            do {
              // Check the four corner cases
              if ((row == numRows) && (column == 0) && !corner1Read) {
                result[resultOffset++] = int(readCorner1(numRows, numColumns));
                row -= 2;
                column +=2;
                corner1Read = true;
              } else if ((row == numRows-2) && (column == 0) && ((numColumns & 0x03) != 0) && !corner2Read) {
                  result[resultOffset++] = int(readCorner2(numRows, numColumns));
                row -= 2;
                column +=2;
                corner2Read = true;
              } else if ((row == numRows+4) && (column == 2) && ((numColumns & 0x07) == 0) && !corner3Read) {
                  result[resultOffset++] = int(readCorner3(numRows, numColumns));
                row -= 2;
                column +=2;
                corner3Read = true;
              } else if ((row == numRows-2) && (column == 0) && ((numColumns & 0x07) == 4) && !corner4Read) {
                  result[resultOffset++] = int(readCorner4(numRows, numColumns));
                row -= 2;
                column +=2;
                corner4Read = true;
              } else {
                // Sweep upward diagonally to the right
                do {
                  if ((row < numRows) && (column >= 0) && !readMappingMatrix._get(column,row)) {
                      result[resultOffset++] = readUtah(row, column, numRows, numColumns);
                  }
                  row -= 2;
                  column +=2;
                } while ((row >= 0) && (column < numColumns));
                row += 1;
                column +=3;
                
                // Sweep downward diagonally to the left
                do {
                  if ((row >= 0) && (column < numColumns) && !readMappingMatrix._get(column,row)) {
                     result[resultOffset++] = readUtah(row, column, numRows, numColumns);
                  }
                  row += 2;
                  column -=2;
                } while ((row < numRows) && (column >= 0));
                row += 3;
                column +=1;
              }
            } while ((row < numRows) || (column < numColumns));

            if (resultOffset != version.getTotalCodewords()) {
              throw new ReaderException("BitMatrixParser : readCodewords : resultOffset != version.getTotalCodewords() : "+resultOffset +" - "+ version.getTotalCodewords());
            }
            // BAS : extra code for Flex : result should be a signed byte array (bit 7 = sign)
            for (var jj:int=0;jj<result.length;jj++)
            {
            	if ((result[jj] & 128) > 0 )
            	{
            		result[jj] = (result[jj] & 127) - 128		
            	}
            }
            return result;
          }
          
          /**
           * <p>Reads a bit of the mapping matrix accounting for boundary wrapping.</p>
           * 
           * @param row Row to read in the mapping matrix
           * @param column Column to read in the mapping matrix
           * @param numRows Number of rows in the mapping matrix
           * @param numColumns Number of columns in the mapping matrix
           * @return value of the given bit in the mapping matrix
           */
          public function readModule(row:int , column:int , numRows:int , numColumns:int ):Boolean {
            // Adjust the row and column indices based on boundary wrapping
            if (row < 0) {
              row += numRows;
              column += 4 - ((numRows + 4) & 0x07);
            }
            if (column < 0) {
              column += numColumns;
              row += 4 - ((numColumns + 4) & 0x07);
            }
            readMappingMatrix._set(column,row);
            return mappingBitMatrix._get(column,row);
          }
          
          /**
           * <p>Reads the 8 bits of the standard utah shaped pattern.</p>
           * 
           * <p>See ISO 16022:2006, 5.8.1 Figure 6</p>
           * 
           * @param row Current row in the mapping matrix, anchored at the 8th bit (LSB) of the pattern
           * @param column Current column in the mapping matrix, anchored at the 8th bit (LSB) of the pattern
           * @param numRows Number of rows in the mapping matrix
           * @param numColumns Number of columns in the mapping matrix
           * @return byte from the utah shape
           */
          public function  readUtah(row:int , column:int , numRows:int , numColumns:int ):int 
          {
            var currentByte:int = 0;
            if (readModule(row - 2, column - 2, numRows, numColumns)) 
            {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(row - 2, column - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(row - 1, column - 2, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(row - 1, column - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(row - 1, column, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(row, column - 2, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(row, column - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(row, column, numRows, numColumns)) {
              currentByte |= 1;
            }
            return currentByte;
          }
          
          /**
           * <p>Reads the 8 bits of the special corner condition 1.</p>
           * 
           * <p>See ISO 16022:2006, Figure F.3</p>
           * 
           * @param numRows Number of rows in the mapping matrix
           * @param numColumns Number of columns in the mapping matrix
           * @return byte from the Corner condition 1
           */
          public function  readCorner1(numRows:int, numColumns:int):int {
            var currentByte:int = 0;
            if (readModule(numRows - 1, 0, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(numRows - 1, 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(numRows - 1, 2, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 2, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(1, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(2, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(3, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            return currentByte;
          }
          
          /**
           * <p>Reads the 8 bits of the special corner condition 2.</p>
           * 
           * <p>See ISO 16022:2006, Figure F.4</p>
           * 
           * @param numRows Number of rows in the mapping matrix
           * @param numColumns Number of columns in the mapping matrix
           * @return byte from the Corner condition 2
           */
          public function readCorner2(numRows:int, numColumns:int):int {
            var currentByte:int = 0;
            if (readModule(numRows - 3, 0, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(numRows - 2, 0, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(numRows - 1, 0, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 4, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 3, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 2, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(1, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            return currentByte;
          }
          
          /**
           * <p>Reads the 8 bits of the special corner condition 3.</p>
           * 
           * <p>See ISO 16022:2006, Figure F.5</p>
           * 
           * @param numRows Number of rows in the mapping matrix
           * @param numColumns Number of columns in the mapping matrix
           * @return byte from the Corner condition 3
           */
          public function readCorner3(numRows:int, numColumns:int):int {
            var currentByte:int = 0;
            if (readModule(numRows - 1, 0, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(numRows - 1, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 3, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 2, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(1, numColumns - 3, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(1, numColumns - 2, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(1, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            return currentByte;
          }
          
          /**
           * <p>Reads the 8 bits of the special corner condition 4.</p>
           * 
           * <p>See ISO 16022:2006, Figure F.6</p>
           * 
           * @param numRows Number of rows in the mapping matrix
           * @param numColumns Number of columns in the mapping matrix
           * @return byte from the Corner condition 4
           */
          public function readCorner4(numRows:int, numColumns:int):int {
            var currentByte:int = 0;
            if (readModule(numRows - 3, 0, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(numRows - 2, 0, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(numRows - 1, 0, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 2, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(0, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(1, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(2, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            currentByte <<= 1;
            if (readModule(3, numColumns - 1, numRows, numColumns)) {
              currentByte |= 1;
            }
            return currentByte;
          }
          
          /**
           * <p>Extracts the data region from a {@link BitMatrix} that contains
           * alignment patterns.</p>
           * 
           * @param bitMatrix Original {@link BitMatrix} with alignment patterns
           * @return BitMatrix that has the alignment patterns removed
           */
          public function extractDataRegion( bitMatrix:BitMatrix):BitMatrix {
             var symbolSizeRows:int = version.getSymbolSizeRows();
             var symbolSizeColumns:int = version.getSymbolSizeColumns();
            
            // TODO(bbrown): Make this work with rectangular codes
            if (bitMatrix.getDimension() != symbolSizeRows) {
              throw new IllegalArgumentException("Dimension of bitMarix must match the version size");
            }
            
            var dataRegionSizeRows:int = version.getDataRegionSizeRows();
            var dataRegionSizeColumns:int = version.getDataRegionSizeColumns();
            
            var numDataRegionsRow:int = symbolSizeRows / dataRegionSizeRows;
            var numDataRegionsColumn:int = symbolSizeColumns / dataRegionSizeColumns;
            
            var sizeDataRegionRow:int = numDataRegionsRow * dataRegionSizeRows;
            //int sizeDataRegionColumn = numDataRegionsColumn * dataRegionSizeColumns;
            
            // TODO(bbrown): Make this work with rectangular codes
            var bitMatrixWithoutAlignment:BitMatrix = new BitMatrix(sizeDataRegionRow);
			    for (var dataRegionRow:int = 0; dataRegionRow < numDataRegionsRow; ++dataRegionRow) {
			      var dataRegionRowOffset:int = dataRegionRow * dataRegionSizeRows;
			      for (var dataRegionColumn:int = 0; dataRegionColumn < numDataRegionsColumn; ++dataRegionColumn) {
			        var dataRegionColumnOffset:int = dataRegionColumn * dataRegionSizeColumns;
			        for (var i:int = 0; i < dataRegionSizeRows; ++i) {
			          var readRowOffset:int = dataRegionRow * (dataRegionSizeRows + 2) + 1 + i;
			          var writeRowOffset:int = dataRegionRowOffset + i;
			          for (var j:int = 0; j < dataRegionSizeColumns; ++j) {
			            var readColumnOffset:int = dataRegionColumn * (dataRegionSizeColumns + 2) + 1 + j;
			            if (bitMatrix._get(readColumnOffset, readRowOffset)) {
			              var writeColumnOffset:int = dataRegionColumnOffset + j;
			              bitMatrixWithoutAlignment._set(writeColumnOffset, writeRowOffset);
			            }
			          }
			        }
			      }
			    }
			    
            return bitMatrixWithoutAlignment;
          }

    }

}