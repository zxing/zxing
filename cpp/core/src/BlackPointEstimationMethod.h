#ifndef __BLACK_POINT_ESTIMATION_METHOD_H__
#define __BLACK_POINT_ESTIMATION_METHOD_H__

/*
 *  BlackPointEstimationMethod.h
 *  zxing
 *
 *  Created by Christian Brunschen on 12/05/2008.
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

typedef enum BlackPointEstimationMethod {
  BlackPointEstimationMethod_None = 0,
  BlackPointEstimationMethod_RowSampling,
  BlackPointEstimationMethod_2D
} BlackPointEstimationMethod;

#endif // __BLACK_POINT_ESTIMATION_METHOD_H__
