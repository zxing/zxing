/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing.oned.rss
{

import com.google.zxing.ResultPoint;

public  class FinderPattern {

	
  protected var value:int;
  protected var startEnd:Array;
  protected var resultPoints:Array;

  public function FinderPattern( value:int, startEnd:Array, start:int, end:int, rowNumber:int) {
    this.value = value;
    this.startEnd = startEnd;
    this.resultPoints = [
        new ResultPoint( start as Number,  rowNumber as Number),
        new ResultPoint( end as Number,  rowNumber as Number),
    ];
  }

  public function getValue():int {
    return value;
  }

  public function getStartEnd():Array {
    return startEnd;
  }

  public function getResultPoints():Array {
    return resultPoints;
  }

}
}