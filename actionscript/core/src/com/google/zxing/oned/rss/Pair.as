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

public class Pair extends DataCharacter {

  protected var finderPattern:FinderPattern;
  protected var count:int;

  public function Pair(value:int, checksumPortion:int, finderPattern:FinderPattern) {
    super(value, checksumPortion);
    this.finderPattern = finderPattern;
  }

  public function getFinderPattern():FinderPattern {
    return finderPattern;
  }

  public function getCount():int {
    return count;
  }

  public function incrementCount():void {
    count++;
  }
}
}