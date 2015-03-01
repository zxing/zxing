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

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A Generator for Wifi networks.
 * 
 * @author Vikram Aggarwal
 * @author Sean Owen
 */
public final class WifiGenerator implements GeneratorSource {

  private Grid table;
  private final TextBox ssid = new TextBox();
  private final TextBox password = new TextBox();
  private final ListBox networkType = new ListBox();
  private final CheckBox hidden = new CheckBox();

  public WifiGenerator(ChangeHandler handler, KeyPressHandler keyListener) {
    networkType.addItem("WEP", "WEP");
    networkType.addItem("WPA/WPA2", "WPA");
    networkType.addItem("No encryption", "nopass");
    TextBox[] widgets = {ssid, password};
    for (TextBox w: widgets) {
      w.addChangeHandler(handler);
      w.addKeyPressHandler(keyListener);
    }
  }
  
  @Override
  public String getName() {
    return "Wifi network";
  }

  @Override
  public String getText() throws GeneratorException {
    String ssid = getSsidField();
    String password = getPasswordField();
    String networkType = getNetworkTypeField();
    boolean hidden = getHiddenField();
    // Build the output with obtained data.
    return getWifiString(ssid, password, networkType, hidden);
  }

  private static String getWifiString(String ssid, String password, String type, boolean hidden) {
    StringBuilder output = new StringBuilder(100);
    output.append("WIFI:");
    output.append("S:").append(ssid).append(';');
    if (type != null && !type.isEmpty() && !"nopass".equals(type)) {
      maybeAppend(output, "T:", type);
    }
    maybeAppend(output, "P:", password);
    if (hidden) {
      maybeAppend(output, "H:", "true");
    }
    output.append(';');
    return output.toString();
  }

  private static void maybeAppend(StringBuilder output, String prefix, String value) {
    if (value != null && !value.isEmpty()) {
      output.append(prefix).append(value).append(';');
    }
  }

  private static String parseTextField(String name, HasText textBox) throws GeneratorException {
    String input = textBox.getText();
    if (input.isEmpty()) {
      return "";
    }
    if (input.contains("\n")) {
      throw new GeneratorException(name + " field must not contain \\n characters.");
    }
    return input.replaceAll("([\\\\:;])", "\\\\$1");
  }
  
  private String getSsidField() throws GeneratorException {
    String input = ssid.getText();
    if (input.isEmpty()) {
      throw new GeneratorException("SSID must be at least 1 character.");
    }
    String parsed = parseTextField("SSID", ssid);
    return quoteHex(parsed); // Android needs hex-like SSIDs quoted or will be read as hex
  }
  
  private String getPasswordField() throws GeneratorException {
  return parseTextField("Password", password);
  }
  
  private String getNetworkTypeField() {
    return networkType.getValue(networkType.getSelectedIndex());
  }

  private boolean getHiddenField() {
    Boolean value = hidden.getValue();
    return value != null && value;
  }
  
  @Override
  public Grid getWidget() {
    if (table != null) {
      // early termination if the table has already been constructed
      return table;
    }
    table = new Grid(4, 2);
    
    table.setText(0, 0, "SSID");
    table.setWidget(0, 1, ssid);
    table.setText(1, 0, "Password");
    table.setWidget(1, 1, password);
    table.setText(2, 0, "Network Type");
    table.setWidget(2, 1, networkType);
    table.setText(3, 0, "Hidden?");
    table.setWidget(3, 1, hidden);

    ssid.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    return table;
  }

  @Override
  public void validate(Widget widget) throws GeneratorException {
    if (widget == ssid) {
      getSsidField();
    }
    if (widget == password) {
      getPasswordField();
    }
    if (widget == networkType) {
      getNetworkTypeField();
    }
    if (widget == hidden) {
      getHiddenField();
    }
  }

  @Override
  public void setFocus() {
    ssid.setFocus(true);
  }

  private static String quoteHex(String value) {
    if (value != null && value.matches("[0-9A-Fa-f]+")) {
      if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
        return value;
      }
      return '\"' + value + '\"';
    }
    return value;
  }

}
