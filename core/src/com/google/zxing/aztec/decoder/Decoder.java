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

package com.google.zxing.aztec.decoder;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.aztec.AztecDetectorResult;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

/**
 * <p>The main class which implements Aztec Code decoding -- as opposed to locating and extracting
 * the Aztec Code from an image.</p>
 *
 * @author David Olivier
 */
public final class Decoder {

  int numCodewords;
  int codewordSize;
  AztecDetectorResult ddata;
	
  public Decoder() {}

  public DecoderResult decode(AztecDetectorResult detectorResult) throws FormatException, ChecksumException {
  	ddata = detectorResult;
  	BitMatrix matrix = detectorResult.getBits();
	  
	  if (!ddata.isCompact()){
	  	matrix = removeDashedLines(ddata.getBits());
	  }
	  
	  boolean[] rawbits = extractBits(matrix);
	  
	  boolean[] correctedBits = correctBits(rawbits);
	  
	  String result = getEncodedData(correctedBits);
	  
	  return new DecoderResult(null, result, null, null);
  }

  
	/**
	 * 
	 * Gets the string encoded in the aztec code bits
	 * 
	 * @param correctedBits
	 * @return the decoded string
	 * @throws FormatException if the input is not valid
	 */
	private String getEncodedData(boolean[] correctedBits) throws FormatException {
		
		int endIndex = codewordSize * ddata.getNbDatablocks();
	  if (endIndex > correctedBits.length){
	  	throw FormatException.getFormatInstance();
	  }
	  
	  int lastTable = UPPER;
	  int table = UPPER;
	  int startIndex = 0;
	  int code;
	  StringBuilder result = new StringBuilder();
	  boolean end = false;
	  boolean shift = false;
	  boolean switchShift = false;
	  
	  while (!end){
 
	  	if (shift){
	  		// the table is for the next character only
	  		switchShift = true;
	  	} else {
	  		// save the current table in case next one is a shift
	  		lastTable = table;
	  	}
	  	
		  switch (table) {
		  case BINARY:
				if (endIndex - startIndex < 8){
					end = true;
					break;
				}
				code = readCode(correctedBits, startIndex, 8);
				startIndex += 8;
				
				result.append((char)(code));
		  	break;
		  	
			default:
				int size = 5;
				
				if (table == DIGIT){
					size = 4;
				}
				
				if (endIndex - startIndex < size){
					end = true;
					break;
				}
				
				code = readCode(correctedBits, startIndex, size);
				startIndex += size;
				
				String str = getCharacter(table, code);
				if (!str.startsWith("CTRL_")){
					result.append(str);
				} else {
					// Table changes
					table = getTable(str.charAt(5));
					
					if (str.charAt(6) == 'S'){
						shift = true;
					}
				}
				
				break;
			}
	  
	  	if (switchShift){
	  		table = lastTable;
	  		shift = false;
	  		switchShift = false;
	  	}
		  
	  }
		return result.toString();
	}


	/**
	 * 
	 * gets the table corresponding to the char passed
	 * 
	 * @param t
	 * @return
	 */
	private int getTable(char t) {
		int table = UPPER;
		
		switch (t){
		case 'U':
			table = UPPER; break;
		case 'L':
			table = LOWER; break;
		case 'P':
			table = PUNCT; break;
		case 'M':
			table = MIXED; break;
		case 'D':
			table = DIGIT; break;
		case 'B':
			table = BINARY; break;
		}
		
		return table;
	}
  
	/**
	 * 
	 * Gets the character (or string) corresponding to the passed code in the given table
	 * 
	 * @param table the table used
	 * @param code the code of the character
	 * @return
	 */
	private String getCharacter(int table, int code) {
		switch (table){
		case UPPER:
			return UPPER_TABLE[code];
		case LOWER:
			return LOWER_TABLE[code];
		case MIXED:
			return MIXED_TABLE[code];
		case PUNCT:
			return PUNCT_TABLE[code];
		case DIGIT:
			return DIGIT_TABLE[code];
		default:
			return "";
		}
	}

