/*
 *  main.cpp
 *  zxing
 *
 *  Created by Ralf Kistner on 16/10/2009.
 *  Copyright 2008 ZXing authors All rights reserved.
 *  Modified by Yakov Okshtein (flyashi@gmail.com) to add 1D barcode support.
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
//#include <zxing/common/LocalBlockBinarizer.h>
#include <exception>
#include <zxing/Exception.h>
#include <zxing/common/IllegalArgumentException.h>
#include <zxing/BinaryBitmap.h>

//#include <zxing/qrcode/detector/Detector.h>
//#include <zxing/qrcode/detector/QREdgeDetector.h>
//#include <zxing/qrcode/decoder/Decoder.h>

using namespace Magick;
using namespace std;
using namespace zxing;
//using namespace zxing::qrcode;

Ref<Result> decode(Ref<BinaryBitmap> image) {
  Ref<Reader> reader(new MultiFormatReader);
  return Ref<Result> (new Result(*reader->decode(image)));
}


int test_image(Image& image, bool localized) {

  string cell_result;
  int res = -1;

  Ref<BitMatrix> matrix(NULL);
  Ref<Binarizer> binarizer(NULL);


  try {
    Ref<MagickBitmapSource> source(new MagickBitmapSource(image));

    if (localized) {
      //binarizer = new LocalBlockBinarizer(source);
    } else {
      binarizer = new GlobalHistogramBinarizer(source);
    }

    Ref<BinaryBitmap> binary(new BinaryBitmap(binarizer));
    Ref<Result> result(decode(binary));
    cell_result = result->getText()->getText();
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

  cout << cell_result;
  return res;
}

int test_image_local(Image& image) {
  return test_image(image, true);
}

int test_image_global(Image& image) {
  return test_image(image, false);
}

int main(int argc, char** argv) {
  if (argc <= 1) {
    cout << "Usage: " << argv[0] << " <filename1> [<filename2> ...]" << endl;
    return 1;
  }

 // int total = argc - 2;
  int gonly = 0;
  int lonly = 0;
  int both = 0;
  int neither = 0;

  for (int i = 1; i < argc; i++) {
    string infilename = argv[i];
//    cerr << "Processing: " << infilename << endl;
    Image image;
    try {
      image.read(infilename);
    } catch (...) {
      cerr << "Unable to open image, ignoring" << endl;
      continue;
    }

    int gresult = 1;
    int lresult = 1;

    gresult = test_image_global(image);
//    lresult = test_image_local(image);

    gresult = gresult == 0;
 //   lresult = lresult == 0;

    gonly += gresult && !lresult;
    lonly += lresult && !gresult;
    both += gresult && lresult;
    neither += !gresult && !lresult;

  }
  return 0;
}


