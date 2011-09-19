#ifndef __LUMINANCESOURCE_H__
#define __LUMINANCESOURCE_H__
/*
 *  LuminanceSource.h
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
#include <string.h>

namespace zxing {

class LuminanceSource : public Counted {
public:
  LuminanceSource();
  virtual ~LuminanceSource();

  virtual int getWidth() const = 0;
  virtual int getHeight() const = 0;

  // Callers take ownership of the returned memory and must call delete [] on it themselves.
  virtual unsigned char* getRow(int y, unsigned char* row) = 0;
  virtual unsigned char* getMatrix() = 0;

  virtual bool isCropSupported() const;
  virtual Ref<LuminanceSource> crop(int left, int top, int width, int height);

  virtual bool isRotateSupported() const;
  virtual Ref<LuminanceSource> rotateCounterClockwise();

};

}

#endif /* LUMINANCESOURCE_H_ */