	/**
	 * 
	 * <p> performs RS error correction on an array of bits </p>
	 * 
	 * @param rawbits
	 * @return the corrected array
	 * @throws FormatException if the input contains too many errors
	 */
	private boolean[] correctBits(boolean[] rawbits) throws FormatException {
	  GenericGF gf;
	  
	  if (ddata.getNbLayers() <= 2){
	  	codewordSize = 6;
	  	gf = GenericGF.AZTEC_DATA_6;
	  } else if (ddata.getNbLayers() <= 8) {
	  	codewordSize = 8;
	  	gf = GenericGF.AZTEC_DATA_8;
	  } else if (ddata.getNbLayers() <= 22) {
	  	codewordSize = 10;
	  	gf = GenericGF.AZTEC_DATA_10;
	  } else {
	  	codewordSize = 12;
	  	gf = GenericGF.AZTEC_DATA_12;
	  }
	  
	    int numDataCodewords = ddata.getNbDatablocks();
	    int numECCodewords = 0;
	    int offset = 0;
	    
	    if (ddata.isCompact()){
	    	offset = NbBitsCompact[ddata.getNbLayers()] - numCodewords*codewordSize;
	    	numECCodewords = NbDatablockCompact[ddata.getNbLayers()] - numDataCodewords;
	    } else {
	    	offset = NbBits[ddata.getNbLayers()] - numCodewords*codewordSize;
	    	numECCodewords = NbDatablock[ddata.getNbLayers()] - numDataCodewords;
	    }

	    int[] dataWords = new int[numCodewords];
	    for (int i = 0; i < numCodewords; i++){
	    	int flag = 1;
	    	for (int j = 1; j <= codewordSize; j++){
	      	if (rawbits[codewordSize*i + codewordSize - j + offset]){
	      		dataWords[i] += flag;
	      	}
	      	flag <<= 1;
	    	}
	    	
	    	if (dataWords[i] >= flag){
	    		flag++;
	    	}
	    }
	    rawbits = null;
	
	    try {
	      ReedSolomonDecoder rsDecoder = new ReedSolomonDecoder(gf);
				rsDecoder.decode(dataWords, numECCodewords);
	    } catch (ReedSolomonException rse) {
	    	System.out.println("END: invalid RS");
		  	throw FormatException.getFormatInstance();
	    }
	    
	    offset = 0;
	    
		  boolean[] correctedBits = new boolean[numDataCodewords*codewordSize];
	    for (int i = 0; i < numDataCodewords; i ++){
		    	
	    		boolean seriesColor = false;
		    	int seriesCount = 0;
	    		int flag = 1 << (codewordSize - 1);
	    		
	    		for (int j = 0; j < codewordSize; j++){

	    			boolean color = (dataWords[i] & flag) == flag;
	    			
	    			if (seriesCount != codewordSize - 1){

		    			if (seriesColor == color){
		    				seriesCount++;
		    			} else {
		    				seriesCount = 1;
		    				seriesColor = color;
		    			}
	    			
		    			correctedBits[i*codewordSize+j-offset] = color;
	    			
	    			} else {
	    				
	    				if (color == seriesColor){
	    					//bit must be inverted
	    					throw FormatException.getFormatInstance();
	    				}
	    				
	    		    seriesColor = false;
	    		    seriesCount = 0;
	    		    offset++;
	    			}
	    			
	      		flag >>>= 1;
	    		}
	    }
		  
		  return correctedBits;
	}
	
	/**
	 * 
	 * Gets the array of bits from an Aztec Code matrix
	 * 
	 * @param matrix
	 * @return the array of bits
	 * @throws FormatException if the matrix is not a valid aztec code
	 */
	private boolean[] extractBits(BitMatrix matrix) throws FormatException {
		
		boolean[] rawbits;
		if (ddata.isCompact()){
	  	if (ddata.getNbLayers() > NbBitsCompact.length){
	  		throw FormatException.getFormatInstance();
	  	}
	  	rawbits = new boolean[NbBitsCompact[ddata.getNbLayers()]];
	  	numCodewords = NbDatablockCompact[ddata.getNbLayers()];
	  } else {
	  	if (ddata.getNbLayers() > NbBits.length){
	  		throw FormatException.getFormatInstance();
	  	}
	  	rawbits = new boolean[NbBits[ddata.getNbLayers()]];
	  	numCodewords = NbDatablock[ddata.getNbLayers()];
	  }
		
		int flip;
	  int layer = ddata.getNbLayers();
	  int size = matrix.height;
	  int rawbitsOffset = 0;
	  int matrixOffset = 0;
	  
	  while (layer != 0){
	  	
		  flip = 0;
		  for (int i = 0; i < 2*size - 4; i++){	
		  	rawbits[rawbitsOffset+i] = matrix.get(matrixOffset + flip, matrixOffset + i/2);
		  	rawbits[rawbitsOffset+2*size - 4 + i] = matrix.get(matrixOffset + i/2, matrixOffset + size-1-flip);
		  	flip = (flip + 1)%2;
		  }
		  
		  flip = 0;
		  for (int i = 2*size+1; i > 5; i--){
		  	rawbits[rawbitsOffset+4*size - 8 + (2*size-i) + 1] = matrix.get(matrixOffset + size-1-flip, matrixOffset + i/2 - 1);
		  	rawbits[rawbitsOffset+6*size - 12 + (2*size-i) + 1] = matrix.get(matrixOffset + i/2 - 1, matrixOffset + flip);
		  	flip = (flip + 1)%2;
		  }
		  
		  matrixOffset += 2;
		  rawbitsOffset += 8*size-16;
		  layer--;
		  size-=4;
	  }
	  
	  return rawbits;
	}
  
	
  /**
   * 
   * <p> Transforms an aztec code matrix by removing the control dashed lines </p>
   * 
   * @param matrix
   * @return
   */
  private BitMatrix removeDashedLines(BitMatrix matrix) {
  	int nbDashed = 1+ 2* ((matrix.width - 1)/2 / 16);
  	BitMatrix newMatrix = new BitMatrix(matrix.width - nbDashed, matrix.height - nbDashed);
  	
  	int nx = 0;
  	int ny = 0;
  	
  	for (int x = 0; x < matrix.width; x++){
  		
  		ny = 0;
  		
  		if ((matrix.width / 2 - x)%16 == 0){
  			continue;
  		}
  		
  		for (int y = 0; y < matrix.height; y++){
  			
  			if ((matrix.width / 2 - y)%16 == 0){
  				continue;
  			}
  			
  			if (matrix.get(x, y)){
  				newMatrix.set(nx, ny);
  			}
  			ny++;
  		}
  		nx++;
  	}
  	
  	return newMatrix;
	}

