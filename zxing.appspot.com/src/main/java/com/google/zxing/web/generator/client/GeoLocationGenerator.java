/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.web.generator.client;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A generator for geo location. It also accepts a google maps links and
 * extracts the coordinates and query from the URL.
 * 
 * @author Yohann Coppel
 */
public final class GeoLocationGenerator implements GeneratorSource {

  private static final String LON_REGEXP = "[+-]?[0-9]+(?:.[0-9]+)?";
  private static final String LAT_REGEXP = "[+-]?[0-9]+(?:.[0-9]+)?";
  
  private Grid table;
  private final TextBox latitude = new TextBox();
  private final TextBox longitude = new TextBox();
  private final TextBox query = new TextBox();
  
  public GeoLocationGenerator(ChangeHandler handler, KeyPressHandler keyListener) {
    latitude.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    latitude.addChangeHandler(handler);
    latitude.addKeyPressHandler(keyListener);
    longitude.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    longitude.addChangeHandler(handler);
    longitude.addKeyPressHandler(keyListener);
    query.addChangeHandler(handler);
    query.addKeyPressHandler(keyListener);
  }
  
  @Override
  public String getName() {
    return "Geo location";
  }

  @Override
  public String getText() throws GeneratorException {
    String que = getQueryField();
    if (que != null && !que.isEmpty()) {
      if (getLatitudeField() == null) {
        latitude.setText("0");
      }
      if (getLongitudeField() == null) {
        longitude.setText("0");
      }
    }
    String lat = getLatitudeField();
    String lon = getLongitudeField();
    
    if (que != null && !que.isEmpty()) {
      return "geo:" + lat + ',' + lon + "?q=" + que;
    }
    return "geo:" + lat + ',' + lon;
  }

  private String getQueryField() {
    String que = query.getText();
    que = que.replace("&", "%26");
    return que;
  }

  private String getLongitudeField() throws GeneratorException {
    String lon = longitude.getText();
    if (!lon.matches(LON_REGEXP)) {
      throw new GeneratorException("Longitude is not a correct value.");
    }
    double val = Double.parseDouble(lon);
    if (val < -180.0 || val > 180.0) {
      throw new GeneratorException("Longitude must be in [-180:180]");
    }
    return lon;
  }

  private String getLatitudeField() throws GeneratorException {
    String lat = latitude.getText();
    if (!lat.matches(LAT_REGEXP)) {
      throw new GeneratorException("Latitude is not a correct value.");
    }
    double val = Double.parseDouble(lat);
    if (val < -90.0 || val > 90.0) {
      throw new GeneratorException("Latitude must be in [-90:90]");
    }
    return lat;
  }

  @Override
  public Grid getWidget() {
    if (table != null) {
      return table;
    }
    table = new Grid(3, 2);
    
    table.setText(0, 0, "Latitude");
    table.setWidget(0, 1, latitude);
    table.setText(1, 0, "Longitude");
    table.setWidget(1, 1, longitude);
    table.setText(2, 0, "Query");
    table.setWidget(2, 1, query);
    return table;
  }

  @Override
  public void validate(Widget widget) throws GeneratorException {
    if (widget == latitude) {
      getLatitudeField();
    }
    if (widget == longitude) {
      getLongitudeField();
    }
  }

  @Override
  public void setFocus() {
    latitude.setFocus(true);
  }

}
