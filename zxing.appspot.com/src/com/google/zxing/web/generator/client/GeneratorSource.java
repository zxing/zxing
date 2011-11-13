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

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

/**
 * Base interface for any generator.
 * 
 * @author Yohann Coppel
 */
public interface GeneratorSource {
  /**
   * @return a GWT Grid object, containing the GUI.
   */
  Grid getWidget();
  /**
   * @return the name of the generator to be used in the GUI.
   */
  String getName();
  /**
   * @return the text to be encoded into the QR code.
   * @throws GeneratorException if the input data contains errors.
   */
  String getText() throws GeneratorException;
  /**
   * @param widget the widget that was last modified, and that we want to
   *        validate the content.
   * @throws GeneratorException if the widget contains errors.
   */
  void validate(Widget widget) throws GeneratorException;
  /**
   * Called when the generator is selected in the list. Using this method,
   * the generator should set the focus to the first widget it defines.
   */
  void setFocus();
}
