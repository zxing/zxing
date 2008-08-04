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
import net.rim.device.api.ui.UiApplication;

/**
 * Starts the application with the MenuScreen screen on the stack.
 * As well, the required permissions are requested and the history and app settings are initialized.
 *
 * This code was contributed by LifeMarks.
 *
 * @author Matt York (matt@lifemarks.mobi)
 */
public final class ZXingUiApplication extends UiApplication {

  private ZXingUiApplication() {
    pushScreen(new ZXingLMMainScreen());
  }

  public static void main(String[] args) {
    AppPermissionsManager.setPermissions();
    DecodeHistory.getInstance();
    AppSettings.getInstance();
    new ZXingUiApplication().enterEventDispatcher();
  }

} 
