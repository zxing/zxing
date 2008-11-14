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

package com.google.zxing.qrcode.encoder;

// class GF_Poly;
// #include "strings/stringpiece.h"
// #include "util/reedsolomon/galois_field.h"
// #include "util/reedsolomon/galois_poly.h"

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class Encoder {

  // The original table is defined in the table 5 of JISX0510:2004 (p.19).
  private static final int kAlphanumericTable[] = {
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x00-0x0f
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x10-0x1f
      36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43,  // 0x20-0x2f
      0,   1,  2,  3,  4,  5,  6,  7,  8,  9, 44, -1, -1, -1, -1, -1,  // 0x30-0x3f
      -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,  // 0x40-0x4f
      25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,  // 0x50-0x5f
  };

  private static final class RSBlockInfo {

    int num_bytes;
    int block_info[][];

    public RSBlockInfo(int num_bytes, int[][] block_info) {
      this.num_bytes = num_bytes;
      this.block_info = block_info;
    }

  }

  // The table is from table 12 of JISX0510:2004 (p. 30). The "block_info" parts are ordered by
  // L, M, Q, H. Within each block_info, the 0th element is num_ec_bytes, and the 1st element is
  // num_rs_blocks. The table was doublechecked by komatsu.
  private static final RSBlockInfo kRSBlockTable[] = {
      new RSBlockInfo(  26, new int[][]{ {  7,  1}, {  10,  1}, {  13,  1}, {  17,  1}}),  // Version  1
      new RSBlockInfo(  44, new int[][]{ { 10,  1}, {  16,  1}, {  22,  1}, {  28,  1}}),  // Version  2
      new RSBlockInfo(  70, new int[][]{ { 15,  1}, {  26,  1}, {  36,  2}, {  44,  2}}),  // Version  3
      new RSBlockInfo( 100, new int[][]{ { 20,  1}, {  36,  2}, {  52,  2}, {  64,  4}}),  // Version  4
      new RSBlockInfo( 134, new int[][]{ { 26,  1}, {  48,  2}, {  72,  4}, {  88,  4}}),  // Version  5
      new RSBlockInfo( 172, new int[][]{ { 36,  2}, {  64,  4}, {  96,  4}, { 112,  4}}),  // Version  6
      new RSBlockInfo( 196, new int[][]{ { 40,  2}, {  72,  4}, { 108,  6}, { 130,  5}}),  // Version  7
      new RSBlockInfo( 242, new int[][]{ { 48,  2}, {  88,  4}, { 132,  6}, { 156,  6}}),  // Version  8
      new RSBlockInfo( 292, new int[][]{ { 60,  2}, { 110,  5}, { 160,  8}, { 192,  8}}),  // Version  9
      new RSBlockInfo( 346, new int[][]{ { 72,  4}, { 130,  5}, { 192,  8}, { 224,  8}}),  // Version 10
      new RSBlockInfo( 404, new int[][]{ { 80,  4}, { 150,  5}, { 224,  8}, { 264, 11}}),  // Version 11
      new RSBlockInfo( 466, new int[][]{ { 96,  4}, { 176,  8}, { 260, 10}, { 308, 11}}),  // Version 12
      new RSBlockInfo( 532, new int[][]{ {104,  4}, { 198,  9}, { 288, 12}, { 352, 16}}),  // Version 13
      new RSBlockInfo( 581, new int[][]{ {120,  4}, { 216,  9}, { 320, 16}, { 384, 16}}),  // Version 14
      new RSBlockInfo( 655, new int[][]{ {132,  6}, { 240, 10}, { 360, 12}, { 432, 18}}),  // Version 15
      new RSBlockInfo( 733, new int[][]{ {144,  6}, { 280, 10}, { 408, 17}, { 480, 16}}),  // Version 16
      new RSBlockInfo( 815, new int[][]{ {168,  6}, { 308, 11}, { 448, 16}, { 532, 19}}),  // Version 17
      new RSBlockInfo( 901, new int[][]{ {180,  6}, { 338, 13}, { 504, 18}, { 588, 21}}),  // Version 18
      new RSBlockInfo( 991, new int[][]{ {196,  7}, { 364, 14}, { 546, 21}, { 650, 25}}),  // Version 19
      new RSBlockInfo(1085, new int[][]{ {224,  8}, { 416, 16}, { 600, 20}, { 700, 25}}),  // Version 20
      new RSBlockInfo(1156, new int[][]{ {224,  8}, { 442, 17}, { 644, 23}, { 750, 25}}),  // Version 21
      new RSBlockInfo(1258, new int[][]{ {252,  9}, { 476, 17}, { 690, 23}, { 816, 34}}),  // Version 22
      new RSBlockInfo(1364, new int[][]{ {270,  9}, { 504, 18}, { 750, 25}, { 900, 30}}),  // Version 23
      new RSBlockInfo(1474, new int[][]{ {300, 10}, { 560, 20}, { 810, 27}, { 960, 32}}),  // Version 24
      new RSBlockInfo(1588, new int[][]{ {312, 12}, { 588, 21}, { 870, 29}, {1050, 35}}),  // Version 25
      new RSBlockInfo(1706, new int[][]{ {336, 12}, { 644, 23}, { 952, 34}, {1110, 37}}),  // Version 26
      new RSBlockInfo(1828, new int[][]{ {360, 12}, { 700, 25}, {1020, 34}, {1200, 40}}),  // Version 27
      new RSBlockInfo(1921, new int[][]{ {390, 13}, { 728, 26}, {1050, 35}, {1260, 42}}),  // Version 28
      new RSBlockInfo(2051, new int[][]{ {420, 14}, { 784, 28}, {1140, 38}, {1350, 45}}),  // Version 29
      new RSBlockInfo(2185, new int[][]{ {450, 15}, { 812, 29}, {1200, 40}, {1440, 48}}),  // Version 30
      new RSBlockInfo(2323, new int[][]{ {480, 16}, { 868, 31}, {1290, 43}, {1530, 51}}),  // Version 31
      new RSBlockInfo(2465, new int[][]{ {510, 17}, { 924, 33}, {1350, 45}, {1620, 54}}),  // Version 32
      new RSBlockInfo(2611, new int[][]{ {540, 18}, { 980, 35}, {1440, 48}, {1710, 57}}),  // Version 33
      new RSBlockInfo(2761, new int[][]{ {570, 19}, {1036, 37}, {1530, 51}, {1800, 60}}),  // Version 34
      new RSBlockInfo(2876, new int[][]{ {570, 19}, {1064, 38}, {1590, 53}, {1890, 63}}),  // Version 35
      new RSBlockInfo(3034, new int[][]{ {600, 20}, {1120, 40}, {1680, 56}, {1980, 66}}),  // Version 36
      new RSBlockInfo(3196, new int[][]{ {630, 21}, {1204, 43}, {1770, 59}, {2100, 70}}),  // Version 37
      new RSBlockInfo(3362, new int[][]{ {660, 22}, {1260, 45}, {1860, 62}, {2220, 74}}),  // Version 38
      new RSBlockInfo(3532, new int[][]{ {720, 24}, {1316, 47}, {1950, 65}, {2310, 77}}),  // Version 39
      new RSBlockInfo(3706, new int[][]{ {750, 25}, {1372, 49}, {2040, 68}, {2430, 81}}),  // Version 40
  };

  private static final int kMaxNumECBytes = 68;  // See the table in Appendix A.

  private static final class ECPolyInfo {

    int ec_length;
    int coeffs[];

    public ECPolyInfo(int ec_length, int[] coefficients) {
      this.ec_length = ec_length;
      this.coeffs = coefficients;
    }

  }

// The numbers were generated using the logic found in http://www.d-project.com/qrcode/. We use
// generated numbers instead of the logic itself (don't want to copy it). The numbers are supposed
// to be identical to the ones in the table in Appendix A of JISX0510:2004 (p. 30). However, there
// are some cases the spec seems to be wrong.
private static final ECPolyInfo kECPolynomials[] = {
    new ECPolyInfo( 7,
        new int[]{   0,  87, 229, 146, 149, 238, 102,  21 }),
    // The spec lacks the coefficient for x^5 (a^46 x^5). Tested a QR code of Version 1-M (uses 10
    // error correction bytes) with a cell phone and it worked.
    new ECPolyInfo( 10,
        new int[]{   0, 251,  67,  46,  61, 118,  70,  64,  94,  32,  45 }),
    new ECPolyInfo( 13,
        new int[]{   0,  74, 152, 176, 100,  86, 100, 106, 104, 130, 218, 206,
            140,  78 }),
    new ECPolyInfo( 15,
        new int[]{   0,   8, 183,  61,  91, 202,  37,  51,  58,  58, 237, 140,
            124,   5,  99, 105 }),
    new ECPolyInfo( 16,
        new int[]{   0, 120, 104, 107, 109, 102, 161,  76,   3,  91, 191, 147,
            169, 182, 194, 225, 120 }),
    new ECPolyInfo( 17,
        new int[]{   0,  43, 139, 206,  78,  43, 239, 123, 206, 214, 147,  24,
            99, 150,  39, 243, 163, 136 }),
    new ECPolyInfo( 18,
        new int[]{   0, 215, 234, 158,  94, 184,  97, 118, 170,  79, 187, 152,
            148, 252, 179,   5,  98,  96, 153 }),
    new ECPolyInfo( 20,
        new int[]{   0,  17,  60,  79,  50,  61, 163,  26, 187, 202, 180, 221,
            225,  83, 239, 156, 164, 212, 212, 188, 190 }),
    new ECPolyInfo( 22,
        new int[]{   0, 210, 171, 247, 242,  93, 230,  14, 109, 221,  53, 200,
            74,   8, 172,  98,  80, 219, 134, 160, 105, 165, 231 }),
    new ECPolyInfo( 24,
        new int[]{   0, 229, 121, 135,  48, 211, 117, 251, 126, 159, 180, 169,
            152, 192, 226, 228, 218, 111,   0, 117, 232,  87,  96, 227,
            21 }),
    new ECPolyInfo( 26,
        new int[]{   0, 173, 125, 158,   2, 103, 182, 118,  17, 145, 201, 111,
            28, 165,  53, 161,  21, 245, 142,  13, 102,  48, 227, 153,
            145, 218,  70 }),
    new ECPolyInfo( 28,
        new int[]{   0, 168, 223, 200, 104, 224, 234, 108, 180, 110, 190, 195,
            147, 205,  27, 232, 201,  21,  43, 245,  87,  42, 195, 212,
            119, 242,  37,   9, 123 }),
    new ECPolyInfo( 30,
        new int[]{   0,  41, 173, 145, 152, 216,  31, 179, 182,  50,  48, 110,
            86, 239,  96, 222, 125,  42, 173, 226, 193, 224, 130, 156,
            37, 251, 216, 238,  40, 192, 180 }),
    // In the spec, the coefficient for x^10 is a^60 but we use the generated number a^69 instead
    // (probably it's typo in the spec).
    //
    // Anyway, there seems to be no way that error correction bytes bigger than 30 can be used in RS
    // blocks, according to table 12. It's weird why the spec has numbers for error correction bytes
    // of 32 and bigger in this table here.
    new ECPolyInfo( 32,
        new int[]{   0,  10,   6, 106, 190, 249, 167,   4,  67, 209, 138, 138,
            32, 242, 123,  89,  27, 120, 185,  80, 156,  38,  69, 171,
            60,  28, 222,  80,  52, 254, 185, 220, 241 }),
    new ECPolyInfo( 34,
        new int[]{   0, 111,  77, 146,  94,  26,  21, 108,  19, 105,  94, 113,
            193,  86, 140, 163, 125,  58, 158, 229, 239, 218, 103,  56,
            70, 114,  61, 183, 129, 167,  13,  98,  62, 129,  51 }),
    new ECPolyInfo( 36,
        new int[]{   0, 200, 183,  98,  16, 172,  31, 246, 234,  60, 152, 115,
            0, 167, 152, 113, 248, 238, 107,  18,  63, 218,  37,  87,
            210, 105, 177, 120,  74, 121, 196, 117, 251, 113, 233,  30,
            120 }),
    // The spec doesn't have a row for 38 but just in case.
    new ECPolyInfo( 38,
        new int[]{   0, 159,  34,  38, 228, 230,  59, 243,  95,  49, 218, 176,
            164,  20,  65,  45, 111,  39,  81,  49, 118, 113, 222, 193,
            250, 242, 168, 217,  41, 164, 247, 177,  30, 238,  18, 120,
            153,  60, 193 }),
    new ECPolyInfo( 40,
        new int[]{   0,  59, 116,  79, 161, 252,  98, 128, 205, 128, 161, 247,
            57, 163,  56, 235, 106,  53,  26, 187, 174, 226, 104, 170,
            7, 175,  35, 181, 114,  88,  41,  47, 163, 125, 134,  72,
            20, 232,  53,  35,  15 }),
    new ECPolyInfo( 42,
        new int[]{   0, 250, 103, 221, 230,  25,  18, 137, 231,   0,   3,  58,
            242, 221, 191, 110,  84, 230,   8, 188, 106,  96, 147,  15,
            131, 139,  34, 101, 223,  39, 101, 213, 199, 237, 254, 201,
            123, 171, 162, 194, 117,  50,  96 }),
    new ECPolyInfo( 44,
        new int[]{   0, 190,   7,  61, 121,  71, 246,  69,  55, 168, 188,  89,
            243, 191,  25,  72, 123,   9, 145,  14, 247,   1, 238,  44,
            78, 143,  62, 224, 126, 118, 114,  68, 163,  52, 194, 217,
            147, 204, 169,  37, 130, 113, 102,  73, 181 }),
    new ECPolyInfo( 46,
        new int[]{   0, 112,  94,  88, 112, 253, 224, 202, 115, 187,  99,  89,
            5,  54, 113, 129,  44,  58,  16, 135, 216, 169, 211,  36,
            1,   4,  96,  60, 241,  73, 104, 234,   8, 249, 245, 119,
            174,  52,  25, 157, 224,  43, 202, 223,  19,  82,  15 }),
    new ECPolyInfo( 48,
        new int[]{   0, 228,  25, 196, 130, 211, 146,  60,  24, 251,  90,  39,
            102, 240,  61, 178,  63,  46, 123, 115,  18, 221, 111, 135,
            160, 182, 205, 107, 206,  95, 150, 120, 184,  91,  21, 247,
            156, 140, 238, 191,  11,  94, 227,  84,  50, 163,  39,  34,
            108 }),
    new ECPolyInfo( 50,
        new int[]{   0, 232, 125, 157, 161, 164,   9, 118,  46, 209,  99, 203,
            193,  35,   3, 209, 111, 195, 242, 203, 225,  46,  13,  32,
            160, 126, 209, 130, 160, 242, 215, 242,  75,  77,  42, 189,
            32, 113,  65, 124,  69, 228, 114, 235, 175, 124, 170, 215,
            232, 133, 205 }),
    new ECPolyInfo( 52,
        new int[]{   0, 116,  50,  86, 186,  50, 220, 251,  89, 192,  46,  86,
            127, 124,  19, 184, 233, 151, 215,  22,  14,  59, 145,  37,
            242, 203, 134, 254,  89, 190,  94,  59,  65, 124, 113, 100,
            233, 235, 121,  22,  76,  86,  97,  39, 242, 200, 220, 101,
            33, 239, 254, 116,  51 }),
    new ECPolyInfo( 54,
        new int[]{   0, 183,  26, 201,  87, 210, 221, 113,  21,  46,  65,  45,
            50, 238, 184, 249, 225, 102,  58, 209, 218, 109, 165,  26,
            95, 184, 192,  52, 245,  35, 254, 238, 175, 172,  79, 123,
            25, 122,  43, 120, 108, 215,  80, 128, 201, 235,   8, 153,
            59, 101,  31, 198,  76,  31, 156 }),
    new ECPolyInfo( 56,
        new int[]{   0, 106, 120, 107, 157, 164, 216, 112, 116,   2,  91, 248,
            163,  36, 201, 202, 229,   6, 144, 254, 155, 135, 208, 170,
            209,  12, 139, 127, 142, 182, 249, 177, 174, 190,  28,  10,
            85, 239, 184, 101, 124, 152, 206,  96,  23, 163,  61,  27,
            196, 247, 151, 154, 202, 207,  20,  61,  10 }),
    new ECPolyInfo( 58,
        new int[]{   0,  82, 116,  26, 247,  66,  27,  62, 107, 252, 182, 200,
            185, 235,  55, 251, 242, 210, 144, 154, 237, 176, 141, 192,
            248, 152, 249, 206,  85, 253, 142,  65, 165, 125,  23,  24,
            30, 122, 240, 214,   6, 129, 218,  29, 145, 127, 134, 206,
            245, 117,  29,  41,  63, 159, 142, 233, 125, 148, 123 }),
    new ECPolyInfo( 60,
        new int[]{   0, 107, 140,  26,  12,   9, 141, 243, 197, 226, 197, 219,
            45, 211, 101, 219, 120,  28, 181, 127,   6, 100, 247,   2,
            205, 198,  57, 115, 219, 101, 109, 160,  82,  37,  38, 238,
            49, 160, 209, 121,  86,  11, 124,  30, 181,  84,  25, 194,
            87,  65, 102, 190, 220,  70,  27, 209,  16,  89,   7,  33,
            240 }),
    // The spec lacks the coefficient for x^5 (a^127 x^5). Anyway the number will not be used. See
    // the comment for 32.
    new ECPolyInfo( 62,
        new int[]{   0,  65, 202, 113,  98,  71, 223, 248, 118, 214,  94,   0,
            122,  37,  23,   2, 228,  58, 121,   7, 105, 135,  78, 243,
            118,  70,  76, 223,  89,  72,  50,  70, 111, 194,  17, 212,
            126, 181,  35, 221, 117, 235,  11, 229, 149, 147, 123, 213,
            40, 115,   6, 200, 100,  26, 246, 182, 218, 127, 215,  36,
            186, 110, 106 }),
    new ECPolyInfo( 64,
        new int[]{   0,  45,  51, 175,   9,   7, 158, 159,  49,  68, 119,  92,
            123, 177, 204, 187, 254, 200,  78, 141, 149, 119,  26, 127,
            53, 160,  93, 199, 212,  29,  24, 145, 156, 208, 150, 218,
            209,   4, 216,  91,  47, 184, 146,  47, 140, 195, 195, 125,
            242, 238,  63,  99, 108, 140, 230, 242,  31, 204,  11, 178,
            243, 217, 156, 213, 231 }),
    new ECPolyInfo( 66,
        new int[]{   0,   5, 118, 222, 180, 136, 136, 162,  51,  46, 117,  13,
            215,  81,  17, 139, 247, 197, 171,  95, 173,  65, 137, 178,
            68, 111,  95, 101,  41,  72, 214, 169, 197,  95,   7,  44,
            154,  77, 111, 236,  40, 121, 143,  63,  87,  80, 253, 240,
            126, 217,  77,  34, 232, 106,  50, 168,  82,  76, 146,  67,
            106, 171,  25, 132,  93,  45, 105 }),
    new ECPolyInfo( 68,
        new int[]{   0, 247, 159, 223,  33, 224,  93,  77,  70,  90, 160,  32,
            254,  43, 150,  84, 101, 190, 205, 133,  52,  60, 202, 165,
            220, 203, 151,  93,  84,  15,  84, 253, 173, 160,  89, 227,
            52, 199,  97,  95, 231,  52, 177,  41, 125, 137, 241, 166,
            225, 118,   2,  54,  32,  82, 215, 175, 198,  43, 238, 235,
            27, 101, 184, 127,   3,   5,   8, 163, 238 }),
};

  private static final int kFieldSize = 8;
  private static GF_Poly[] g_ec_polynomials = new GF_Poly[kMaxNumECBytes + 1];

  // Encode "bytes" with the error correction level "ec_level". The
  // encoding mode will be chosen internally by ChooseMode().
  // On success, store the result in "qr_code" and return true.  On
  // error, return false.  We recommend you to use QRCode.EC_LEVEL_L
  // (the lowest level) for "ec_level" since our primary use is to
  // show QR code on desktop screens.  We don't need very strong error
  // correction for this purpose.
  //
  // Note that there is no way to encode bytes in MODE_KANJI.  We might
  // want to add EncodeWithMode() with which clients can specify the
  // encoding mode.  For now, we don't need the functionality.
  public static boolean Encode(final StringPiece bytes, int ec_level, QRCode qr_code) {
    // Step 1: Choose the mode (encoding).
    final int mode = ChooseMode(bytes);

    // Step 2: Append "bytes" into "data_bits" in appropriate encoding.
    BitVector data_bits = new BitVector();
    if (!AppendBytes(bytes, mode, data_bits)) {
      return false;
    }
    // Step 3: Initialize QR code that can contain "data_bits".
    final int num_input_bytes = data_bits.num_bytes();
    if (!InitQRCode(num_input_bytes, ec_level, mode, qr_code)) {
      return false;
    }

    // Step 4: Build another bit vector that contains header and data.
    BitVector header_and_data_bits = new BitVector();
    if (!AppendModeInfo(qr_code.mode(), header_and_data_bits)) {
      return false;
    }
    if (!AppendLengthInfo(bytes.size(), qr_code.version(), qr_code.mode(),
        header_and_data_bits)) {
      return false;
    }
    header_and_data_bits.AppendBitVector(data_bits);

    // Step 5: Terminate the bits properly.
    if (!TerminateBits(qr_code.num_data_bytes(), header_and_data_bits)) {
      return false;
    }

    // Step 6: Interleave data bits with error correction code.
    BitVector final_bits = new BitVector();
    InterleaveWithECBytes(header_and_data_bits,
        qr_code.num_total_bytes(),
        qr_code.num_data_bytes(),
        qr_code.num_rs_blocks(),
        final_bits);

    // Step 7: Choose the mask pattern and set to "qr_code".
    Matrix matrix = new Matrix(qr_code.matrix_width(), qr_code.matrix_width());
    qr_code.set_mask_pattern(ChooseMaskPattern(final_bits,
        qr_code.ec_level(),
        qr_code.version(),
        matrix));
    if (qr_code.mask_pattern() == -1) {
      // There was an error.
      return false;
    }

    // Step 8.  Build the matrix and set it to "qr_code".
    MatrixUtil.BuildMatrix(final_bits,
        qr_code.ec_level(),
        qr_code.version(),
        qr_code.mask_pattern(), matrix);
    qr_code.set_matrix(matrix);
    // Step 9.  Make sure we have a valid QR Code.
    if (!qr_code.IsValid()) {
      Debug.LOG_ERROR("Invalid QR code: " + qr_code.DebugString());
      return false;
    }
    return true;
  }

  // Return the code point of the table used in alphanumeric mode. Return -1 if there is no
  // corresponding code in the table.
  private static int GetAlphanumericCode(int code) {
    if (code < kAlphanumericTable.length) {
      return kAlphanumericTable[code];
    }
    return -1;
  }

  // Choose the best mode from the content of "bytes".
  // The function is guaranteed to return valid mode.
  //
  // Note that the function does not return MODE_KANJI, as we cannot
  // distinguish Shift_JIS from other encodings such as ISO-8859-1, from
  // data bytes alone.  For example "\xE0\xE0" can be interpreted as one
  // character in Shift_JIS, but also two characters in ISO-8859-1.
  public static int ChooseMode(final StringPiece bytes) {
    boolean has_numeric = false;
    boolean has_alphanumeric = false;
    boolean has_other = false;
    for (int i = 0; i < bytes.size(); ++i) {
      final int byte = bytes[i];
      if (byte >= '0' && byte <= '9') {
      has_numeric = true;
    } else if (GetAlphanumericCode(byte) != -1) {
      has_alphanumeric = true;
    } else {
      has_other = true;
    }
    }
    if (has_other) {
      return QRCode.MODE_8BIT_BYTE;
    } else if (has_alphanumeric) {
      return QRCode.MODE_ALPHANUMERIC;
    } else if (has_numeric) {
      return QRCode.MODE_NUMERIC;
    }
    // "bytes" must be empty to reach here.
    Debug.DCHECK(bytes.empty());
    return QRCode.MODE_8BIT_BYTE;
  }

  private static int ChooseMaskPattern(final BitVector bits, int ec_level, int version,
      Matrix matrix) {
    if (!QRCode.IsValidMatrixWidth(matrix.width())) {
      Debug.LOG_ERROR("Invalid matrix width: " + matrix.width());
      return -1;
    }

    int min_penalty = Integer.MAX_VALUE;  // Lower penalty is better.
    int best_mask_pattern = -1;
    // We try all mask patterns to choose the best one.
    for (int i = 0; i < QRCode.kNumMaskPatterns; ++i) {
      final int mask_pattern = i;
      if (!MatrixUtil.BuildMatrix(bits, ec_level, version,
          mask_pattern, matrix)) {
        return -1;
      }
      final int penalty = MaskUtil.CalculateMaskPenalty(matrix);
      Debug.LOG_INFO("mask_pattern: " + mask_pattern + ", " + "penalty: " + penalty);
      if (penalty < min_penalty) {
        min_penalty = penalty;
        best_mask_pattern = mask_pattern;
      }
    }
    return best_mask_pattern;
  }

  // Initialize "qr_code" according to "num_input_bytes", "ec_level", and "mode". On success, modify
  // "qr_code" and return true. On error, return false.
  private static boolean InitQRCode(int num_input_bytes, int ec_level, int mode, QRCode qr_code) {
    qr_code.set_ec_level(ec_level);
    qr_code.set_mode(mode);

    if (!QRCode.IsValidECLevel(ec_level)) {
    Debug.LOG_ERROR("Invalid EC level: " + ec_level);
    return false;
  }

    // In the following comments, we use numbers of Version 7-H.
    for (int i = 0; i < kRSBlockTable.length; ++i) {
      final RSBlockInfo row = kRSBlockTable[i];
      // num_bytes = 196
      final int num_bytes = row.num_bytes;
      // num_ec_bytes = 130
      final int num_ec_bytes  = row.block_info[ec_level][0];
      // num_rs_blocks = 5
      final int num_rs_blocks = row.block_info[ec_level][1];
      // num_data_bytes = 196 - 130 = 66
      final int num_data_bytes = num_bytes - num_ec_bytes;
      // We want to choose the smallest version which can contain data
      // of "num_input_bytes" + some extra bits for the header (mode
      // info and length info). The header can be three bytes
      // (precisely 4 + 16 bits) at most.  Hence we do +3 here.
      if (num_data_bytes >= num_input_bytes + 3) {
        // Yay, we found the proper rs block info!
        qr_code.set_version(i + 1);
        qr_code.set_num_total_bytes(num_bytes);
        qr_code.set_num_data_bytes(num_data_bytes);
        qr_code.set_num_rs_blocks(num_rs_blocks);
        // num_ec_bytes = 196 - 66 = 130
        qr_code.set_num_ec_bytes(num_bytes - num_data_bytes);
        // num_matrix_width = 21 + 6 * 4 = 45
        qr_code.set_matrix_width(21 + i * 4);
        return true;
      }
    }
    Debug.LOG_ERROR("Cannot find proper rs block info (input data too big?)");
    return false;
  }

  // Terminate bits as described in 8.4.8 and 8.4.9 of JISX0510:2004 (p.24).
  static boolean TerminateBits(int num_data_bytes, BitVector bits) {
    final int capacity = num_data_bytes * 8;
    if (bits.size() > capacity) {
      Debug.LOG_ERROR("data bits cannot fit in the QR Code" + bits.size() + " > " + capacity);
      return false;
    }
    // Append termination bits. See 8.4.8 of JISX0510:2004 (p.24) for details.
    for (int i = 0; i < 4 && bits.size() < capacity; ++i) {
      bits.AppendBit(0);
    }
    final int num_bits_in_last_byte = bits.size() % 8;
    // If the last byte isn't 8-bit aligned, we'll add padding bits.
    if (num_bits_in_last_byte > 0) {
      final int num_padding_bits = 8 - num_bits_in_last_byte;
      for (int i = 0; i < num_padding_bits; ++i) {
        bits.AppendBit(0);
      }
    }
    // Should be 8-bit aligned here.
    Debug.DCHECK_EQ(0, bits.size() % 8);
    // If we have more space, we'll fill the space with padding patterns defined in 8.4.9 (p.24).
    final int num_padding_bytes = num_data_bytes - bits.num_bytes();
    for (int i = 0; i < num_padding_bytes; ++i) {
      if (i % 2 == 0) {
        bits.AppendBits(0xec, 8);
      } else {
        bits.AppendBits(0x11, 8);
      }
    }
    Debug.DCHECK_EQ(bits.size(), capacity);  // Should be same.
    return bits.size() == capacity;
  }

  // Get number of data bytes and number of error correction bytes for block id "block_id". Store
  // the result in "num_data_bytes_in_block", and "num_ec_bytes_in_block". See table 12 in 8.5.1 of
  // JISX0510:2004 (p.30)
  static void GetNumDataBytesAndNumECBytesForBlockID(int num_total_bytes, int num_data_bytes,
      int num_rs_blocks, int block_id, Integer num_data_bytes_in_block,
      Integer num_ec_bytes_in_block) {
    Debug.DCHECK_LT(block_id, num_rs_blocks);
    // num_rs_blocks_in_group2 = 196 % 5 = 1
    final int num_rs_blocks_in_group2 = num_total_bytes % num_rs_blocks;
    // num_rs_blocks_in_group1 = 5 - 1 = 4
    final int num_rs_blocks_in_group1 = num_rs_blocks - num_rs_blocks_in_group2;
    // num_total_bytes_in_group1 = 196 / 5 = 39
    final int num_total_bytes_in_group1 = num_total_bytes / num_rs_blocks;
    // num_total_bytes_in_group2 = 39 + 1 = 40
    final int num_total_bytes_in_group2 = num_total_bytes_in_group1 + 1;
    // num_data_bytes_in_group1 = 66 / 5 = 13
    final int num_data_bytes_in_group1 = num_data_bytes / num_rs_blocks;
    // num_data_bytes_in_group2 = 13 + 1 = 14
    final int num_data_bytes_in_group2 = num_data_bytes_in_group1 + 1;
    // num_ec_bytes_in_group1 = 39 - 13 = 26
    final int num_ec_bytes_in_group1 = num_total_bytes_in_group1 -
        num_data_bytes_in_group1;
    // num_ec_bytes_in_group2 = 40 - 14 = 26
    final int num_ec_bytes_in_group2 = num_total_bytes_in_group2 -
        num_data_bytes_in_group2;
    // Sanity checks.
    // 26 = 26
    Debug.DCHECK_EQ(num_ec_bytes_in_group1, num_ec_bytes_in_group2);
    // 5 = 4 + 1.
    Debug.DCHECK_EQ(num_rs_blocks, num_rs_blocks_in_group1 + num_rs_blocks_in_group2);
    // 196 = (13 + 26) * 4 + (14 + 26) * 1
    Debug.DCHECK_EQ(num_total_bytes,
        ((num_data_bytes_in_group1 + num_ec_bytes_in_group1) *
            num_rs_blocks_in_group1) +
            ((num_data_bytes_in_group2 + num_ec_bytes_in_group2) *
                num_rs_blocks_in_group2));

    if (block_id < num_rs_blocks_in_group1) {
      num_data_bytes_in_block = num_data_bytes_in_group1;
      num_ec_bytes_in_block = num_ec_bytes_in_group1;
    } else {
      num_data_bytes_in_block = num_data_bytes_in_group2;
      num_ec_bytes_in_block = num_ec_bytes_in_group2;
    }
  }

  // Interleave "bits" with corresponding error correction bytes.  On
  // success, store the result in "result" and return true.  On error,
  // return false.
  // The interleave rule is complicated.  See 8.6 of JISX0510:2004
  // (p.37) for details.
  static boolean InterleaveWithECBytes(final BitVector bits,
                                       int num_total_bytes,
                                       int num_data_bytes,
                                       int num_rc_blocks,
                                       BitVector result) {
    // "bits" must have "num_data_bytes" bytes of data.
    Debug.DCHECK(bits.num_bytes() == num_data_bytes);

    // Step 1.  Divide data bytes into blocks and generate error
    // correction bytes for them.  We'll store the divided data bytes
    // blocks and error correction bytes blocks into "blocks".
    typedef pair<StringPiece, String> BlockPair;
    int data_bytes_offset = 0;
    // JAVAPORT: This is not a String, it's really a byte[]
    final String &encoded_bytes = bits.ToString();
    int max_num_data_bytes = 0;  // StringPiece's size is "int".
    size_t max_num_ec_bytes = 0;  // STL String's size is "size_t".
    vector<BlockPair> blocks;
    // Since, we know the number of reedsolmon blocks, we can initialize
    // the vector with the number.
    blocks.resize(num_rs_blocks);

    for (int i = 0; i < num_rs_blocks; ++i) {
      int num_data_bytes_in_block, num_ec_bytes_in_block;
      GetNumDataBytesAndNumECBytesForBlockID(
          num_total_bytes, num_data_bytes, num_rs_blocks, i,
          &num_data_bytes_in_block, &num_ec_bytes_in_block);
      // We modify the objects in the vector instead of copying new
      // objects to the vector.  In particular, we want to avoid String
      // copies.
      StringPiece *data_bytes = &(blocks[i].first);
      String *ec_bytes = &(blocks[i].second);

      data_bytes.set(encoded_bytes.data() + data_bytes_offset,
          num_data_bytes_in_block);
      GenerateECBytes(*data_bytes, num_ec_bytes_in_block, ec_bytes);

      max_num_data_bytes = max(max_num_data_bytes, data_bytes.size());
      max_num_ec_bytes = max(max_num_ec_bytes, ec_bytes.size());
      data_bytes_offset += num_data_bytes_in_block;
    }
    Debug.DCHECK_EQ(num_data_bytes, data_bytes_offset);

    // First, place data blocks.
    for (int i = 0; i < max_num_data_bytes; ++i) {
      for (int j = 0; j < blocks.size(); ++j) {
        final StringPiece &data_bytes = blocks[j].first;
        if (i < data_bytes.size()) {
          result.AppendBits(data_bytes[i], 8);
        }
      }
    }
    // Then, place error correction blocks.
    for (int i = 0; i < max_num_ec_bytes; ++i) {
      for (int j = 0; j < blocks.size(); ++j) {
        final String &ec_bytes = blocks[j].second;
        if (i < ec_bytes.size()) {
          result.AppendBits(ec_bytes[i], 8);
        }
      }
    }
    if (num_total_bytes == result.num_bytes()) {  // Should be same.
      return true;
    }
    Debug.LOG_ERROR("Interleaving error: " + num_total_bytes + " and " + result.num_bytes() +
        "differ.");
    return false;
  }

  // Append mode info.  On success, store the result in "bits" and
  // return true.  On error, return false.
  static boolean AppendModeInfo(int mode, BitVector bits) {
    final int code = QRCode.GetModeCode(mode);
    if (code == -1) {
      Debug.LOG_ERROR("Invalid mode: " + mode);
      return false;
    }
    bits.AppendBits(code, 4);
    return true;
  }


  // Append length info.  On success, store the result in "bits" and
  // return true.  On error, return false.
  static boolean AppendLengthInfo(int num_bytes, int version, int mode, BitVector bits) {
    int num_letters = num_bytes;
    // In Kanji mode, a letter is represented in two bytes.
    if (mode == QRCode.MODE_KANJI) {
    Debug.DCHECK_EQ(0, num_letters % 2);
    num_letters /= 2;
  }

    final int num_bits = QRCode.GetNumBitsForLength(version, mode);
    if (num_bits == -1) {
      Debug.LOG_ERROR("num_bits unset");
      return false;
    }
    if (num_letters > ((1 << num_bits) - 1)) {
      Debug.LOG_ERROR(num_letters + "is bigger than" + ((1 << num_bits) - 1));
      return false;
    }
    bits.AppendBits(num_letters, num_bits);
    return true;
  }

  // Append "bytes" in "mode" mode (encoding) into "bits". On success, store the result in "bits"
  // and return true. On error, return false.
  static boolean AppendBytes(final StringPiece bytes, int mode, BitVector bits) {
    switch (mode) {
      case QRCode.MODE_NUMERIC:
      return AppendNumericBytes(bytes, bits);
      case QRCode.MODE_ALPHANUMERIC:
      return AppendAlphanumericBytes(bytes, bits);
      case QRCode.MODE_8BIT_BYTE:
      return Append8BitBytes(bytes, bits);
      case QRCode.MODE_KANJI:
      return AppendKanjiBytes(bytes, bits);
      default:
        break;
    }
    Debug.LOG_ERROR("Invalid mode: " + mode);
    return false;
  }

  // Append "bytes" to "bits" using QRCode.MODE_NUMERIC mode. On success, store the result in "bits"
  // and return true. On error, return false.
  static boolean AppendNumericBytes(final StringPiece bytes, BitVector bits) {
    // Validate all the bytes first.
    for (int i = 0; i < bytes.size(); ++i) {
      if (!isdigit(bytes[i])) {
        return false;
      }
    }
    for (int i = 0; i < bytes.size();) {
      final int num1 = bytes[i] - '0';
      if (i + 2 < bytes.size()) {
        // Encode three numeric letters in ten bits.
        final int num2 = bytes[i + 1] - '0';
        final int num3 = bytes[i + 2] - '0';
        bits.AppendBits(num1 * 100 + num2 * 10 + num3, 10);
        i += 3;
      } else if (i + 1 < bytes.size()) {
        // Encode two numeric letters in seven bits.
        final int num2 = bytes[i + 1] - '0';
        bits.AppendBits(num1 * 10 + num2, 7);
        i += 2;
      } else {
        // Encode one numeric letter in four bits.
        bits.AppendBits(num1, 4);
        ++i;
      }
    }
    return true;
  }

  // Append "bytes" to "bits" using QRCode.MODE_ALPHANUMERIC mode.
  // On success, store the result in "bits" and return true.  On error,
  // return false.
  static boolean AppendAlphanumericBytes(final StringPiece bytes, BitVector bits) {
    for (int i = 0; i < bytes.size();) {
      final int code1 = GetAlphanumericCode(bytes[i]);
      if (code1 == -1) {
        return false;
      }
      if (i + 1 < bytes.size()) {
        final int code2 = GetAlphanumericCode(bytes[i + 1]);
        if (code2 == -1) {
          return false;
        }
        // Encode two alphanumeric letters in 11 bits.
        bits.AppendBits(code1 * 45 + code2, 11);
        i += 2;
      } else {
        // Encode one alphanumeric letter in six bits.
        bits.AppendBits(code1, 6);
        ++i;
      }
    }
    return true;
  }

  // Append "bytes" to "bits" using QRCode.MODE_8BIT_BYTE mode.
  // On success, store the result in "bits" and return true.  On error,
  // return false.
  static boolean Append8BitBytes(final StringPiece bytes, BitVector bits) {
    for (int i = 0; i < bytes.size(); ++i) {
      bits.AppendBits(bytes[i], 8);
    }
    return true;
  }

  // Append "bytes" to "bits" using QRCode.MODE_KANJI mode.
  // On success, store the result in "bits" and return true.  On error,
  // return false.
  // See 8.4.5 of JISX0510:2004 (p.21) for how to encode Kanji bytes.
  static boolean AppendKanjiBytes(final StringPiece bytes, BitVector bits) {
    if (bytes.size() % 2 != 0) {
      Debug.LOG_ERROR("Invalid byte sequence: " + bytes);
      return false;
    }
    for (int i = 0; i < bytes.size(); i += 2) {
      Debug.DCHECK(IsValidKanji(bytes[i], bytes[i + 1]));
      final int code = (static_cast<int>(bytes[i]) << 8 | bytes[i + 1]);
      int subtracted = -1;
      if (code >= 0x8140 && code <= 0x9ffc) {
        subtracted = code - 0x8140;
      } else if (code >= 0xe040 && code <= 0xebbf) {
        subtracted = code - 0xc140;
      }
      if (subtracted == -1) {
        Debug.LOG_ERROR("Invalid byte sequence: " + bytes);
        return false;
      }
      final int encoded = ((subtracted >> 8) * 0xc0) + (subtracted & 0xff);
      bits.AppendBits(encoded, 13);
    }
    return true;
  }

  // Only call once
  static {
    InitECPolynomials();
  }

  // Initialize "g_ec_polynomials" with numbers in kECPolynomials.
  private static void InitECPolynomials() {
    final GaloisField &field = GaloisField.GetField(kFieldSize);
    for (int i = 0; i < arraysize(kECPolynomials); ++i) {
      final ECPolyInfo& ec_poly_info = kECPolynomials[i];
      final int ec_length = ec_poly_info.ec_length;
      vector<GF_Element> *coeffs = new vector<GF_Element>;
      // The number of coefficients is one more than "ec_length".
      // That's why the termination condition is <= instead of <.
      for (int j = 0; j <= ec_length; ++j) {
        // We need exp'ed numbers for later use.
        final int coeff = field.Exp(ec_poly_info.coeffs[j]);
        coeffs.push_back(coeff);
      }
      // Reverse the coefficients since the numbers in kECPolynomials
      // are ordered in reverse order to the order GF_Poly expects.
      reverse(coeffs.begin(), coeffs.end());

      GF_Poly *ec_poly = new GF_Poly(coeffs, GaloisField.GetField(kFieldSize));
      g_ec_polynomials[ec_length] = ec_poly;
    }
  }

  // Get error correction polynomials.  The polynomials are
  // defined in Appendix A of JISX0510 2004 (p. 59). In the appendix,
  // they use exponential notations for the polynomials.  We need to
  // apply GaloisField.Log() to all coefficients generated by the
  // function to compare numbers with the ones in the appendix.
  //
  // Example:
  // - Input: 17
  // - Output (in reverse order)
  //   {119,66,83,120,119,22,197,83,249,41,143,134,85,53,125,99,79}
  // - Log()'ed output (in reverse order)
  //   {0,43,139,206,78,43,239,123,206,214,147,24,99,150,39,243,163,136}
  private static final GF_Poly GetECPoly(int ec_length) {
    Debug.DCHECK_GE(kMaxNumECBytes, ec_length);
    final GF_Poly ec_poly = g_ec_polynomials[ec_length];
    Debug.DCHECK(ec_poly);
    return ec_poly;
  }

  // Generate error correction bytes of "ec_length".
  //
  // Example:
  // - Input:  {32,65,205,69,41,220,46,128,236}, ec_length = 17
  // - Output: {42,159,74,221,244,169,239,150,138,70,237,85,224,96,74,219,61}
  private static void GenerateECBytes(final StringPiece data_bytes, int ec_length, String ec_bytes) {
    // First, fill the vector with "ec_length" copies of 0.
    // They are low-order zero coefficients.
    vector<GF_Element> *coeffs = new vector<GF_Element>(ec_length, 0);
    // Then copy data_bytes backward.
    copy(data_bytes.rbegin(), data_bytes.rend(), back_inserter(*coeffs));
    // Now we have data polynomial.
    GF_Poly data_poly(coeffs, GaloisField.GetField(kFieldSize));

    // Get error correction polynomial.
    final GF_Poly &ec_poly = GetECPoly(ec_length);
    pair<GF_Poly*, GF_Poly*> divrem = GF_Poly.DivRem(data_poly, ec_poly);

    // Basically, the coefficients in the remainder polynomial are the
    // error correction bytes.
    GF_Poly *remainder = divrem.second;
    ec_bytes.reserve(ec_length);
    // However, high-order zero cofficients in the remainder polynomial
    // are ommited.  We should add zero by ourselvs.
    final int num_pruned_zero_coeffs = ec_length - (remainder.degree() + 1);
    for (int i = 0; i < num_pruned_zero_coeffs; ++i) {
      ec_bytes.push_back(0);
    }
    // Copy the remainder numbers to "ec_bytes".
    for (int i = remainder.degree(); i >= 0; --i) {
      ec_bytes.push_back(remainder.coeff(i));
    }
    Debug.DCHECK_EQ(ec_length, ec_bytes.size());
  }

  // Check if "byte1" and "byte2" can compose a valid Kanji letter
  // (2-byte Shift_JIS letter).
  // The numbers are from http://ja.wikipedia.org/wiki/Shift_JIS.
  private static boolean IsValidKanji(final char byte1, final char byte2) {
    return (byte2 != 0x7f &&
        ((byte1 >= 0x81 && byte1 <= 0x9f &&
            byte2 >= 0x40 && byte2 <= 0xfc) ||
            ((byte1 >= 0xe0 && byte1 <= 0xfc &&
                byte2 >= 0x40 && byte2 <= 0xfc))));
  }

  // Check if "bytes" is a valid Kanji sequence.
  private static boolean IsValidKanjiSequence(final StringPiece bytes) {
    if (bytes.size() % 2 != 0) {
      return false;
    }
    int i = 0;
    for (; i < bytes.size(); i += 2) {
      if (!IsValidKanji(bytes[i], bytes[i + 1])) {
        break;
      }
    }
    return i == bytes.size();  // Consumed all bytes?
  }

}
