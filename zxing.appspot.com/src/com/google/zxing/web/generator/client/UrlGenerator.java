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
 * A generator for URL addresses.
 * 
 * @author Yohann Coppel
 */
public final class UrlGenerator implements GeneratorSource {

  private Grid table;
  private final TextBox url = new TextBox();
  
  public UrlGenerator(ChangeHandler handler, KeyPressHandler keyListener) {
    url.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    url.addChangeHandler(handler);
    url.addKeyPressHandler(keyListener);
  }
  
  @Override
  public Grid getWidget() {
    if (table != null) {
      // early termination if the table has already been constructed
      return table;
    }
    
    table = new Grid(1, 2);
    table.getColumnFormatter().addStyleName(0, "firstColumn");
    
    url.setText("http://");
    
    table.setText(0, 0, "URL");
    table.setWidget(0, 1, url);
    
    return table;
  }

  @Override
  public String getName() {
    return "URL";
  }

  @Override
  public String getText() throws GeneratorException {
    return getUrlField();
  }

  private String getUrlField() throws GeneratorException {
    String input = url.getText();
    Validators.validateUrl(input);
    return input;
  }
  
  @Override
  public void validate(Widget widget) throws GeneratorException {
    if (widget == url) {
      getUrlField();
    }
  }
  
  @Override
  public void setFocus() {
    url.setFocus(true);
  }
}
