// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  MultiFormatOneDReader.cpp
 *  ZXing
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

#include "MultiFormatOneDReader.h"

#include <zxing/oned/MultiFormatUPCEANReader.h>
#include <zxing/oned/Code39Reader.h>
#include <zxing/oned/Code128Reader.h>
#include <zxing/oned/Code93Reader.h>
#include <zxing/oned/CodaBarReader.h>
#include <zxing/oned/ITFReader.h>
#include <zxing/ReaderException.h>
#include <zxing/NotFoundException.h>

using zxing::Ref;
using zxing::Result;
using zxing::oned::MultiFormatOneDReader;

MultiFormatOneDReader::MultiFormatOneDReader(DecodeHints hints) : readers() {
  if (hints.containsFormat(BarcodeFormat::EAN_13) ||
      hints.containsFormat(BarcodeFormat::EAN_8) ||
      hints.containsFormat(BarcodeFormat::UPC_A) ||
      hints.containsFormat(BarcodeFormat::UPC_E)) {
    readers.push_back(Ref<OneDReader>(new MultiFormatUPCEANReader(hints)));
  }
  if (hints.containsFormat(BarcodeFormat::CODE_39)) {
    readers.push_back(Ref<OneDReader>(new Code39Reader()));
  }
  if (hints.containsFormat(BarcodeFormat::CODE_93)) {
    readers.push_back(Ref<OneDReader>(new Code93Reader()));
  }
  if (hints.containsFormat(BarcodeFormat::CODE_128)) {
    readers.push_back(Ref<OneDReader>(new Code128Reader()));
  }
  if (hints.containsFormat(BarcodeFormat::ITF)) {
    readers.push_back(Ref<OneDReader>(new ITFReader()));
  }
  if (hints.containsFormat(BarcodeFormat::CODABAR)) {
    readers.push_back(Ref<OneDReader>(new CodaBarReader()));
  }
/*
  if (hints.containsFormat(BarcodeFormat::RSS_14)) {
    readers.push_back(Ref<OneDReader>(new RSS14Reader()));
  }
*/
/*
  if (hints.containsFormat(BarcodeFormat::RSS_EXPANDED)) {
    readers.push_back(Ref<OneDReader>(new RSS14ExpandedReader()));
  }
*/
  if (readers.size() == 0) {
    readers.push_back(Ref<OneDReader>(new MultiFormatUPCEANReader(hints)));
    readers.push_back(Ref<OneDReader>(new Code39Reader()));
    readers.push_back(Ref<OneDReader>(new CodaBarReader()));
    readers.push_back(Ref<OneDReader>(new Code93Reader()));
    readers.push_back(Ref<OneDReader>(new Code128Reader()));
    readers.push_back(Ref<OneDReader>(new ITFReader()));
    // readers.push_back(Ref<OneDReader>(new RSS14Reader()));
    // readers.push_back(Ref<OneDReader>(new RSS14ExpandedReader()));
  }
}

#include <typeinfo>

Ref<Result> MultiFormatOneDReader::decodeRow(int rowNumber, Ref<BitArray> row) {
  int size = readers.size();
  for (int i = 0; i < size; i++) {
    OneDReader* reader = readers[i];
    try {
      // std::cerr << "v 1 " << typeid(*reader).name() << " " << rowNumber << std::endl;
      Ref<Result> result = reader->decodeRow(rowNumber, row);
      // std::cerr << "^ 1 " << typeid(*reader).name() << " " << rowNumber << std::endl;
      return result;
    } catch (ReaderException const& re) {
      // std::cerr << "^ * " << typeid(*reader).name() << " " << rowNumber << std::endl;
      // continue
    }
  }
  // std::cerr << "throwing nfe" << std::endl;
  throw NotFoundException();
}
