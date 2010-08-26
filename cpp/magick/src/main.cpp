/*
 *  main.cpp
 *  zxing
 *
 *  Copyright 2010 ZXing authors All rights reserved.
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

#include <iostream>
#include <fstream>
#include <string>
#include <Magick++.h>
#include "MagickBitmapSource.h"
#include <zxing/common/Counted.h>
//#include <zxing/qrcode/QRCodeReader.h>
#include <zxing/Binarizer.h>
#include <zxing/MultiFormatReader.h>
#include <zxing/Result.h>
#include <zxing/ReaderException.h>
#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/common/HybridBinarizer.h>
#include <exception>
#include <zxing/Exception.h>
#include <zxing/common/IllegalArgumentException.h>
#include <zxing/BinaryBitmap.h>
#include <zxing/DecodeHints.h>

//#include <zxing/qrcode/detector/Detector.h>
//#include <zxing/qrcode/detector/QREdgeDetector.h>
//#include <zxing/qrcode/decoder/Decoder.h>

using namespace Magick;
using namespace std;
using namespace zxing;
//using namespace zxing::qrcode;

static bool raw_dump = false;
static bool show_format = false;
static bool tryHarder = false;
static bool show_filename = false;

static const int MAX_EXPECTED = 1024;

Ref<Result> decode(Ref<BinaryBitmap> image, DecodeHints hints) {
  Ref<Reader> reader(new MultiFormatReader);
  return reader->decode(image, hints);
}


int test_image(Image& image, bool hybrid, string expected = "") {

  string cell_result;
  int res = -1;

  Ref<BitMatrix> matrix(NULL);
  Ref<Binarizer> binarizer(NULL);
  const char* result_format = "";

  try {
    Ref<MagickBitmapSource> source(new MagickBitmapSource(image));

    if (hybrid) {
      binarizer = new HybridBinarizer(source);
    } else {
      binarizer = new GlobalHistogramBinarizer(source);
    }

    DecodeHints hints(DecodeHints::DEFAULT_HINT);
    hints.setTryHarder(tryHarder);
    Ref<BinaryBitmap> binary(new BinaryBitmap(binarizer));
    Ref<Result> result(decode(binary, hints));
    cell_result = result->getText()->getText();
    result_format = barcodeFormatNames[result->getBarcodeFormat()];
    res = 0;
  } catch (ReaderException e) {
    cell_result = "zxing::ReaderException: " + string(e.what());
    res = -2;
  } catch (zxing::IllegalArgumentException& e) {
    cell_result = "zxing::IllegalArgumentException: " + string(e.what());
    res = -3;
  } catch (zxing::Exception& e) {
    cell_result = "zxing::Exception: " + string(e.what());
    res = -4;
  } catch (std::exception& e) {
    cell_result = "std::exception: " + string(e.what());
    res = -5;
  }

  if (cell_result.compare(expected)) {
    res = -6;
    if (!raw_dump) {
        cout << (hybrid ? "Hybrid" : "Global") << " binarizer failed:\n";
        if (expected.length() >= 0) {
          cout << "  Expected: " << expected << "\n";
        }
        cout << "  Detected: " << cell_result << endl;
    }
  }


  if (raw_dump && !hybrid) {/* don't print twice, and global is a bit better */
    cout << cell_result;
    if (show_format) {
      cout << " " << result_format;
    }
    cout << endl;

  }
  return res;
}

int test_image_hybrid(Image& image, string expected = "") {
  return test_image(image, true, expected);
}

int test_image_global(Image& image, string expected = "") {
  return test_image(image, false, expected);
}

string get_expected(string imagefilename) {
  string textfilename = imagefilename;
  int dotpos = textfilename.rfind(".");
  textfilename.replace(dotpos+1, textfilename.length() - dotpos - 1, "txt");
  char data[MAX_EXPECTED];
  FILE *fp = fopen(textfilename.data(), "rb");

  if (!fp) {
    // could not open file
    return "";
  }
  // get file size
  fseek(fp, 0, SEEK_END);
  int toread = ftell(fp);
  rewind(fp);
  
  if (toread > MAX_EXPECTED) {
  	cerr << "MAX_EXPECTED = " << MAX_EXPECTED << " but file '" << textfilename << "' has " << toread
  	     << " bytes! Skipping..." << endl;
    fclose(fp);
    return "";
  }
  
  int nread = fread(data, sizeof(char), toread, fp);
  if (nread != toread) {
    cerr << "Could not read entire contents of file '" << textfilename << "'! Skipping..." << endl;
    fclose(fp);
    return "";
  }
  fclose(fp);
  data[nread] = '\0';
  string expected(data);
  return expected;
}

int main(int argc, char** argv) {
  if (argc <= 1) {
    cout << "Usage: " << argv[0] << " [--dump-raw] [--show-format] [--try-harder] [--show-filename] <filename1> [<filename2> ...]" << endl;
    return 1;
  }

  int total = 0;
  int gonly = 0;
  int honly = 0;
  int both = 0;
  int neither = 0;

  if (argc == 2) raw_dump = true;

  for (int i = 1; i < argc; i++) {
    string infilename = argv[i];
    if (infilename.substr(infilename.length()-3,3).compare("txt") == 0) {
      continue;
    }
    if (infilename.compare("--dump-raw") == 0) {
      raw_dump = true;
      continue;
    }
    if (infilename.compare("--show-format") == 0) {
      show_format = true;
      continue;
    }
    if (infilename.compare("--try-harder") == 0) {
      tryHarder = true;
      continue;
    }
    if (infilename.compare("--show-filename") == 0) {
      show_filename = true;
      continue;
    }
    if (!raw_dump)
      cerr << "Processing: " << infilename << endl;
    if (show_filename)
      cout << infilename << " ";
    Image image;
    try {
      image.read(infilename);
    } catch (...) {
      cerr << "Unable to open image, ignoring" << endl;
      continue;
    }

    string expected;
    expected = get_expected(infilename);

    int gresult = 1;
    int hresult = 1;

    hresult = test_image_hybrid(image, expected);
    gresult = test_image_global(image, expected);

    gresult = gresult == 0;
    hresult = hresult == 0;

    gonly += gresult && !hresult;
    honly += hresult && !gresult;
    both += gresult && hresult;
    neither += !gresult && !hresult;
    total = total + 1;
  }

  if (!raw_dump)
    cout << (honly+both)  << " passed hybrid, " << (gonly+both) << " passed global, "
      << both << " pass both, " << neither << " pass neither, " << honly
      << " passed only hybrid, " << gonly << " passed only global, of " << total
      << " total." << endl;

  return 0;
}


