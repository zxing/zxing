/*
 *  Exception.cpp
 *  ZXing
 *
 *  Created by Christian Brunschen on 03/06/2008.
 *  Copyright 2008 ZXing authors All rights reserved.
 *
 */

#include <zxing/Exception.h>

namespace zxing {

Exception::Exception(const char *msg) :
    message(msg) {
}

const char* Exception::what() const throw() {
  return message.c_str();
}

Exception::~Exception() throw() {
}

}
