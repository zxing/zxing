/*
 *  TransformingMonochroeBitmapSource.cpp
 *  ZXing
 *
 *  Created by Christian Brunschen on 03/06/2008.
 *  Copyright 2008 ZXing authors All rights reserved.
 *
 */

#include "TransformingMonochromeBitmapSource.h"

bool TransformingMonochromeBitmapSource::isRotateSupported() { 
  return true;
}

Ref<MonochromeBitmapSource> TMBS0::rotateCounterClockwise() {
  Ref<MonochromeBitmapSource> result (new TMBS90(source_, scale_));
  return result;
}


Ref<MonochromeBitmapSource> TMBS90::rotateCounterClockwise() {
  Ref<MonochromeBitmapSource> result (new TMBS180(source_, scale_));
  return result;
}


Ref<MonochromeBitmapSource> TMBS180::rotateCounterClockwise() {
  Ref<MonochromeBitmapSource> result (new TMBS270(source_, scale_));
  return result;
}


Ref<MonochromeBitmapSource> TMBS270::rotateCounterClockwise() {
  Ref<MonochromeBitmapSource> result (new TMBS0(source_, scale_));
  return result;
}
