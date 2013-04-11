/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.aztec.encoder;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import com.google.zxing.FormatException;
import com.google.zxing.aztec.AztecWriter;
import org.junit.Assert;
import org.junit.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.ResultPoint;
import com.google.zxing.aztec.AztecDetectorResult;
import com.google.zxing.aztec.decoder.Decoder;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;

/**
 * Aztec 2D generator unit tests.
 *
 * @author Rustam Abdullaev
 */
public final class EncoderTest extends Assert {

  private static final Charset LATIN_1 = Charset.forName("ISO-8859-1");
  private static final Pattern DOTX = Pattern.compile("[^.X]");
  public static final ResultPoint[] NO_POINTS = new ResultPoint[0];

  // real life tests

  @Test
  public void testEncode1() throws Exception {
    testEncode("This is an example Aztec symbol for Wikipedia.", true, 3,
        "X     X X       X     X X     X     X         \n" +
        "X         X     X X     X   X X   X X       X \n" +
        "X X   X X X X X   X X X                 X     \n" +
        "X X                 X X   X       X X X X X X \n" +
        "    X X X   X   X     X X X X         X X     \n" +
        "  X X X   X X X X   X     X   X     X X   X   \n" +
        "        X X X X X     X X X X   X   X     X   \n" +
        "X       X   X X X X X X X X X X X     X   X X \n" +
        "X   X     X X X               X X X X   X X   \n" +
        "X     X X   X X   X X X X X   X X   X   X X X \n" +
        "X   X         X   X       X   X X X X       X \n" +
        "X       X     X   X   X   X   X   X X   X     \n" +
        "      X   X X X   X       X   X     X X X     \n" +
        "    X X X X X X   X X X X X   X X X X X X   X \n" +
        "  X X   X   X X               X X X   X X X X \n" +
        "  X   X       X X X X X X X X X X X X   X X   \n" +
        "  X X   X       X X X   X X X       X X       \n" +
        "  X               X   X X     X     X X X     \n" +
        "  X   X X X   X X   X   X X X X   X   X X X X \n" +
        "    X   X   X X X   X   X   X X X X     X     \n" +
        "        X               X                 X   \n" +
        "        X X     X   X X   X   X   X       X X \n" +
        "  X   X   X X       X   X         X X X     X \n");
  }
  
  @Test
  public void testEncode2() throws Exception {
    testEncode("Aztec Code is a public domain 2D matrix barcode symbology" +
    		" of nominally square symbols built on a square grid with a " +
    		"distinctive square bullseye pattern at their center.", false, 6,
        "        X X     X X     X     X     X   X X X         X   X         X   X X       \n" +
        "  X       X X     X   X X   X X       X             X     X   X X   X           X \n" +
        "  X   X X X     X   X   X X     X X X   X   X X               X X       X X     X \n" +
        "X X X             X   X         X         X     X     X   X     X X       X   X   \n" +
        "X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X \n" +
        "    X X   X   X   X X X               X       X       X X     X X   X X       X   \n" +
        "X X     X       X       X X X X   X   X X       X   X X   X       X X   X X   X   \n" +
        "  X       X   X         X     X   X         X X       X         X     X   X   X X \n" +
        "X X   X X   X   X   X X       X X     X X     X X X   X X   X X   X X   X X X     \n" +
        "  X       X   X   X X     X X   X X         X X X   X     X     X X   X     X X X \n" +
        "  X   X X X   X X X   X   X X   X   X   X X   X X   X X X X X   X X X   X X     X \n" +
        "    X     X   X X   X   X X X X       X       X       X X X         X X     X   X \n" +
        "X X X   X           X X X X     X X X X X X X X   X       X X X     X   X   X   X \n" +
        "          X       X   X X X X     X   X           X   X X       X                 \n" +
        "  X     X X   X   X X   X X X X X X X X X X X X X X X X   X X       X   X X X     \n" +
        "    X X           X X       X                       X X X X X X             X X X \n" +
        "        X   X X   X X X   X X   X X X X X X X X X   X   X               X X X X   \n" +
        "          X X X       X     X   X               X   X X   X       X X X           \n" +
        "X X     X     X   X     X X X   X   X X X X X   X   X X       X         X   X X X \n" +
        "X X X X       X     X   X X X   X   X       X   X   X       X X X   X X       X X \n" +
        "X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X \n" +
        "    X     X       X     X   X   X   X       X   X   X       X                     \n" +
        "        X X     X X X X X   X   X   X X X X X   X   X X X     X     X   X         \n" +
        "X     X   X   X   X X X X   X   X               X   X X X   X X     X     X   X   \n" +
        "  X   X X X   X     X X X X X   X X X X X X X X X   X X X X X           X X X X   \n" +
        "    X X   X   X     X X     X                       X X X X       X   X     X     \n" +
        "    X X X X   X       X     X X X X X X X X X X X X X X       X     X   X X   X X \n" +
        "            X   X X     X     X X X X X     X X X       X X X X X   X         X   \n" +
        "X       X         X           X X   X X X X   X X   X X X     X X   X   X       X \n" +
        "X     X       X X     X     X X     X             X X   X       X     X   X X     \n" +
        "  X X X X X       X   X     X           X     X   X X X X   X X X X     X X   X X \n" +
        "X             X   X X X     X X       X       X X   X   X X     X X X         X X \n" +
        "    X   X X       X     X       X   X X X X X X   X X   X X X X X X X X X   X X   \n" +
        "    X         X X   X       X     X   X   X       X     X X X     X       X X     \n" +
        "X     X X     X X X X X X             X X X   X               X   X     X       X \n" +
        "X   X X     X               X X X X X     X X     X X X X X X X X     X   X   X X \n" +
        "X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X   X \n" +
        "X           X     X X X X     X     X         X         X   X       X X   X X X   \n" +
        "X   X   X X   X X X   X         X X     X X X X     X X   X   X     X   X       X \n" +
        "      X     X     X     X X     X   X X   X X   X         X X       X       X   X \n" +
        "X       X           X   X   X     X X   X               X     X     X X X         \n");
  }
  
