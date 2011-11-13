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
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A generator for a sms address. Gives the option of filling up the content
 * of the message as well.
 * 
 * @author Yohann Coppel
 */
public final class SmsAddressGenerator implements GeneratorSource {

  private Grid table;
  private final TextBox number = new TextBox();
  private final TextArea message = new TextArea();

  public SmsAddressGenerator(ChangeHandler handler, KeyPressHandler keyListener) {
    number.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    number.addChangeHandler(handler);
    number.addKeyPressHandler(keyListener);
    message.addChangeHandler(handler);
  }
  
  @Override
  public String getName() {
    return "SMS";
  }

  @Override
  public String getText() throws GeneratorException {
    String inputNumber = getTelField();    
    String inputMessage = getMessageField();
    
    String output = inputNumber;
    // we add the text only if there actually is something in the field.
    if (inputMessage.length() > 0) {
      output += ':' + inputMessage;
    }
    
    return "smsto:" + output;
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
 
  private String getMessageField() throws GeneratorException {
    String inputMessage = message.getText();
    if (inputMessage.length() > 150) {
      throw new GeneratorException("Sms message can not be longer than 150 characters.");
    }
    return inputMessage;
  }
  
  @Override
  public Grid getWidget() {
    if (table != null) {
      return table;
    }
    table = new Grid(2, 2);

    table.setText(0, 0, "Phone number");
    table.setWidget(0, 1, number);

    table.setText(1, 0, "Message");
    table.setWidget(1, 1, message);

    return table;
  }

  @Override
  public void validate(Widget widget) throws GeneratorException {
    if (widget == number) {
      getTelField();
    }
    if (widget == message) {
      getMessageField();
    }
  }
  
  @Override
  public void setFocus() {
    number.setFocus(true);
  }
}
