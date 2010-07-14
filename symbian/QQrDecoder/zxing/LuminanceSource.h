/*
 *  LuminanceSource.h
 *  zxing
 *
 *  Created by Ralf Kistner on 16/10/2009.
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

#ifndef LUMINANCESOURCE_H_
#define LUMINANCESOURCE_H_

#include <zxing/common/Counted.h>

namespace zxing {

class LuminanceSource : public Counted {
public:
  LuminanceSource();
  virtual ~LuminanceSource();

  virtual int getWidth() const = 0;
  virtual int getHeight() const = 0;

  virtual unsigned char getPixel(int x, int y) const = 0;
  virtual unsigned char* copyMatrix();
};

}

#endif /* LUMINANCESOURCE_H_ */
