package com.google.zxing.client.result
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

import com.google.zxing.common.flexdatatypes.StringBuilder;
/**
 * @author Sean Owen
 */
public final class SMSParsedResult extends ParsedResult {

  private var smsURI:String;
  private var number:String;
  private var via:String;
  private var subject:String;
  private var body:String;
  private var title:String;

  public function SMSParsedResult(smsURI:String ,number:String ,via:String,subject:String, body:String, title:String) {
    super(ParsedResultType.SMS);
    this.smsURI = smsURI;
    this.number = number;
    this.via = via;
    this.subject = subject;
    this.body = body;
    this.title = title;
  }

  public function getSMSURI():String {
    return smsURI;
  }

  public function getNumber():String {
    return number;
  }

  public function getVia():String {
    return via;
  }

  public function getSubject():String {
    return subject;
  }

  public function getBody():String {
    return body;
  }

  public function getTitle():String {
    return title;
  }

  public override function getDisplayResult():String {
    var result:StringBuilder  = new StringBuilder();
    maybeAppend(number, result);
    maybeAppend(via, result);
    maybeAppend(subject, result);
    maybeAppend(body, result);
    maybeAppend(title, result);
    return result.toString();
  }

}
}