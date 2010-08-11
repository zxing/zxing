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
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A Generator for contact informations, output is in MeCard format.
 * 
 * @author Yohann Coppel
 */
public class ContactInfoGenerator implements GeneratorSource {
  Grid table = null;
  TextBox name = new TextBox();
  //TextBox company = new TextBox();
  TextBox tel = new TextBox();
  TextBox url = new TextBox();
  TextBox email = new TextBox();
  TextBox address = new TextBox();
  TextBox address2 = new TextBox();
  TextBox memo = new TextBox();
  TextBox[] widgets = {name, tel, url, email, address, address2, memo};
  
  public ContactInfoGenerator(ChangeListener changeListener,
      KeyPressHandler keyListener) {
    for (TextBox w: widgets) {
      w.addChangeListener(changeListener);
      w.addKeyPressHandler(keyListener);
    }
  }
  
  public String getName() {
    return "Contact information";
  }

  public String getText() throws GeneratorException {
    String name = getNameField();
    //String company = getCompanyField();
    String tel = getTelField();
    String url = getUrlField();
    String email = getEmailField();
    String address = getAddressField();
    String address2 = getAddress2Field();
    String memo = getMemoField();
    
    // Build the output with obtained data.
    // note that some informations may just be "" if they were not specified.
    //return getVCard(name, company, tel, url, email, address, memo);
    return getMeCard(name, tel, url, email, address, address2, memo);
  }

  private String getMeCard(String name, String tel, String url,
      String email, String address, String address2, String memo) {
    StringBuilder output = new StringBuilder();
    output.append("MECARD:");
    name = name.replace(",", ""); // remove commas -- reserved char in MECARD
    output.append("N:").append(name).append(';');
    //maybeAppend(output, "ORG:", company); // Not standard; don't generate
    maybeAppend(output, "TEL:", tel);
    maybeAppend(output, "URL:", url);
    maybeAppend(output, "EMAIL:", email);
    if (address.length() > 0 || address2.length() > 0) {
      output.append("ADR:");
      if (address.length() > 0) {
        output.append(address);
      }
      if (address2.length() > 0) {
        if (address.length() > 0) {
          output.append(' ');
        }
        output.append(address2);
      }
      output.append(';');
    }
    maybeAppend(output, "NOTE:", memo);
    output.append(';');
    return output.toString();
  }

  private static void maybeAppend(StringBuilder output, String prefix, String value) {
    if (value.length() > 0) {
      output.append(prefix).append(value).append(';');
    }
  }
  
  /*// VCARD GENERATION. Keep this in case we want to go back to vcard format
    // or have the option.
  private String getVCard(String name, String company, String tel, String url,
      String email, String address, String memo) {
    String output = "BEGIN:VCARD\n";
    output += "N:" + name + "\n";
    output += company.length() > 0 ? "ORG:" + company + "\n" : "";
    output += tel.length() > 0 ? "TEL:" + tel + "\n" : "";
    output += url.length() > 0 ? "URL:" + url + "\n" : "";
    output += email.length() > 0 ? "EMAIL:" + email + "\n" : "";
    output += address.length() > 0 ? "ADR:" + address + "\n" : "";
    output += memo.length() > 0 ? "NOTE:" + memo + "\n" : "";
    output += "END:VCARD";
    
    return output;    
  }
  */

  private static String parseTextField(String name, TextBox textBox) throws GeneratorException {
    String input = textBox.getText();
    if (input.length() < 1) {
      return "";
    }
    if (input.contains("\n")) {
      throw new GeneratorException(name + " field must not contain \\n characters.");
    }
    if (input.contains(";")) {
      throw new GeneratorException(name + " field must not contains ; characters");
    }
    return input;
  }
  
  private String getNameField() throws GeneratorException {
    String input = name.getText();
    if (input.length() < 1) {
      throw new GeneratorException("Name must be at least 1 character.");
    }
    return parseTextField("Name", name);
  }
  
  //private String getCompanyField() throws GeneratorException {
  //  return parseTextField("Company", company);
  //}

  private String getTelField() throws GeneratorException {
    String input = Validators.filterNumber(tel.getText());
    if (input.length() < 1) {
      return "";
    }
    Validators.validateNumber(input);
    if (input.contains(";")) {
      throw new GeneratorException("Tel must not contains ; characters");
    }
    return input;
  }
  
  private String getUrlField() throws GeneratorException {
    String input = url.getText();
    if (input != null && input.length() > 0) {
      Validators.validateUrl(input);
    }
    return input;
  }
  
  private String getEmailField() throws GeneratorException {
    String input = email.getText();
    if (input.length() < 1) {
      return "";
    }
    Validators.validateEmail(input);
    if (input.contains(";")) {
      throw new GeneratorException("Email must not contains ; characters");
    }
    return input;
  }
  
  private String getAddressField() throws GeneratorException {
    return parseTextField("Address", address);
  }

  private String getAddress2Field() throws GeneratorException {
    return parseTextField("Address 2", address2);
  }
  
  private String getMemoField() throws GeneratorException {
    return parseTextField("Memo", memo);
  }
  
  public Grid getWidget() {
    if (table != null) {
      // early termination if the table has already been constructed
      return table;
    }
    table = new Grid(7, 2);
    
    table.setText(0, 0, "Name");
    table.setWidget(0, 1, name);
    table.setText(1, 0, "Phone number");
    table.setWidget(1, 1, tel);
    table.setText(2, 0, "Email");
    table.setWidget(2, 1, email);
    table.setText(3, 0, "Address");
    table.setWidget(3, 1, address);
    table.setText(4, 0, "Address 2");
    table.setWidget(4, 1, address2);
    table.setText(5, 0, "Website");
    table.setWidget(5, 1, url);
    table.setText(6, 0, "Memo");
    table.setWidget(6, 1, memo);
    
    name.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    return table;
  }

  public void validate(Widget widget) throws GeneratorException {
    if (widget == name) getNameField();
    //if (widget == company) getCompanyField();
    if (widget == tel) getTelField();
    if (widget == email) getEmailField();
    if (widget == address) getAddressField();
    if (widget == address2) getAddress2Field();
    if (widget == url) getUrlField();
    if (widget == memo) getMemoField();
  }

  public void setFocus() {
    name.setFocus(true);
  }
}
