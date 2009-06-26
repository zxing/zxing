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

package com.google.zxing.client.bug.app;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.bug.AWTImageLuminanceSource;
import com.google.zxing.client.bug.ImageCanvas;
import com.google.zxing.common.GlobalHistogramBinarizer;

import com.buglabs.bug.module.camera.pub.ICameraDevice;
import com.buglabs.bug.module.camera.pub.ICameraModuleControl;
import com.buglabs.device.ButtonEvent;
import com.buglabs.device.IButtonEventListener;
import com.buglabs.device.IButtonEventProvider;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.IOException;

/**
 * @author David Albert
 */
public final class BugBarcodeApp implements IButtonEventListener, ImageObserver {

  private final ICameraDevice camera;
  private final ICameraModuleControl cameraControl;
  private final Frame frame;
  private Image image;
  private ImageCanvas imageCanvas;
  private Label barcodeLabel;
  private boolean pictureTaken;
  private final Reader reader;

  public BugBarcodeApp(Frame frame,
      ICameraDevice camera,
      ICameraModuleControl cameraControl,
      IButtonEventProvider buttonProvider) {
    this.frame = frame;
    this.camera = camera;
    this.reader = new MultiFormatReader();
    this.cameraControl = cameraControl;
    pictureTaken = false;
    buttonProvider.addListener(this);
    createUI();
  }

  private void createUI() {
    frame.setTitle("BugBarcode");
    frame.setBackground(Color.WHITE);
    frame.setLayout(new BorderLayout());
    barcodeLabel = new Label("Take a picture of a barcode!", Label.CENTER);
    frame.add(barcodeLabel, BorderLayout.SOUTH);
    imageCanvas = new ImageCanvas(null);
    frame.setVisible(true);
  }

  private void shoot() throws IOException {
    // get image from camera for use with physical bug
    cameraControl.setLEDFlash(true);
    image = Toolkit.getDefaultToolkit().createImage(camera.getImage()).getScaledInstance(400, 300,
        Image.SCALE_FAST);
    cameraControl.setLEDFlash(false);
    if (Toolkit.getDefaultToolkit().prepareImage(image, -1, -1, this)) {
      drawAndScan();
    }
  }

  private void drawAndScan() {
    imageCanvas.setImage(image.getScaledInstance(216, 150, Image.SCALE_FAST));
    if (!pictureTaken) {
      frame.add(imageCanvas, BorderLayout.CENTER);
      pictureTaken = true;
      frame.setVisible(true);
    }
    imageCanvas.repaint();
    try {
      LuminanceSource source = new AWTImageLuminanceSource(image);
      BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
      Result result = reader.decode(bitmap);
      barcodeLabel.setText(result.getText());
    } catch (ReaderException re) {
      barcodeLabel.setText("I can't find a barcode here");
    }
  }

  public void buttonEvent(ButtonEvent event) {
    if (event.getButton() == ButtonEvent.BUTTON_HOTKEY_1 && event.getAction() == 0) {
      try {
        shoot();
      } catch (IOException ioe) {
        // continue
      }
    }
  }

  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
    if ((infoflags & ALLBITS) != 0) {
      drawAndScan();
      return false;
    }
    return true;
  }
}
