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

package com.google.zxing.client.rim.persistence.history;

import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;

import java.util.Vector;

/**
 * Singleton used to persist the history of qrcode URLs decoded by the client.
 *
 * This code was contributed by LifeMarks.
 *
 * @author Matt York (matt@lifemarks.mobi)
 */
public class DecodeHistory {

  private static final long ID_LONG = 0xb7cc76147b48ad0dL;

  private static DecodeHistory instance;

  private final PersistentObject store;
  private final Vector historyItems;

  private DecodeHistory() {
    store = PersistentStore.getPersistentObject(ID_LONG);
    Vector temp = (Vector) store.getContents();
    historyItems = temp == null ? new Vector() : temp;
  }

  /**
   * Returns the single instance of this class.
   */
  public static DecodeHistory getInstance() {
    if (instance == null) {
      instance = new DecodeHistory();
    }
    return instance;
  }

  /**
   * Adds a history object.
   */
  public void addHistoryItem(DecodeHistoryItem item) {
    historyItems.addElement(item);
  }

  /**
   * Returns all history objects.
   */
  public Vector getItems() {
    return historyItems;
  }

  /**
   * Gets a particular history object at a given index.
   */
  public DecodeHistoryItem getItemAt(int index) {
    return (DecodeHistoryItem) historyItems.elementAt(index);
  }

  /**
   * Returns the number of history objects.
   */
  public int getNumItems() {
    return historyItems.size();
  }

  /**
   * Clears the history.
   */
  public void clear() {
    historyItems.setSize(0);
  }

  /**
   * Persists the history to the device.
   */
  public void persist() {
    synchronized (store) {
      store.setContents(historyItems);
      store.commit();
    }
  }

}
