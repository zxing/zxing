/*
 *  MagickBitmapSource.h
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

#ifndef MAGICKMONOCHROMEBITMAPSOURCE_H_
#define MAGICKMONOCHROMEBITMAPSOURCE_H_

#include <Magick++.h>
#include <zxing/LuminanceSource.h>

class MagickBitmapSource : public zxing::LuminanceSource {
private:
  Magick::Image& image_;
  int width;
  int height;
  const Magick::PixelPacket* pixel_cache;

public:
  MagickBitmapSource(Magick::Image& image);

  ~MagickBitmapSource();

  int getWidth();
  int getHeight();
  unsigned char getPixel(int x, int y);
};

#endif /* MAGICKMONOCHROMEBITMAPSOURCE_H_ */
