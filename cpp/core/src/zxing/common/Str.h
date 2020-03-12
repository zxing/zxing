#pragma once

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

#include <zxing/common/Counted.h>
#include <string>

namespace pping {

class String : public Counted {
private:
  std::string text_;
public:
  String(const std::string &text);
  const std::string &getText() const;
  void append(const std::string &tail);
  void append(char c);
};

#if (defined _MSC_VER) && (_MSC_VER<1300)      //* hfn for eMbedded c++ compiler
        //* 2012-05-07 hfn class StringComposer, that uses the operator "<<" similarly
        //* as the stream classes. For eMbedded VC++ only because stream classes are not
        //* defined there.
namespace hfn {

class StringComposer: public String {
public:
    StringComposer();
    virtual ~StringComposer();
    StringComposer(const String &s);
    StringComposer& operator<< (char c);
    StringComposer& operator<< (const std::string src);
    StringComposer& operator<< (int n);
    operator const char*() const;
    const std::string &str() const;
};

}

#endif

#if (!defined _MSC_VER) || (_MSC_VER>=1300)		//* hfn not for eMbedded c++ compiler
#define _STRING_RESULT		mb::stringstreamlite
#else
#define _STRING_RESULT		hfn::StringComposer
#endif

}

