/*
 *  main.cpp
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
#include <zxing/common/Counted.h>
#include <zxing/qrcode/QRCodeReader.h>
#include <zxing/Result.h>
#include <zxing/ReaderException.h>
#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/common/LocalBlockBinarizer.h>
#include <exception>
#include <zxing/Exception.h>
#include <zxing/common/IllegalArgumentException.h>
#include <zxing/BinaryBitmap.h>

#include <zxing/qrcode/detector/Detector.h>
#include <zxing/qrcode/detector/QREdgeDetector.h>
#include <zxing/qrcode/decoder/Decoder.h>

using namespace Magick;
using namespace std;
using namespace zxing;
using namespace zxing::qrcode;

void draw_matrix(Image& image, Ref<BitMatrix> matrix) {
  int width = matrix->getWidth();
  int height = matrix->getHeight();
//	image.modifyImage();
//	image.type(TrueColorType);

  PixelPacket* pixels = image.getPixels(0, 0, width, height);

  PixelPacket* pixel = pixels;
  ColorMono color;
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      color.mono(!matrix->get(x, y));
      *pixel = color;
      pixel++;
    }
  }
  image.syncPixels();
}

void save_matrix(Ref<BitMatrix> matrix, string filename, float scale = 1.0) {
  Image image(Geometry(matrix->getWidth(), matrix->getHeight()), Color(MaxRGB, MaxRGB, MaxRGB, 0));
  int width = matrix->getWidth();
  int height = matrix->getHeight();
  draw_matrix(image, matrix);
  image.scale(Geometry(width*scale, height*scale));
  image.write(filename);
}

void save_grid(Ref<BitMatrix> matrix, string filename, Ref<PerspectiveTransform> transform, int dimension) {
  Image image(Geometry(matrix->getWidth(), matrix->getHeight()), Color(MaxRGB, MaxRGB, MaxRGB, 0));

  draw_matrix(image, matrix);

  image.strokeColor(Color(MaxRGB, 0, 0, MaxRGB / 3));
  image.fillColor(Color(0, 0, 0, MaxRGB));
  image.strokeWidth(1);

  for (int i = 0; i <= dimension; i++) {
    vector<float> tpoints(4);

    tpoints[0] = 0;
    tpoints[1] = i;
    tpoints[2] = dimension;
    tpoints[3] = i;
    transform->transformPoints(tpoints);

    DrawableLine line1(tpoints[0], tpoints[1], tpoints[2], tpoints[3]);
    image.draw(line1);

    tpoints[0] = i;
    tpoints[1] = 0;
    tpoints[2] = i;
    tpoints[3] = dimension;
    transform->transformPoints(tpoints);

    DrawableLine line2(tpoints[0], tpoints[1], tpoints[2], tpoints[3]);
    image.draw(line2);
  }

  image.write(filename);
}

Ref<Result> decode(string out_prefix, Ref<BinaryBitmap> image, string& cell_grid, string& cell_transformed) {
  Decoder decoder;

  QREdgeDetector detector = QREdgeDetector(image->getBlackMatrix());

  Ref<DetectorResult> detectorResult(detector.detect());

  if (out_prefix.size()) {
    // Grid image
    string gridfile = out_prefix + ".grid.gif";
    Ref<PerspectiveTransform> transform = detectorResult->getTransform();
    int dimension = detectorResult->getBits()->getDimension();
    save_grid(image->getBlackMatrix(), gridfile, transform, dimension);
    cell_grid = "<img src=\"" + gridfile + "\" />";

    // Transformed image
    string tfile = out_prefix + ".transformed.png";
    save_matrix(detectorResult->getBits(), tfile, 5);
    cell_transformed = "<img src=\"" + tfile + "\" />";
  }


  vector<Ref<ResultPoint> > points(detectorResult->getPoints());

  Ref<DecoderResult> decoderResult(decoder.decode(detectorResult->getBits()));

  Ref<Result> result(new Result(decoderResult->getText(),
                                decoderResult->getRawBytes(),
                                points,
                                BarcodeFormat_QR_CODE));

  return result;
}




int test_image(Image& image, string out_prefix, bool localized) {
  string cell_mono;
  string cell_transformed;
  string cell_result;
  string cell_grid;
  string result_color = "red";
  int res = -1;

  Ref<BitMatrix> matrix(NULL);
  Ref<Binarizer> binarizer(NULL);


  try {
    Ref<MagickBitmapSource> source(new MagickBitmapSource(image));

    if (localized) {
      binarizer = new LocalBlockBinarizer(source);
    } else {
      binarizer = new GlobalHistogramBinarizer(source);
    }

    if (out_prefix.size()) {
      string monofile = out_prefix + ".mono.png";
      matrix = binarizer->getBlackMatrix();
      save_matrix(matrix, monofile);
      cell_mono = "<img src=\"" + monofile + "\" />";
    }

    Ref<BinaryBitmap> binary(new BinaryBitmap(binarizer));
    Ref<Result> result(decode(out_prefix, binary, cell_grid, cell_transformed));
    cell_result = result->getText()->getText();
    result_color = "green";
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

  cout << "<td>" << cell_mono << "</td>" << endl;
  cout << "<td>" << cell_grid << "</td>" << endl;
  cout << "<td>" << cell_transformed << "</td>" << endl;
  cout << "<td bgcolor=\"" << result_color << "\">" << cell_result << "</td>" << endl;
  return res;
}

int test_image_local(Image& image, string out_prefix) {
  return test_image(image, out_prefix, true);
}

int test_image_global(Image& image, string out_prefix) {
  return test_image(image, out_prefix, false);
}


int main(int argc, char** argv) {
  if (argc <= 2) {
    cout << "Usage: " << argv[0] << " [<outfolder> | \"-\"] <filename1> [<filename2> ...]" << endl;
    return 1;
  }
  string outfolder = argv[1];

  int total = argc - 2;
  int gonly = 0;
  int lonly = 0;
  int both = 0;
  int neither = 0;

  cout << "<html><body><table border=\"1\">" << endl;
  for (int i = 2; i < argc; i++) {
    string infilename = argv[i];
    cerr << "Processing: " << infilename << endl;
    Image image;
    try {
      image.read(infilename);
    } catch (...) {
      cerr << "Unable to open image, ignoring" << endl;
      continue;
    }
    cout << "<tr><td colspan=\"5\">" << infilename << "</td></tr>" << endl;
    cout << "<tr>" << endl;

    cout << "<td><img src=\"" << infilename << "\" /></td>" << endl;


    int gresult = 1;
    int lresult = 1;

    if (outfolder == string("-")) {
      gresult = test_image_global(image, "");
      lresult = test_image_local(image, "");
    } else {
      replace(infilename.begin(), infilename.end(), '/', '_');
      string prefix = string(outfolder) + string("/") + infilename;
      gresult = test_image_global(image, prefix + ".g");
      lresult = test_image_local(image, prefix + ".l");
    }

    gresult = gresult == 0;
    lresult = lresult == 0;

    gonly += gresult && !lresult;
    lonly += lresult && !gresult;
    both += gresult && lresult;
    neither += !gresult && !lresult;

    cout << "</tr>" << endl;
  }
  cout << "</table>" << endl;

  cout << "<table>" << endl;
  cout << "<tr><td>Total</td><td>" << total << "</td></tr>" << endl;
  cout << "<tr><td>Both correct</td><td>" << both << "</td></tr>" << endl;
  cout << "<tr><td>Neither correct</td><td>" << neither << "</td></tr>" << endl;
  cout << "<tr><td>Global only</td><td>" << gonly << "</td></tr>" << endl;
  cout << "<tr><td>Local only</td><td>" << lonly << "</td></tr>" << endl;

  cout << "</table>" << endl;
  cout << "</body></html>" << endl;

  return 0;
}


