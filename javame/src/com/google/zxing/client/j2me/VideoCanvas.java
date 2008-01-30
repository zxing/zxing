/*
 * Copyright 2007 Google Inc.
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

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;

/**
 * @author Sean Owen (srowen@google.com)
 */
final class VideoCanvas extends Canvas implements CommandListener {

  private static final Command exit = new Command("Exit", Command.EXIT, 1);

  private final ZXingMIDlet zXingMIDlet;

  VideoCanvas(ZXingMIDlet zXingMIDlet) {
    this.zXingMIDlet = zXingMIDlet;
    addCommand(exit);
    setCommandListener(this);
  }

  protected void paint(Graphics graphics) {
    // do nothing
  }

  protected void keyPressed(int keyCode) {
    // Any valid game key will trigger a capture
    if (getGameAction(keyCode) != 0) {
      SnapshotThread.startThread(zXingMIDlet);
    }
  }

  public void commandAction(Command command, Displayable displayable) {
    if (command.getCommandType() == Command.EXIT || command.getCommandType() == Command.STOP) {
      zXingMIDlet.stop();
    }
  }
}