	/**
	 * 
	 * Reads a code of given length and at given index in an array of bits
	 * 
	 * @param rawbits
	 * @param startIndex
	 * @param length
	 * @return
	 */
	private int readCode(boolean[] rawbits, int startIndex, int length) {
	  int res = 0;
	  
  	for (int i = startIndex; i < startIndex + length; i++){
  		res = res << 1;
  		if (rawbits[i]){
  			res++;
  		}
	  }
  	
  	return res;
	}

	
	
	
	static final int UPPER = 0;
	static final int LOWER = 1;
	static final int MIXED = 2;
	static final int DIGIT = 3;
	static final int PUNCT = 4;
	static final int BINARY = 5;

  static final int NbBitsCompact[] = {
  	0,104, 240, 408, 608
  };
  
  static final int NbBits[] = {
    0, 128, 288, 480, 704, 960, 1248, 1568, 1920, 2304, 2720, 3168, 3648, 4160, 4704, 5280, 5888, 6528, 7200, 7904, 8640, 9408, 10208, 11040, 11904, 12800, 13728, 14688, 15680, 16704, 17760, 18848, 19968
  };
  
  static final int NbDatablockCompact[] = {
  	0, 17, 40, 51, 76
  };
  
  static final int NbDatablock[] = {
  	0, 21, 48, 60, 88, 120, 156, 196, 240, 230, 272, 316, 364, 416, 470, 528, 588, 652, 720, 790, 864, 940, 1020, 920, 992, 1066, 1144, 1224, 1306, 1392, 1480, 1570, 1664
  };
  
  static final String UPPER_TABLE[] = {
  	"CTRL_PS", " ", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "CTRL_LL", "CTRL_ML", "CTRL_DL", "CTRL_BS"
  };
  
  static final String LOWER_TABLE[] = {
  	"CTRL_PS", " ", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "CTRL_US", "CTRL_ML", "CTRL_DL", "CTRL_BS"
  };
  
  static final String MIXED_TABLE[] = {
  	"CTRL_PS", " ", ""+(char)1, ""+(char)2, ""+(char)3, ""+(char)4, ""+(char)5, ""+(char)6, ""+(char)7, ""+(char)8, ""+(char)9, ""+(char)10, ""+(char)11, ""+(char)12, ""+(char)13, ""+(char)27, ""+(char)28, ""+(char)29, ""+(char)30, ""+(char)31, "@", "\\", "^", ""+(char)95, "`", "|", "~", ""+(char)127, "CTRL_LL", "CTRL_UL", "CTRL_PL", "CTRL_BS"
  };
  
  static final String PUNCT_TABLE[] = {
  	"", ""+(char)13, ""+(char)13+(char)10, ""+(char)46+(char)32, ""+(char)44+(char)32, ""+(char)58+(char)32, "!", "\"", "#", "$", "%", "&", "'", "(", ")", ""+(char)42, "+", ",", "-", ".", "/", ":", ";", "<", "=", ">", "?", "[", "]", "{", "}", "CTRL_UL"
  };
  
  static final String DIGIT_TABLE[] = {
  	"CTRL_PS", " ", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ",", ".", "CTRL_UL", "CTRL_US"
  };
}
