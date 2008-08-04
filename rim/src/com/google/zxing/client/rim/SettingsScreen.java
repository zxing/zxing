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

import com.google.zxing.client.rim.persistence.AppSettings;
import com.google.zxing.client.rim.persistence.history.DecodeHistory;
import com.google.zxing.client.rim.util.Log;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiEngine;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * Screen used to change application settings.
 *
 * This code was contributed by LifeMarks.
 *
 * @author Matt York (matt@lifemarks.mobi)
 */
final class SettingsScreen extends MainScreen {

  private boolean changes;
  private final AppSettings settings;
  private final CheckboxField camResMsgCheckBox;

  SettingsScreen() {
    setTitle(new LabelField("ZXing - Settings", DrawStyle.ELLIPSIS | USE_ALL_WIDTH));
    Manager vfm = new VerticalFieldManager(FIELD_HCENTER);

    settings = AppSettings.getInstance();
    Boolean cameraResMsgSetting = settings.getBooleanItem(AppSettings.SETTING_CAM_RES_MSG);
    boolean cameraResMsgSettingBool = (cameraResMsgSetting != null) && cameraResMsgSetting.booleanValue();
    // 0
    camResMsgCheckBox = new CheckboxField("Don't show camera resolution message", cameraResMsgSettingBool);
    camResMsgCheckBox.setChangeListener(new ButtonListener(this));
    vfm.add(camResMsgCheckBox);

    // 1
    Field clearHistoryButton = new ButtonField("Clear History",FIELD_HCENTER | ButtonField.CONSUME_CLICK);
    clearHistoryButton.setChangeListener(new ButtonListener(this));
    vfm.add(clearHistoryButton);

    // 2
    Field okButton = new ButtonField("OK", FIELD_HCENTER | ButtonField.CONSUME_CLICK);
    okButton.setChangeListener(new ButtonListener(this));
    vfm.add(okButton);

    add(vfm);
  }

  /**
   * Listens for button clicks and executes the appropriate action.
   */
  private final class ButtonListener implements FieldChangeListener {
    private final Screen screen;
    private ButtonListener(Screen screen) {
      this.screen = screen;
    }
    public void fieldChanged(Field field, int context) {
      Log.debug("Field: " + field.getIndex() + " , context: " + context);
      switch (field.getIndex()) {
        case 0:
          settings.addItem(AppSettings.SETTING_CAM_RES_MSG,
                           (camResMsgCheckBox.getChecked()) ? Boolean.TRUE : Boolean.FALSE);
          changes = true;
          break;
        case 1:
          // TODO confirm
          DecodeHistory.getInstance().clear();
          DecodeHistory.getInstance().persist();
        case 2: //ok
          if (changes) {
            AppSettings.getInstance().persist();
          }
          UiEngine ui = Ui.getUiEngine();
          ui.popScreen(screen);
          break;
      }
    }
  }

}
