/*
 * Copyright (C) 2010 ZXing authors
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

/*
 * These authors would like to acknowledge the Spanish Ministry of Industry,
 * Tourism and Trade, for the support in the project TSI020301-2008-2
 * "PIRAmIDE: Personalizable Interactions with Resources on AmI-enabled
 * Mobile Dynamic Environments", led by Treelogic
 * ( http://www.treelogic.com/ ):
 *
 *   http://www.piramidepse.com/
 */

package com.google.zxing.client.result;

import java.util.Hashtable;

import junit.framework.TestCase;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

/**
 * @author Antonio Manuel Benjumea Conde, Servinform, S.A.
 * @author Agustín Delgado, Servinform, S.A.
 */
public final class ExpandedProductParsedResultTestCase extends TestCase {

  public void test_RSSExpanded() {
    String text = "(01)66546(13)001205(3932)4455(3102)6544(123)544654";
    String productID = "66546";
    String sscc = "-";
    String lotNumber = "-";
    String productionDate = "-";
    String packagingDate = "001205";
    String bestBeforeDate = "-";
    String expirationDate = "-";
    String weight = "6544";
    String weightType = "KG";
    String weightIncrement = "2";
    String price = "5";
    String priceIncrement = "2";
    String priceCurrency = "445";
    Hashtable uncommonAIs = new Hashtable();
    uncommonAIs.put("123", "544654");

    Result result = new Result(text, null, null, BarcodeFormat.RSS_EXPANDED);
    ExpandedProductParsedResult o = ExpandedProductResultParser
        .parse(result);
    assertEquals(productID, o.getProductID());
    assertEquals(sscc, o.getSscc());
    assertEquals(lotNumber, o.getLotNumber());
    assertEquals(productionDate, o.getProductionDate());
    assertEquals(packagingDate, o.getPackagingDate());
    assertEquals(bestBeforeDate, o.getBestBeforeDate());
    assertEquals(expirationDate, o.getExpirationDate());
    assertEquals(weight, o.getWeight());
    assertEquals(weightType, o.getWeightType());
    assertEquals(weightIncrement, o.getWeightIncrement());
    assertEquals(price, o.getPrice());
    assertEquals(priceIncrement, o.getPriceIncrement());
    assertEquals(priceCurrency, o.getPriceCurrency());
    assertEquals(uncommonAIs, o.getUncommonAIs());
  }
}