  @Test
  public void testAztecWriter() throws Exception {
    testWriter("\u20AC 1 sample data.", "ISO-8859-1", 25, true, 2);
    testWriter("\u20AC 1 sample data.", "ISO-8859-15", 25, true, 2);
    testWriter("\u20AC 1 sample data.", "UTF-8",  25, true, 2);
    testWriter("\u20AC 1 sample data.", "UTF-8", 100, true, 3);
    testWriter("\u20AC 1 sample data.", "UTF-8", 300, true, 4);
    testWriter("\u20AC 1 sample data.", "UTF-8", 500, false, 5);
    // Test AztecWriter defaults
    String data = "In ut magna vel mauris malesuada";
    AztecWriter writer = new AztecWriter();
    BitMatrix matrix = writer.encode(data, BarcodeFormat.AZTEC, 0, 0);
    AztecCode aztec = Encoder.encode(data.getBytes(LATIN_1), Encoder.DEFAULT_EC_PERCENT);
    BitMatrix expectedMatrix = aztec.getMatrix();
    assertEquals(matrix, expectedMatrix);
  }
  
  // synthetic tests (encode-decode round-trip)

  @Test
  public void testEncodeDecode1() throws Exception {
    testEncodeDecode("Abc123!", true, 1);
  }
  
  @Test
  public void testEncodeDecode2() throws Exception {
    testEncodeDecode("Lorem ipsum. http://test/", true, 2);
  }
  
  @Test
  public void testEncodeDecode3() throws Exception {
    testEncodeDecode("AAAANAAAANAAAANAAAANAAAANAAAANAAAANAAAANAAAANAAAAN", true, 3);
  }
  
  @Test
  public void testEncodeDecode4() throws Exception {
    testEncodeDecode("http://test/~!@#*^%&)__ ;:'\"[]{}\\|-+-=`1029384", true, 4);
  }
  
  @Test
  public void testEncodeDecode5() throws Exception {
    testEncodeDecode("http://test/~!@#*^%&)__ ;:'\"[]{}\\|-+-=`1029384756<>/?abc", false, 5);
  }
  
  @Test
  public void testEncodeDecode10() throws Exception {
    testEncodeDecode("In ut magna vel mauris malesuada dictum. Nulla ullamcorper metus quis diam" +
        " cursus facilisis. Sed mollis quam id justo rutrum sagittis. Donec laoreet rutrum" +
        " est, nec convallis mauris condimentum sit amet. Phasellus gravida, justo et congue" +
        " auctor, nisi ipsum viverra erat, eget hendrerit felis turpis nec lorem. Nulla" +
        " ultrices, elit pellentesque aliquet laoreet, justo erat pulvinar nisi, id" +
        " elementum sapien dolor et diam.", false, 10);
  }
  
