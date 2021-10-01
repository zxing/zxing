package com.google.zxing.qrcode.encoder;

import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.Version;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;

/* Experimental encoder that encodes minimally using Divide and Conquer with Memoization
 *
 * Major limitation:
 * The implementation currently recurses for every character in the input so that it will overflow the stack on longer input.
 *
 * Version selection:
 * The version can be preset in the constructor. If it isn't specified then the algorithm will compute three solutions for the three different version classes 1-9, 10-26 and 27-40.
 *
 * It is not clear to me if ever a solution using for example Medium (Versions 10-26) could be smaller than a Small solution (Versions 1-9) (proof for or against would be nice to have).
 * With hypothetical values for the number of length bits, the number of bits per mode and the number of bits per encoded character it can be shown that it can happen at all as follows:
 * We hypothetically assume that a mode is encoded using 1 bit (instead of 4) and a character is encoded in BYTE mode using 2 bit (instead of 8). Using these values we now attempt to encode the 
 * four characters "1234".
 * If we furthermore assume that in Version 1-9 the length field has 1 bit length so that it can encode up to 2 characters and that in Version 10-26 it has 2 bits length so that we can encode up
 * to 2 characters then it is more efficient to encode with Version 10-26 than with Version 1-9 as shown below:
 *
 * Number of length bits small version (1-9): 1
 * Number of length bits large version (10-26): 2
 * Number of bits per mode item: 1
 * Number of bits per character item: 2
 * BYTE(1,2),BYTE(3,4): 1+1+2+2,1+1+2+2=12 bits
 * BYTE(1,2,3,4): 1+2+2+2+2+2          =11 bits
 *
 * If we however change the capacity of the large encoding from 2 bit to 4 bit so that it potentially can encode 16 items, then it is more efficient to encode using the small encoding
 * as shown below:
 *
 * Number of length bits small version (1-9): 1
 * Number of length bits large version (10-26): 4
 * Number of bits per mode item: 1
 * Number of bits per character item: 2
 * BYTE(1,2),BYTE(3,4): 1+1+2+2,1+1+2+2=12 bits
 * BYTE(1,2,3,4): 1+4+2+2+2+2          =13 bits
 *
 * But as mentioned, it is not clear to me if this can ever happen with the actual values.
 *
 * ECI switching:
 *
 * In multi language content the algorithm selects the most compact representation using ECI modes. For example the
 * it is more compactly represented using one ECI to UTF-8 rather than two ECIs to ISO-8859-6 and ISO-8859-1 if the text contains more ASCII characters (since they are represented as
 * one byte sequence) as opposed to the case where there are proportionally more Arabic characters that require two bytes in UTF-8 and only one in ISO-8859-6.
 */
public class MinimalEncoder {

    static final boolean PRINT_STATS = false; //print memoization memory usage

    enum VersionSize {
        Small,
        Medium,
        Large;
        public String toString() {
            return "Small".equals(name()) ? "version 1-9" : "Medium".equals(name()) ? "version 10-26" : "version 27-40";
        }
    }

