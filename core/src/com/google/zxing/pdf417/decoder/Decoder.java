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
package com.google.zxing.pdf417.decoder;

import com.google.zxing.ReaderException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
//import com.google.zxing.pdf417.reedsolomon.ReedSolomonDecoder;

/**
 * <p>The main class which implements PDF417 Code decoding -- as 
 * opposed to locating and extracting the PDF417 Code from an image.</p>
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 */
public final class Decoder {
	public static final boolean debug = false;
	private static final int MAX_ERRORS = 3;
	private static final int MAX_EC_CODEWORDS = 512;
	//private final ReedSolomonDecoder rsDecoder;

	public Decoder() {
		// TODO MGMG
		//rsDecoder = new ReedSolomonDecoder();
	}

	/**
	 * <p>Convenience method that can decode a PDF417 Code represented as a 2D array of booleans.
	 * "true" is taken to mean a black module.</p>
	 *
	 * @param image booleans representing white/black PDF417 modules
	 * @return text and bytes encoded within the PDF417 Code
	 * @throws ReaderException if the PDF417 Code cannot be decoded
	 */
	public DecoderResult decode(boolean[][] image) throws ReaderException {
		int dimension = image.length;
		BitMatrix bits = new BitMatrix(dimension);
		for (int i = 0; i < dimension; i++) {
			for (int j = 0; j < dimension; j++) {
				if (image[i][j]) {
					bits.set(i, j);
				}
			}
		}
		return decode(bits);
	}

	/**
	 * <p>Decodes a PDF417 Code represented as a {@link BitMatrix}. 
	 * A 1 or "true" is taken to mean a black module.</p>
	 *
	 * @param bits booleans representing white/black PDF417 Code modules
	 * @return text and bytes encoded within the PDF417 Code
	 * @throws ReaderException if the PDF417 Code cannot be decoded
	 */
	public DecoderResult decode(BitMatrix bits) throws ReaderException {
		// Construct a parser to read the data codewords and error-correction level
		long t2;
		long t1 = System.currentTimeMillis();
		BitMatrixParser parser = new BitMatrixParser(bits);
		int[] codewords = parser.readCodewords();
		if (codewords == null || codewords.length == 0) {
			if (debug) System.out.println("No codewords read"); 
			throw ReaderException.getInstance();
		}
		if (debug) {
			t2 = System.currentTimeMillis();
			System.out.println("Elapsed time in ms - BitMatrixParser " +(t2-t1));
		}

		int ecLevel = parser.getECLevel();
		int numECCodewords = 1 << (ecLevel+1);
		int erasures[] = parser.getErasures();
		
		t1 = System.currentTimeMillis();
		correctErrors(codewords, erasures, numECCodewords);
		if (debug) {
			t2 = System.currentTimeMillis();
			System.out.println("Elapsed time in ms - Reed Solomon " +(t2-t1));
		}
		verifyCodewordCount(codewords, numECCodewords);
		
		t1 = System.currentTimeMillis();
		DecoderResult dr = DecodedBitStreamParser.decode(codewords);
		if (debug) {
			t2 = System.currentTimeMillis();
			System.out.println("Elapsed time in ms - DecodeBitStreamParser " +(t2-t1));
		}
		// Decode the codewords
		return dr;
	}

	/**
	 * Verify that all is OK with the codeword array.
	 * @param codewords
	 * @return an index to the first data codeword.
	 * @throws ReaderException
	 */
	private int verifyCodewordCount(int codewords[], int numECCodewords) throws ReaderException {
		int numberOfCodewords = 0;
		if (codewords.length < 4) {
			// Codeword array size should be at least 4 allowing for
			// Count CW, At least one Data CW, Error Correction CW, Error Correction CW
			if (debug) System.out.println("Not enough codewords " + codewords.length);
			throw ReaderException.getInstance();
		}
		// The first codeword, the Symbol Length Descriptor, shall always encode the total number of data
		// codewords in the symbol, including the Symbol Length Descriptor itself, data codewords and pad
		// codewords, but excluding the number of error correction codewords.		
		numberOfCodewords = codewords[0];
		if (numberOfCodewords > codewords.length) {
			if (debug) System.out.println("Invalid number of codewords[0]=" + numberOfCodewords + " codewords.length=" + codewords.length);
			throw ReaderException.getInstance();
		}
		if (numberOfCodewords == 0) {
			// Reset to the length of the array - 8 (Allow for at least level 3 Error Correction (8 Error Codewords)
			if (numECCodewords < codewords.length) {
				codewords[0] = codewords.length - numECCodewords;
				if (debug) System.out.println("Codewords[0] is zero, resetting to " +(codewords[0])); 
			} else {
				if (debug) System.out.println("Codewords[0] is zero, no data codewords"); 
				throw ReaderException.getInstance();
			}
		}
		return 1; // Index to first data codeword
	}

	/**
	 * <p>Given data and error-correction codewords received, possibly corrupted by errors, attempts to
	 * correct the errors in-place using Reed-Solomon error correction.</p>
	 *
	 * @param codewordBytes data and error correction codewords
	 * @param numDataCodewords number of codewords that are data bytes
	 * @throws ReaderException if error correction fails
	 */
	private int correctErrors(int[] codewords, int[] erasures, int numECCodewords) throws ReaderException {
		if ((erasures != null && erasures.length > numECCodewords/2 + MAX_ERRORS) ||
			(numECCodewords < 0 || numECCodewords > MAX_EC_CODEWORDS)) {
			// Too many errors or EC Codewords is corrupted
			throw ReaderException.getInstance();  
		}
		// Try to correct the errors
		int result = 0; // rsDecoder.correctErrors(codewords, numECCodewords);
		if (debug) {
			System.out.println("Corrected errors: " + result);
		}
		if (erasures != null) {
			int numErasures = erasures.length;
			if (result > 0) {
				numErasures = numErasures - result;
			}	
			if (numErasures > MAX_ERRORS) {
				// Still too many errors
				throw ReaderException.getInstance();
			}
		}	
		return result;
	}
}
