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

package com.google.zxing.web.generator.client;

/**
 * Helpers methods to check for phone numbers, email addresses, and URL. Other
 * general purpose check methods should go here as well.
 * 
 * @author Yohann Coppel
 */
public final class Validators {
  public static String filterNumber(String number) {
    return number.replaceAll("[ \\.,\\-\\(\\)]", "");
  }
  
  public static void validateNumber(String number) throws GeneratorException {    
    if (!number.matches("\\+?[0-9]+")) {
      throw new GeneratorException("Phone number must be digits only.");
    }
  }
  
  public static void validateUrl(String url) throws GeneratorException {
    if (!isBasicallyValidURI(url)) {
      throw new GeneratorException("URL is not valid.");
    }
  }

  private static boolean isBasicallyValidURI(String uri) {
    if (uri == null || uri.indexOf(' ') >= 0 || uri.indexOf('\n') >= 0) {
      return false;
    }
    int period = uri.indexOf('.');
    // Look for period in a domain but followed by at least a two-char TLD
    if (period >= uri.length() - 2) {
      return false;
    }
    return period >= 0 || uri.indexOf(':') >= 0;
  }
  
  public static void validateEmail(String email) throws GeneratorException {
    //FIXME: we can have a better check for email here.
    if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
      throw new GeneratorException("Email is not valid.");
    }
  }

}
