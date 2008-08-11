/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.client.rim;

import com.google.zxing.client.rim.persistence.history.DecodeHistory;
import com.google.zxing.client.rim.persistence.history.DecodeHistoryItem;
import com.google.zxing.client.rim.util.Log;
import net.rim.blackberry.api.browser.Browser;
import net.rim.blackberry.api.browser.BrowserSession;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * The screen used to display the qrcode decoding history.
 *
 * This code was contributed by LifeMarks.
 *
 * @author Matt York (matt@lifemarks.mobi)
 */
final class HistoryScreen extends MainScreen {

  HistoryScreen() {
    setTitle(new LabelField("ZXing - History", DrawStyle.ELLIPSIS | USE_ALL_WIDTH));
    Manager vfm = new VerticalFieldManager(FIELD_HCENTER | VERTICAL_SCROLL);
    Log.debug("Num history items: " + DecodeHistory.getInstance().getNumItems());
    DecodeHistory history = DecodeHistory.getInstance();
    FieldChangeListener itemListener = new ButtonListener();
    for (int i = 0; i < history.getNumItems(); i++) {
      DecodeHistoryItem item = history.getItemAt(i);
      Field labelButton = new ButtonField(item.getURI(), FIELD_HCENTER | ButtonField.CONSUME_CLICK);
      labelButton.setChangeListener(itemListener);
      vfm.add(labelButton);
    }

    Field okButton = new ButtonField("OK", FIELD_HCENTER | ButtonField.CONSUME_CLICK);
    okButton.setChangeListener(itemListener);
    add(vfm);
  }

  /**
   * Closes the screen when the OK button is pressed.
   */
  private static class ButtonListener implements FieldChangeListener {
    public void fieldChanged(Field field, int context) {
      if (field instanceof ButtonField) {
        BrowserSession browserSession = Browser.getDefaultSession();
        // This cannot be weakened to FieldLabelProvider -- not a public API
        browserSession.displayPage(((ButtonField) field).getLabel());
      }
    }
  }

  /**
   * Overriding this method removes the save changes prompt
   */
  public boolean onSavePrompt() {
    setDirty(false);
    return true;
  }

}
