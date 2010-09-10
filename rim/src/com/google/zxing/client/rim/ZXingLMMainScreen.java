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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.client.j2me.LCDUIImageLuminanceSource;
import com.google.zxing.client.rim.persistence.AppSettings;
import com.google.zxing.client.rim.persistence.history.DecodeHistory;
import com.google.zxing.client.rim.persistence.history.DecodeHistoryItem;
import com.google.zxing.client.rim.util.Log;
import com.google.zxing.client.rim.util.ReasonableTimer;
import com.google.zxing.client.rim.util.URLDecoder;
import net.rim.blackberry.api.browser.Browser;
import net.rim.blackberry.api.browser.BrowserSession;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.DialogFieldManager;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * The main appication menu screen.
 *
 * This code was contributed by LifeMarks.
 *
 * @author Matt York (matt@lifemarks.mobi)
 */
final class ZXingLMMainScreen extends MainScreen {

  private final ZXingUiApplication app;
  private final QRCapturedJournalListener imageListener;
  private PopupScreen popup;
  private final Reader reader;
  private final Hashtable readerHints;

  ZXingLMMainScreen() {
    super(DEFAULT_MENU | DEFAULT_CLOSE);
    setTitle(new LabelField("ZXing", DrawStyle.ELLIPSIS | USE_ALL_WIDTH));
    setChangeListener(null);

    Manager vfm = new VerticalFieldManager(USE_ALL_WIDTH);
    FieldChangeListener buttonListener = new ButtonListener();

    //0
    Field snapButton = new ButtonField("Snap", FIELD_HCENTER | ButtonField.CONSUME_CLICK | USE_ALL_WIDTH);
    snapButton.setChangeListener(buttonListener);
    vfm.add(snapButton);

    //1
    Field historyButton = new ButtonField("History", FIELD_HCENTER | ButtonField.CONSUME_CLICK);
    historyButton.setChangeListener(buttonListener);
    vfm.add(historyButton);

    //2
    Field settingsButton = new ButtonField("Settings", FIELD_HCENTER | ButtonField.CONSUME_CLICK);
    settingsButton.setChangeListener(buttonListener);
    vfm.add(settingsButton);

    //3
    Field aboutButton = new ButtonField("About", FIELD_HCENTER | ButtonField.CONSUME_CLICK);
    aboutButton.setChangeListener(buttonListener);
    vfm.add(aboutButton);

    //4
    Field helpButton = new ButtonField("Help", FIELD_HCENTER | ButtonField.CONSUME_CLICK);
    helpButton.setChangeListener(buttonListener);
    vfm.add(helpButton);

    vfm.setChangeListener(null);
    add(vfm);


    app = (ZXingUiApplication) UiApplication.getUiApplication();
    imageListener = new QRCapturedJournalListener(this);

    reader = new MultiFormatReader();
    readerHints = new Hashtable(1);
    readerHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
  }


  /**
   * Handles the newly created file. If the file is a jpg image, from the camera, the images is assumed to be
   * a qrcode and decoding is attempted.
   */
  void imageSaved(String imagePath) {
    Log.info("Image saved: " + imagePath);
    app.removeFileSystemJournalListener(imageListener);
    if (imagePath.endsWith(".jpg") && imagePath.indexOf("IMG") >= 0) // a blackberry camera image file
    {
      Log.info("imageSaved - Got file: " + imagePath);
      Camera.getInstance().exit();
      Log.info("camera exit finished");
      app.requestForeground();

      DialogFieldManager manager = new DialogFieldManager();
      popup = new PopupScreen(manager);
      manager.addCustomField(new LabelField("Decoding image..."));

      app.pushScreen(popup); // original
      Log.info("started progress screen.");

      Runnable fct = new FileConnectionThread(imagePath);
      Log.info("Starting file connection thread.");
      app.invokeLater(fct);
      Log.info("Finished file connection thread.");
    } else {
      Log.error("Failed to locate camera image.");
    }
  }

  /**
   * Closes the application and persists all required data.
   */
  public void close() {
    app.removeFileSystemJournalListener(imageListener);
    DecodeHistory.getInstance().persist();
    super.close();
  }

  /**
   * This method is overriden to remove the 'save changes' dialog when exiting.
   */
  public boolean onSavePrompt() {
    setDirty(false);
    return true;
  }

