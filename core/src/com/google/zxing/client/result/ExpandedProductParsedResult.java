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

/**
 * @author Antonio Manuel Benjumea Conde, Servinform, S.A.
 * @author Agustín Delgado, Servinform, S.A.
 */
public class ExpandedProductParsedResult extends ParsedResult {

  public static final String KILOGRAM = "KG";
  public static final String POUND = "LB";

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
  private final Hashtable uncommonAIs;

  ExpandedProductParsedResult() {
    super(ParsedResultType.PRODUCT);
    this.productID = "";
    this.sscc = "";
    this.lotNumber = "";
    this.productionDate = "";
    this.packagingDate = "";
    this.bestBeforeDate = "";
    this.expirationDate = "";
    this.weight = "";
    this.weightType = "";
    this.weightIncrement = "";
    this.price = "";
    this.priceIncrement = "";
    this.priceCurrency = "";
    this.uncommonAIs = new Hashtable();
  }

  public ExpandedProductParsedResult(String productID, String sscc,
      String lotNumber, String productionDate, String packagingDate,
      String bestBeforeDate, String expirationDate, String weight,
      String weightType, String weightIncrement, String price,
      String priceIncrement, String priceCurrency, Hashtable uncommonAIs) {
    super(ParsedResultType.PRODUCT);
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

  public boolean equals(Object o){
    if (!(o instanceof ExpandedProductParsedResult)) {
      return false;
    }

    ExpandedProductParsedResult other = (ExpandedProductParsedResult)o;

    return this.productID.equals(       other.productID)
      && this.sscc.equals(            other.sscc)
      && this.lotNumber.equals(       other.lotNumber)
      && this.productionDate.equals(  other.productionDate)
      && this.bestBeforeDate.equals(  other.bestBeforeDate)
      && this.expirationDate.equals(  other.expirationDate)
      && this.weight.equals(          other.weight)
      && this.weightType.equals(      other.weightType)
      && this.weightIncrement.equals( other.weightIncrement)
      && this.price.equals(           other.price)
      && this.priceIncrement.equals(  other.priceIncrement)
      && this.priceCurrency.equals(   other.priceCurrency)
      && this.uncommonAIs.equals(     other.uncommonAIs);
  }

  public int hashCode(){
    int hash1 = this.productID.hashCode();
    hash1 = 31 * hash1 + this.sscc.hashCode();
    hash1 = 31 * hash1 + this.lotNumber.hashCode();
    hash1 = 31 * hash1 + this.productionDate.hashCode();
    hash1 = 31 * hash1 + this.bestBeforeDate.hashCode();
    hash1 = 31 * hash1 + this.expirationDate.hashCode();
    hash1 = 31 * hash1 + this.weight.hashCode();

    int hash2 = this.weightType.hashCode();
    hash2 = 31 * hash2 + this.weightIncrement.hashCode();
    hash2 = 31 * hash2 + this.price.hashCode();
    hash2 = 31 * hash2 + this.priceIncrement.hashCode();
    hash2 = 31 * hash2 + this.priceCurrency.hashCode();
    hash2 = 31 * hash2 + this.uncommonAIs.hashCode();
    return hash1 ^ hash2;
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

  public Hashtable getUncommonAIs() {
    return uncommonAIs;
  }

  public String getDisplayResult() {
    return productID;
  }
}
