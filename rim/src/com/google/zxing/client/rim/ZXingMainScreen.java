/*
 * Copyright 2008 Google Inc.
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

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2me.LCDUIImageMonochromeBitmapSource;
import net.rim.blackberry.api.invoke.CameraArguments;
import net.rim.blackberry.api.invoke.Invoke;
import net.rim.device.api.system.Characters;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.MainScreen;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Image;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Sean Owen (srowen@google.com)
 */
final class ZXingMainScreen extends MainScreen {

  private final ZXingUIApp app;
  private final ImageCapturedJournalListener captureListener;

  ZXingMainScreen() {
    setTitle("Barcode Reader");
    add(new LabelField("ZXing"));
    app = (ZXingUIApp) UiApplication.getUiApplication();
    captureListener = new ImageCapturedJournalListener(this);
    app.addFileSystemJournalListener(captureListener);
  }

  public boolean keyChar(char c, int status, int time) {
    if (c == Characters.ENTER) {
      Invoke.invokeApplication(Invoke.APP_TYPE_CAMERA, new CameraArguments());
      return true;
    } else {
      return super.keyChar(c, status, time);
    }
  }

  public void close() {
    app.removeFileSystemJournalListener(captureListener);
    super.close();
  }

  private void showMessage(String msg) {
    synchronized (app.getAppEventLock()) {
      Dialog.alert(msg);
    }
  }

  void handleFile(String path) {
    if (path.endsWith(".jpg") && path.indexOf("IMG") >= 0) {
      app.requestForeground();
      try {
        FileConnection file = null;
        InputStream is = null;
        Image capturedImage;
        try {
          file = (FileConnection) Connector.open("file://" + path);
          is = file.openInputStream();
          capturedImage = Image.createImage(is);
        } finally {
          if (is != null) {
            try {
              is.close();
            } catch (IOException ioe ) {
              // continue
            }
          }
          if (file != null) {
            try {
              file.close();
            } catch (IOException ioe ) {
              // continue
            }
          }
        }
        MonochromeBitmapSource source = new LCDUIImageMonochromeBitmapSource(capturedImage);
        Reader reader = new MultiFormatReader();
        Result result = reader.decode(source);
        try {
          file.delete();
        } catch (IOException ioe) {
          // continue
        }
        showMessage(result.getText());
      } catch (IOException ioe) {
        showMessage(ioe.getMessage());
      } catch (ReaderException re) {
        showMessage("Sorry, no barcode was found.");
      }
    }
  }

}
