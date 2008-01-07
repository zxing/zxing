/*
 * Copyright 2007 Google Inc.
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

package com.google.zxing.qrcode;

import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.Reader;
import com.google.zxing.client.j2se.BufferedImageMonochromeBitmapSource;

import junit.framework.TestCase;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class QRCodeReaderTestCase extends TestCase {

  public void testDecode() throws Exception {
    doTestURI("http://writerresponsetheory.org/wordpress/wp-content/uploads/qrcode_01.png",
              "When we at WRT talk about \\\"text,\\\" we are generally talking about a particular kind of readable " +
              "information encoding - and readable is a complex proposition. Text may be stylized in a way we are " +
              "unfamiliar with, as in blackletter - it may be interspersed with some markup we don\\'t understand, " +
              "such as HTML - it may be be a substitution system we aren\\'t familiar with, such as braille or " +
              "morse code - or it may be a system that, while technically human-readable, isn\\'t particularly " +
              "optimized for reading by humans, as with barcodes (although barcodes can be read).");
    doTestURI("http://writerresponsetheory.org/query/poe/qrcode%20outputs/qrcodegen-examples/chunk-from-128-bug-head/" +
              "encodes-by-nfggames/qr-chunk-from-128-bughead-ground.png",
              "LANDBYASCARC   PERCEPTIBLEC EEK,OOZI GITSWAYTHROU  AWILDERNESSOFBESUPPOSED,I  " +
              "CANT,ORATL ASTDWARFISH.NO REESOFANYM  NITUDEARETOBESOMEMISERA  EFRAMEBUIL I GS,TENANTED, U " +
              "INGSUMMER   THEFUGITIVE;BUTTHEWHO  ISLAND,WI H   EXCEPTIONOFT   W STERNPOI  ,ANDALINEOFLLIAMLEGR   " +
              ".HEWASOF N    ENTHUGUENOTF    Y ANDHADON  BEENWEALTHTIONCONSE  ENTUPONH SD   STERS,HELEFTNE   " +
              "LE NS,THEC   OFHISFOREOUTHCARO   A.THISISLA    AVERYSINGULARO    TCONSISTSO  " +
              "ITTLEELSEDSAQUART   FAMILE. TI   PARATEDFROMTHEMA   AN BYASCAR   YPERCEPTERESORT     " +
              "MARSH EN   EVEGETATION,ASMIGH   SU POSED,   CANT,ORATREMITY,      " +
              "ORT OULTRIESTANDS,ANDWHEREARESOM MIS      FRAMEBUIFEVER,MAYBE      " +
              "INDEED,                TO;BUTT      EISLAND,WITNYYEARSAGO,IC    ACTED                    " +
              "LLIAM    AND.HEWASOFANNESHADREDUCEDHIM OWA                       " +
              "IONC NSEQUENTUPONHISDSIDENCEATSULLIVA \\'S                          HC ROLINA.THISISLANOUTTHR    " +
              "LESLON .        THATN  OINTE        U RTEROF    E.ITISTHROU       ERNE       DSANDSLI  ,AFAVOR        " +
              "TOFT       HEN.T");
    doTestURI("http://www.malcolmhall.com/wp-content/uploads/2006/07/200607260214.jpg",
              "http://www.malcolmhall.com");
    doTestURI("http://www.qrcodeblog.com/qr/0609/060902_qr_kawasaki_st02.jpg",
              "http://wwws.keihin.ktr.mlit.go.jp/keitai/");
    doTestURI("http://mobile.kaywa.com/files/images/2007/4/480/mob181_1175524511.jpg",
              "2021200000");
    doTestURI("http://www.smoothplanet.com/files/images/2007/2/mob281_1170754866.jpg",
              "http://d.kaywa.com/20207100");
    doTestURI("http://www.mobileviews.com/blog/wp-content/uploads/2006/11/livebarcode.gif",
              "BIZCARD:N:Todd;X:Ogasawara;T:Tech Geek;C:MobileViews.com;A:MobileTown USA;E:editor@mobileviews.com;;");
    doTestURI("http://staticrooster.com/tshirts/qr_sm.gif",
              "http://staticrooster.com");
    doTestURI("http://www.ihaveanidea.org/blogs/uploads/i/interactive/270.png",
              "Morden");
    doTestURI("http://www.google.co.jp/mobile/images/qrcode_mobile.gif",
              "Google \u30e2\u30d0\u30a4\u30eb\r\nhttp://google.jp");
  }

  private static void doTestURI(final String uriString, final String expected)
      throws URISyntaxException, IOException, ReaderException {
    URI uri = new URI(uriString);
    InputStream is = uri.toURL().openStream();
    try {
      BufferedImage image = ImageIO.read(is);
      MonochromeBitmapSource source = new BufferedImageMonochromeBitmapSource(image);
      Reader reader = new QRCodeReader();
      Result result = reader.decode(source);
      assertEquals(expected, result.getText());
    } finally {
      is.close();
    }
  }

}