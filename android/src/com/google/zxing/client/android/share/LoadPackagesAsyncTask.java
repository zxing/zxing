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

import android.app.ListActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import com.google.zxing.client.android.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads a list of packages installed on the device asynchronously.
 *
 * @author Sean Owen
 */
final class LoadPackagesAsyncTask extends AsyncTask<Object,Object,List<AppInfo>> {

  private static final String[] PKG_PREFIX_SAFELIST = {
      "com.google.android.apps.",
  };
  private static final String[] PKG_PREFIX_BLOCKLIST = {
      "com.android.",
      "android",
      "com.google.android.",
      "com.htc",
  };

  private final ListActivity activity;

  LoadPackagesAsyncTask(ListActivity activity) {
    this.activity = activity;
  }

  @Override
  protected List<AppInfo> doInBackground(Object... objects) {
    List<AppInfo> labelsPackages = new ArrayList<>();
    PackageManager packageManager = activity.getPackageManager();
    Iterable<ApplicationInfo> appInfos = packageManager.getInstalledApplications(0);
    for (PackageItemInfo appInfo : appInfos) {
      String packageName = appInfo.packageName;
      if (!isHidden(packageName)) {
        CharSequence label = appInfo.loadLabel(packageManager);
        Drawable icon = appInfo.loadIcon(packageManager);
        if (label != null) {
          labelsPackages.add(new AppInfo(packageName, label.toString(), icon));
        }
      }
    }
    Collections.sort(labelsPackages);
    return labelsPackages;
  }

  private static boolean isHidden(String packageName) {
    if (packageName == null) {
      return true;
    }
    for (String prefix : PKG_PREFIX_SAFELIST) {
      if (packageName.startsWith(prefix)) {
        return false;
      }
    }
    for (String prefix : PKG_PREFIX_BLOCKLIST) {
      if (packageName.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void onPostExecute(final List<AppInfo> results) {
    ListAdapter listAdapter = new ArrayAdapter<AppInfo>(activity,
                                                        R.layout.app_picker_list_item,
                                                        R.id.app_picker_list_item_label,
                                                        results) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        Drawable icon = results.get(position).getIcon();
        if (icon != null) {
          ((ImageView) view.findViewById(R.id.app_picker_list_item_icon)).setImageDrawable(icon);
        }
        return view;
      }
    };
    activity.setListAdapter(listAdapter);
  }

}
