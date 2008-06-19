#ifndef __TRANSFORMING_MONCHROME_BITMAP_SOURCE_H__
#define __TRANSFORMING_MONCHROME_BITMAP_SOURCE_H__

/*
 *  TransformingMonochromeBitmapSource.h
 *  ZXing
 *
 *  Created by Christian Brunschen on 03/06/2008.
 *  Copyright 2008 ZXing authors All rights reserved.
 *
 */

#include "MonochromeBitmapSource.h"

class TransformingMonochromeBitmapSource : public MonochromeBitmapSource {
protected:
  Ref<MonochromeBitmapSource> source_;
  float scale_;
  
public:
  TransformingMonochromeBitmapSource(Ref<MonochromeBitmapSource> source,
                                     float scale) :
  MonochromeBitmapSource(),
  source_(source),
  scale_(scale) { }
  
  virtual ~TransformingMonochromeBitmapSource() { }
  
  virtual size_t getWidth() = 0;
  virtual size_t getHeight() = 0;
  virtual unsigned char getPixel(size_t x, size_t y) = 0;
  virtual Ref<MonochromeBitmapSource> rotateCounterClockwise() = 0;
  virtual bool isRotateSupported();  
};

class TMBS0;
class TMBS90;
class TMBS180;
class TMBS270;

class TMBS0 : public TransformingMonochromeBitmapSource {
private:
public:
  TMBS0(Ref<MonochromeBitmapSource> source, float scale) :
  TransformingMonochromeBitmapSource(source, scale) { }
  virtual size_t getWidth() { return source_->getWidth() / scale_; }
  virtual size_t getHeight() { return source_->getHeight() / scale_; }
  virtual unsigned char getPixel(size_t x, size_t y) {
    return source_->getPixel(x * scale_, y * scale_);
  }
  virtual Ref<MonochromeBitmapSource> rotateCounterClockwise();
};

class TMBS90 : public TransformingMonochromeBitmapSource {
private:
  size_t xOffset_;
public:
  TMBS90(Ref<MonochromeBitmapSource> source, float scale) :
  TransformingMonochromeBitmapSource(source, scale), 
  xOffset_(source->getHeight() - 1) { }
  virtual size_t getWidth() { return source_->getHeight() / scale_; }
  virtual size_t getHeight() { return source_->getWidth() / scale_; }
  virtual unsigned char getPixel(size_t x, size_t y) {
    size_t sourceX = y * scale_;
    size_t sourceY = xOffset_ - x * scale_;
    //cout << "tmbs90: (" << x << "," << y << ") => (" << sourceX << "," << sourceY << ")\n";
    return source_->getPixel(sourceX, sourceY);
  }
  virtual Ref<MonochromeBitmapSource> rotateCounterClockwise();
};

class TMBS180 : public TransformingMonochromeBitmapSource {
private:
  size_t xOffset_;
  size_t yOffset_;
public:
  TMBS180(Ref<MonochromeBitmapSource> source, float scale) :
  TransformingMonochromeBitmapSource(source, scale), 
  xOffset_(source->getWidth() - 1),
  yOffset_(source->getHeight() - 1) { }
  virtual size_t getWidth() { return source_->getWidth() / scale_; }
  virtual size_t getHeight() { return source_->getHeight() / scale_; }
  virtual unsigned char getPixel(size_t x, size_t y) {
    size_t sourceX = xOffset_ - x * scale_;
    size_t sourceY = yOffset_ - y * scale_;
    //cout << "tmbs180: (" << x << "," << y << ") => (" << sourceX << "," << sourceY << ")\n";
    return source_->getPixel(sourceX, sourceY);
  }
  virtual Ref<MonochromeBitmapSource> rotateCounterClockwise();
};

class TMBS270 : public TransformingMonochromeBitmapSource {
private:
  size_t yOffset_;
public:
  TMBS270(Ref<MonochromeBitmapSource> source, float scale) :
  TransformingMonochromeBitmapSource(source, scale), 
  yOffset_(source->getWidth() - 1) { }
  virtual size_t getWidth() { return source_->getHeight() / scale_; }
  virtual size_t getHeight() { return source_->getWidth() / scale_; }
  virtual unsigned char getPixel(size_t x, size_t y) {
    return source_->getPixel(yOffset_ - y * scale_, x * scale_);
  }
  virtual Ref<MonochromeBitmapSource> rotateCounterClockwise();
};

#endif // __TRANSFORMING_MONCHROME_BITMAP_SOURCE_H__