  /**
   * Listens for selected buttons and starts the required screen.
   */
  private final class ButtonListener implements FieldChangeListener {
    public void fieldChanged(Field field, int context) {
      Log.debug("*** fieldChanged: " + field.getIndex());
      switch (field.getIndex()) {
        case 0: // snap
          try {
            app.addFileSystemJournalListener(imageListener);
            Camera.getInstance().invoke(); // start camera
            return;
          }
          catch (Exception e) {
            Log.error("!!! Problem invoking camera.!!!: " + e);
          }
          break;
        case 1: // history
          app.pushScreen(new HistoryScreen());
          break;
        case 2: // settings
          app.pushScreen(new SettingsScreen());
          break;
        case 3: //about
          app.pushScreen(new AboutScreen());
          break;
        case 4: //help
          app.pushScreen(new HelpScreen());
          break;
      }
    }

  }

  /**
   * Thread that decodes the newly created image. If the image is successfully decoded and the data is a URL,
   * the browser is invoked and pointed to the given URL.
   */
  private final class FileConnectionThread implements Runnable {

    private final String imagePath;

    private FileConnectionThread(String imagePath) {
      this.imagePath = imagePath;
    }

    public void run() {
      FileConnection file = null;
      InputStream is = null;
      Image capturedImage = null;
      try {
        file = (FileConnection) Connector.open("file://" + imagePath, Connector.READ_WRITE);
        is = file.openInputStream();
        capturedImage = Image.createImage(is);
      } catch (IOException e) {
        Log.error("Problem creating image: " + e);
        removeProgressBar();
        invalidate();
        showMessage("An error occured processing the image.");
        return;
      } finally {
        try {
          if (is != null) {
            is.close();
          }
          if (file != null && file.exists()) {
            if (file.isOpen()) {
              file.delete();              
              file.close();
            }
            Log.info("Deleted image file.");
          }
        } catch (IOException ioe) {
          Log.error("Error while closing file: " + ioe);
        }
      }

      if (capturedImage != null) {
        Log.info("Got image...");
        LuminanceSource source = new LCDUIImageLuminanceSource(capturedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
        Result result;
        ReasonableTimer decodingTimer = null;
        try {
          decodingTimer = new ReasonableTimer();
          Log.info("Attempting to decode image...");
          result = reader.decode(bitmap, readerHints);
          decodingTimer.finished();
        } catch (ReaderException e) {
          Log.error("Could not decode image: " + e);
          decodingTimer.finished();
          removeProgressBar();
          invalidate();
          boolean showResolutionMsg =
                  !AppSettings.getInstance().getBooleanItem(AppSettings.SETTING_CAM_RES_MSG).booleanValue();
          if (showResolutionMsg) {
            showMessage("A QR Code was not found in the image. " +
                        "We detected that the decoding process took quite a while. " +
                        "It will be much faster if you decrease your camera's resolution (640x480).");
          } else {
            showMessage("A QR Code was not found in the image.");
          }
          return;
        }
        if (result != null) {
          String resultText = result.getText();
          Log.info("result: " + resultText);
          if (isURI(resultText)) {
            resultText = URLDecoder.decode(resultText);
            removeProgressBar();
            invalidate();
            if (!decodingTimer.wasResonableTime() &&
                !AppSettings.getInstance().getBooleanItem(AppSettings.SETTING_CAM_RES_MSG).booleanValue()) {
              showMessage("We detected that the decoding process took quite a while. " +
                          "It will be much faster if you decrease your camera's resolution (640x480).");
            }
            DecodeHistory.getInstance().addHistoryItem(new DecodeHistoryItem(resultText));
            invokeBrowser(resultText);
            return;
          }
        } else {
          removeProgressBar();
          invalidate();
          showMessage("A QR Code was not found in the image.");
          return;
        }

      }

      removeProgressBar();
      invalidate();
    }

    /**
     * Quick check to see if the result of decoding the qr code was a valid uri.
     */
    private boolean isURI(String uri) {
      return uri.startsWith("http://");
    }

    /**
     * Invokes the web browser and browses to the given uri.
     */
    private void invokeBrowser(String uri) {
      BrowserSession browserSession = Browser.getDefaultSession();
      browserSession.displayPage(uri);
    }

    /**
     * Syncronized version of removing progress dialog.
     * NOTE: All methods accessing the gui that are in seperate threads should syncronize on app.getEventLock()
     */
    private void removeProgressBar() {
      synchronized (app.getAppEventLock()) {
        if (popup != null) {
          app.popScreen(popup);
        }
      }
    }

    /**
     * Syncronized version of showing a message dialog.
     * NOTE: All methods accessing the gui that are in seperate threads should syncronize on app.getEventLock()
     */
    private void showMessage(String message) {
      synchronized (app.getAppEventLock()) {
        Dialog.alert(message);
      }
    }
  }

}
