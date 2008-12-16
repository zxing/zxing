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
  TextBox company = new TextBox();
  TextBox tel = new TextBox();
  TextBox url = new TextBox();
  TextBox email = new TextBox();
  TextBox address = new TextBox();
  TextBox memo = new TextBox();
  TextBox[] widgets = {name, company, tel, url, email, address, memo};
  
  public ContactInfoGenerator(ChangeListener changeListener) {
    for (TextBox w: widgets) {
      w.addChangeListener(changeListener);
    }
  }
  
  public String getName() {
    return "Contact information";
  }

  public String getText() throws GeneratorException {
    String name = getNameField();
    String company = getCompanyField();
    String tel = getTelField();
    String url = getUrlField();
    String email = getEmailField();
    String address = getAddressField();
    String memo = getMemoField();
    
    // Build the output with obtained data.
    // note that some informations may just be "" if they were not specified.
    //return getVCard(name, company, tel, url, email, address, memo);
    return getMeCard(name, company, tel, url, email, address, memo);
  }

  private String getMeCard(String name, String company, String tel, String url,
      String email, String address, String memo) {
    String output = "MECARD:";
    output += "N:" + name + ";";
    output += company.length() > 0 ? "ORG:" + company + ";" : "";
    output += tel.length() > 0 ? "TEL:" + tel + ";" : "";
    output += url.length() > 0 ? "URL:" + url + ";" : "";
    output += email.length() > 0 ? "EMAIL:" + email + ";" : "";
    output += address.length() > 0 ? "ADR:" + address + ";" : "";
    output += memo.length() > 0 ? "NOTE:" + memo + ";" : "";
    output += ";";    
    return output;    
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
  
  private String getNameField() throws GeneratorException {
    String inputName = name.getText();
    if (inputName.length() < 1) {
      throw new GeneratorException("Name must be at least 1 character.");
    }
    if (inputName.contains("\n")) {
      throw new GeneratorException("Name should not contanains \\n characters.");
    }
    if (inputName.contains(";")) {
      throw new GeneratorException("Name must not contains ; characters");
    }
    return inputName;
  }
  
  private String getCompanyField() throws GeneratorException {
    String input = company.getText();
    if (input.length() < 1) {
      return "";
    }
    if (input.contains("\n")) {
      throw new GeneratorException("Company should not contanains \\n characters.");
    }
    if (input.contains(";")) {
      throw new GeneratorException("Company must not contains ; characters");
    }
    // the input contains some informations. 
    return input;
  }

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
    if (input.length() < 1) {
      return "";
    }
    Validators.validateUrl(input);
    if (input.contains(";")) {
      throw new GeneratorException("URL must not contains ; characters");
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
    String input = address.getText();
    if (input.length() < 1) {
      return "";
    }
    if (input.contains("\n")) {
      throw new GeneratorException("Address must not contain \\n characters.");
    }
    if (input.contains(";")) {
      throw new GeneratorException("Address must not contains ; characters");
    }
    return input;
  }
  
  private String getMemoField() throws GeneratorException {
    String input = memo.getText();
    if (input.length() < 1) {
      return "";
    }
    if (input.contains("\n")) {
      throw new GeneratorException("Memo must not contain \\n characters.");
    }
    if (input.contains(";")) {
      throw new GeneratorException("Memo must not contains ; characters");
    }
    return input;
  }
  
  public Grid getWidget() {
    if (table != null) {
      // early termination if the table has already been constructed
      return table;
    }
    table = new Grid(7, 2);
    
    table.setText(0, 0, "Name");
    table.setWidget(0, 1, name);
    table.setText(1, 0, "Company");
    table.setWidget(1, 1, company);
    table.setText(2, 0, "Phone number");
    table.setWidget(2, 1, tel);
    table.setText(3, 0, "Email");
    table.setWidget(3, 1, email);
    table.setText(4, 0, "Address");
    table.setWidget(4, 1, address);
    table.setText(5, 0, "Website");
    table.setWidget(5, 1, url);
    table.setText(6, 0, "Memo");
    table.setWidget(6, 1, memo);
    
    name.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    return table;
  }

  public void validate(Widget widget) throws GeneratorException {
    if (widget == name) getNameField();
    if (widget == company) getCompanyField();
    if (widget == tel) getTelField();
    if (widget == email) getEmailField();
    if (widget == address) getAddressField();
    if (widget == url) getUrlField();
    if (widget == memo) getMemoField();
  }

  public void setFocus() {
    name.setFocus(true);
  }
}
