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

#pragma once

#include "zxing/common/Error.hpp"

#include <string>
#include <map>     // for map

namespace pping {
namespace common {

class CharacterSetECI {
private:
  static std::map<int, CharacterSetECI*> VALUE_TO_ECI;
  static std::map<std::string, CharacterSetECI*> NAME_TO_ECI;
  static const bool inited;
  static bool init_tables();

  int const* const values_;
  char const* const* const names_;

  CharacterSetECI(int const* values, char const* const* names);

  static void addCharacterSet(int const* value, char const* const* encodingNames);

public:
  char const* name() const;
  int getValue() const;

  static pping::Fallible<CharacterSetECI*> getCharacterSetECIByValue(int value) noexcept;
  static CharacterSetECI* getCharacterSetECIByName(std::string const& name);
};

}
}
