/*
 * Copyright 2009 ZXing authors
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

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

/**
 * The Menu form simply adds Command Listener functionality
 * to the standard List User Interface component.
 *
 * @author Simon Flannery (simon.flannery@gmail.com)
 */
final class Menu extends List implements CommandListener {

  private final ZXingMIDlet zXingMIDlet;
  private final Command cancelCommand;
  private final Command barcodeCommand;

  /**
   * Creates a new Search List and initialises all components.
   *
   * @param parent The Parent ZXing MIDlet.
   * @param title The title of the List.
   * @param item The caption of the item action Command.
   */
  Menu(ZXingMIDlet parent, String title, String item) {
    super(title, IMPLICIT); // Set the title of the form
    zXingMIDlet = parent;
    // Build the UI components
    cancelCommand  = new Command("Cancel", Command.CANCEL, 0);
    barcodeCommand = new Command(item, Command.ITEM, 0);
    addCommand(cancelCommand);
    addCommand(barcodeCommand);
    setCommandListener(this);
  }

  /**
   * A convenience method for getting the selected option item.
   *
   * @return The selected option represented as a String. If no option is
   *         selected, then the empty string is returned.
   */
  public String getSelectedString() {
    String result = "";
    if (getSelectedIndex() != -1) {
      result = getString(getSelectedIndex());
    }
    return result;
  }

  /**
   * A convenience method for removing all items from the list.
   * While the size of the list does not equal zero, the first item of the list is deleted.
   */
  public void clear() {
    while (size() != 0) { // Delete the first-most element until there is no first-most element
      delete(0);
    }
  }

  /**
   * CommandListener Required Implementation for capturing soft key presses.
   * This is where all call back methods (of the MIDlet) are serviced.
   *
   * @param command The command requiring attention.
   * @param displayable The Display.
   */
  public void commandAction(Command command, Displayable displayable) {
    if (command == cancelCommand) { /* Detecting the soft key press. */
      Display.getDisplay(zXingMIDlet).setCurrent(zXingMIDlet.getCanvas());
    } else if (command == barcodeCommand || command == SELECT_COMMAND) {
      if (getSelectedIndex() != -1) {
        zXingMIDlet.itemRequest();
      }
    }
  }

}
