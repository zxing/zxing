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
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * A generator for any text content.
 * 
 * @author Yohann Coppel
 */
public final class TextGenerator implements GeneratorSource {

  private Grid table;
  private final TextArea text = new TextArea();

  public TextGenerator(ChangeHandler handler) {
    text.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    text.addChangeHandler(handler);
    text.setVisibleLines(5);
  }
  
  @Override
  public String getName() {
    return "Text";
  }

  @Override
  public String getText() throws GeneratorException {
    return getTextField();
  }
  
  String getTextField() throws GeneratorException {
    String input = text.getText();
    if (input.isEmpty()) {
      throw new GeneratorException("Text should be at least 1 character.");
    }
    return input;
  }

  @Override
  public Grid getWidget() {
    if (table != null) {
      // early termination if the table has already been constructed
      return table;
    }
    
    table = new Grid(1, 2);
    table.getColumnFormatter().addStyleName(0, "firstColumn");
    
    table.setText(0, 0, "Text content");
    table.setWidget(0, 1, text);
    
    return table;
  }

  public static String getErrorMessage() {
    return "";
  }

  @Override
  public void validate(Widget widget) throws GeneratorException {
    if (widget == text) {
      getTextField();
    }
  }
  
  @Override
  public void setFocus() {
    text.setFocus(true);
  }
}
