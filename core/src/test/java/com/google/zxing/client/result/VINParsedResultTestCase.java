/*
 * Copyright 2014 ZXing authors
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link VINParsedResult}.
 */
public final class VINParsedResultTestCase extends Assert {

  @Test
  public void testNotVIN() {
    Result fakeResult = new Result("1M8GDM9A1KP042788", null, null, BarcodeFormat.CODE_39);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertEquals(ParsedResultType.TEXT, result.getType());
    fakeResult = new Result("1M8GDM9AXKP042788", null, null, BarcodeFormat.CODE_128);
    result = ResultParser.parseResult(fakeResult);
    assertEquals(ParsedResultType.TEXT, result.getType());
  }

  @Test
  public void testVIN() {
    doTest("1M8GDM9AXKP042788", "1M8", "GDM9AX", "KP042788", "US", "GDM9A", 1989, 'P', "042788");
    doTest("I1M8GDM9AXKP042788", "1M8", "GDM9AX", "KP042788", "US", "GDM9A", 1989, 'P', "042788");
    doTest("LJCPCBLCX11000237", "LJC", "PCBLCX", "11000237", "CN", "PCBLC", 2001, '1', "000237");
  }

  private static void doTest(String contents,
                             String wmi,
                             String vds,
                             String vis,
                             String country,
                             String attributes,
                             int year,
                             char plant,
                             String sequential) {
    Result fakeResult = new Result(contents, null, null, BarcodeFormat.CODE_39);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertSame(ParsedResultType.VIN, result.getType());
    VINParsedResult vinResult = (VINParsedResult) result;
    assertEquals(wmi, vinResult.getWorldManufacturerID());
    assertEquals(vds, vinResult.getVehicleDescriptorSection());
    assertEquals(vis, vinResult.getVehicleIdentifierSection());
    assertEquals(country, vinResult.getCountryCode());
    assertEquals(attributes, vinResult.getVehicleAttributes());
    assertEquals(year, vinResult.getModelYear());
    assertEquals(plant, vinResult.getPlantCode());
    assertEquals(sequential, vinResult.getSequentialNumber());
  }

}