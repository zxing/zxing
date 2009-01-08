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

using System;
namespace com.google.zxing.qrcode.decoder
{
     using com.google.zxing.common;  
    using com.google.zxing.common.reedsolomon;  

    public sealed class Decoder
    { 
          private ReedSolomonDecoder rsDecoder;

          public Decoder() {
            rsDecoder = new ReedSolomonDecoder(GF256.QR_CODE_FIELD);
          }

          /**
           * <p>Convenience method that can decode a QR Code represented as a 2D array of booleans.
           * "true" is taken to mean a black module.</p>
           *
           * @param image booleans representing white/black QR Code modules
           * @return text and bytes encoded within the QR Code
           * @throws ReaderException if the QR Code cannot be decoded
           */
          public DecoderResult decode(bool[][] image) {
              try{
                int dimension = image.Length;
                BitMatrix bits = new BitMatrix(dimension);
                for (int i = 0; i < dimension; i++) {
                  for (int j = 0; j < dimension; j++) {
                    if (image[i][j]) {
                      bits.set(i, j);
                    }
                  }
                }
                return decode(bits);
              }catch (Exception e){
                throw  new ReaderException(e.Message);
              }            
          }

          /**
           * <p>Decodes a QR Code represented as a {@link BitMatrix}. A 1 or "true" is taken to mean a black module.</p>
           *
           * @param bits booleans representing white/black QR Code modules
           * @return text and bytes encoded within the QR Code
           * @throws ReaderException if the QR Code cannot be decoded
           */
          public DecoderResult decode(BitMatrix bits){
              try{
                // Construct a parser and read version, error-correction level
                BitMatrixParser parser = new BitMatrixParser(bits);
                Version version = parser.readVersion();
                ErrorCorrectionLevel ecLevel = parser.readFormatInformation().getErrorCorrectionLevel();

                // Read codewords
                sbyte[] codewords = parser.readCodewords();
                // Separate into data blocks
                DataBlock[] dataBlocks = DataBlock.getDataBlocks(codewords, version, ecLevel);

                // Count total number of data bytes
                int totalBytes = 0;
                for (int i = 0; i < dataBlocks.Length; i++) {
                  totalBytes += dataBlocks[i].NumDataCodewords;
                }
                sbyte[] resultBytes = new sbyte[totalBytes];
                int resultOffset = 0;

                // Error-correct and copy data blocks together into a stream of bytes
                for (int j = 0; j < dataBlocks.Length; j++) {
                  DataBlock dataBlock = dataBlocks[j];
                  sbyte[] codewordBytes = dataBlock.Codewords;
                  int numDataCodewords = dataBlock.NumDataCodewords;
                  correctErrors(codewordBytes, numDataCodewords);
                  for (int i = 0; i < numDataCodewords; i++) {
                    resultBytes[resultOffset++] = codewordBytes[i];
                  }
                }

                // Decode the contents of that stream of bytes
                string sResult = DecodedBitStreamParser.decode(resultBytes, version);
                return new DecoderResult(resultBytes, sResult, null);
              }catch(Exception e){
                throw new ReaderException(e.Message);
              }
            
          }

          /**
           * <p>Given data and error-correction codewords received, possibly corrupted by errors, attempts to
           * correct the errors in-place using Reed-Solomon error correction.</p>
           *
           * @param codewordBytes data and error correction codewords
           * @param numDataCodewords number of codewords that are data bytes
           * @throws ReaderException if error correction fails
           */
          private void correctErrors(sbyte[] codewordBytes, int numDataCodewords){
              try
              {
                  int numCodewords = codewordBytes.Length;
                  // First read into an array of ints
                  int[] codewordsInts = new int[numCodewords];
                  for (int i = 0; i < numCodewords; i++)
                  {
                      codewordsInts[i] = codewordBytes[i] & 0xFF;
                  }
                  int numECCodewords = codewordBytes.Length - numDataCodewords;
                  try
                  {
                      rsDecoder.decode(codewordsInts, numECCodewords);
                  }
                  catch (ReedSolomonException rse)
                  {
                      throw new ReaderException(rse.Message);
                  }
                  // Copy back into array of bytes -- only need to worry about the bytes that were data
                  // We don't care about errors in the error-correction codewords
                  for (int i = 0; i < numDataCodewords; i++)
                  {
                      codewordBytes[i] = (sbyte)codewordsInts[i];
                  }
              }
              catch (Exception e) {
                  throw new ReaderException(e.Message);
              }            
          }
    }
}