  @Test
  public void testEncodeDecode23() throws Exception {
    testEncodeDecode("In ut magna vel mauris malesuada dictum. Nulla ullamcorper metus quis diam" +
        " cursus facilisis. Sed mollis quam id justo rutrum sagittis. Donec laoreet rutrum" +
        " est, nec convallis mauris condimentum sit amet. Phasellus gravida, justo et congue" +
        " auctor, nisi ipsum viverra erat, eget hendrerit felis turpis nec lorem. Nulla" +
        " ultrices, elit pellentesque aliquet laoreet, justo erat pulvinar nisi, id" +
        " elementum sapien dolor et diam. Donec ac nunc sodales elit placerat eleifend." +
        " Sed ornare luctus ornare. Vestibulum vehicula, massa at pharetra fringilla, risus" +
        " justo faucibus erat, nec porttitor nibh tellus sed est. Ut justo diam, lobortis eu" +
        " tristique ac, p.In ut magna vel mauris malesuada dictum. Nulla ullamcorper metus" +
        " quis diam cursus facilisis. Sed mollis quam id justo rutrum sagittis. Donec" +
        " laoreet rutrum est, nec convallis mauris condimentum sit amet. Phasellus gravida," +
        " justo et congue auctor, nisi ipsum viverra erat, eget hendrerit felis turpis nec" +
        " lorem. Nulla ultrices, elit pellentesque aliquet laoreet, justo erat pulvinar" +
        " nisi, id elementum sapien dolor et diam. Donec ac nunc sodales elit placerat" +
        " eleifend. Sed ornare luctus ornare. Vestibulum vehicula, massa at pharetra" +
        " fringilla, risus justo faucibus erat, nec porttitor nibh tellus sed est. Ut justo" +
        " diam, lobortis eu tristique ac, p. In ut magna vel mauris malesuada dictum. Nulla" +
        " ullamcorper metus quis diam cursus facilisis. Sed mollis quam id justo rutrum" +
        " sagittis. Donec laoreet rutrum est, nec convallis mauris condimentum sit amet." +
        " Phasellus gravida, justo et congue auctor, nisi ipsum viverra erat, eget hendrerit" +
        " felis turpis nec lorem. Nulla ultrices, elit pellentesque aliquet laoreet, justo" +
        " erat pulvinar nisi, id elementum sapien dolor et diam.", false, 23);
  }

