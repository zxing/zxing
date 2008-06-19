#ifndef __RESULT_H__
#define __RESULT_H__

/*
 *  Result.h
 *  zxing
 *
 *  Created by Christian Brunschen on 13/05/2008.
 *  Copyright 2008 Google Inc. All rights reserved.
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
#include "common/Array.h"
#include "common/Counted.h"
#include "common/Str.h"
#include "ResultPoint.h"
#include "BarcodeFormat.h"

using namespace common;

class Result : public Counted {
private:
  Ref<String> text_;
  ArrayRef<unsigned char> rawBytes_;
  ArrayRef<Ref<ResultPoint> > resultPoints_;
  BarcodeFormat format_;
public:
  Result(Ref<String> text, ArrayRef<unsigned char> rawBytes,
         ArrayRef<Ref<ResultPoint> > resultPoints,
         BarcodeFormat format) :
  text_(text), rawBytes_(rawBytes), 
  resultPoints_(resultPoints), format_(format) { }
  ~Result() { }
  Ref<String> getText() { return text_; }
  ArrayRef<unsigned char> getRawBytes() { return rawBytes_; }
  ArrayRef<Ref<ResultPoint> > getResultPoints() { return resultPoints_; }
  BarcodeFormat getBarcodeFormat() { return format_; }
  
  friend ostream& operator<<(ostream &out, Result& result);
};

#endif // __RESULT_H__