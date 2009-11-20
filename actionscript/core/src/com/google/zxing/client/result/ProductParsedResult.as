package com.google.zxing.client.result
{
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

/**
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ProductParsedResult extends ParsedResult {

  public var productID:String;
  public var normalizedProductID:String;

/*
  public function ProductParsedResult(productID:String) 
  {
    this(productID, productID);
  }
*/
  public function ProductParsedResult(productID:String, normalizedProductID:String='') 
  {
  	if (normalizedProductID == '')
  	{
		normalizedProductID = productID  		
  	}
    super(ParsedResultType.PRODUCT);
    this.productID = productID;
    this.normalizedProductID = normalizedProductID;
  }

  public function getProductID():String {
    return productID;
  }

  public function getNormalizedProductID():String {
    return normalizedProductID;
  }

  public override function getDisplayResult():String {
    return productID;
  }

}

}