// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 * Copyright 2008-2011 ZXing authors
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

#include <zxing/common/ECI.h>
#include <sstream>
#include <zxing/common/CharacterSetECI.h>
#include <zxing/common/IllegalArgumentException.h>

using zxing::common::ECI;
using zxing::IllegalArgumentException;

ECI::ECI(int value_) : value(value_) {}

int ECI::getValue() const {
  return value;
}

ECI* ECI::getECIByValue(int value) {
  if (value < 0 || value > 999999) {
    std::ostringstream oss;
    oss << "Bad ECI value: " << value;
    throw IllegalArgumentException(oss.str().c_str());
  }
  if (value < 900) { // Character set ECIs use 000000 - 000899
    return CharacterSetECI::getCharacterSetECIByValue(value);
  }
  return 0;
}
