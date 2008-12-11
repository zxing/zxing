/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.client.android.result;

import android.app.Activity;
import android.telephony.PhoneNumberUtils;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.SMSParsedResult;

public final class SMSResultHandler extends ResultHandler {

  private static final int[] mButtons = {
      R.string.button_sms,
      R.string.button_mms
  };

  public SMSResultHandler(Activity activity, ParsedResult result) {
    super(activity, result);
  }

  @Override
  public int getButtonCount() {
    return mButtons.length;
  }

  @Override
  public int getButtonText(int index) {
    return mButtons[index];
  }

  @Override
  public void handleButtonPress(int index) {
    SMSParsedResult smsResult = (SMSParsedResult) mResult;
    switch (index) {
      case 0:
        sendSMS(smsResult.getNumber(), smsResult.getBody());
        break;
      case 1:
        sendMMS(smsResult.getNumber(), smsResult.getSubject(), smsResult.getBody());
        break;
    }
  }

  @Override
  public CharSequence getDisplayContents() {
    SMSParsedResult smsResult = (SMSParsedResult) mResult;
    StringBuffer contents = new StringBuffer();
    ParsedResult.maybeAppend(PhoneNumberUtils.formatNumber(smsResult.getNumber()), contents);
    ParsedResult.maybeAppend(smsResult.getVia(), contents);
    ParsedResult.maybeAppend(smsResult.getSubject(), contents);
    ParsedResult.maybeAppend(smsResult.getBody(), contents);
    ParsedResult.maybeAppend(smsResult.getTitle(), contents);
    return contents.toString();
  }

  @Override
  public int getDisplayTitle() {
    return R.string.result_sms;
  }

}
