#ifndef __ARRAY_H__
#define __ARRAY_H__

/*
 *  Array.h
 *  zxing
 *
 *  Created by Christian Brunschen on 07/05/2008.
 *  Copyright 2008 Google UK. All rights reserved.
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

#include <vector>

#ifdef DEBUG_COUNTING
#include <iostream>
#include <typeinfo>
#endif

#include <zxing/common/Counted.h>


namespace zxing {

template<typename T> class Array : public Counted {
protected:
public:
  std::vector<T> values_;
  Array(size_t n) :
      Counted(), values_(n, T()) {
  }
  Array(T *ts, size_t n) :
      Counted(), values_(ts, ts+n) {
  }
  Array(T v, size_t n) :
      Counted(), values_(n, v) {
  }
  Array(std::vector<T> &v) :
      Counted(), values_(v) {
  }
  Array(Array<T> &other) :
      Counted(), values_(other.values_) {
  }
  Array(Array<T> *other) :
      Counted(), values_(other->values_) {
  }
  virtual ~Array() {
  }
  Array<T>& operator=(const Array<T> &other) {
#ifdef DEBUG_COUNTING
    cout << "assigning values from Array " << &other << " to this Array " << this << ", ";
#endif
    values_ = other.values_;
#ifdef DEBUG_COUNTING
    cout << "new size = " << values_.size() << "\n";
#endif
    return *this;
  }
  Array<T>& operator=(const std::vector<T> &array) {
#ifdef DEBUG_COUNTING
    cout << "assigning values from Array " << &array << " to this Array " << this << ", ";
#endif
    values_ = array;
#ifdef DEBUG_COUNTING
    cout << "new size = " << values_.size() << "\n";
#endif
    return *this;
  }
  T operator[](size_t i) const {
    return values_[i];
  }
  T& operator[](size_t i) {
    return values_[i];
  }
  size_t size() const {
    return values_.size();
  }
  std::vector<T> values() const {
    return values_;
  }
  std::vector<T>& values() {
    return values_;
  }
};

template<typename T> class ArrayRef {
private:
public:
  Array<T> *array_;
  ArrayRef() :
      array_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating empty ArrayRef " << this << "\n";
#endif
  }
  ArrayRef(size_t n) :
      array_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating ArrayRef " << this << "with size " << n << "\n";
#endif
    reset(new Array<T> (n));
  }
  ArrayRef(T *ts, size_t n) :
      array_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating ArrayRef " << this << "with " << n << " elements at " << (void *)ts << "\n";
#endif
    reset(new Array<T> (ts, n));
  }
  ArrayRef(Array<T> *a) :
      array_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating ArrayRef " << this << " from pointer:\n";
#endif
    reset(a);
  }
  ArrayRef(const Array<T> &a) :
      array_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating ArrayRef " << this << " from reference to Array " << (void *)&a << ":\n";
#endif
    reset(const_cast<Array<T> *>(&a));
  }
  ArrayRef(const ArrayRef &other) :
      array_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating ArrayRef " << this << " from ArrayRef " << &other << ":\n";
#endif
    reset(other.array_);
  }

  template<class Y>
  ArrayRef(const ArrayRef<Y> &other) :
      array_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating ArrayRef " << this << " from ArrayRef " << &other << ":\n";
#endif
    reset(static_cast<const Array<T> *>(other.array_));
  }

  ~ArrayRef() {
#ifdef DEBUG_COUNTING
    cout << "destroying ArrayRef " << this << " with " << (array_ ? typeid(*array_).name() : "NULL") << " "
         << array_ << "\n";
#endif
    if (array_) {
      array_->release();
    }
    array_ = 0;
  }

  T operator[](size_t i) const {
    return (*array_)[i];
  }
  T& operator[](size_t i) {
    return (*array_)[i];
  }
  size_t size() const {
    return array_->size();
  }

  void reset(Array<T> *a) {
#ifdef DEBUG_COUNTING
    cout << "resetting ArrayRef " << this << " from " << (array_ ? typeid(*array_).name() : "NULL") << " "
         << array_ << " to " << (a ? typeid(*a).name() : "NULL") << " " << a << "\n";
#endif
    if (a) {
      a->retain();
    }
    if (array_) {
      array_->release();
    }
    array_ = a;
  }
  void reset(const ArrayRef<T> &other) {
    reset(other.array_);
  }
  ArrayRef<T>& operator=(const ArrayRef<T> &other) {
    reset(other);
    return *this;
  }
  ArrayRef<T>& operator=(Array<T> *a) {
    reset(a);
    return *this;
  }

  Array<T>& operator*() {
    return *array_;
  }
  Array<T>* operator->() {
    return array_;
  }
};

} // namespace zxing

#endif // __ARRAY_H__
