//
//  GrayBytesMonochromeBitmapSource.h
//  ZXing
//
//  Created by Thomas Recloux, Norsys on 04/12/2009.
/*
 * Copyright 2008 ZXing authors
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

#ifndef __GRAY_BYTES_MONOCHROM_BITMAP_SOURCE_H__
#define __GRAY_BYTES_MONOCHROM_BITMAP_SOURCE_H__

#include <zxing/LuminanceSource.h>

class GrayBytesMonochromeBitmapSource : public zxing::LuminanceSource {
private:
	int width_;
	int height_;
	const unsigned char *bytes_;
	int bytesPerRow_;
	
public:
	GrayBytesMonochromeBitmapSource(const unsigned char *bytes, 
									int width, 
									int height,
									int bytesPerRow);
	virtual ~GrayBytesMonochromeBitmapSource() { }
	
	virtual unsigned char getPixel(int x, int y) const;
	
	virtual int getWidth() const;
	virtual int getHeight() const;

private:
  GrayBytesMonochromeBitmapSource(const GrayBytesMonochromeBitmapSource&);
  GrayBytesMonochromeBitmapSource& operator=(const GrayBytesMonochromeBitmapSource&);  
};

#endif // __GRAY_BYTES_MONOCHROM_BITMAP_SOURCE_H__
