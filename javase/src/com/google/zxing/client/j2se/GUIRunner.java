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

package com.google.zxing.client.j2se;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * <p>Simple GUI frontend to the library. Right now, only decodes a local file.
 * This definitely needs some improvement. Just throwing something down to start.</p>
 *
 * @author Sean Owen
 */
public final class GUIRunner extends JFrame {

  private final JLabel imageLabel;
  private final JTextArea textArea;

  private GUIRunner() {
    imageLabel = new JLabel();
    textArea = new JTextArea();
    textArea.setEditable(false);
    textArea.setMaximumSize(new Dimension(400, 200));
    Container panel = new JPanel();
    panel.setLayout(new FlowLayout());
    panel.add(imageLabel);
    panel.add(textArea);
    setTitle("ZXing");
    setSize(400, 400);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setContentPane(panel);
    setLocationRelativeTo(null);
  }

  public static void main(String[] args) throws MalformedURLException {
    GUIRunner runner = new GUIRunner();
    runner.setVisible(true);
    runner.chooseImage();
  }

  private void chooseImage() throws MalformedURLException {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.showOpenDialog(this);
    File file = fileChooser.getSelectedFile();
    Icon imageIcon = new ImageIcon(file.toURI().toURL());
    setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight() + 100);
    imageLabel.setIcon(imageIcon);
    String decodeText = getDecodeText(file);
    textArea.setText(decodeText);
  }

  private static String getDecodeText(File file) {
    BufferedImage image;
    try {
      image = ImageIO.read(file);
    } catch (IOException ioe) {
      return ioe.toString();
    }
    if (image == null) {
      return "Could not decode image";
    }
    LuminanceSource source = new BufferedImageLuminanceSource(image);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    Result result;
    try {
      result = new MultiFormatReader().decode(bitmap);
    } catch (ReaderException re) {
      return re.toString();
    }
    return result.getText();
  }

}
