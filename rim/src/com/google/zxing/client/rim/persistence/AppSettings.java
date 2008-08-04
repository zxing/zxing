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

package com.google.zxing.client.rim.persistence;

import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;

import java.util.Hashtable;

/**
 * Singleton object that represents the persistent application settings data.
 *
 * This code was contributed by LifeMarks.
 * 
 * @author Matt York (matt@lifemarks.mobi)
 */
public final class AppSettings {

  public static final String SETTING_CAM_RES_MSG = "setting_cam_res_msg";
  private static final long ID_LONG = 0x92ac4e8ac35b8aa0L;

  private static AppSettings instance;

  private final PersistentObject store;
  private final Hashtable settingsItems;

  private AppSettings() {
    store = PersistentStore.getPersistentObject(ID_LONG);
    Hashtable temp = (Hashtable) store.getContents();
    settingsItems = temp == null ? new Hashtable() : temp;
  }

  public static AppSettings getInstance() {
    if (instance == null) {
      instance = new AppSettings();
    }
    return instance;
  }

  /**
   * Adds a setting object.
   */
  public void addItem(String itemName, Object itemValue) {
    settingsItems.put(itemName, itemValue);
  }

  /**
   * Returns all settings objects.
   */
  public Hashtable getItems() {
    return settingsItems;
  }

  /**
   * Gets a particular settings object by name.
   */
  Object getItem(String itemName) {
    return settingsItems.get(itemName);
  }

  /**
   * Gets a particular boolean type settings object by name.
   */
  public Boolean getBooleanItem(String itemName) {
    Object value = getItem(itemName);
    return value instanceof Boolean ? (Boolean) value : Boolean.FALSE;
  }

  /**
   * Returns the number of settings.
   */
  public int getNumItems() {
    return settingsItems.size();
  }

  /**
   * Persists the settings to the device.
   */
  public void persist() {
    synchronized (store) {
      store.setContents(settingsItems);
      store.commit();
    }
  }

}
