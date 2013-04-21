// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  String.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 20/05/2008.
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

#include <zxing/common/Str.h>

using std::string;
using zxing::String;
using zxing::Ref;

String::String(const std::string &text) :
  text_(text) {
}

String::String(int capacity) {
  text_.reserve(capacity);
}

const std::string& String::getText() const {
  return text_;
}

char String::charAt(int i) const { return text_[i]; }

int String::size() const { return text_.size(); }

int String::length() const { return text_.size(); }

Ref<String> String::substring(int i) const {
  return Ref<String>(new String(text_.substr(i)));
}

void String::append(const std::string &tail) {
  text_.append(tail);
}

void String::append(char c) {
  text_.append(1,c);
}

std::ostream& zxing::operator << (std::ostream& out, String const& s) {
  out << s.text_;
  return out;
}
