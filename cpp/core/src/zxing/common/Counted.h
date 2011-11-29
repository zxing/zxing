// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#ifndef __COUNTED_H__
#define __COUNTED_H__

/*
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

//#define DEBUG_COUNTING

#include <iostream>

#ifdef DEBUG_COUNTING
#include <typeinfo>
#endif

namespace zxing {

/* base class for reference-counted objects */
class Counted {
private:
  unsigned int count_;
public:
  Counted() :
      count_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating " << typeid(*this).name() << " " << this <<
         " @ " << count_ << "\n";
#endif
  }
  virtual ~Counted() {
  }
  Counted *retain() {
#ifdef DEBUG_COUNTING
    cout << "retaining " << typeid(*this).name() << " " << this <<
         " @ " << count_;
#endif
    count_++;
#ifdef DEBUG_COUNTING
    cout << "->" << count_ << "\n";
#endif
    return this;
  }
  void release() {
#ifdef DEBUG_COUNTING
    cout << "releasing " << typeid(*this).name() << " " << this <<
         " @ " << count_;
#endif
    if (count_ == 0 || count_ == 54321) {
#ifdef DEBUG_COUNTING
      cout << "\nOverreleasing already-deleted object " << this << "!!!\n";
#endif
      throw 4711;
    }
    count_--;
#ifdef DEBUG_COUNTING
    cout << "->" << count_ << "\n";
#endif
    if (count_ == 0) {
#ifdef DEBUG_COUNTING
      cout << "deleting " << typeid(*this).name() << " " << this << "\n";
#endif
      count_ = 0xDEADF001;
      delete this;
    }
  }


  /* return the current count for denugging purposes or similar */
  int count() const {
    return count_;
  }
};

/* counting reference to reference-counted objects */
template<typename T> class Ref {
private:
public:
  T *object_;
  explicit Ref(T *o = 0) :
      object_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating Ref " << this << " from pointer" << o << "\n";
#endif
    reset(o);
  }

  explicit Ref(const T &o) :
      object_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating Ref " << this << " from reference\n";
#endif
    reset(const_cast<T *>(&o));
  }

  Ref(const Ref &other) :
      object_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating Ref " << this << " from Ref " << &other << "\n";
#endif
    reset(other.object_);
  }

  template<class Y>
  Ref(const Ref<Y> &other) :
      object_(0) {
#ifdef DEBUG_COUNTING
    cout << "instantiating Ref " << this << " from reference\n";
#endif
    reset(other.object_);
  }

  ~Ref() {
#ifdef DEBUG_COUNTING
    cout << "destroying Ref " << this << " with " <<
         (object_ ? typeid(*object_).name() : "NULL") << " " << object_ << "\n";
#endif
    if (object_) {
      object_->release();
    }
  }

  void reset(T *o) {
#ifdef DEBUG_COUNTING
    cout << "resetting Ref " << this << " from " <<
         (object_ ? typeid(*object_).name() : "NULL") << " " << object_ <<
         " to " << (o ? typeid(*o).name() : "NULL") << " " << o << "\n";
#endif
    if (o) {
      o->retain();
    }
    if (object_ != 0) {
      object_->release();
    }
    object_ = o;
  }
  Ref& operator=(const Ref &other) {
    reset(other.object_);
    return *this;
  }
  template<class Y>
  Ref& operator=(const Ref<Y> &other) {
    reset(other.object_);
    return *this;
  }
  Ref& operator=(T* o) {
    reset(o);
    return *this;
  }
  template<class Y>
  Ref& operator=(Y* o) {
    reset(o);
    return *this;
  }

  T& operator*() {
    return *object_;
  }
  T* operator->() const {
    return object_;
  }
  operator T*() const {
    return object_;
  }

  bool operator==(const T* that) {
    return object_ == that;
  }
  bool operator==(const Ref &other) const {
    return object_ == other.object_ || *object_ == *(other.object_);
  }
  template<class Y>
  bool operator==(const Ref<Y> &other) const {
    return object_ == other.object_ || *object_ == *(other.object_);
  }

  bool operator!=(const T* that) {
    return !(*this == that);
  }

  bool empty() const {
    return object_ == 0;
  }

  template<class Y>
  friend std::ostream& operator<<(std::ostream &out, Ref<Y>& ref);
};
}

#endif // __COUNTED_H__
