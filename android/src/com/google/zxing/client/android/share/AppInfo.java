/*
 * Copyright (C) 2013 ZXing authors
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

package com.google.zxing.client.android.share;

import android.graphics.drawable.Drawable;

final class AppInfo implements Comparable<AppInfo> {
  
  private final String packageName;
  private final String label;
  private final Drawable icon;

  AppInfo(String packageName, String label, Drawable icon) {
    this.packageName = packageName;
    this.label = label;
    this.icon = icon;
  }

  String getPackageName() {
    return packageName;
  }

  String getLabel() {
    return label;
  }
  
  Drawable getIcon() {
    return icon;
  }

  @Override
  public String toString() {
    return label;
  }

  @Override
  public int compareTo(AppInfo another) {
    return label.compareTo(another.label);
  }
  
  @Override
  public int hashCode() {
    return label.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof AppInfo)) {
      return false;
    }
    AppInfo another = (AppInfo) other;
    return label.equals(another.label);
  }

}
