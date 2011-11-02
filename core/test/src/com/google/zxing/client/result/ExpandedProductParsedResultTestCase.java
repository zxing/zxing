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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Antonio Manuel Benjumea Conde, Servinform, S.A.
 * @author Agustín Delgado, Servinform, S.A.
 */
public final class ExpandedProductParsedResultTestCase extends Assert {

  @Test
  public void test_RSSExpanded() {
    Map<String,String> uncommonAIs = new HashMap<String,String>();
    uncommonAIs.put("123", "544654");
    Result result =
        new Result("(01)66546(13)001205(3932)4455(3102)6544(123)544654", null, null, BarcodeFormat.RSS_EXPANDED);
    ExpandedProductParsedResult o = new ExpandedProductResultParser().parse(result);
    assertNotNull(o);
    assertEquals("66546", o.getProductID());
    assertNull(o.getSscc());
    assertNull(o.getLotNumber());
    assertNull(o.getProductionDate());
    assertEquals("001205", o.getPackagingDate());
    assertNull(o.getBestBeforeDate());
    assertNull(o.getExpirationDate());
    assertEquals("6544", o.getWeight());
    assertEquals("KG", o.getWeightType());
    assertEquals("2", o.getWeightIncrement());
    assertEquals("5", o.getPrice());
    assertEquals("2", o.getPriceIncrement());
    assertEquals("445", o.getPriceCurrency());
    assertEquals(uncommonAIs, o.getUncommonAIs());
  }
}
