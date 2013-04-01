#ifndef __RESULT_H__
#define __RESULT_H__

/*
 *  Result.h
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
#include <zxing/common/Array.h>
#include <zxing/common/Counted.h>
#include <zxing/common/Str.h>
#include <zxing/ResultPoint.h>
#include <zxing/BarcodeFormat.h>

namespace zxing {

class Result : public Counted {
private:
  Ref<String> text_;
  ArrayRef<char> rawBytes_;
  ArrayRef< Ref<ResultPoint> > resultPoints_;
  BarcodeFormat format_;

public:
  Result(Ref<String> text,
         ArrayRef<char> rawBytes,
         ArrayRef< Ref<ResultPoint> > resultPoints,
         BarcodeFormat format);
  ~Result();
  Ref<String> getText();
  ArrayRef<char> getRawBytes();
  ArrayRef< Ref<ResultPoint> > const& getResultPoints() const;
  ArrayRef< Ref<ResultPoint> >& getResultPoints();
  BarcodeFormat getBarcodeFormat() const;

  friend std::ostream& operator<<(std::ostream &out, Result& result);
};

}
#endif // __RESULT_H__
