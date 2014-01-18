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


public final class VINParsedResult extends ParsedResult {

  private final String vin;
  private final String worldManufacturerID;
  private final String vehicleDescriptorSection;
  private final String vehicleIdentifierSection;
  private final String countryCode;
  private final String vehicleAttributes;
  private final int modelYear;
  private final char plantCode;
  private final String sequentialNumber;

  public VINParsedResult(String vin,
                         String worldManufacturerID,
                         String vehicleDescriptorSection,
                         String vehicleIdentifierSection,
                         String countryCode,
                         String vehicleAttributes,
                         int modelYear,
                         char plantCode,
                         String sequentialNumber) {
    super(ParsedResultType.VIN);
    this.vin = vin;
    this.worldManufacturerID = worldManufacturerID;
    this.vehicleDescriptorSection = vehicleDescriptorSection;
    this.vehicleIdentifierSection = vehicleIdentifierSection;
    this.countryCode = countryCode;
    this.vehicleAttributes = vehicleAttributes;
    this.modelYear = modelYear;
    this.plantCode = plantCode;
    this.sequentialNumber = sequentialNumber;
  }
  
  public String getVIN() {
    return vin;
  }

  public String getWorldManufacturerID() {
    return worldManufacturerID;
  }

  public String getVehicleDescriptorSection() {
    return vehicleDescriptorSection;
  }

  public String getVehicleIdentifierSection() {
    return vehicleIdentifierSection;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public String getVehicleAttributes() {
    return vehicleAttributes;
  }

  public int getModelYear() {
    return modelYear;
  }

  public char getPlantCode() {
    return plantCode;
  }

  public String getSequentialNumber() {
    return sequentialNumber;
  }

  @Override
  public String getDisplayResult() {
    StringBuilder result = new StringBuilder(50);
    result.append(worldManufacturerID).append(' ');
    result.append(vehicleDescriptorSection).append(' ');
    result.append(vehicleIdentifierSection).append('\n');
    if (countryCode != null) {
      result.append(countryCode).append(' ');
    }
    result.append(modelYear).append(' ');
    result.append(plantCode).append(' ');
    result.append(sequentialNumber).append('\n');
    return result.toString();
  }
}
