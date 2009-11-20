package com.google.zxing.client.result.optional
{
/*
 * Copyright 2008 ZXing authors
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

import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ParsedResult;

/**
 * @author Sean Owen
 */
public final class NDEFSmartPosterParsedResult extends ParsedResult 
{

  public static var ACTION_UNSPECIFIED:int = -1;
  public static var ACTION_DO:int = 0;
  public static var ACTION_SAVE:int = 1;
  public static var ACTION_OPEN:int = 2;

  private var title:String;
  private var uri:String;
  private var action:int;

  public function NDEFSmartPosterParsedResult(action:int, uri:String, title:String) {
    super(ParsedResultType.NDEF_SMART_POSTER);
    this.action = action;
    this.uri = uri;
    this.title = title;
  }

  public function getTitle():String {
    return title;
  }

  public function getURI():String {
    return uri;
  }

  public function getAction():int {
    return action;
  }

  public override function getDisplayResult():String {
    if (title == null) {
      return uri;
    } else {
      return title + '\n' + uri;
    }
  }

}
}