  @Test
  public void testEncodeDecode31() throws Exception {
    testEncodeDecode("In ut magna vel mauris malesuada dictum. Nulla ullamcorper metus quis diam" +
  		" cursus facilisis. Sed mollis quam id justo rutrum sagittis. Donec laoreet rutrum" +
  		" est, nec convallis mauris condimentum sit amet. Phasellus gravida, justo et congue" +
  		" auctor, nisi ipsum viverra erat, eget hendrerit felis turpis nec lorem. Nulla" +
  		" ultrices, elit pellentesque aliquet laoreet, justo erat pulvinar nisi, id" +
  		" elementum sapien dolor et diam. Donec ac nunc sodales elit placerat eleifend." +
  		" Sed ornare luctus ornare. Vestibulum vehicula, massa at pharetra fringilla, risus" +
  		" justo faucibus erat, nec porttitor nibh tellus sed est. Ut justo diam, lobortis eu" +
  		" tristique ac, p.In ut magna vel mauris malesuada dictum. Nulla ullamcorper metus" +
  		" quis diam cursus facilisis. Sed mollis quam id justo rutrum sagittis. Donec" +
  		" laoreet rutrum est, nec convallis mauris condimentum sit amet. Phasellus gravida," +
  		" justo et congue auctor, nisi ipsum viverra erat, eget hendrerit felis turpis nec" +
  		" lorem. Nulla ultrices, elit pellentesque aliquet laoreet, justo erat pulvinar" +
  		" nisi, id elementum sapien dolor et diam. Donec ac nunc sodales elit placerat" +
  		" eleifend. Sed ornare luctus ornare. Vestibulum vehicula, massa at pharetra" +
  		" fringilla, risus justo faucibus erat, nec porttitor nibh tellus sed est. Ut justo" +
  		" diam, lobortis eu tristique ac, p. In ut magna vel mauris malesuada dictum. Nulla" +
  		" ullamcorper metus quis diam cursus facilisis. Sed mollis quam id justo rutrum" +
  		" sagittis. Donec laoreet rutrum est, nec convallis mauris condimentum sit amet." +
  		" Phasellus gravida, justo et congue auctor, nisi ipsum viverra erat, eget hendrerit" +
  		" felis turpis nec lorem. Nulla ultrices, elit pellentesque aliquet laoreet, justo" +
  		" erat pulvinar nisi, id elementum sapien dolor et diam. Donec ac nunc sodales elit" +
  		" placerat eleifend. Sed ornare luctus ornare. Vestibulum vehicula, massa at" +
  		" pharetra fringilla, risus justo faucibus erat, nec porttitor nibh tellus sed est." +
  		" Ut justo diam, lobortis eu tristique ac, p.In ut magna vel mauris malesuada" +
  		" dictum. Nulla ullamcorper metus quis diam cursus facilisis. Sed mollis quam id" +
  		" justo rutrum sagittis. Donec laoreet rutrum est, nec convallis mauris condimentum" +
  		" sit amet. Phasellus gravida, justo et congue auctor, nisi ipsum viverra erat," +
  		" eget hendrerit felis turpis nec lorem. Nulla ultrices, elit pellentesque aliquet" +
  		" laoreet, justo erat pulvinar nisi, id elementum sapien dolor et diam. Donec ac" +
  		" nunc sodales elit placerat eleifend. Sed ornare luctus ornare. Vestibulum vehicula," +
  		" massa at pharetra fringilla, risus justo faucibus erat, nec porttitor nibh tellus" +
  		" sed est. Ut justo diam, lobortis eu tris. In ut magna vel mauris malesuada dictum." +
  		" Nulla ullamcorper metus quis diam cursus facilisis. Sed mollis quam id justo rutrum" +
  		" sagittis. Donec laoreet rutrum est, nec convallis mauris condimentum sit amet." +
  		" Phasellus gravida, justo et congue auctor, nisi ipsum viverra erat, eget" +
  		" hendrerit felis turpis nec lorem.", false, 31);
  }

  @Test
  public void testGenerateModeMessage() {
    testModeMessage(true, 2, 29, ".X .XXX.. ...X XX.. ..X .XX. .XX.X");
    testModeMessage(true, 4, 64, "XX XXXXXX .X.. ...X ..XX .X.. XX..");
    testModeMessage(false, 21, 660,  "X.X.. .X.X..X..XX .XXX ..X.. .XXX. .X... ..XXX");
    testModeMessage(false, 32, 4096, "XXXXX XXXXXXXXXXX X.X. ..... XXX.X ..X.. X.XXX");
  }

  @Test
  public void testStuffBits() {
    testStuffBits(5, ".X.X. X.X.X .X.X.",
        ".X.X. X.X.X .X.X.");
    testStuffBits(5, ".X.X. ..... .X.X",
        ".X.X. ....X ..X.X");
    testStuffBits(3, "XX. ... ... ..X XXX .X. ..",
        "XX. ..X ..X ..X ..X .XX XX. .X. ..X");
    testStuffBits(6, ".X.X.. ...... ..X.XX",
        ".X.X.. .....X. ..X.XX XXXX.");
    testStuffBits(6, ".X.X.. ...... ...... ..X.X.",
        ".X.X.. .....X .....X ....X. X.XXXX");
    testStuffBits(6, ".X.X.. XXXXXX ...... ..X.XX",
        ".X.X.. XXXXX. X..... ...X.X XXXXX.");
    testStuffBits(6,
        "...... ..XXXX X..XX. .X.... .X.X.X .....X .X.... ...X.X .....X ....XX ..X... ....X. X..XXX X.XX.X",
        ".....X ...XXX XX..XX ..X... ..X.X. X..... X.X... ....X. X..... X....X X..X.. .....X X.X..X XXX.XX .XXXXX");
  }

  @Test
  public void testHighLevelEncode() throws Exception {
    testHighLevelEncodeString("A. b.",
        "...X. ..... ...XX XXX.. ...XX XXXX. XX.X");
    testHighLevelEncodeString("Lorem ipsum.",
        ".XX.X XXX.. X.... X..XX ..XX. .XXX. ....X .X.X. X...X X.X.. X.XX. .XXX. XXXX. XX.X");
    testHighLevelEncodeString("Lo. Test 123.",
        ".XX.X XXX.. X.... ..... ...XX XXX.. X.X.X ..XX. X.X.. X.X.X ....X XXXX. ..XX .X.. .X.X XX.X");
    testHighLevelEncodeString("Lo...x",
        ".XX.X XXX.. X.... XXXX. XX.X XX.X XX.X XXX. XXX.. XX..X");
    testHighLevelEncodeString(". x://abc/.",
        "..... ...XX XXX.. XX..X ..... X.X.X ..... X.X.. ..... X.X.. ...X. ...XX ..X.. ..... X.X.. XXXX. XX.X");
  }

