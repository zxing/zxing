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

/**
 * <p>Represents a record in an NDEF message. This class only supports certain types
 * of records -- namely, non-chunked records, where ID length is omitted, and only
 * "short records".</p>
 *
 * @author Sean Owen
 */
 
 import com.google.zxing.common.flexdatatypes.Utils;
public final class NDEFRecord {

  private static var SUPPORTED_HEADER_MASK:int = 0x3F; // 0 0 1 1 1 111 (the bottom 6 bits matter)
  private static var SUPPORTED_HEADER:int = 0x11;      // 0 0 0 1 0 001

  public static var TEXT_WELL_KNOWN_TYPE:String = "T";
  public static var URI_WELL_KNOWN_TYPE:String = "U";
  public static var SMART_POSTER_WELL_KNOWN_TYPE:String = "Sp";
  public static var ACTION_WELL_KNOWN_TYPE:String = "act";

  private var header:int;
  private var type:String;
  private var payload:Array;
  private var totalRecordLength:int;

  public function NDEFRecord(header:int, type:String, payload:Array, totalRecordLength:int) {
    this.header = header;
    this.type = type;
    this.payload = payload;
    this.totalRecordLength = totalRecordLength;
  }

  public static function readRecord(bytes:Array, offset:int):NDEFRecord {
    var header:int = bytes[offset] & 0xFF;
    // Does header match what we support in the bits we care about?
    // XOR figures out where we differ, and if any of those are in the mask, fail
    if (((header ^ SUPPORTED_HEADER) & SUPPORTED_HEADER_MASK) != 0) {
      return null;
    }
    var typeLength:int = bytes[offset + 1] & 0xFF;

    var payloadLength:int = bytes[offset + 2] & 0xFF;

    var type:String = AbstractNDEFResultParser.bytesToString(bytes, offset + 3, typeLength, "US-ASCII");

    var payload:Array = new Array(payloadLength);
    Utils.arraycopy(bytes, offset + 3 + typeLength, payload, 0, payloadLength);

    return new NDEFRecord(header, type, payload, 3 + typeLength + payloadLength);
  }

  public function isMessageBegin():Boolean {
    return (header & 0x80) != 0;
  }

  public function isMessageEnd():Boolean {
    return (header & 0x40) != 0;
  }

  public function getType():String {
    return type;
  }

  public function getPayload():Array {
    return payload;
  }

  public function getTotalRecordLength():int {
    return totalRecordLength;
  }

}
}