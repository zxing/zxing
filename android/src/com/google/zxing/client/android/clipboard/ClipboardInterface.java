/*
 * Copyright (C) 2012 ZXing authors
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

package com.google.zxing.client.android.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

public final class ClipboardInterface {
  
  private static final String TAG = ClipboardInterface.class.getSimpleName();

  private ClipboardInterface() {
  }

  public static CharSequence getText(Context context) {
    ClipboardManager clipboard = getManager(context);
    ClipData clip = clipboard.getPrimaryClip();
    return hasText(context) ? clip.getItemAt(0).coerceToText(context) : null;
  }

  public static void setText(CharSequence text, Context context) {
    if (text != null) {
      try {
        getManager(context).setPrimaryClip(ClipData.newPlainText(null, text));
      } catch (NullPointerException | IllegalStateException e) {
        // Have seen this in the wild, bizarrely
        Log.w(TAG, "Clipboard bug", e);
      }
    }
  }

  public static boolean hasText(Context context) {
    ClipboardManager clipboard = getManager(context);
    ClipData clip = clipboard.getPrimaryClip();
    return clip != null && clip.getItemCount() > 0;
  }
  
  private static ClipboardManager getManager(Context context) {
    return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
  }

}