  @Test
  public void testHighLevelEncodeBinary() throws Exception {
    // binary short form single byte
    testHighLevelEncodeString("N\0N",
        ".XXXX XXXXX ...X. ........ .X..XXX.");
    // binary short form consecutive bytes
    testHighLevelEncodeString("N\0\u0080 A",
        ".XXXX XXXXX ...X. ........ X....... ....X ...X.");
    // binary skipping over single character
    testHighLevelEncodeString("\0a\u00FF\u0080 A",
        "XXXXX ..X.. ........ .XX....X XXXXXXXX X....... ....X ...X.");
    // binary long form optimization into 2 short forms (saves 1 bit)
    testHighLevelEncodeString(
        "\0\0\0\0 \0\0\0\0 \0\0\0\0 \0\0\0\0 \0\0\0\0 \0\0\0\0 \u0082\u0084\u0088\0 \0\0\0\0 \0\0\0\0 ",
        "XXXXX XXXXX ........ ........ ........ ........ ..X....." +
        " ........ ........ ........ ........ ..X....." +
        " ........ ........ ........ ........ ..X....." +
        " ........ ........ ........ ........ ..X....." +
        " ........ ........ ........ ........ ..X....." +
        " ........ ........ ........ ........ ..X....." +
        " X.....X. XXXXX .XXX. X....X.. X...X... ........ ..X....." +
        " ........ ........ ........ ........ ..X....." +
        " ........ ........ ........ ........ ..X.....");
    // binary long form
    testHighLevelEncodeString(
        "\0\0\0\0 \0\0\1\0 \0\0\2\0 \0\0\3\0 \0\0\4\0 \0\0\5\0 \0\0\6\0 \0\0\7\0 \0\0\u0008" +
            "\0 \0\0\u0009\0 \0\0\u00F0\0 \0\0\u00F1\0 \0\0\u00F2\0A",
        "XXXXX ..... .....X...X. ........ ........ ........ ........ ..X....." +
        " ........ ........ .......X ........ ..X....." +
        " ........ ........ ......X. ........ ..X....." +
        " ........ ........ ......XX ........ ..X....." +
        " ........ ........ .....X.. ........ ..X....." +
        " ........ ........ .....X.X ........ ..X....." +
        " ........ ........ .....XX. ........ ..X....." +
        " ........ ........ .....XXX ........ ..X....." +
        " ........ ........ ....X... ........ ..X....." +
        " ........ ........ ....X..X ........ ..X....." +
        " ........ ........ XXXX.... ........ ..X....." +
        " ........ ........ XXXX...X ........ ..X....." +
        " ........ ........ XXXX..X. ........ .X.....X");
  }
  
  // Helper routines

  private static void testEncode(String data, boolean compact, int layers, String expected) throws Exception {
    AztecCode aztec = Encoder.encode(data.getBytes(LATIN_1), 33);
    assertEquals("Unexpected symbol format (compact)", compact, aztec.isCompact());
    assertEquals("Unexpected nr. of layers", layers, aztec.getLayers());
    BitMatrix matrix = aztec.getMatrix();
    assertEquals("encode() failed", expected, matrix.toString());
  }

  private static void testEncodeDecode(String data, boolean compact, int layers) throws Exception {
    AztecCode aztec = Encoder.encode(data.getBytes(LATIN_1), 25);
    assertEquals("Unexpected symbol format (compact)", compact, aztec.isCompact());
    assertEquals("Unexpected nr. of layers", layers, aztec.getLayers());
    BitMatrix matrix = aztec.getMatrix();
    AztecDetectorResult r = 
        new AztecDetectorResult(matrix, NO_POINTS, aztec.isCompact(), aztec.getCodeWords(), aztec.getLayers());
    DecoderResult res = new Decoder().decode(r);
    assertEquals(data, res.getText());
    // Check error correction by introducing a few minor errors
    Random random = getPseudoRandom();
    matrix.flip(random.nextInt(matrix.getWidth()), random.nextInt(2));
    matrix.flip(random.nextInt(matrix.getWidth()), matrix.getHeight() - 2 + random.nextInt(2));
    matrix.flip(random.nextInt(2), random.nextInt(matrix.getHeight()));
    matrix.flip(matrix.getWidth() - 2 + random.nextInt(2), random.nextInt(matrix.getHeight()));
    r = new AztecDetectorResult(matrix, NO_POINTS, aztec.isCompact(), aztec.getCodeWords(), aztec.getLayers());
    res = new Decoder().decode(r);
    assertEquals(data, res.getText());
  }

