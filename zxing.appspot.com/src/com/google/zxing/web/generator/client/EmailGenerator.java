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
 * Generator for email address.
 * 
 * @author Yohann Coppel
 */
public final class EmailGenerator implements GeneratorSource {

  private Grid table;
  private final TextBox email = new TextBox();
  
  public EmailGenerator(ChangeHandler handler, KeyPressHandler keyListener) {
    email.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    email.addChangeHandler(handler);
    email.addKeyPressHandler(keyListener);
  }
  
  @Override
  public String getName() {
    return "Email address";
  }

  @Override
  public String getText() throws GeneratorException {
    return "mailto:" + getEmailField();
  }

  private String getEmailField() throws GeneratorException {
    String input = email.getText();
    if (input.isEmpty()) {
      throw new GeneratorException("Email must be present.");
    }
    Validators.validateEmail(input);
    return input;
  }
  
  @Override
  public Grid getWidget() {
    if (table != null) {
      return table;
    }
    table = new Grid(1, 2);
    table.setText(0, 0, "Address");
    table.setWidget(0, 1, email);
    return table;
  }

  @Override
  public void validate(Widget widget) throws GeneratorException {
    if (widget == email) {
      getEmailField();
    }
  }

  @Override
  public void setFocus() {
    email.setFocus(true);
  }
}
