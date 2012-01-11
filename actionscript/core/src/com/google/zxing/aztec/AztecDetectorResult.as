/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.aztec
{
	import com.google.zxing.common.DetectorResult;
	
	public final class AztecDetectorResult extends DetectorResult 
	{

		import com.google.zxing.ResultPoint;
		import com.google.zxing.common.BitMatrix;
		import com.google.zxing.common.DetectorResult;


  		private var compact:Boolean;
  		private var nbDatablocks:int;
  		private var nbLayers:int;

 		public function AztecDetectorResult(bits:BitMatrix ,  points:Array,  compact:Boolean,  nbDatablocks:int,  nbLayers:int) 
  		{
    		super(bits, points);
    		this.compact = compact;
    		this.nbDatablocks = nbDatablocks;
    		this.nbLayers = nbLayers;
  		}

 		 public function  getNbLayers():int 
 		 {
    		return nbLayers;
  		 }

  		 public function getNbDatablocks():int 
  		 {
    		return nbDatablocks;
  		 }

  		 public function isCompact():Boolean 
  		{
    		return compact;
  		}

	}
}