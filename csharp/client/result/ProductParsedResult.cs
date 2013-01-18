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

namespace com.google.zxing.client.result
{

	/// <summary>
	/// @author dswitkin@google.com (Daniel Switkin)
	/// </summary>
	public sealed class ProductParsedResult : ParsedResult
	{

	  private readonly string productID;
	  private readonly string normalizedProductID;

	  internal ProductParsedResult(string productID) : this(productID, productID)
	  {
	  }

	  internal ProductParsedResult(string productID, string normalizedProductID) : base(ParsedResultType.PRODUCT)
	  {
		this.productID = productID;
		this.normalizedProductID = normalizedProductID;
	  }

	  public string ProductID
	  {
		  get
		  {
			return productID;
		  }
	  }

	  public string NormalizedProductID
	  {
		  get
		  {
			return normalizedProductID;
		  }
	  }

	  public override string DisplayResult
	  {
		  get
		  {
			return productID;
		  }
	  }

	}

}