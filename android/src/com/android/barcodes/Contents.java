/*
 * Copyright (C) 2008 Google Inc.
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

package com.android.barcodes;


public final class Contents {

    /**
     * All the formats we know about.
     */
    public static final class Format {
        public static final String UPC_A = "UPC_A";
        public static final String UPC_E = "UPC_E";
        public static final String EAN_8 = "EAN_8";
        public static final String EAN_13 = "EAN_13";
        public static final String CODE_39 = "CODE_39";
        public static final String CODE_128 = "CODE_128";
        public static final String QR_CODE = "QR_CODE";
    }

    public static final class Type {
        /**
         * Plain text. Use Intent.putExtra(DATA, string). This can be used for URLs too, but string
         * must include "http://" or "https://".
         */
        public static final String TEXT = "TEXT_TYPE";

        /**
         * An email type. Use Intent.putExtra(DATA, string) where string is the email address.
         */
        public static final String EMAIL = "EMAIL_TYPE";

        /**
         * Use Intent.putExtra(DATA, string) where string is the phone number to call.
         */
        public static final String PHONE = "PHONE_TYPE";

        /**
         * An SMS type. Use Intent.putExtra(DATA, string) where string is the number to SMS.
         */
        public static final String SMS = "SMS_TYPE";

        /**
         * A contact. Send a request to encode it as follows:
         *
         * import android.provider.Contacts;
         *
         * Intent intent = new Intent(Intents.Encode.ACTION);
         * intent.putExtra(Intents.Encode.TYPE, CONTACT);
         * Bundle bundle = new Bundle();
         * bundle.putString(Contacts.Intents.Insert.NAME, "Jenny");
         * bundle.putString(Contacts.Intents.Insert.PHONE, "8675309");
         * bundle.putString(Contacts.Intents.Insert.EMAIL, "jenny@the80s.com");
         * intent.putExtra(Intents.Encode.DATA, bundle);
         */
        public static final String CONTACT = "CONTACT_TYPE";

        /**
         * A geographic location. Use as follows:
         * Bundle bundle = new Bundle();
         * bundle.putFloat("LAT", latitude);
         * bundle.putFloat("LONG", longitude);
         * intent.putExtra(Intents.Encode.DATA, bundle);
         */
        public static final String LOCATION = "LOCATION_TYPE";
    }

}