    String stringToEncode;
    Version version = null;
    boolean isGS1 = false;
    CharsetEncoder[] encoders;
    

//Average usage density should perhaps be monitored to see if not a hashtable is preferable. The GS1 test example uses 67% of an array of 1727 entries. Encoding the URL 'http://www.google.com'
//uses 27% of an array of size 756.
//the dimension sizes of the array are:
//- position-in-input: length of input in characters
//- character-encoding: minimally 3 (ISO-8859-1, UTF-8 and UTF-16). The input is scanned up front and for example ISO-8859-6 is added if an arabic character is encountered.
//- version-class: exactly 3 representing version 1-9, 10-26 and 27-40.
//- Mode: exactly 4 (representing the modes KANJI, ALPHANUMERIC, NUMERIC and BYTE)
    ResultNode[][][][] memoizedResults; //[position-in-input][character-encoding][version-class][Mode]

/**Encoding is optional (default ISO-8859-1) and version is optional (minimal version is computed if not specified*/
    MinimalEncoder(String stringToEncode,Version version,boolean isGS1) {
        this.stringToEncode = stringToEncode;
        if (version != null) {
            this.version = version;
        }
        this.isGS1 = isGS1;
        CharsetEncoder[] isoEncoders = new CharsetEncoder[15];
        isoEncoders[0] = StandardCharsets.ISO_8859_1.newEncoder(); //shouldn't it be Shift_JIS?
        //isoEncoders[0] = Encoder.DEFAULT_BYTE_MODE_ENCODING.newEncoder(); 
        for (int i = 0; i < stringToEncode.length(); i++) {
            int cnt = 0;
            int j;
            for (j = 0; j < 15; j++) {
                if (isoEncoders[j] != null) {
                    cnt++;
                    if (isoEncoders[j].canEncode(stringToEncode.charAt(i))) {
                        break;
                    }
                }
            }
            if (cnt == 14) { //we need all. Can stop looking further.
                break;
            }
            if (j >= 15) { //no encoder found 
                for (j = 0; j < 15; j++) {
                    if (j != 11 && isoEncoders[j] == null) { // ISO-8859-12 doesn't exist
                        try {
                            CharsetEncoder ce = Charset.forName("ISO-8859-" + (j + 1)).newEncoder();
                            if (ce.canEncode(stringToEncode.charAt(i))) {
                                isoEncoders[j] = ce;
                                break;
                            }
                        } catch (UnsupportedCharsetException e) { }
                    }
                }
            }
        }
        int numberOfEncoders = 0;
        for (int j = 0; j < 15; j++) {
            if (isoEncoders[j] != null) {
                numberOfEncoders++;
            }
        }
        encoders = new CharsetEncoder[numberOfEncoders + 2];
        int index = 0;
        for (int j = 0; j < 15; j++) {
            if (isoEncoders[j] != null) {
                encoders[index++] = isoEncoders[j];
            }
        }
        encoders[index++] = StandardCharsets.UTF_8.newEncoder();
        encoders[index++] = StandardCharsets.UTF_16BE.newEncoder();
        
        memoizedResults = new ResultNode[stringToEncode.length()][encoders.length][3][4];
    }
    public static void main(String[] args) throws Exception {

//Invocation examples:
//java -ea MinimalEncoder A AB ABC ABCD ABCDE ABCDEF 1 12 123 1234 12345 123456 123A A1 A12 A123 A1234 AB1 AB12 AB123 AB1234 ABC1 ABC12 ABC1234 http://foo.com HTTP://FOO.COM 1001114670010%01201220%107211220%140045003267781 --unicodeEscapes '\u0150' --unicodeEscapes '\u015C' --unicodeEscapes '\u0150\u015C' --unicodeEscapes '\u0150\u015C\u0150' --unicodeEscapes 'abcdef\u0150ghij' --unicodeEscapes 'that particularly stands out to me is \u0625\u0650\u062C\u064E\u0651\u0627\u0635 (\u02BEijj\u0101\u1E63) \u201Cpear\u201D, suggested to have originated from Hebrew \u05D0\u05B7\u05D2\u05B8\u05BC\u05E1 (ag\u00E1s)'
        for (int i = 0; i < args.length; i++) {
            String input;
            boolean isGS1 = false;
            if ("--unicodeEscapes".equals(args[i]) && i + 1 < args.length) {
                input = parseUnicodeEscapes(args[i + 1]);
                i++;
            } else if ("--gs1".equals(args[i]) && i + 1 < args.length) {
                isGS1 = true;
                input = args[i + 1];
                i++;
            } else if ("--nonascii".equals(args[i]) && i + 1 < args.length) {
                input = createNonASCII(Integer.parseInt(args[i + 1]));
                i++;
            } else {
                input = args[i];
            }
            ResultNode result = encode(input,null,isGS1);
            System.err.println("Minimal encoding of string \"" + input + "\" requires " + result.getSize() + " bits in version " + result.getVersion(ErrorCorrectionLevel.L) + " and is encoded as follows:" + result.toString());
            //testDecode(result);
        }
    }
    static ResultNode encode(String stringToEncode,Version version,boolean isGS1) {
        return new MinimalEncoder(stringToEncode,version,isGS1).encode();
    }
    ResultNode encode() {
        if (version == null) { //compute minimal encoding trying the three version sizes.
            ResultNode[] results = {encode(0,0,VersionSize.Small,null,0),
                                  encode(0,0,VersionSize.Medium,null,0),
                                  encode(0,0,VersionSize.Large,null,0)};
            return postProcess(smallest(results));
        } else { //compute minimal encoding for a given version
            return postProcess(encode(0,0,getVersionSize(version),null,0));
        }
    }
    static String parseUnicodeEscapes(String s) {
        String result = "";
        for (int i = 0; i < s.length(); i++) {
            if (i + 5 < s.length() && s.charAt(i) == '\\' && s.charAt(i + 1) == 'u') {
                result += (char) Integer.parseInt(s.substring(i + 2,i + 2 + 4),16);
                i += 5;
            } else {
                result += s.charAt(i);
            }
        }
        return result;
    }
    static String createNonASCII(int length) {
        String result = "";
        while (length-- > 0) {
            result += "\u0081";
        }
        return result;
    }
    static VersionSize getVersionSize(Version version) {
        return version.getVersionNumber() <= 9 ? VersionSize.Small : version.getVersionNumber() <= 26 ? VersionSize.Medium : VersionSize.Large;
    }
    static Version getVersion(VersionSize versionSize) {
        switch (versionSize) {
            case Small: return Version.getVersionForNumber(9);
            case Medium: return Version.getVersionForNumber(26);
            case Large: 
            default: return Version.getVersionForNumber(40);
        }
    }
    static boolean isNumeric(char c) {
        return c >= '0' && c <= '9';
    }
/* Probably can be implemented in a faster way*/
    static boolean isDoubleByteKanji(char c) {
        return isOnlyDoubleByteKanji("" + c);
    }
    static boolean isAlphanumeric(char c) {
        return getAlphanumericCode(c) != -1;
    }

/** Example: to encode alphanumerically at least 2 characters are needed (5.5 bits per character). Similarily three digits are needed to encode numerically (3+1/3 bits per digit)*/
    static int getEncodingGranularity(Mode mode) {
        switch (mode) {
            case KANJI: return 1;
            case ALPHANUMERIC: return 2;
            case NUMERIC: return 3;
            case BYTE: return 1;
            default:
                return 0;
        }
    }
/** Example: to encode alphanumerically 11 bits are used per 2 characters. Similarily 10 bits are used to encode 3 numeric digits.*/
    static int getBitsPerEncodingUnit(Mode mode) {
        switch (mode) {
            case KANJI: return 16;
            case ALPHANUMERIC: return 11;
            case NUMERIC: return 10;
            case BYTE: return 8;
            case ECI:
            default:
                return 0;
        }
    }
/** Returns the maximum number of encodeable characters in the given mode for the given version. Example: in Version 1, 2^10 digits or 2^8 bytes can be encoded. In Version 3 it is 2^14 digits and 2^16 bytes*/
    static int getMaximumNumberOfEncodeableCharacters(Version version,Mode mode) {
        int count = mode.getCharacterCountBits(version);
        return count == 0 ? 0 : 1 << count;
    }
    static int getMaximumNumberOfEncodeableCharacters(VersionSize versionSize,Mode mode) {
        return getMaximumNumberOfEncodeableCharacters(getVersion(versionSize),mode);
    }
    boolean canEncode(Mode mode,char c) {
        switch (mode) {
            case KANJI: return isDoubleByteKanji(c) ;
            case ALPHANUMERIC: return isAlphanumeric(c) ;
            case NUMERIC: return isNumeric(c) ;
            case BYTE: return true; //any character can be encoded as byte(s). Up to the caller to manage splitting into multiple bytes when String.getBytes(Charset) return more than one byte.
            default:
                 return false;
        }
    }
    static int getCompactedOrdinal(Mode mode) {
        if (mode == null) {
            return 0;
        }
        switch (mode) {
            case KANJI: return 0;
            case ALPHANUMERIC: return 1;
            case NUMERIC: return 2;
            case BYTE: return 3;
            default:
                 assert false;
                 return -1;
        }
    }
    static ResultNode smallest(ResultNode[] results) {
        ResultNode smallestResult = null;
        for (int i = 0; i < results.length; i++) {
            if (smallestResult == null || (results[i] != null && results[i].getSize() < smallestResult.getSize())) {
                smallestResult = results[i];
            }
        }
        return smallestResult;
    }
    static ResultNode smallest(Vector<ResultNode> results) {
        ResultNode smallestResult = null;
        for (int i = 0; i < results.size(); i++) {
            if (smallestResult == null || (results.get(i) != null && results.get(i).getSize() < smallestResult.getSize())) {
                smallestResult = results.get(i);
            }
        }
        return smallestResult;
    }
    ResultNode postProcess(ResultNode result) {
System.err.println("DEBUG 0");
        if (isGS1) {
            if (result.mode != Mode.ECI) {
System.err.println("DEBUG 1");
                ResultNode current = result.next;
                while (current != null && current.mode != Mode.ECI) {
                    current = current.next;
                }
                if (current != null) { // there is an ECI somewhere
System.err.println("DEBUG 2");
                    //prepend a default character set ECI
                    result = new ResultNode(Mode.ECI,result.version,true,0,0,result);
                }
            }
            if (result.mode != Mode.ECI) {
System.err.println("DEBUG 3");
                //prepend a FNC1_FIRST_POSITION
                result = new ResultNode(Mode.FNC1_FIRST_POSITION,result.version,true,0,0,result);
            } else {
System.err.println("DEBUG 4");
                //insert a FNC1_FIRST_POSITION after the ECI
                result.next = new ResultNode(Mode.FNC1_FIRST_POSITION,result.version,true,0,0,result.next);
            }
        }
        ResultNode current = result;
        while (current.next != null) {
            current = current.next;
        }
        //Add TERMINATOR according to "8.4.8 Terminator"
        current.next = new ResultNode(Mode.TERMINATOR,result.version,true,stringToEncode.length(),result.charsetEncoderIndex,null);
        if (PRINT_STATS) {
            int total = 0;
            int used = 0;
            for (int i = 0; i < memoizedResults.length; i++) {
                for (int j = 0; j < memoizedResults[i].length; j++) {
                    for (int k = 0; k < memoizedResults[i][j].length; k++) {
                        for (int l = 0; l < memoizedResults[i][j][k].length; l++) {
                            total++;
                            if (memoizedResults[i][j][k][l] != null) {
                                used++;
                            }
                        }
                    }
                }
            }
            System.err.println("INFO: total size=" + total + ", used=" + used + " (" + String.format("%2.2f",100.0 * used / total) + "%)");
        }
        return result;
    }
/**Encode the string stringToEncode for the version size versionSize starting at position position starting in the mode mode. The function returns the number of bits it used to encode the string. The number is minimal. 
 * When the function is called recursively without changing the mode, numberOfCharactersSinceLastModeChange needs to be incremented by one*/
//TODO: change this method to be iterative
    ResultNode encode(int position,int charsetEncoderIndex,VersionSize versionSize,Mode mode,int numberOfCharactersSinceLastModeChange) {
        if (memoizedResults[position][charsetEncoderIndex][versionSize.ordinal()][getCompactedOrdinal(mode)] != null) {
            return memoizedResults[position][charsetEncoderIndex][versionSize.ordinal()][getCompactedOrdinal(mode)];
        }
        assert position < stringToEncode.length();

        if (mode != null) {
            //is is up to the caller to ensure that the number of processed characters is a multiple of the granularity in which characters in the current mode are packed.
            assert getEncodingGranularity(mode) == 0 || numberOfCharactersSinceLastModeChange % getEncodingGranularity(mode) == 0;
            //is is up to the caller to ensure that the number of processed characters doesn't exceed the maximum number of characters that can follow the current mode type in the current version.
            assert numberOfCharactersSinceLastModeChange <= getMaximumNumberOfEncodeableCharacters(versionSize,mode);
        }

//compute results for KANJI, ALPHANUMERIC and NUMERIC
        final Mode[]   modes = {Mode.KANJI, Mode.ALPHANUMERIC, Mode.NUMERIC};
        Vector<ResultNode> results = new Vector<ResultNode>();
        for (int i = 0; i < modes.length; i++) {
            Mode newMode = modes[i];
            int need = getEncodingGranularity(newMode);
            assert need > 0;
            if (position + need <= stringToEncode.length()) {
                boolean canEncode = true;
                for (int j = 0; j < need; j++) {
                    if (!canEncode(newMode,stringToEncode.charAt(position + j))) {
                        canEncode = false;
                        break;
                    }
                }
                if (canEncode) {
                    boolean needNewModeToken = mode != newMode || numberOfCharactersSinceLastModeChange + need > getMaximumNumberOfEncodeableCharacters(versionSize,newMode);
                    ResultNode next = position + need >= stringToEncode.length() ? null : encode(position + need,charsetEncoderIndex,versionSize,newMode,needNewModeToken ? need : numberOfCharactersSinceLastModeChange + need);
                    results.add(new ResultNode(newMode,getVersion(versionSize),needNewModeToken,position,charsetEncoderIndex,next));
                }
            }
        }
//compute results for BYTE
        Mode newMode = Mode.BYTE;
        for (int i = 0; i < encoders.length; i++) {
            if (encoders[i].canEncode(stringToEncode.charAt(position))) {
                int need = getBytesOfCharacter(position,i).length;
                boolean needECI = i != charsetEncoderIndex;
                boolean needNewModeToken = needECI || mode != newMode || numberOfCharactersSinceLastModeChange + need > getMaximumNumberOfEncodeableCharacters(versionSize,newMode);
                ResultNode next = position + 1 >= stringToEncode.length() ? null : encode(position + 1,i,versionSize,newMode,needNewModeToken ? need : numberOfCharactersSinceLastModeChange + need);
                next = new ResultNode(newMode,getVersion(versionSize),needNewModeToken,position,i,next);
                if (needECI) {
                    next = new ResultNode(Mode.ECI,getVersion(versionSize),true,position,i,next);
                }
                results.add(next);
            }
        }
//choose the smallest result
        ResultNode result = smallest(results);
        memoizedResults[position][charsetEncoderIndex][versionSize.ordinal()][getCompactedOrdinal(mode)] = result;
        return result;
    }
    byte[] getBytesOfCharacter(int position,int charsetEncoderIndex) {
        //TODO: Is there a more efficient way for a single character?
        return stringToEncode.substring(position,position + 1).getBytes(encoders[charsetEncoderIndex].charset());
    }
    class ResultNode {
        Mode mode;
        Version version;
        boolean declaresMode;
        int position;
        int charsetEncoderIndex;
        ResultNode next;
        ResultNode(Mode mode,Version version,boolean declaresMode,int position,int charsetEncoderIndex,ResultNode next) {
            assert mode != null;
            this.mode = mode;
            this.version = version;
            this.declaresMode = declaresMode;
            this.position = position;
            this.charsetEncoderIndex = charsetEncoderIndex;
            this.next = next;
        }
/** returns the size in bits*/
//TODO: change this method to be iterative
        int getSize() {
            int size = declaresMode ? 4 + mode.getCharacterCountBits(version) : 0;
            if (mode == Mode.ECI) {
                size += 8; // the ECI assignment numbers for ISO-8859-x, UTF-8 and UTF-16 are all 8 bit long
            } else if (mode == Mode.BYTE) {
                size += 8 * getBytesOfCharacter(position,charsetEncoderIndex).length;
            } else {
                size += getBitsPerEncodingUnit(mode);
            }
            if (next != null) {
                size += next.getSize();
            }
            return size;
        }
/** returns the length in encoding units*/
        int getLength() {
            if (getBitsPerEncodingUnit(mode) == 0) {
                return 0;
            }
            assert declaresMode;
            int count = 1;
            ResultNode current = next;
            while (current != null && !current.declaresMode) {
                count++;
                current = current.next;
            }
            return count;
        }
//TODO: change this method to be iterative
        public void getBits(BitArray bits) throws WriterException {
            // append mode
            bits.appendBits(mode.getBits(),4);
            if (mode == Mode.ECI) {
                String canonicalCharsetName = encoders[charsetEncoderIndex].charset().name();
                bits.appendBits(CharacterSetECI.getCharacterSetECIByName(canonicalCharsetName).getValue(),8);
                if (next != null) {
                    next.getBits(bits);
                }
            } else {
                int characterLength = getLength() * getEncodingGranularity(mode);
                if (characterLength > 0) {
                    String canonicalCharsetName = encoders[charsetEncoderIndex].charset().name();
                    String pieceToEncode = stringToEncode.substring(position,position + characterLength);
                    // append length
                    try {
                        bits.appendBits(mode == Mode.BYTE ? pieceToEncode.getBytes(canonicalCharsetName).length : characterLength,mode.getCharacterCountBits(version));
                    } catch (UnsupportedEncodingException uee) {
                        throw new WriterException(uee);
                    }
                    // append data
                    appendBytes(pieceToEncode,mode,bits,canonicalCharsetName);
                    ResultNode current = next;
                    while (current != null && !current.declaresMode) {
                        current = current.next;
                    }
                    if (current != null) {
                        current.getBits(bits);
                    }
                } else {
                    if (next != null) {
                        next.getBits(bits);
                    }
                }
            } 
        }
        public Version getVersion(ErrorCorrectionLevel ecLevel) {
            int versionNumber = version.getVersionNumber();
            int lowerLimit;
            int upperLimit;
            switch (getVersionSize(version)) {
                case Small:
                    lowerLimit = 1;
                    upperLimit = 9;
                    break;
                case Medium:
                    lowerLimit = 10;
                    upperLimit = 26;
                    break;
                case Large:
                default:
                    lowerLimit = 27;
                    upperLimit = 40;
                    break;
            }
//increase version if needed
            while (versionNumber < upperLimit && !willFit(getSize(), Version.getVersionForNumber(versionNumber), ecLevel)) {
                versionNumber++;
            }
//shrink version if possible
            while (versionNumber > lowerLimit && willFit(getSize(), Version.getVersionForNumber(versionNumber - 1), ecLevel)) {
                versionNumber--;
            }
            return Version.getVersionForNumber(versionNumber);
        }
//TODO: change this method to be iterative
        public String toString() {
            String result = "";
            if (declaresMode) {
                result += mode + "(";
            }
            if (mode == Mode.ECI) {
                result += encoders[charsetEncoderIndex].charset().displayName();
            } else {
                result += makePrintable(stringToEncode.substring(position,position + getEncodingGranularity(mode)));
            }
            if (next != null) {
                result += (next.declaresMode ? ")," : ",") + next.toString();
            } else {
                result += ")";
            }
            return result;
        }
        String makePrintable(String s) {
            String result = "";
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) < 32 || s.charAt(i) > 126) {
                    result += ".";
                } else {
                    result += s.charAt(i);
                }
            }
            return result;
        }
    }
