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

import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.control.SmallMapControl;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.event.MarkerDragEndHandler;
import com.google.gwt.maps.client.event.MapClickHandler.MapClickEvent;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.maps.client.overlay.MarkerOptions;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A generator for geo location. It also accepts a google maps links and
 * extracts the coordinates and query from the URL.
 * 
 * @author Yohann Coppel
 */
public class GeoLocationGenerator implements GeneratorSource, ChangeListener {
  private static final String LON_REGEXP = "[+-]?[0-9]+(.[0-9]+)?";
  private static final String LAT_REGEXP = "[+-]?[0-9]+(.[0-9]+)?";
  
  Grid table = null;
  TextBox latitude = new TextBox();
  TextBox longitude = new TextBox();
  TextBox query = new TextBox();
  TextBox mapsLink = new TextBox();
  MapWidget map = new MapWidget();
  Marker mapMarker = null;
  private ChangeListener changeListener;
  
  public GeoLocationGenerator(ChangeListener listener,
      KeyPressHandler keyListener) {
    this.changeListener = listener;
    latitude.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    latitude.addChangeListener(listener);
    latitude.addChangeListener(this);
    latitude.addKeyPressHandler(keyListener);
    longitude.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    longitude.addChangeListener(listener);
    longitude.addChangeListener(this);
    longitude.addKeyPressHandler(keyListener);
    query.addChangeListener(listener);
    query.addKeyPressHandler(keyListener);
  }
  
  public String getName() {
    return "Geo location";
  }

  public String getText() throws GeneratorException {
    String que = getQueryField();
    if (null != que && que.length() > 0) {
      if (null == getLatitudeField()) {
        latitude.setText("0");
      }
      if (null == getLongitudeField()) {
        longitude.setText("0");
      }
    }
    String lat = getLatitudeField();
    String lon = getLongitudeField();
    
    if (null != que && que.length() > 0) {
      return "geo:"+lat+ ',' +lon+"?q="+que;
    }

    return "geo:"+lat+ ',' +lon;
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
    if (val < -180 || val > 180) {
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
    if (val < -90 || val > 90) {
      throw new GeneratorException("Latitude must be in [-90:90]");
    }
    return lat;
  }

  public Grid getWidget() {
    if (table != null) {
      return table;
    }
    table = new Grid(7, 2);
    
    table.setText(0, 0, "Latitude");
    table.setWidget(0, 1, latitude);
    table.setText(1, 0, "Longitude");
    table.setWidget(1, 1, longitude);
    table.setText(2, 0, "Query");
    table.setWidget(2, 1, query);
    table.setText(3, 0, "OR");
    table.setText(3, 1, "enter a Google Maps link and click Fill:");
    // looks like this:
    // http://maps.google.com/?ie=UTF8&ll=40.741404,-74.00322&spn=0.001484,0.003101&z=18
    Button fill = new Button("Fill &uarr;");
    fill.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        fillWithMaps();
      }
    });
    table.setWidget(4, 0, fill);
    table.setWidget(4, 1, mapsLink);
    
    map.setSize("256px", "256px");
    map.addControl(new SmallMapControl());
    map.getElement().getStyle().setProperty("overflow", "hidden");
    map.addMapClickHandler(new MapClickHandler() {
      public void onClick(MapClickEvent event) {
        mapClick(event);
      }
    });
    table.setText(5, 0, "OR");
    table.setText(5, 1, "use the map to select a location:");
    SimplePanel sp = new SimplePanel();
    sp.add(map);
    table.setWidget(6, 1, sp);
    return table;
  }

  protected void mapClick(MapClickEvent event) {
    latitude.setText(String.valueOf(event.getLatLng().getLatitude()));
    longitude.setText(String.valueOf(event.getLatLng().getLongitude()));
    setMapMarker(event.getLatLng().getLatitude(), event.getLatLng().getLongitude(), false);
    changeListener.onChange(latitude);
    changeListener.onChange(longitude);
  }
  
  protected void mapMarkerMoved() {
    latitude.setText(String.valueOf(mapMarker.getLatLng().getLatitude()));
    longitude.setText(String.valueOf(mapMarker.getLatLng().getLongitude()));
    changeListener.onChange(latitude);
    changeListener.onChange(longitude);
  }
  
  protected void setMapMarker(double lat, double lon, boolean zoomAndCenter) {
    if (mapMarker != null) {
      map.removeOverlay(mapMarker);
    }
    LatLng ll = LatLng.newInstance(lat, lon);
    if (zoomAndCenter) {
      map.setCenter(ll);
      map.setZoomLevel(12);
    }
    if (mapMarker != null) {
      mapMarker.setLatLng(ll);
    } else {
      MarkerOptions opt = MarkerOptions.newInstance();
      opt.setDraggable(true);
      mapMarker = new Marker(ll, opt);
      mapMarker.addMarkerDragEndHandler(new MarkerDragEndHandler() {
        public void onDragEnd(MarkerDragEndEvent event) {
          mapMarkerMoved();
        }
      });
    }
    map.addOverlay(mapMarker);  
  }

  protected void fillWithMaps() {
    String link = mapsLink.getText();
    if (!link.matches("http://maps.google.com/.*")) {
      return;
    }
    String q = "";
    if (link.matches(".*&q=[^&]*&.*")) {
      StringBuilder qBuilder = new StringBuilder();
      for (int i = link.indexOf("&q=") + 3;
          i < link.length() && link.charAt(i) != '&'; ++i) {
        qBuilder.append(link.charAt(i));
      }
      q = qBuilder.toString();
      // special cases:
      q = q.replace("+", " ");
      q = q.replace("%26", "&");
    }
    
    StringBuilder lat = new StringBuilder();
    StringBuilder lon = new StringBuilder();
    if (link.matches(".*&s?ll=[^&]*&.*")) {
      int start;
      if (link.indexOf("&sll=") == -1) {
        start = link.indexOf("&ll=") + 4;
      } else {
        start = link.indexOf("&sll=") + 5;
      }
      boolean beforeComma = true;
      for (int i = start; i < link.length() && link.charAt(i) != '&'; ++i) {
        char c = link.charAt(i);
        if (beforeComma) {
          if (c == ',') {
            beforeComma = false;
          } else {
            lat.append(c);
          }
        } else {
          lon.append(c);
        }
      }
    }
    
    query.setText(URL.decode(q));
    latitude.setText(lat.toString());
    longitude.setText(lon.toString());
    changeListener.onChange(latitude);
    changeListener.onChange(longitude);
    this.onChange(latitude);
  }

  public void validate(Widget widget) throws GeneratorException {
    if (widget == latitude) {
      getLatitudeField();
    }
    if (widget == longitude) {
      getLongitudeField();
    }
  }

  public void setFocus() {
    latitude.setFocus(true);
  }

  public void onChange(Widget sender) {
    if (sender == latitude || sender == longitude) {
      try {
        double lat = Double.parseDouble(getLatitudeField());
        double lon = Double.parseDouble(getLongitudeField());
        setMapMarker(lat, lon, true);
      } catch (NumberFormatException e) {
      } catch (GeneratorException e) {
      }
    }
  }
}
