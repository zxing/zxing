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

#include <zxing/common/CharacterSetECI.h>
#include <sstream>
#include <zxing/common/IllegalArgumentException.h>

using std::string;

using zxing::common::CharacterSetECI;
using zxing::IllegalArgumentException;

std::map<int, CharacterSetECI*> CharacterSetECI::VALUE_TO_ECI;
std::map<std::string, CharacterSetECI*> CharacterSetECI::NAME_TO_ECI;

const bool CharacterSetECI::inited = CharacterSetECI::init_tables();

bool CharacterSetECI::init_tables() {
  addCharacterSet(0, "Cp437");
  { char const* s[] = {"ISO8859_1", "ISO-8859-1", 0};
    addCharacterSet(1, s); }
  addCharacterSet(2, "Cp437");
  { char const* s[] = {"ISO8859_1", "ISO-8859-1", 0};
    addCharacterSet(3, s); }
  addCharacterSet(4, "ISO8859_2");
  addCharacterSet(5, "ISO8859_3");
  addCharacterSet(6, "ISO8859_4");
  addCharacterSet(7, "ISO8859_5");
  addCharacterSet(8, "ISO8859_6");
  addCharacterSet(9, "ISO8859_7");
  addCharacterSet(10, "ISO8859_8");
  addCharacterSet(11, "ISO8859_9");
  addCharacterSet(12, "ISO8859_10");
  addCharacterSet(13, "ISO8859_11");
  addCharacterSet(15, "ISO8859_13");
  addCharacterSet(16, "ISO8859_14");
  addCharacterSet(17, "ISO8859_15");
  addCharacterSet(18, "ISO8859_16");
  { char const* s[] = {"SJIS", "Shift_JIS", 0};
    addCharacterSet(20, s ); }
  return true;
}

CharacterSetECI::CharacterSetECI(int value, char const* encodingName_) 
  : ECI(value), encodingName(encodingName_) {}

char const* CharacterSetECI::getEncodingName() {
  return encodingName;
}

void CharacterSetECI::addCharacterSet(int value, char const* encodingName) {
  CharacterSetECI* eci = new CharacterSetECI(value, encodingName);
  VALUE_TO_ECI[value] = eci; // can't use valueOf
  NAME_TO_ECI[string(encodingName)] = eci;
}

void CharacterSetECI::addCharacterSet(int value, char const* const* encodingNames) {
  CharacterSetECI* eci = new CharacterSetECI(value, encodingNames[0]);
  VALUE_TO_ECI[value] = eci;
  for (int i = 0; encodingNames[i]; i++) {
    NAME_TO_ECI[string(encodingNames[i])] = eci;
  }
}

CharacterSetECI* CharacterSetECI::getCharacterSetECIByValue(int value) {
  if (value < 0 || value >= 900) {
    std::ostringstream oss;
    oss << "Bad ECI value: " << value;
    throw IllegalArgumentException(oss.str().c_str());
  }
  return VALUE_TO_ECI[value];
}

CharacterSetECI* CharacterSetECI::getCharacterSetECIByName(string const& name) {
  return NAME_TO_ECI[name];
}