/*
    static void testDecode(ResultNode rn) throws Exception {
        BitArray bits = new BitArray();
        rn.getBits(bits);
        int size = bits.getSize();
        byte[] bytes = new byte[size/8+1];
        bits.toBytes(0,bytes,0,size/8);
        com.google.zxing.common.DecoderResult result = com.google.zxing.qrcode.decoder.DecodedBitStreamParser.decode(bytes,rn.getVersion(ErrorCorrectionLevel.L),null,new java.util.EnumMap<>(com.google.zxing.DecodeHintType.class));
    }
*/

//Everthing below this line is copied from com.google.zxing.qrcode.encoder.Encoder and can be removed if this class is put in the same package.
//
  // The original table is defined in the table 5 of JISX0510:2004 (p.19).
  private static final int[] ALPHANUMERIC_TABLE = {
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x00-0x0f
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x10-0x1f
      36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43,  // 0x20-0x2f
      0,   1,  2,  3,  4,  5,  6,  7,  8,  9, 44, -1, -1, -1, -1, -1,  // 0x30-0x3f
      -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,  // 0x40-0x4f
      25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,  // 0x50-0x5f
  };

  /**
   * @return the code point of the table used in alphanumeric mode or
   *  -1 if there is no corresponding code in the table.
   */
  static int getAlphanumericCode(int code) {
    if (code < ALPHANUMERIC_TABLE.length) {
      return ALPHANUMERIC_TABLE[code];
    }
    return -1;
  }
  private static boolean isOnlyDoubleByteKanji(String content) {
    byte[] bytes;
    try {
      bytes = content.getBytes("Shift_JIS");
    } catch (UnsupportedEncodingException ignored) {
      return false;
    }
    int length = bytes.length;
    if (length % 2 != 0) {
      return false;
    }
    for (int i = 0; i < length; i += 2) {
      int byte1 = bytes[i] & 0xFF;
      if ((byte1 < 0x81 || byte1 > 0x9F) && (byte1 < 0xE0 || byte1 > 0xEB)) {
        return false;
      }
    }
    return true;
  }

  /*
   * Append "bytes" in "mode" mode (encoding) into "bits". On success, store the result in "bits".
   */
  static void appendBytes(String content,
                          Mode mode,
                          BitArray bits,
                          String encoding) throws WriterException {
    switch (mode) {
      case NUMERIC:
        appendNumericBytes(content, bits);
        break;
      case ALPHANUMERIC:
        appendAlphanumericBytes(content, bits);
        break;
      case BYTE:
        append8BitBytes(content, bits, encoding);
        break;
      case KANJI:
        appendKanjiBytes(content, bits);
        break;
      default:
        throw new WriterException("Invalid mode: " + mode);
    }
  }

  static void appendNumericBytes(CharSequence content, BitArray bits) {
    int length = content.length();
    int i = 0;
    while (i < length) {
      int num1 = content.charAt(i) - '0';
      if (i + 2 < length) {
        // Encode three numeric letters in ten bits.
        int num2 = content.charAt(i + 1) - '0';
        int num3 = content.charAt(i + 2) - '0';
        bits.appendBits(num1 * 100 + num2 * 10 + num3, 10);
        i += 3;
      } else if (i + 1 < length) {
        // Encode two numeric letters in seven bits.
        int num2 = content.charAt(i + 1) - '0';
        bits.appendBits(num1 * 10 + num2, 7);
        i += 2;
      } else {
        // Encode one numeric letter in four bits.
        bits.appendBits(num1, 4);
        i++;
      }
    }
  }

  static void appendAlphanumericBytes(CharSequence content, BitArray bits) throws WriterException {
    int length = content.length();
    int i = 0;
    while (i < length) {
      int code1 = getAlphanumericCode(content.charAt(i));
      if (code1 == -1) {
        throw new WriterException();
      }
      if (i + 1 < length) {
        int code2 = getAlphanumericCode(content.charAt(i + 1));
        if (code2 == -1) {
          throw new WriterException();
        }
        // Encode two alphanumeric letters in 11 bits.
        bits.appendBits(code1 * 45 + code2, 11);
        i += 2;
      } else {
        // Encode one alphanumeric letter in six bits.
        bits.appendBits(code1, 6);
        i++;
      }
    }
  }

  static void append8BitBytes(String content, BitArray bits, String encoding)
      throws WriterException {
    byte[] bytes;
    try {
      bytes = content.getBytes(encoding);
    } catch (UnsupportedEncodingException uee) {
      throw new WriterException(uee);
    }
    for (byte b : bytes) {
      bits.appendBits(b, 8);
    }
  }

  static void appendKanjiBytes(String content, BitArray bits) throws WriterException {
    byte[] bytes;
    try {
      bytes = content.getBytes("Shift_JIS");
    } catch (UnsupportedEncodingException uee) {
      throw new WriterException(uee);
    }
    if (bytes.length % 2 != 0) {
      throw new WriterException("Kanji byte size not even");
    }
    int maxI = bytes.length - 1; // bytes.length must be even
    for (int i = 0; i < maxI; i += 2) {
      int byte1 = bytes[i] & 0xFF;
      int byte2 = bytes[i + 1] & 0xFF;
      int code = (byte1 << 8) | byte2;
      int subtracted = -1;
      if (code >= 0x8140 && code <= 0x9ffc) {
        subtracted = code - 0x8140;
      } else if (code >= 0xe040 && code <= 0xebbf) {
        subtracted = code - 0xc140;
      }
      if (subtracted == -1) {
        throw new WriterException("Invalid byte sequence");
      }
      int encoded = ((subtracted >> 8) * 0xc0) + (subtracted & 0xff);
      bits.appendBits(encoded, 13);
    }
  }

  /**
   * @return true if the number of input bits will fit in a code with the specified version and
   * error correction level.
   */
  private static boolean willFit(int numInputBits, Version version, ErrorCorrectionLevel ecLevel) {
      // In the following comments, we use numbers of Version 7-H.
      // numBytes = 196
      int numBytes = version.getTotalCodewords();
      // getNumECBytes = 130
      Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
      int numEcBytes = ecBlocks.getTotalECCodewords();
      // getNumDataBytes = 196 - 130 = 66
      int numDataBytes = numBytes - numEcBytes;
      int totalInputBytes = (numInputBits + 7) / 8;
      return numDataBytes >= totalInputBytes;
  }



}

