/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.datamatrix.encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public class DDDocEncoder implements Encoder{

    @Override
    public int getEncodingMode() {
        return HighLevelEncoder.DDDOC_ENCODATION;
    }

    @Override
    public void encode(EncoderContext context) {
        String message = context.getMessage();

        try {
            ByteArrayInputStream input = new ByteArrayInputStream(message.getBytes("ISO-8859-1"));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            encodeToC40(input, output);
            input.close();
            output.close();

            byte[] byteArray = output.toByteArray();
            output = new ByteArrayOutputStream();

            int dataLength = encodeDataMatrix(new ByteArrayInputStream(byteArray), output);
            input.close();

            addPadding(dataLength, getDataMatrixStorageCapacity(dataLength), output, isReturnedToASCII(byteArray));

            output.close();

            byte[] byteEncoded = output.toByteArray();
            context.writeCodewords(new String(byteEncoded, "ISO-8859-1"));
        } catch (IOException arg0){
            arg0.printStackTrace();
        }
    }

    /**
     * Encode to C40 the given InputStream into the OutputStream.
     *
     * @param input  The InputStream to be encoded.
     * @param output The OutputStream where encoded data will be written
     * @throws IOException
     */
    private void encodeToC40(InputStream input, OutputStream output) throws IOException {
        int reader;

        while((reader = input.read()) > -1){
            if(reader >= 128){
                output.write(1);
                output.write(30);
                reader -= 128;
            }

            if(reader >= 0 && reader <= 31){
                output.write(0);
                output.write(reader);
            } else if(reader == 32){
                output.write(3);
            } else if(reader >= 33 && reader <= 47){
                output.write(1);
                output.write(reader-33);
            } else if(reader >= 48 && reader <= 57){
                output.write(reader-44);
            } else if(reader >= 58 && reader <= 64){
                output.write(1);
                output.write(reader-43);
            } else if(reader >= 65 && reader <= 90){
                output.write(reader-51);
            } else if(reader >= 91 && reader <= 95){
                output.write(1);
                output.write(reader-69);
            } else if(reader >= 96 && reader <= 127){
                output.write(2);
                output.write(reader-96);
            } else{
                throw new IOException("Byte greater than 256 ! : "+reader);
            }
        }
    }

    /**
     * Encode to Data Matrix the given InputStream into the OutputStream.
     *
     * @param input  The InputStream to be encoded.
     * @param output The OutputStream where encoded data will be written.
     * @return The length of encoded data.
     * @throws IOException
     */
    private int encodeDataMatrix(InputStream input, OutputStream output) throws IOException{
        int dataLength = 0;
        int c1, c2, c3;

        output.write(0xe6); //begin C40 encoding
        dataLength++;

        while((c1 = input.read())> -1){
            if((c2 = input.read()) > -1){
                if((c3 = input.read()) <= -1){
                    c3 = 0;
                }

                int val = 1600*c1 + 40*c2 + c3 +1;

                output.write(val >> 8);
                output.write(val & 0xff);

                dataLength += 2;
            } else {
                output.write(0xfe); //return to ASCII encoding

                if(c1 == 3){
                    output.write(32);
                } else if(c1 >= 4 && c1 <= 13){
                    output.write(c1+44);
                } else if(c1 >= 14 && c1 <= 39){
                    output.write(c1+51);
                }

                dataLength += 2;
            }
        }

        return dataLength;
    }

    private boolean isReturnedToASCII(byte[] data){
        return (data.length % 3 == 1);
    }

    private void addPadding(int dataLength, int dataMatrixStorageCapacity, OutputStream output, boolean isReturnedToASCII) throws IOException{
        int countSpaceUsed = dataLength;
        boolean flagPadding = false;

        while (countSpaceUsed < dataMatrixStorageCapacity) {
            if (isReturnedToASCII) {

                if (flagPadding) {
                    output.write(paddingRandomAlgorithm(countSpaceUsed + 1));
                    countSpaceUsed++;
                } else {
                    output.write(129);
                    flagPadding = true; //indicate that the padding has been started
                    countSpaceUsed++;
                }
            } else {
                output.write(0xfe); //return to ASCII encoding
                isReturnedToASCII = true;
                countSpaceUsed++;
            }
        }

    }

    private int getDataMatrixStorageCapacity(int dataLength) throws InvalidParameterException{
        int[] arrayOfDataMatrixStorageCapacity = {114, 144, 174, 204, 280, 368, 456, 576, 696, 816, 1050, 1304, 1558};
        int dataMatrixStorageCapacity = arrayOfDataMatrixStorageCapacity[0];

        for(int i = 0; i < arrayOfDataMatrixStorageCapacity.length; i++){
            if(dataLength > arrayOfDataMatrixStorageCapacity[i]){
                if(i == arrayOfDataMatrixStorageCapacity.length - 1) {
                    throw new InvalidParameterException("There is not enough storage capacity : "+dataLength+" -> max : "+dataMatrixStorageCapacity);
                }
                dataMatrixStorageCapacity = arrayOfDataMatrixStorageCapacity[i+1];
            } else{
                break;
            }
        }

        return dataMatrixStorageCapacity;
    }

    /**
     * Generate the padding value in accordance with the 253-state randomising algorithm.
     *
     * @param paddingPosition  The padding position.
     * @return The padding value for a Data Matrix padding.
     */
    private int paddingRandomAlgorithm(int paddingPosition){
        //253-state randomising algorithm applies to 2D-Doc padding
        int value = ((149*paddingPosition) % 253) + 1;
        int var = 129 + value; //value of padding = 129

        if(var <= 254){
            return var;
        } else{
            return var-254;
        }
    }
}
