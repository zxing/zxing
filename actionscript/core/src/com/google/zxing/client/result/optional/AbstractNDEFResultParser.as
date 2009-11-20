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


import com.google.zxing.client.result.ResultParser;

/**
 * <p>Superclass for classes encapsulating results in the NDEF format.
 * See <a href="http://www.nfc-forum.org/specs/">http://www.nfc-forum.org/specs/</a>.</p>
 *
 * <p>This code supports a limited subset of NDEF messages, ones that are plausibly
 * useful in 2D barcode formats. This generally includes 1-record messages, no chunking,
 * "short record" syntax, no ID field.</p>
 *
 * @author Sean Owen
 */
public  class AbstractNDEFResultParser extends ResultParser {

  public static function bytesToString(bytes:Array, offset:int, length:int, encoding:String):String {
    try {
    	// can't do this in Actionscript
      //return new String(bytes, offset, length, encoding);
      throw new Error("Platform does not support encoding");
    } catch (uee:Error) {
      // This should only be used when 'encoding' is an encoding that must necessarily
      // be supported by the JVM, like UTF-8
      throw new Error("Platform does not support required encoding: " + uee);
    }
    throw new Error("Platform does not support encoding");
  }

}}