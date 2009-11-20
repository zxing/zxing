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

package com.google.zxing
{
/**
 * The general exception class throw when something goes wrong during decoding of a barcode.
 * This includes, but is not limited to, failing checksums / error correction algorithms, being
 * unable to locate finder timing patterns, and so on.
 *
 * @author Sean Owen
 */

	public class ReaderException extends Error
	{
		  // TODO: Currently we throw up to 400 ReaderExceptions while scanning a single 240x240 image before
  // rejecting it. This involves a lot of overhead and memory allocation, and affects both performance
  // and latency on continuous scan clients. In the future, we should change all the decoders not to
  // throw exceptions for routine events, like not finding a barcode on a given row. Instead, we
  // should return error codes back to the callers, and simply delete this class. In the mean time, I
  // have altered this class to be as lightweight as possible, by ignoring the exception string, and
  // by disabling the generation of stack traces, which is especially time consuming. These are just
  // temporary measures, pending the big cleanup.

 	private static var instance:ReaderException = new ReaderException();

  public function ReaderException(message:String=""):void {
  	super(message);
    // do nothing
  }

  public static function getInstance():ReaderException {
    return instance;
  }

  // Prevent stack traces from being taken
  // srowen says: huh, my IDE is saying this is not an override. native methods can't be overridden?
  // This, at least, does not hurt. Because we use a singleton pattern here, it doesn't matter anyhow.
  /*public function  fillInStackTrace():Throwable {
    return null;
  }*/
 
	}
}