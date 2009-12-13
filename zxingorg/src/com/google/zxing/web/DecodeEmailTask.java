/*
 * Copyright 2008 ZXing authors
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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Service;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * A {@link TimerTask} which repeatedly checks an e-mail account for messages with an attached
 * image. When one is found it attempts to decode the image and replies with the decoded messages
 * by e-mail.
 *
 * @author Sean Owen
 */
final class DecodeEmailTask extends TimerTask {

  private static final Logger log = Logger.getLogger(DecodeEmailTask.class.getName());

  private static final String SMTP_HOST = "smtp.gmail.com";
  private static final String POP_HOST = "pop.gmail.com";
  private static final String SMTP_PORT = "465";
  private static final String POP_PORT = "995";
  private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
  private static final Properties sessionProperties = new Properties();
  static {
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
  private final Address fromAddress;

  DecodeEmailTask(String emailAddress, Authenticator emailAuthenticator) {
    this.emailAuthenticator = emailAuthenticator;
    try {
      fromAddress = new InternetAddress(emailAddress, "ZXing By Email");
    } catch (UnsupportedEncodingException uee) {
      // Can't happen?
      throw new RuntimeException(uee);
    }
  }

  @Override
  public void run() {
    log.info("Checking email...");
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
        processMessage(session, message);
      }
    } catch (Throwable t) {
      log.log(Level.WARNING, "Unexpected error", t);
    } finally {
      closeResources(store, inbox);
    }
  }

  private void processMessage(Session session, Message message) throws MessagingException,
      IOException {
    Object content = message.getContent();
    if (content instanceof MimeMultipart) {
      MimeMultipart mimeContent = (MimeMultipart) content;
      int numParts = mimeContent.getCount();
      for (int j = 0; j < numParts; j++) {
        MimeBodyPart part = (MimeBodyPart) mimeContent.getBodyPart(j);
        processMessagePart(session, message, part);
      }
    }
    message.setFlag(Flags.Flag.DELETED, true);
  }

  private void processMessagePart(Session session, Message message, MimeBodyPart part)
      throws MessagingException, IOException {
    String contentType = part.getContentType();
    if (contentType.startsWith("image/")) {
      BufferedImage image = ImageIO.read(part.getInputStream());
      if (image != null) {
        Reader reader = new MultiFormatReader();
        Result result = null;
        try {
          LuminanceSource source = new BufferedImageLuminanceSource(image);
          BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
          result = reader.decode(bitmap, DecodeServlet.HINTS);
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

  private static void closeResources(Service service, Folder inbox) {
    try {
      if (inbox != null) {
        inbox.close(true);
      }
      if (service != null) {
        service.close();
      }
    } catch (MessagingException me) {
      // continue
    }
  }

  public static void main(String[] args) {
    String emailAddress = args[0];
    String emailPassword = args[1];
    Authenticator emailAuthenticator = new EmailAuthenticator(emailAddress, emailPassword);
    new DecodeEmailTask(emailAddress, emailAuthenticator).run();
  }

}
