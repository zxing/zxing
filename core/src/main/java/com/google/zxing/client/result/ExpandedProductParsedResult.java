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

import java.util.Map;

/**
 * Represents a parsed result that encodes extended product information as encoded
 * by the RSS format, like weight, price, dates, etc.
 *
 * @author Antonio Manuel Benjumea Conde, Servinform, S.A.
 * @author Agustín Delgado, Servinform, S.A.
 */
public final class ExpandedProductParsedResult extends ParsedResult {

  public static final String KILOGRAM = "KG";
  public static final String POUND = "LB";

  private final String rawText;
  private final String productID;
  private final String sscc;
  private final String lotNumber;
  private final String productionDate;
  private final String packagingDate;
  private final String bestBeforeDate;
  private final String expirationDate;
  private final String weight;
  private final String weightType;
  private final String weightIncrement;
  private final String price;
  private final String priceIncrement;
  private final String priceCurrency;
  // For AIS that not exist in this object
  private final Map<String,String> uncommonAIs;

  public ExpandedProductParsedResult(String rawText,
                                     String productID,
                                     String sscc,
                                     String lotNumber,
                                     String productionDate,
                                     String packagingDate,
                                     String bestBeforeDate,
                                     String expirationDate,
                                     String weight,
                                     String weightType,
                                     String weightIncrement,
                                     String price,
                                     String priceIncrement,
                                     String priceCurrency,
                                     Map<String,String> uncommonAIs) {
    super(ParsedResultType.PRODUCT);
    this.rawText = rawText;
    this.productID = productID;
    this.sscc = sscc;
    this.lotNumber = lotNumber;
    this.productionDate = productionDate;
    this.packagingDate = packagingDate;
    this.bestBeforeDate = bestBeforeDate;
    this.expirationDate = expirationDate;
    this.weight = weight;
    this.weightType = weightType;
    this.weightIncrement = weightIncrement;
    this.price = price;
    this.priceIncrement = priceIncrement;
    this.priceCurrency = priceCurrency;
    this.uncommonAIs = uncommonAIs;
  }

  @Override
  public boolean equals(Object o){
    if (!(o instanceof ExpandedProductParsedResult)) {
      return false;
    }

    ExpandedProductParsedResult other = (ExpandedProductParsedResult)o;

    return equalsOrNull(productID, other.productID)
        && equalsOrNull(sscc, other.sscc)
        && equalsOrNull(lotNumber, other.lotNumber)
        && equalsOrNull(productionDate, other.productionDate)
        && equalsOrNull(bestBeforeDate, other.bestBeforeDate)
        && equalsOrNull(expirationDate, other.expirationDate)
        && equalsOrNull(weight, other.weight)
        && equalsOrNull(weightType, other.weightType)
        && equalsOrNull(weightIncrement, other.weightIncrement)
        && equalsOrNull(price, other.price)
        && equalsOrNull(priceIncrement, other.priceIncrement)
        && equalsOrNull(priceCurrency, other.priceCurrency)
        && equalsOrNull(uncommonAIs, other.uncommonAIs);
  }

  private static boolean equalsOrNull(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

  @Override
  public int hashCode(){
    int hash = 0;
    hash ^= hashNotNull(productID);
    hash ^= hashNotNull(sscc);
    hash ^= hashNotNull(lotNumber);
    hash ^= hashNotNull(productionDate);
    hash ^= hashNotNull(bestBeforeDate);
    hash ^= hashNotNull(expirationDate);
    hash ^= hashNotNull(weight);
    hash ^= hashNotNull(weightType);
    hash ^= hashNotNull(weightIncrement);
    hash ^= hashNotNull(price);
    hash ^= hashNotNull(priceIncrement);
    hash ^= hashNotNull(priceCurrency);
    hash ^= hashNotNull(uncommonAIs);
    return hash;
  }

  private static int hashNotNull(Object o) {
    return o == null ? 0 : o.hashCode();
  }

  public String getRawText() {
    return rawText;
  }

  public String getProductID() {
    return productID;
  }

  public String getSscc() {
    return sscc;
  }

  public String getLotNumber() {
    return lotNumber;
  }

  public String getProductionDate() {
    return productionDate;
  }

  public String getPackagingDate() {
    return packagingDate;
  }

  public String getBestBeforeDate() {
    return bestBeforeDate;
  }

  public String getExpirationDate() {
    return expirationDate;
  }

  public String getWeight() {
    return weight;
  }

  public String getWeightType() {
    return weightType;
  }

  public String getWeightIncrement() {
    return weightIncrement;
  }

  public String getPrice() {
    return price;
  }

  public String getPriceIncrement() {
    return priceIncrement;
  }

  public String getPriceCurrency() {
    return priceCurrency;
  }

  public Map<String,String> getUncommonAIs() {
    return uncommonAIs;
  }

  @Override
  public String getDisplayResult() {
    return String.valueOf(rawText);
  }
}
