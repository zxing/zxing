/*
 * Copyright 2012 ZXing authors
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
package com.google.zxing.integration.android;

import android.content.Intent;
import android.support.v4.app.Fragment;

/**
 * IntentIntegrator for the V4 Android compatibility package.
 * 
 * @author Lachezar Dobrev
 */
public final class IntentIntegratorSupportV4 extends IntentIntegrator {

  private final Fragment fragment;

  /**
   * @param fragment Fragment to handle activity response.
   */
  public IntentIntegratorSupportV4(Fragment fragment) {
    super(fragment.getActivity());
    this.fragment = fragment;
  }

  @Override
  protected void startActivityForResult(Intent intent, int code) {
    fragment.startActivityForResult(intent, code);
  }

}