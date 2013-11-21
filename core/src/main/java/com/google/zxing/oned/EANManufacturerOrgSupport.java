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

package com.google.zxing.oned;

import java.util.ArrayList;
import java.util.List;

/**
 * Records EAN prefix to GS1 Member Organization, where the member organization
 * correlates strongly with a country. This is an imperfect means of identifying
 * a country of origin by EAN-13 barcode value. See
 * <a href="http://en.wikipedia.org/wiki/List_of_GS1_country_codes">
 * http://en.wikipedia.org/wiki/List_of_GS1_country_codes</a>.
 *
 * @author Sean Owen
 */
final class EANManufacturerOrgSupport {

  private final List<int[]> ranges = new ArrayList<int[]>();
  private final List<String> countryIdentifiers = new ArrayList<String>();

  String lookupCountryIdentifier(String productCode) {
    initIfNeeded();
    int prefix = Integer.parseInt(productCode.substring(0, 3));
    int max = ranges.size();
    for (int i = 0; i < max; i++) {
      int[] range = ranges.get(i);
      int start = range[0];
      if (prefix < start) {
        return null;
      }
      int end = range.length == 1 ? start : range[1];
      if (prefix <= end) {
        return countryIdentifiers.get(i);
      }
    }
    return null;
  }
  
  private void add(int[] range, String id) {
    ranges.add(range);
    countryIdentifiers.add(id);
  }
  
  private synchronized void initIfNeeded() {
    if (!ranges.isEmpty()) {
      return;
    }
    add(new int[] {0,19},    "US/CA");
    add(new int[] {30,39},   "US");
    add(new int[] {60,139},  "US/CA");
    add(new int[] {300,379}, "FR");
    add(new int[] {380},     "BG");
    add(new int[] {383},     "SI");
    add(new int[] {385},     "HR");
    add(new int[] {387},     "BA");
    add(new int[] {400,440}, "DE");
    add(new int[] {450,459}, "JP");
    add(new int[] {460,469}, "RU");
    add(new int[] {471},     "TW");
    add(new int[] {474},     "EE");
    add(new int[] {475},     "LV");
    add(new int[] {476},     "AZ");
    add(new int[] {477},     "LT");
    add(new int[] {478},     "UZ");
    add(new int[] {479},     "LK");
    add(new int[] {480},     "PH");
    add(new int[] {481},     "BY");
    add(new int[] {482},     "UA");
    add(new int[] {484},     "MD");
    add(new int[] {485},     "AM");
    add(new int[] {486},     "GE");
    add(new int[] {487},     "KZ");
    add(new int[] {489},     "HK");
    add(new int[] {490,499}, "JP");    
    add(new int[] {500,509}, "GB");    
    add(new int[] {520},     "GR");
    add(new int[] {528},     "LB");
    add(new int[] {529},     "CY");
    add(new int[] {531},     "MK");
    add(new int[] {535},     "MT");
    add(new int[] {539},     "IE");
    add(new int[] {540,549}, "BE/LU");    
    add(new int[] {560},     "PT");
    add(new int[] {569},     "IS");
    add(new int[] {570,579}, "DK");
    add(new int[] {590},     "PL");
    add(new int[] {594},     "RO");
    add(new int[] {599},     "HU");
    add(new int[] {600,601}, "ZA");
    add(new int[] {603},     "GH");    
    add(new int[] {608},     "BH");
    add(new int[] {609},     "MU");
    add(new int[] {611},     "MA");
    add(new int[] {613},     "DZ");
    add(new int[] {616},     "KE");
    add(new int[] {618},     "CI");    
    add(new int[] {619},     "TN");
    add(new int[] {621},     "SY");
    add(new int[] {622},     "EG");
    add(new int[] {624},     "LY");
    add(new int[] {625},     "JO");
    add(new int[] {626},     "IR");
    add(new int[] {627},     "KW");
    add(new int[] {628},     "SA");
    add(new int[] {629},     "AE");
    add(new int[] {640,649}, "FI");
    add(new int[] {690,695}, "CN");
    add(new int[] {700,709}, "NO");
    add(new int[] {729},     "IL");
    add(new int[] {730,739}, "SE");
    add(new int[] {740},     "GT");
    add(new int[] {741},     "SV");
    add(new int[] {742},     "HN");
    add(new int[] {743},     "NI");
    add(new int[] {744},     "CR");
    add(new int[] {745},     "PA");
    add(new int[] {746},     "DO");
    add(new int[] {750},     "MX");
    add(new int[] {754,755}, "CA");
    add(new int[] {759},     "VE");
    add(new int[] {760,769}, "CH");
    add(new int[] {770},     "CO");
    add(new int[] {773},     "UY");
    add(new int[] {775},     "PE");
    add(new int[] {777},     "BO");
    add(new int[] {779},     "AR");
    add(new int[] {780},     "CL");
    add(new int[] {784},     "PY");
    add(new int[] {785},     "PE");  
    add(new int[] {786},     "EC");
    add(new int[] {789,790}, "BR");
    add(new int[] {800,839}, "IT");
    add(new int[] {840,849}, "ES");
    add(new int[] {850},     "CU");
    add(new int[] {858},     "SK");
    add(new int[] {859},     "CZ");
    add(new int[] {860},     "YU");
    add(new int[] {865},     "MN");    
    add(new int[] {867},     "KP");
    add(new int[] {868,869}, "TR");
    add(new int[] {870,879}, "NL");
    add(new int[] {880},     "KR");
    add(new int[] {885},     "TH");
    add(new int[] {888},     "SG");
    add(new int[] {890},     "IN");
    add(new int[] {893},     "VN");
    add(new int[] {896},     "PK");    
    add(new int[] {899},     "ID");
    add(new int[] {900,919}, "AT");
    add(new int[] {930,939}, "AU");
    add(new int[] {940,949}, "AZ");
    add(new int[] {955},     "MY");
    add(new int[] {958},     "MO");
  }

}
