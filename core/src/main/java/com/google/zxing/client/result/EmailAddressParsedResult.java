/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.client.result;

/**
 * @author Sean Owen
 */
public final class EmailAddressParsedResult extends ParsedResult {

  private final String[] tos;
  private final String[] ccs;
  private final String[] bccs;
  private final String subject;
  private final String body;

  EmailAddressParsedResult(String to) {
    this(new String[] {to}, null, null, null, null);
  }

  EmailAddressParsedResult(String[] tos,
                           String[] ccs,
                           String[] bccs,
                           String subject,
                           String body) {
    super(ParsedResultType.EMAIL_ADDRESS);
    this.tos = tos;
    this.ccs = ccs;
    this.bccs = bccs;
    this.subject = subject;
    this.body = body;
  }

  /**
   * @return first elements of {@link #getTos()} or {@code null} if none
   * @deprecated use {@link #getTos()}
   */
  @Deprecated
  public String getEmailAddress() {
    return tos == null || tos.length == 0 ? null : tos[0];
  }

  public String[] getTos() {
    return tos;
  }

  public String[] getCCs() {
    return ccs;
  }

  public String[] getBCCs() {
    return bccs;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  /**
   * @return "mailto:"
   * @deprecated without replacement
   */
  @Deprecated
  public String getMailtoURI() {
    return "mailto:";
  }

  @Override
  public String getDisplayResult() {
    StringBuilder result = new StringBuilder(30);
    maybeAppend(tos, result);
    maybeAppend(ccs, result);
    maybeAppend(bccs, result);
    maybeAppend(subject, result);
    maybeAppend(body, result);
    return result.toString();
  }

}