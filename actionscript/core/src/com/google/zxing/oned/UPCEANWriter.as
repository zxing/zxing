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

package com.google.zxing.oned
{

	import com.google.zxing.common.ByteMatrix;
	import com.google.zxing.common.flexdatatypes.HashTable;
	import com.google.zxing.Writer;

/**
 * @author Ari Pollak
 */
public interface UPCEANWriter extends Writer 
{

  	/** @return a byte array of horizontal pixels (0 = white, 1 = black) */
  	//function encode(contents:String,  format:BarcodeFormat, width:int, height:int, hints:HashTable=undefined):ByteMatrix;
}
}