  private static void testWriter(String data, 
                                 String charset, 
                                 int eccPercent, 
                                 boolean compact, 
                                 int layers) throws FormatException {
    // 1. Perform an encode-decode round-trip because it can be lossy.
    // 2. Aztec Decoder currently always decodes the data with a LATIN-1 charset:
    String expectedData = new String(data.getBytes(Charset.forName(charset)), LATIN_1);
    Map<EncodeHintType,Object> hints = new EnumMap<EncodeHintType,Object>(EncodeHintType.class);
    hints.put(EncodeHintType.CHARACTER_SET, charset);
    hints.put(EncodeHintType.ERROR_CORRECTION, eccPercent);
    AztecWriter writer = new AztecWriter();
    BitMatrix matrix = writer.encode(data, BarcodeFormat.AZTEC, 0, 0, hints);
    AztecCode aztec = Encoder.encode(data.getBytes(Charset.forName(charset)), eccPercent);
    assertEquals("Unexpected symbol format (compact)", compact, aztec.isCompact());
    assertEquals("Unexpected nr. of layers", layers, aztec.getLayers());
    BitMatrix matrix2 = aztec.getMatrix();
    assertEquals(matrix, matrix2);
    AztecDetectorResult r = 
        new AztecDetectorResult(matrix, NO_POINTS, aztec.isCompact(), aztec.getCodeWords(), aztec.getLayers());
    DecoderResult res = new Decoder().decode(r);
    assertEquals(expectedData, res.getText());
    // Check error correction by introducing up to eccPercent errors
    int ecWords = aztec.getCodeWords() * eccPercent / 100;
    Random random = getPseudoRandom();
    for (int i = 0; i < ecWords; i++) {
      // don't touch the core
      int x = random.nextBoolean() ?
                random.nextInt(aztec.getLayers() * 2)
                : matrix.getWidth() - 1 - random.nextInt(aztec.getLayers() * 2);
      int y = random.nextBoolean() ?
                random.nextInt(aztec.getLayers() * 2)
                : matrix.getHeight() - 1 - random.nextInt(aztec.getLayers() * 2);
      matrix.flip(x, y);
    }
    r = new AztecDetectorResult(matrix, NO_POINTS, aztec.isCompact(), aztec.getCodeWords(), aztec.getLayers());
    res = new Decoder().decode(r);
    assertEquals(expectedData, res.getText());
  }

  static Random getPseudoRandom() {
    return new SecureRandom(new byte[] {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF});
  }

  private static void testModeMessage(boolean compact, int layers, int words, String expected) {
    BitArray in = Encoder.generateModeMessage(compact, layers, words);
    assertEquals("generateModeMessage() failed", expected.replace(" ", ""), in.toString().replace(" ", ""));
  }

  private static void testStuffBits(int wordSize, String bits, String expected) {
    BitArray in = toBitArray(bits);
    BitArray stuffed = Encoder.stuffBits(in, wordSize);
    assertEquals("stuffBits() failed for input string: " + bits, 
                 expected.replace(" ", ""), 
                 stuffed.toString().replace(" ", ""));
  }

  private static BitArray toBitArray(CharSequence bits) {
    BitArray in = new BitArray();
    char[] str = DOTX.matcher(bits).replaceAll("").toCharArray();
    for (char aStr : str) {
      in.appendBit(aStr == 'X');
    }
    return in;
  }

  private static void testHighLevelEncodeString(String s, String expectedBits) {
    BitArray bits = Encoder.highLevelEncode(s.getBytes(LATIN_1));
    String receivedBits = bits.toString().replace(" ", "");
    assertEquals("highLevelEncode() failed for input string: " + s, expectedBits.replace(" ", ""), receivedBits);
  }

}
