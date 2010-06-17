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

package com.google.zxing.web.generator.client;

import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A Generator for Wifi networks.
 * 
 * @author Vikram Aggarwal
 */
public class WifiGenerator implements GeneratorSource {
  Grid table = null;
  TextBox ssid = new TextBox();
  TextBox password = new TextBox();
  final boolean multipleSelections = false;
  ListBox networkType = new ListBox(multipleSelections);
  TextBox[] widgets = {ssid, password };
  
  public WifiGenerator(ChangeListener changeListener, KeyPressHandler keyListener) {
	networkType.addItem("WEP", "WEP");
	networkType.addItem("WPA/WPA2", "WPA");
	networkType.addItem("No encryption", "nopass");
	
    for (TextBox w: widgets) {
      w.addChangeListener(changeListener);
      w.addKeyPressHandler(keyListener);
    }
  }
  
  public String getName() {
    return "Wifi network";
  }

  public String getText() throws GeneratorException {
    String ssid = getSsidField();
    String password = getPasswordField();
    String networkType = getNetworkTypeField();
    
    // Build the output with obtained data.
    return getWifiString(ssid, password, networkType);
  }

  private String getWifiString(String ssid, String password, String type) {
    StringBuilder output = new StringBuilder();
    output.append("WIFI:");
    output.append("S:").append(ssid).append(';');
    maybeAppend(output, "T:", type);
    maybeAppend(output, "P:", password);
    output.append(';');
    return output.toString();
  }

  private static void maybeAppend(StringBuilder output, String prefix, String value) {
    if (value.length() > 0) {
      output.append(prefix).append(value).append(';');
    }
  }

  private static String parseTextField(String name, TextBox textBox) throws GeneratorException {
    String input = textBox.getText();
    if (input.length() < 1) {
      return "";
    }
    if (input.contains("\n")) {
      throw new GeneratorException(name + " field must not contain \\n characters.");
    }
    input = input.replace(";", "\\;");
    return input;
  }
  
  private String getSsidField() throws GeneratorException {
    String input = ssid.getText();
    if (input.length() < 1) {
      throw new GeneratorException("SSID must be at least 1 character.");
    }
    return parseTextField("SSID", ssid);
  }
  
  private String getPasswordField() throws GeneratorException {
	return parseTextField("Password", password);
  }
  
  private String getNetworkTypeField() throws GeneratorException {
	String input = networkType.getValue(networkType.getSelectedIndex());
	return input;
  }
  
  public Grid getWidget() {
    if (table != null) {
      // early termination if the table has already been constructed
      return table;
    }
    table = new Grid(8, 2);
    
    table.setText(0, 0, "SSID");
    table.setWidget(0, 1, ssid);
    table.setText(1, 0, "Password");
    table.setWidget(1, 1, password);
    table.setText(2, 0, "Network Type");
    table.setWidget(2, 1, networkType);

    ssid.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    return table;
  }

  public void validate(Widget widget) throws GeneratorException {
    if (widget == ssid) getSsidField();
    if (widget == password) getPasswordField();
    if (widget == networkType) getNetworkTypeField();
  }

  public void setFocus() {
    ssid.setFocus(true);
  }
}
