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
import java.util.Objects;

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
  public boolean equals(Object o) {
    if (!(o instanceof ExpandedProductParsedResult)) {
      return false;
    }

    ExpandedProductParsedResult other = (ExpandedProductParsedResult) o;

    return Objects.equals(productID, other.productID)
        && Objects.equals(sscc, other.sscc)
        && Objects.equals(lotNumber, other.lotNumber)
        && Objects.equals(productionDate, other.productionDate)
        && Objects.equals(bestBeforeDate, other.bestBeforeDate)
        && Objects.equals(expirationDate, other.expirationDate)
        && Objects.equals(weight, other.weight)
        && Objects.equals(weightType, other.weightType)
        && Objects.equals(weightIncrement, other.weightIncrement)
        && Objects.equals(price, other.price)
        && Objects.equals(priceIncrement, other.priceIncrement)
        && Objects.equals(priceCurrency, other.priceCurrency)
        && Objects.equals(uncommonAIs, other.uncommonAIs);
  }

  @Override
  public int hashCode() {
    int hash = Objects.hashCode(productID);
    hash ^= Objects.hashCode(sscc);
    hash ^= Objects.hashCode(lotNumber);
    hash ^= Objects.hashCode(productionDate);
    hash ^= Objects.hashCode(bestBeforeDate);
    hash ^= Objects.hashCode(expirationDate);
    hash ^= Objects.hashCode(weight);
    hash ^= Objects.hashCode(weightType);
    hash ^= Objects.hashCode(weightIncrement);
    hash ^= Objects.hashCode(price);
    hash ^= Objects.hashCode(priceIncrement);
    hash ^= Objects.hashCode(priceCurrency);
    hash ^= Objects.hashCode(uncommonAIs);
    return hash;
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
