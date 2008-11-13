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

//class StringPiece;
// #include "third_party/png/png.h"

/**
 * JAVAPORT: This class may get thrown out in the future, or it may turn into the object which
 * returns a MonochromeBitmapSource.
 *
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class Renderer {

  // See 7.3.7 of JISX0510:2004 (p. 11).
  private static final int kQuietZoneSize = 4;

  // Render QR Code as PNG image with "cell_size".  On success, store
  // the result in "result" and return true.  On error, return false.
  // The recommended cell size for desktop screens is 3.  This
  // setting generates 87x87 pixels PNG image for version 1 QR Code
  // (21x21).  87 = (21 + 4 + 4) * 3.  4 is for surrounding white
  // space (they call it quiet zone).
  // Sorry for the long function but libpng's API is a bit complecated.
// See http://www.libpng.org/pub/png/libpng-1.2.5-manual.html for
  // details.
  public static boolean RenderAsPNG(final QRCode &qr_code, int cell_size,
                                    String *result) {
    // First, clear the result String.
    result.clear();

    // Create PNG class.
    png_structp png_ptr =  png_create_write_struct(PNG_LIBPNG_VER_STRING,
        null, null, null);
    if (png_ptr == null) {
      Debug.LOG_ERROR("Unable to create png_strupctp");
      return false;
    }

    // Create PNG info.
    png_infop info_ptr = png_create_info_struct(png_ptr);
    if (info_ptr == null) {
      Debug.LOG_ERROR("Unable to create png_infop");
      png_destroy_write_struct(&png_ptr, (png_infopp)null);
      return false;
    }

    // Calculate the width of the resulting image.  Note that the height
    // is equal to the width (i.e. the resulting image is square).
    final int image_width = (qr_code.matrix_width() +
        kQuietZoneSize * 2) * cell_size;
    // Since we use 1-bit color depth, we only need 1 bit per pixel.
    final int num_bytes_in_row = image_width / 8 +
        (image_width % 8 == 0 ? 0 : 1);
    // We'll use this storage later but we should prepare this before
    // setjmp() so that this will be deleted on error.  Today's lesson
    // is that RAII isn't reliable with setjmp/longjmp!
    scoped_array<char> row(new char[num_bytes_in_row]);

    // Erorr handling of libpng is a bit tricky.  If something bad
    // happens in libpng, they call longjmp() to get to here.
    if (setjmp(png_ptr.jmpbuf)) {
      Debug.LOG_ERROR("Something bad happened in libpng");
      png_destroy_write_struct(&png_ptr, &info_ptr);
      return false;
    }

    // Attach the pointer to the result String and the pointer to the
    // writer function.
    png_set_write_fn(png_ptr, static_cast<void*>(result), PNGWriter, null);

    // Set the image information.
    png_set_IHDR(png_ptr, info_ptr, image_width, image_width,
        1,  // The color depth is 1 (black and white).
        PNG_COLOR_TYPE_GRAY,
        PNG_INTERLACE_NONE,
        PNG_COMPRESSION_TYPE_BASE,
        PNG_FILTER_TYPE_BASE);

    // Write the file header information.
    png_write_info(png_ptr, info_ptr);

    // Quiet zone at the top.
    FillRowWithWhite(num_bytes_in_row, row.get());
    WriteRowNumTimes(png_ptr, row.get(), kQuietZoneSize * cell_size);
    // Fill data.
    for (int y = 0; y < qr_code.matrix_width(); ++y) {
      FillRowWithData(num_bytes_in_row, qr_code, y, cell_size, row.get());
      WriteRowNumTimes(png_ptr, row.get(), cell_size);
    }
    // Quiet zone at the bottom.
    FillRowWithWhite(num_bytes_in_row, row.get());
    WriteRowNumTimes(png_ptr, row.get(), kQuietZoneSize * cell_size);

    // Cleanups for libpng stuff.
    png_write_end(png_ptr, info_ptr);
    png_destroy_write_struct(&png_ptr, &info_ptr);

    // Finally, it's all done!
    return true;
  }

  // Similar to RenderAsPNG but it renders QR code from data in
  // "bytes" with error correction level "ec_level".  This is the
  // friendliest function in the QR code library.
  public static boolean RenderAsPNGFromData(final StringPiece& bytes,
                                            QRCode.ECLevel ec_level,
                                            int cell_size,
                                            String *result) {
    QRCode qr_code;
    if (!Encoder.Encode(bytes, ec_level, &qr_code)) {
    return false;
  }
    return RenderAsPNG(qr_code, cell_size, result);
  }

  // Callback function which gets called by png_write_row().
  private static void PNGWriter(png_structp png_ptr, png_bytep data, png_size_t length) {
    String* out = static_cast<String*>(png_get_io_ptr(png_ptr));
    out.append(reinterpret_cast<char*>(data), length);
  }

  // Fill all pixels in "row" with white.
  private static void FillRowWithWhite(final int num_bytes_in_row, char *row) {
    memset(row, 0xff, num_bytes_in_row);
  }

  // Set the bit in "row" pointed by "index" to 1 (1 is for white).
  private static void SetBit(final int index, char *row) {
    final int byte_index = index / 8;
    final int bit_index = index % 8;
    row[byte_index] |= 0x80 >> bit_index;
  }

  // Fill pixels in "row" with data in "qr_code".
  private static void FillRowWithData(final int num_bytes_in_row,
                                      final QRCode &qr_code,
                                      final int y,
                                      final int cell_size,
                                      char *row) {
    memset(row, 0, num_bytes_in_row);  // Fill all pixels with black.

    int index = 0;
    for (int i = 0; i < kQuietZoneSize * cell_size; ++i) {
      SetBit(index++, row);  // Cells in the quite zone should be white.
    }
    for (int x = 0; x < qr_code.matrix_width(); ++x) {
      for (int i = 0; i < cell_size; ++i) {
        if (qr_code.at(x, y) == 0) {  // White cell.
          SetBit(index, row);
        }
        ++index;
      }
    }
    for (int i = 0; i < kQuietZoneSize * cell_size; ++i) {
      SetBit(index++, row);
    }
  }

  // Write pixels in "row" to "png_ptr" "num" times.
  private static void WriteRowNumTimes(png_structp png_ptr, char *row, final int num) {
    for (int i = 0; i < num; ++i) {
      png_write_row(png_ptr, reinterpret_cast<png_bytep>(row));
    }
  }

}
