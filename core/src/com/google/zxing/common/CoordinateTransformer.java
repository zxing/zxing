/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.common;

/**
 * This class supports the rotation of a bit matrix. To keep it simple it only supports rotation of 90, 180 and 270 degrees.
 * It could be made more generic to support cropping and arbitrary transformations by a provided transformer. 
 * @author Guenther Grau
 */
public interface CoordinateTransformer {

  int getX(int x, int y, int width, int height);

  int getY(int x, int y, int width, int height);
}
