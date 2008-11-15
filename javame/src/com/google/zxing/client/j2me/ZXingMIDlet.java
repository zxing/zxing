/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.client.j2me;

import com.google.zxing.Result;
import com.google.zxing.client.result.EmailAddressParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.client.result.SMSParsedResult;
import com.google.zxing.client.result.TelParsedResult;
import com.google.zxing.client.result.ProductParsedResult;
import com.google.zxing.client.result.URIParsedResult;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import java.io.IOException;

/**
 * <p>The actual reader application {@link MIDlet}.</p>
 *
 * @author Sean Owen
 */
public final class ZXingMIDlet extends MIDlet {

  private Canvas canvas;
  private Player player;
  private VideoControl videoControl;

  Displayable getCanvas() {
    return canvas;
  }

  Player getPlayer() {
    return player;
  }

  VideoControl getVideoControl() {
    return videoControl;
  }

  protected void startApp() throws MIDletStateChangeException {
    try {
      player = createPlayer();
      player.realize();
      MultimediaManager multimediaManager = new AdvancedMultimediaManager();
      // Comment line above / uncomment below to make the basic version
      //MultimediaManager multimediaManager = new DefaultMultimediaManager();
      multimediaManager.setZoom(player);
      multimediaManager.setExposure(player);
      videoControl = (VideoControl) player.getControl("VideoControl");
      canvas = new VideoCanvas(this);
      canvas.setFullScreenMode(true);
      videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, canvas);
      videoControl.setDisplayLocation(0, 0);
      videoControl.setDisplaySize(canvas.getWidth(), canvas.getHeight());
      videoControl.setVisible(true);
      player.start();
      Display.getDisplay(this).setCurrent(canvas);
    } catch (IOException ioe) {
      throw new MIDletStateChangeException(ioe.toString());
    } catch (MediaException me) {
      throw new MIDletStateChangeException(me.toString());
    }
  }

  private static Player createPlayer() throws IOException, MediaException {
    // Try a workaround for Nokias, which want to use capture://image in some cases
    Player player = null;
    String platform = System.getProperty("microedition.platform");
    if (platform != null && platform.indexOf("Nokia") >= 0) {
      try {
        player = Manager.createPlayer("capture://image");
      } catch (MediaException me) {
        // if this fails, just continue with capture://video
      } catch (Error e) {
        // Ugly, but, it seems the Nokia N70 throws "java.lang.Error: 136" here
        // We should still try to continue
      }
    }
    if (player == null) {
      player = Manager.createPlayer("capture://video");
    }
    return player;
  }

  protected void pauseApp() {
    if (player != null) {
      try {
        player.stop();
      } catch (MediaException me) {
        // continue?
        showError(me);
      }
    }
  }

  protected void destroyApp(boolean unconditional) {
    if (player != null) {
      videoControl = null;
      try {
        player.stop();
      } catch (MediaException me) {
        // continue
      }
      player.deallocate();
      player.close();
      player = null;
    }
  }

  void stop() {
    destroyApp(false);
    notifyDestroyed();
  }

  // Convenience methods to show dialogs

  private void showOpenURL(String title, String display, final String uri) {
    Alert alert = new Alert(title, display, null, AlertType.CONFIRMATION);
    alert.setTimeout(Alert.FOREVER);
    Command yes = new Command("Yes", Command.OK, 1);
    alert.addCommand(yes);
    Command no = new Command("No", Command.CANCEL, 1);
    alert.addCommand(no);
    CommandListener listener = new CommandListener() {
      public void commandAction(Command command, Displayable displayable) {
        if (command.getCommandType() == Command.OK) {
          try {
            platformRequest(uri);
          } catch (ConnectionNotFoundException cnfe) {
            showError(cnfe);
          } finally {
            stop();
          }
        } else {
          // cancel
          Display.getDisplay(ZXingMIDlet.this).setCurrent(getCanvas());
        }
      }
    };
    alert.setCommandListener(listener);
    showAlert(alert);
  }

  private void showAlert(String title, String text) {
    Alert alert = new Alert(title, text, null, AlertType.INFO);
    alert.setTimeout(Alert.FOREVER);
    showAlert(alert);
  }

  void showError(Throwable t) {
    String message = t.getMessage();
    if (message != null && message.length() > 0) {
      showError(message);
    } else {
      showError(t.toString());
    }
  }

  void showError(String message) {
    showAlert(new Alert("Error", message, null, AlertType.ERROR));
  }

  private void showAlert(Alert alert) {
    Display display = Display.getDisplay(this);
    display.setCurrent(alert, canvas);
  }

  void handleDecodedText(Result theResult) {
    ParsedResult result = ResultParser.parseResult(theResult);
    ParsedResultType type = result.getType();
    if (type.equals(ParsedResultType.URI)) {
      String uri = ((URIParsedResult) result).getURI();
      showOpenURL("Open Web Page?", uri, uri);
    } else if (type.equals(ParsedResultType.EMAIL_ADDRESS)) {
      EmailAddressParsedResult emailResult = (EmailAddressParsedResult) result;
      showOpenURL("Compose E-mail?", emailResult.getEmailAddress(), emailResult.getMailtoURI());
    } else if (type.equals(ParsedResultType.SMS)) {
      SMSParsedResult smsResult = (SMSParsedResult) result;
      showOpenURL("Compose SMS?", smsResult.getNumber(), smsResult.getSMSURI());
    } else if (type.equals(ParsedResultType.PRODUCT)) {
      ProductParsedResult productResult = (ProductParsedResult) result;
      String uri = "http://www.upcdatabase.com/item.asp?upc=" + productResult.getNormalizedProductID();
      showOpenURL("Look Up Barcode Online?", productResult.getProductID(), uri);
    } else if (type.equals(ParsedResultType.TEL)) {
      TelParsedResult telResult = (TelParsedResult) result;
      showOpenURL("Dial Number?", telResult.getNumber(), telResult.getTelURI());
    } else {
      showAlert("Barcode Detected", result.getDisplayResult());
    }
  }

}
