#ifndef __MAGICK_BITMAP_SOURCE_H_
#define __MAGICK_BITMAP_SOURCE_H_
/*
 *  MagickBitmapSource.h
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

#include <Magick++.h>
#include <zxing/LuminanceSource.h>

namespace zxing {

class MagickBitmapSource : public LuminanceSource {
private:
  Magick::Image& image_;
  int width;
  int height;
  const Magick::PixelPacket* pixel_cache;

public:
  MagickBitmapSource(Magick::Image& image);

  ~MagickBitmapSource();

  int getWidth() const;
  int getHeight() const;
  unsigned char* getRow(int y, unsigned char* row);
  unsigned char* getMatrix();
  bool isRotateSupported() const;
  Ref<LuminanceSource> rotateCounterClockwise();
};

}

#endif /* MAGICKMONOCHROMEBITMAPSOURCE_H_ */
