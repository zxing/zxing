#ifndef __BARCODE_FORMAT_H__
#define __BARCODE_FORMAT_H__

/*
 *  BarcodeFormat.h
 *  zxing
 *
 *  Created by Christian Brunschen on 13/05/2008.
 *  Copyright 2008 Google Inc. All rights reserved.
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

typedef enum BarcodeFormat {
  BarcodeFormat_None = 0,
  BarcodeFormat_QR_CODE
} BarcodeFormat;

#endif // __BARCODE_FORMAT_H__
