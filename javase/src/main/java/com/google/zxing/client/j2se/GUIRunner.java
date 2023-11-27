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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;

/**
 * <p>Simple GUI frontend to the library. Right now, only decodes a local file.
 * This definitely needs some improvement. Just throwing something down to start.</p>
 *
 * @author Sean Owen
 */

public final class GUIRunner extends JFrame {

  private final GUIManager guiManager;
  private final Decoder decoder;

  private GUIRunner() {
    guiManager = new GUIManager();
    decoder = new Decoder();
    setTitle("ZXing");
    setSize(400, 400);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setContentPane(guiManager.getPanel());
    setLocationRelativeTo(null);
  }

  public static void main(String[] args) throws MalformedURLException {
    SwingUtilities.invokeLater(() -> {
      GUIRunner runner = new GUIRunner();
      runner.setVisible(true);
      runner.chooseImage();
    });
  }

  private void chooseImage() {
    Path file = guiManager.showOpenDialog();
    Icon imageIcon = guiManager.createImageIcon(file);
    setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight() + 100);
    guiManager.setImageIcon(imageIcon);
    String decodeText = decoder.getDecodeText(file);
    guiManager.setDecodeText(decodeText);
  }

}

class GUIManager {

  private final JLabel imageLabel;
  private final JTextComponent textArea;
  private final JPanel panel;

  public GUIManager() {
    imageLabel = new JLabel();
    textArea = new JTextArea();
    textArea.setEditable(false);
    textArea.setMaximumSize(new Dimension(400, 200));
    panel = new JPanel();
    panel.setLayout(new FlowLayout());
    panel.add(imageLabel);
    panel.add(textArea);
  }

  public JPanel getPanel() {
    return panel;
  }

  public Path showOpenDialog() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.showOpenDialog(null);
    return fileChooser.getSelectedFile().toPath();
  }

  public Icon createImageIcon(Path file) {
    try {
      return new ImageIcon(file.toUri().toURL());
    } catch (MalformedURLException muee) {
      throw new IllegalArgumentException(muee);
    }
  }

  public void setImageIcon(Icon imageIcon) {
    imageLabel.setIcon(imageIcon);
  }

  public void setDecodeText(String decodeText) {
    textArea.setText(decodeText);
  }
}

class Decoder {

  public String getDecodeText(Path file) {
    BufferedImage image;
    try {
      image = ImageReader.readImage(file.toUri());
    } catch (IOException ioe) {
      return ioe.toString();
    }
    LuminanceSource source = new BufferedImageLuminanceSource(image);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    Result result;
    try {
      result = new MultiFormatReader().decode(bitmap);
    } catch (ReaderException re) {
      return re.toString();
    }
    return String.valueOf(result.getText());
  }
}
