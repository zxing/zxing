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
 * Generator for email address.
 * 
 * @author Yohann Coppel
 */
public class EmailGenerator implements GeneratorSource {
  Grid table = null;
  TextBox email = new TextBox();
  
  public EmailGenerator(ChangeListener listener, KeyPressHandler keyListener) {
    email.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    email.addChangeListener(listener);
    email.addKeyPressHandler(keyListener);
  }
  
  public String getName() {
    return "Email address";
  }

  public String getText() throws GeneratorException {
    String email = getEmailField();
    return "mailto:"+email;
  }

  private String getEmailField() throws GeneratorException {
    String input = email.getText();
    if (input.length() < 1) {
      throw new GeneratorException("Email must be present.");
    }
    Validators.validateEmail(input);
    return input;
  }
  
  public Grid getWidget() {
    if (table != null) {
      return table;
    }
    table = new Grid(1, 2);

    table.setText(0, 0, "Address");
    table.setWidget(0, 1, email);
    
    return table;
  }

  public void validate(Widget widget) throws GeneratorException {
    if (widget == email) {
      getEmailField();
    }
  }

  public void setFocus() {
    email.setFocus(true);
  }
}
