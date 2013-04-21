// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#ifndef __STR_H__
#define __STR_H__

/*
 *  Str.h
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

#include <string>
#include <iostream>
#include <zxing/common/Counted.h>

namespace zxing {

class String;
std::ostream& operator << (std::ostream& out, String const& s);

class String : public Counted {
private:
  std::string text_;
public:
  explicit String(const std::string &text);
  explicit String(int);
  char charAt(int) const;
  Ref<String> substring(int) const;
  const std::string& getText() const;
  int size() const;
  void append(std::string const& tail);
  void append(char c);
  int length() const;
  friend std::ostream& zxing::operator << (std::ostream& out, String const& s);
};

}

#endif // __COMMON__STRING_H__
