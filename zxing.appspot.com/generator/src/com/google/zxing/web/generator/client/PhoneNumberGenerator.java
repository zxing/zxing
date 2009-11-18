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
 * A generator for a phone number.
 * 
 * @author Yohann Coppel
 */
public class PhoneNumberGenerator implements GeneratorSource {
  Grid table = null;
  TextBox number = new TextBox();
  
  public PhoneNumberGenerator(ChangeListener listener,
      KeyPressHandler keyListener) {
    number.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    number.addChangeListener(listener);
    number.addKeyPressHandler(keyListener);
  }
  
  public String getName() {
    return "Phone number";
  }

  public String getText() throws GeneratorException {
    String tel = getTelField();
    return "tel:" + tel;
  }

  private String getTelField() throws GeneratorException {
    String input = number.getText();
    if (input.length() < 1) {
      throw new GeneratorException("Phone number must be present.");
    }
    input = Validators.filterNumber(input);
    Validators.validateNumber(input);
    return input;
  }
  
  public Grid getWidget() {
    if (table != null) {
      return table;
    }
    table = new Grid(1, 2);

    table.setText(0, 0, "Phone number");
    table.setWidget(0, 1, number);
    
    return table;
  }

  public void validate(Widget widget) throws GeneratorException {
    if (widget == number) {
      getTelField();
    }
  }
  
  public void setFocus() {
    number.setFocus(true);
  }
}
