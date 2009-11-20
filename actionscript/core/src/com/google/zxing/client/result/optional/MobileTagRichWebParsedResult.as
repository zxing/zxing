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


import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;

/**
 * @author Sean Owen
 */
public final class MobileTagRichWebParsedResult extends ParsedResult {

  // Example: "http://www.tagserver.com/script.asp?id="
  public static var TAGSERVER_URI_PREFIX:String;// = System.getProperty("zxing.mobiletag.tagserver");

  private var id:String;
  private var action:int;

  public function MobileTagRichWebParsedResult(id:String, action:int, tagserver:String) 
  {
    super(ParsedResultType.MOBILETAG_RICH_WEB);
    this.id = id;
    this.action = action;
    MobileTagRichWebParsedResult.TAGSERVER_URI_PREFIX = tagserver;
  }

  public static function getTagserverURIPrefix():String {
    return MobileTagRichWebParsedResult.TAGSERVER_URI_PREFIX;
  }

  public function getId():String {
    return id;
  }

  public function getAction():int {
    return action;
  }

  public function getTagserverURI():String {
    return TAGSERVER_URI_PREFIX + id;
  }

  public override function getDisplayResult():String {
    return id;
  }

}
}