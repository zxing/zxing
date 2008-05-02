/*
 * Copyright 2008 Google Inc.
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

package com.google.zxing.web;

import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageMonochromeBitmapSource;

import javax.imageio.ImageIO;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Owen
 */
final class DecodeEmailTask extends TimerTask {

  private static final Logger log = Logger.getLogger(DecodeEmailTask.class.getName());

  private static final String SMTP_HOST = "smtp.gmail.com";
  private static final String POP_HOST = "pop.gmail.com";
  private static final String SMTP_PORT = "465";
  private static final String POP_PORT = "995";
  private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
  private static final Address fromAddress;
  private static final Properties sessionProperties = new Properties();
  static {
    try {
      fromAddress = new InternetAddress("w@zxing.org", "ZXing By Email");
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException(uee);
    }
    sessionProperties.setProperty("mail.transport.protocol", "smtp");
    sessionProperties.setProperty("mail.smtp.host", SMTP_HOST);
    sessionProperties.setProperty("mail.smtp.auth", "true");
    sessionProperties.setProperty("mail.smtp.port", SMTP_PORT);
    sessionProperties.setProperty("mail.smtp.socketFactory.port", SMTP_PORT);
    sessionProperties.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
    sessionProperties.setProperty("mail.smtp.socketFactory.fallback", "false");
    sessionProperties.setProperty("mail.smtp.quitwait", "false");
    sessionProperties.setProperty("mail.pop3.host", POP_HOST);
    sessionProperties.setProperty("mail.pop3.auth", "true");
    sessionProperties.setProperty("mail.pop3.port", POP_PORT);
    sessionProperties.setProperty("mail.pop3.socketFactory.port", POP_PORT);
    sessionProperties.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
    sessionProperties.setProperty("mail.pop3.socketFactory.fallback", "false");
  }

  private final Authenticator emailAuthenticator;

  DecodeEmailTask(Authenticator emailAuthenticator) {
    this.emailAuthenticator = emailAuthenticator;
  }

  @Override
  public void run() {
    log.info("Checking email...");
    try {
      Session session = Session.getInstance(sessionProperties, emailAuthenticator);
      Store store = null;
      Folder inbox = null;
      try {
        store = session.getStore("pop3");
        store.connect();
        inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);
        int count = inbox.getMessageCount();
        if (count > 0) {
          log.info("Found " + count + " messages");
        }
        for (int i = 1; i <= count; i++) {
          log.info("Processing message " + i);
          Message message = inbox.getMessage(i);
          Object content = message.getContent();
          if (content instanceof MimeMultipart) {
            MimeMultipart mimeContent = (MimeMultipart) content;
            int numParts = mimeContent.getCount();
            for (int j = 0; j < numParts; j++) {
              MimeBodyPart part = (MimeBodyPart) mimeContent.getBodyPart(j);
              String contentType = part.getContentType();
              if (!contentType.startsWith("image/")) {
                continue;
              }
              BufferedImage image = ImageIO.read(part.getInputStream());
              if (image != null) {
                Reader reader = new MultiFormatReader();
                Result result = null;
                try {
                  result = reader.decode(new BufferedImageMonochromeBitmapSource(image), DecodeServlet.HINTS);
                } catch (ReaderException re) {
                  log.info("Decoding FAILED");
                }

                Message reply = new MimeMessage(session);
                Address sender = message.getFrom()[0];
                reply.setRecipient(Message.RecipientType.TO, sender);
                reply.setFrom(fromAddress);
                if (result == null) {
                  reply.setSubject("Decode failed");
                  reply.setContent("Sorry, we could not decode that image.", "text/plain");
                } else {
                  String text = result.getText();
                  reply.setSubject("Decode succeeded");
                  reply.setContent(text, "text/plain");
                }
                log.info("Sending reply");
                Transport.send(reply);
              }
            }
          }
          message.setFlag(Flags.Flag.DELETED, true);
        }
      } finally {
        try {
          if (inbox != null) {
            inbox.close(true);
          }
          if (store != null) {
            store.close();
          }
        } catch (MessagingException me) {
          // continue
        }
      }
    } catch (Throwable t) {
      log.log(Level.WARNING, "Unexpected error", t);
    }
  }

  public static void main(String[] args) {
    Authenticator emailAuthenticator = new EmailAuthenticator(args[0], args[1]);
    new DecodeEmailTask(emailAuthenticator).run();
  }

}