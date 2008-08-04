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

import com.google.zxing.client.rim.util.Log;
import net.rim.blackberry.api.invoke.CameraArguments;
import net.rim.blackberry.api.invoke.Invoke;
import net.rim.device.api.system.Characters;
import net.rim.device.api.system.EventInjector;
import net.rim.device.api.ui.UiApplication;

/**
 * Singleton used to control access to the camera.
 * Unfortunatly, the Camera API only allows invoking the camera.
 * 
 * Note: This code still contains experimental code to determine and set the camera resolution by
 * using system level key events, but didn't not function reliably and is not used.
 *
 * This code was contributed by LifeMarks.
 *
 * @author Matt York (matt@lifemarks.mobi)
 */
final class Camera {

  /** milliseconds to wait before starting key strokes */
  private static final int INITIALIZATION_TIME_MS = 500; // simulator seems to need >= 500
  private static final int KEY_PAUSE_TIME_MS = 100; // simulator seems to need >= 100

  private static Camera instance;

  /** Attempting to set camera resolution is disabled. */
  private final boolean setResolution = false;

  private Camera() {
  }

  /**
   * Returns the single instance of the camera.
   */
  static Camera getInstance() {
    if (instance == null) {
      instance = new Camera();
    }
    return instance;
  }

  /**
   * Starts the blackberry camera application.
   */
  void invoke() {
    Invoke.invokeApplication(Invoke.APP_TYPE_CAMERA, new CameraArguments());
    if (setResolution) {
      sleep(INITIALIZATION_TIME_MS);
      setMinResolution();
    }
  }

  /**
   * Exits the blackberry camera application.
   */
  void exit() {
    if (setResolution) {
      setMaxResolution(); // for now, we dont know the original resolution setting. Assume it was max res.
      sleep(KEY_PAUSE_TIME_MS);
    }
    sleep(3000); // this sleep is needed for the esc to be processed(3000 originally)
    UiApplication app = UiApplication.getUiApplication();
    if (app != null) {
      Log.info("active app: " + app.getClass().getName());
      if (app.isForeground()) {
        Log.info("Lifemarks is the foreground app.");
      } else {
        Log.info("Lifemarks is not the foreground app. Attempt to close camera.");
        keyUpAndDown(Characters.ESCAPE); // need two (no timeout in between esc key presses seems to work best)
        keyUpAndDown(Characters.ESCAPE);
      }
    } else {
      Log.error("??? app is null ???");
    }
  }

  /**
   * Sets the camera resolution to it's minimum.
   * Note: currently disabled.
   */
  private static void setMaxResolution() {
    Log.info("Setting resolution to max.");
    accessResolutionMenuAfterSave();
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_DOWN);
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_DOWN); // min res
    sleep(KEY_PAUSE_TIME_MS);
    trackBallClick(); // out of res menu
    sleep(KEY_PAUSE_TIME_MS);
    trackBallClick(); // into res menu
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_UP);
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_UP); // max res
    sleep(KEY_PAUSE_TIME_MS);
    trackBallClick(); // out of res menu
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.ESCAPE); // out of options
    sleep(KEY_PAUSE_TIME_MS);
    trackBallClick(); // yes to changes, even if there werent really any!
    Log.info("Finished setting resolution to max.");
  }

  /**
   * Sets the camera resolution to it's maximum.
   * Note: currently disabled.
   */
  private static void setMinResolution() {
    Log.info("Setting resolution to min.");
    accessResolutionMenu();
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_UP);
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_UP); // max res
    sleep(KEY_PAUSE_TIME_MS);
    trackBallClick(); // out of res menu
    sleep(KEY_PAUSE_TIME_MS);
    trackBallClick(); // into res menu
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_DOWN);
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_DOWN); // min res
    sleep(KEY_PAUSE_TIME_MS);
    trackBallClick(); // out of res menu
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.ESCAPE); // out of options
    trackBallClick(); // yes to changes, even if there werent really any!
  }

  private static void accessResolutionMenu() {
    keyUpAndDown(Characters.CONTROL_MENU);
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_DOWN);
    sleep(KEY_PAUSE_TIME_MS);
    trackBallClick();
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_DOWN);
    sleep(KEY_PAUSE_TIME_MS);
    keyUpAndDown(Characters.CONTROL_DOWN);
    sleep(KEY_PAUSE_TIME_MS);
    trackBallClick();
  }

  private static void accessResolutionMenuAfterSave() {
    keyUpAndDown(Characters.CONTROL_MENU);
    keyUpAndDown(Characters.CONTROL_DOWN, 6, 0); // seems to be down 6 items on bb and 4 on simulator
    trackBallClick();
    keyUpAndDown(Characters.CONTROL_DOWN);
    keyUpAndDown(Characters.CONTROL_DOWN);
    trackBallClick();
  }

  /**
   * Puts the current thread to sleep for a given amount of time.
   */
  private static void sleep(int time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException ie) {
      // continue
    }
  }


  private static void trackBallClick() {
    EventInjector.invokeEvent(
            new EventInjector.NavigationEvent(EventInjector.NavigationEvent.NAVIGATION_CLICK, 0, 0, 1));
    EventInjector.invokeEvent(
            new EventInjector.NavigationEvent(EventInjector.NavigationEvent.NAVIGATION_UNCLICK, 0, 0, 1));
  }

  /**
   * Sends system level key events a given number of times with the given delay between them.
   */
  private static void keyUpAndDown(char character, int times, int delay) {
    for (int i = 0; i < times; i++) {
      keyUpAndDown(character);
      if (delay > 0) {
        sleep(delay);
      }
    }
  }

  /**
   * Sends one system level key event.
   */
  private static void keyUpAndDown(char character) {
    EventInjector.invokeEvent(new EventInjector.KeyEvent(EventInjector.KeyEvent.KEY_DOWN, character, 0, 1));
    EventInjector.invokeEvent(new EventInjector.KeyEvent(EventInjector.KeyEvent.KEY_UP, character, 0, 1));
  }

}

