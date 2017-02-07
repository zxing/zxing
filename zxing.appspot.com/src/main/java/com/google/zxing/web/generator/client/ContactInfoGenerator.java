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
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A Generator for contact information, output is in MeCard format.
 * 
 * @author Yohann Coppel
 * @author Sean Owen
 */
public final class ContactInfoGenerator implements GeneratorSource {

  private Grid table;
  private final ListBox encoding = new ListBox();
  private final TextBox name = new TextBox();
  private final TextBox company = new TextBox();
  private final TextBox title = new TextBox();
  private final TextBox tel = new TextBox();
  private final TextBox url = new TextBox();
  private final TextBox email = new TextBox();
  private final TextBox address = new TextBox();
  private final TextBox address2 = new TextBox();
  private final TextBox memo = new TextBox();

  public ContactInfoGenerator(ChangeHandler changeHandler, KeyPressHandler keyListener) {
    TextBox[] widgets = {name, company, tel, url, email, address, address2, memo};
    for (TextBox w: widgets) {
      w.addChangeHandler(changeHandler);
      w.addKeyPressHandler(keyListener);
    }
    encoding.addItem("MECARD");
    encoding.addItem("vCard");
    encoding.setSelectedIndex(0);
  }
  
  @Override
  public String getName() {
    return "Contact information";
  }

  @Override
  public String getText() throws GeneratorException {
    String name = getNameField();
    String company = getCompanyField();
    String title = getTitleField();
    String tel = getTelField();
    String url = getUrlField();
    String email = getEmailField();
    String address = getAddressField();
    String address2 = getAddress2Field();
    String memo = getMemoField();
    
    // Build the output with obtained data.
    // note that some informations may just be "" if they were not specified.
    if ("vCard".equals(encoding.getValue(encoding.getSelectedIndex()))) {
      return getVCard(name, company, title, tel, url, email, address, address2, memo);
    }
    return getMeCard(name, company, title, tel, url, email, address, address2, memo);
  }

  private static String getMeCard(String name,
                                  String company,
                                  String title,
                                  String tel,
                                  String url,
                                  String email,
                                  String address,
                                  String address2,
                                  String memo) {
    StringBuilder output = new StringBuilder(100);
    output.append("MECARD:");
    maybeAppendMECARD(output, "N", name.replace(",", ""));
    maybeAppendMECARD(output, "ORG", company);
    maybeAppendMECARD(output, "TEL", tel == null ? null : tel.replaceAll("[^0-9+]+", ""));
    maybeAppendMECARD(output, "URL", url);
    maybeAppendMECARD(output, "EMAIL", email);
    maybeAppendMECARD(output, "ADR", buildAddress(address, address2));
    StringBuilder memoContents = new StringBuilder();
    if (memo != null) {
      memoContents.append(memo);
    }
    if (title != null) {
      if (memoContents.length() > 0) {
        memoContents.append('\n');
      }
      memoContents.append(title);
    }
    maybeAppendMECARD(output, "NOTE", memoContents.toString());
    output.append(';');
    return output.toString();
  }

  private static String buildAddress(String address, String address2) {
    if (!address.isEmpty()) {
      if (!address2.isEmpty()) {
        return address + ' ' + address2;
      }
      return address;
    }
    if (!address2.isEmpty()) {
      return address2;
    }
    return "";
  }

  private static void maybeAppendMECARD(StringBuilder output, String prefix, String value) {
    if (value != null && !value.isEmpty()) {
      value = value.replaceAll("([\\\\:;])", "\\\\$1");
      value = value.replaceAll("\\n", "");
      output.append(prefix).append(':').append(value).append(';');
    }
  }
  
  private static String getVCard(String name,
                                 String company,
                                 String title,
                                 String tel,
                                 String url,
                                 String email,
                                 String address,
                                 String address2,
                                 String memo) {
    StringBuilder output = new StringBuilder(100);
    output.append("BEGIN:VCARD\n");
    output.append("VERSION:3.0\n");
    maybeAppendvCard(output, "N", name);
    maybeAppendvCard(output, "ORG", company);
    maybeAppendvCard(output, "TITLE", title);
    maybeAppendvCard(output, "TEL", tel);
    maybeAppendvCard(output, "URL", url);
    maybeAppendvCard(output, "EMAIL", email);
    maybeAppendvCard(output, "ADR", buildAddress(address, address2));
    maybeAppendvCard(output, "NOTE", memo);
    output.append("END:VCARD");
    return output.toString();
  }
  
  private static void maybeAppendvCard(StringBuilder output, String prefix, String value) {
    if (!value.isEmpty()) {
      value = value.replaceAll("([\\\\,;])", "\\\\$1");
      value = value.replaceAll("\\n", "\\\\n");
      output.append(prefix).append(':').append(value).append('\n');
    }
  }
  
  private String getNameField() throws GeneratorException {
    String input = name.getText();
    if (input.isEmpty()) {
      throw new GeneratorException("Name must be at least 1 character.");
    }
    return input;
  }
  
  private String getCompanyField() {
    return company.getText();
  }

  private String getTitleField() {
    return title.getText();
  }

  private String getTelField() throws GeneratorException {
    String input = Validators.filterNumber(tel.getText());
    if (input.isEmpty()) {
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
    if (input != null && !input.isEmpty()) {
      Validators.validateUrl(input);
    }
    return input;
  }
  
  private String getEmailField() throws GeneratorException {
    String input = email.getText();
    if (input.isEmpty()) {
      return "";
    }
    Validators.validateEmail(input);
    if (input.contains(";")) {
      throw new GeneratorException("Email must not contains ; characters");
    }
    return input;
  }
  
  private String getAddressField() {
    return address.getText();
  }

  private String getAddress2Field() {
    return address2.getText();
  }
  
  private String getMemoField() {
    return memo.getText();
  }
  
  @Override
  public Grid getWidget() {
    if (table != null) {
      // early termination if the table has already been constructed
      return table;
    }
    table = new Grid(10, 2);
    
    table.setText(0, 0, "Name");
    table.setWidget(0, 1, name);
    table.setText(1, 0, "Company");
    table.setWidget(1, 1, company);
    table.setText(2, 0, "Title");
    table.setWidget(2, 1, title);
    table.setText(3, 0, "Phone number");
    table.setWidget(3, 1, tel);
    table.setText(4, 0, "Email");
    table.setWidget(4, 1, email);
    table.setText(5, 0, "Address");
    table.setWidget(5, 1, address);
    table.setText(6, 0, "Address 2");
    table.setWidget(6, 1, address2);
    table.setText(7, 0, "Website");
    table.setWidget(7, 1, url);
    table.setText(8, 0, "Memo");
    table.setWidget(8, 1, memo);
    table.setText(9, 0, "Encoding");
    table.setWidget(9, 1, encoding);
    
    name.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    return table;
  }

  @Override
  public void validate(Widget widget) throws GeneratorException {
    if (widget == name) {
      getNameField();
    }
    if (widget == company) {
      getCompanyField();
    }
    if (widget == title) {
      getTitleField();
    }
    if (widget == tel) {
      getTelField();
    }
    if (widget == email) {
      getEmailField();
    }
    if (widget == address) {
      getAddressField();
    }
    if (widget == address2) {
      getAddress2Field();
    }
    if (widget == url) {
      getUrlField();
    }
    if (widget == memo) {
      getMemoField();
    }
  }

  @Override
  public void setFocus() {
    name.setFocus(true);
  }
}
