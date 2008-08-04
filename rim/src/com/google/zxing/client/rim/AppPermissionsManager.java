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
import net.rim.device.api.applicationcontrol.ApplicationPermissions;
import net.rim.device.api.applicationcontrol.ApplicationPermissionsManager;

/**
 * Requests the necessary permissions for the application.
 *
 * This code was contributed by LifeMarks.
 *
 * @author Matt York (matt@lifemarks.mobi)
 */
final class AppPermissionsManager {

  private static final ApplicationPermissionsManager apm = ApplicationPermissionsManager.getInstance();

  private AppPermissionsManager() {
  }

  /**
   * Requests the required application permissions. Currently the required permissions are
   * event injection (sending system level key strokes to other running applications) and
   * accessing files (accessing the file when a qrcode image is saved to the file system).
   */
  static void setPermissions() {
    setPermission(ApplicationPermissions.PERMISSION_EVENT_INJECTOR);
    setPermission(ApplicationPermissions.PERMISSION_FILE_API);
  }

  private static boolean setPermission(int permission) {
    boolean updatedPermissions = false;
    ApplicationPermissions ap = apm.getApplicationPermissions();
    if (ap.containsPermissionKey(permission)) {
      int eventInjectorPermission = ap.getPermission(permission);
      Log.info("permission (" + permission + "): " + eventInjectorPermission);
      if (eventInjectorPermission != ApplicationPermissions.VALUE_ALLOW) {
        Log.info("Setting permission to VALUE_ALLOW.");
        ap.addPermission(permission);
        updatedPermissions = apm.invokePermissionsRequest(ap);
      }
    } else {
      Log.info("Setting permission (" + permission + ") to VALUE_ALLOW.");
      ap.addPermission(permission);
      updatedPermissions = apm.invokePermissionsRequest(ap);
    }
    Log.info("updatedPermissions: " + updatedPermissions);
    return updatedPermissions;
  }

}
