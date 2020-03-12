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

namespace pping {
using namespace std;

String::String(const std::string &text) :
    text_(text) {
}

const std::string& String::getText() const {
  return text_;
}

//* 2012-06-01 two methods "append" added, needed in DecodedBitStreamParser (PDF417)
void String::append(const std::string &tail)
{
    text_.append(tail);
}

void String::append(char c)
{
    text_.append(1,c);
}

#if (!defined _MSC_VER) || (_MSC_VER>=1300)		//* hfn not for eMbedded c++ compiler


#else

namespace hfn {

StringComposer::StringComposer()
: String(std::string(""))
{
}

StringComposer::~StringComposer()
{
}

StringComposer::StringComposer(const String &s)
: String(s.getText())
{}

StringComposer& StringComposer::operator<< (char c)
{
    *this = StringComposer(String(getText() + c));
    return *this;
}

StringComposer& StringComposer::operator<< (const std::string src)
{
    *this = StringComposer(String(getText() + src));
    return *this;
}

StringComposer& StringComposer::operator<< (int n)
{
    char buf[32];
    sprintf(buf,"%d",n);
    *this = StringComposer(String(getText() + std::string(buf)));
    return *this;
}

StringComposer::operator const char*() const
{
    return getText().c_str();
}

const std::string &StringComposer::str() const
{
    return getText();
}

}

#endif

}
