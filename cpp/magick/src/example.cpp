/*
 *  example.cpp
 *  zxing
 *
 *  Created by Ralf Kistner on 16/10/2009.
 *  Copyright 2008 ZXing authors All rights reserved.
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
#include <zxing/qrcode/QRCodeReader.h>
#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/common/LocalBlockBinarizer.h>
#include <zxing/Exception.h>

using namespace Magick;
using namespace std;
using namespace zxing;
using namespace zxing::qrcode;


void decode_image(Image& image, bool localized) {
  try {
    Ref<MagickBitmapSource> source(new MagickBitmapSource(image));
    
    Ref<Binarizer> binarizer(NULL);
    if (localized) {
      binarizer = new LocalBlockBinarizer(source);
    } else {
      binarizer = new GlobalHistogramBinarizer(source);
    }
    

    Ref<BinaryBitmap> image(new BinaryBitmap(binarizer));
    QRCodeReader reader;
    Ref<Result> result(reader.decode(image));
    
    cout << result->getText()->getText() << endl;
  } catch (zxing::Exception& e) {
    cerr << "Error: " << e.what() << endl;
  }
}


int main(int argc, char** argv) {
  if (argc < 2) {
    cout << "Usage: " << argv[0] << "<filename1> [<filename2> ...]" << endl;
    return 1;
  }
  for (int i = 1; i < argc; i++) {
    string infilename = argv[i];
    cout << "Processing: " << infilename << endl;
    Image image;
    try {
      image.read(infilename);
    } catch (...) {
      cerr << "Unable to open image, ignoring" << endl;
      continue;
    }
    
    bool local = true;	// Use local thresholding
    
    decode_image(image, local);
  }
  return 0;
}


