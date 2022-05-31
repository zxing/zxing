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

package com.google.zxing.client.result;

/**
 * Represents a parsed result that encodes a product by an identifier of some kind.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ProductParsedResult extends ParsedResult {

  private final String productID;
  private final String normalizedProductID;

  ProductParsedResult(String productID) {
    this(productID, productID);
  }

  ProductParsedResult(String productID, String normalizedProductID) {
    super(ParsedResultType.PRODUCT);
    this.productID = productID;
    this.normalizedProductID = normalizedProductID;
  }

  public String getProductID() {
    return productID;
  }

  public String getNormalizedProductID() {
    return normalizedProductID;
  }

  @Override
  public String getDisplayResult() {
    return productID;
  }

}
