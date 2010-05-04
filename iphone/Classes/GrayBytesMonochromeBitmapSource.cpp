//
//  GrayBytesMonochromeBitmapSource.cpp
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

#include "GrayBytesMonochromeBitmapSource.h"
#include <zxing/ReaderException.h>


GrayBytesMonochromeBitmapSource::GrayBytesMonochromeBitmapSource(const unsigned char *bytes, 
                                int width, 
                                int height,
								int bytesPerRow)  : 
				width_(width), 
				height_(height),
				bytes_(bytes), 
				bytesPerRow_(bytesPerRow) { }


int GrayBytesMonochromeBitmapSource::getWidth() const{
	return width_;
}

int GrayBytesMonochromeBitmapSource::getHeight() const {
	return height_;
}

unsigned char GrayBytesMonochromeBitmapSource::getPixel(int x, int y) const {
/*	if (x >= width_ || y >= height_) {
		throw new ReaderException("bitmap coordinate out of bounds");
	}*/
	int index = y * bytesPerRow_ + x;
	return bytes_[index];
}
