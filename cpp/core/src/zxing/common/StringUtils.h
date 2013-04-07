// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-

#ifndef __STRING_UTILS__
#define __STRING_UTILS__

/*
 * Copyright (C) 2010-2011 ZXing authors
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

#include <string>
#include <map>
#include <zxing/DecodeHints.h>

namespace zxing {
namespace common {

class StringUtils {
private:
  static char const* const PLATFORM_DEFAULT_ENCODING;

  StringUtils() {}

public:
  static char const* const ASCII;
  static char const* const SHIFT_JIS;
  static char const* const GB2312;
  static char const* const EUC_JP;
  static char const* const UTF8;
  static char const* const ISO88591;
  static const bool ASSUME_SHIFT_JIS;

  typedef std::map<DecodeHintType, std::string> Hashtable;

  static std::string guessEncoding(char* bytes, int length, Hashtable const& hints);
};

}
}

#endif
