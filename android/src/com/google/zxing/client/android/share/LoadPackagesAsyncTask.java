/*
 * Copyright (C) 2009 ZXing authors
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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Loads a list of packages installed on the device asynchronously.
 *
 * @author Sean Owen
 */
final class LoadPackagesAsyncTask extends AsyncTask<List<String[]>,Void,List<String[]>> {

  private static final String[] PKG_PREFIX_WHITELIST = {
      "com.google.android.apps.",
  };
  private static final String[] PKG_PREFIX_BLACKLIST = {
      "com.android.",
      "android",
      "com.google.android.",
      "com.htc",
  };


  private final AppPickerActivity appPickerActivity;

  LoadPackagesAsyncTask(AppPickerActivity appPickerActivity) {
    this.appPickerActivity = appPickerActivity;
  }

  @Override
  protected List<String[]> doInBackground(List<String[]>... objects) {
    List<String[]> labelsPackages = objects[0];
    PackageManager packageManager = appPickerActivity.getPackageManager();
    List<ApplicationInfo> appInfos = packageManager.getInstalledApplications(0);
    for (ApplicationInfo appInfo : appInfos) {
      CharSequence label = appInfo.loadLabel(packageManager);
      if (label != null) {
        String packageName = appInfo.packageName;
        if (!isHidden(packageName)) {
          labelsPackages.add(new String[]{label.toString(), packageName});
        }
      }
    }
    Collections.sort(labelsPackages, new ByFirstStringComparator());
    return labelsPackages;
  }

  private static boolean isHidden(String packageName) {
    if (packageName == null) {
      return true;
    }
    for (String prefix : PKG_PREFIX_WHITELIST) {
      if (packageName.startsWith(prefix)) {
        return false;
      }
    }
    for (String prefix : PKG_PREFIX_BLACKLIST) {
      if (packageName.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void onPostExecute(List<String[]> results) {
    List<String> labels = new ArrayList<String>(results.size());
    for (String[] result : results) {
      labels.add(result[0]);
    }
    ListAdapter listAdapter = new ArrayAdapter<String>(
        appPickerActivity, android.R.layout.simple_list_item_1, labels);
    appPickerActivity.setListAdapter(listAdapter);
    appPickerActivity.getProgressDialog().dismiss();
  }

  private static class ByFirstStringComparator implements Comparator<String[]>, Serializable {
    public int compare(String[] o1, String[] o2) {
      return o1[0].compareTo(o2[0]);
    }
  }

}
