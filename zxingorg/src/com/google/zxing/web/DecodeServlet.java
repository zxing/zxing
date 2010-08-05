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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.awt.color.CMMException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpServlet} which decodes images containing barcodes. Given a URL, it will
 * retrieve the image and decode it. It can also process image files uploaded via POST.
 *
 * @author Sean Owen
 */
public final class DecodeServlet extends HttpServlet {

  // No real reason to let people upload more than a 2MB image
  private static final long MAX_IMAGE_SIZE = 2000000L;
  // No real reason to deal with more than maybe 2 megapixels
  private static final int MAX_PIXELS = 1 << 21;

  private static final Logger log = Logger.getLogger(DecodeServlet.class.getName());

  static final Hashtable<DecodeHintType, Object> HINTS;
  static final Hashtable<DecodeHintType, Object> HINTS_PURE;

  static {
    HINTS = new Hashtable<DecodeHintType, Object>(5);
    HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    Collection<BarcodeFormat> possibleFormats = new Vector<BarcodeFormat>(17);
    possibleFormats.add(BarcodeFormat.UPC_A);
    possibleFormats.add(BarcodeFormat.UPC_E);
    possibleFormats.add(BarcodeFormat.EAN_8);
    possibleFormats.add(BarcodeFormat.EAN_13);
    possibleFormats.add(BarcodeFormat.CODE_39);
    possibleFormats.add(BarcodeFormat.CODE_93);
    possibleFormats.add(BarcodeFormat.CODE_128);
    //possibleFormats.add(BarcodeFormat.CODABAR);
    possibleFormats.add(BarcodeFormat.ITF);
    possibleFormats.add(BarcodeFormat.RSS14);
    possibleFormats.add(BarcodeFormat.QR_CODE);
    possibleFormats.add(BarcodeFormat.DATA_MATRIX);
    possibleFormats.add(BarcodeFormat.PDF417);
    HINTS.put(DecodeHintType.POSSIBLE_FORMATS, possibleFormats);
    HINTS_PURE = new Hashtable<DecodeHintType, Object>(HINTS);
    HINTS_PURE.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
  }

  private HttpParams params;
  private SchemeRegistry registry;
  private DiskFileItemFactory diskFileItemFactory;

  @Override
  public void init(ServletConfig servletConfig) {

    Logger logger = Logger.getLogger("com.google.zxing");
    logger.addHandler(new ServletContextLogHandler(servletConfig.getServletContext()));

    params = new BasicHttpParams();
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

    registry = new SchemeRegistry();
    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

    diskFileItemFactory = new DiskFileItemFactory();

    log.info("DecodeServlet configured");
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String imageURIString = request.getParameter("u");
    if (imageURIString == null || imageURIString.length() == 0) {
      log.fine("URI was empty");
      response.sendRedirect("badurl.jspx");
      return;
    }

    imageURIString = imageURIString.trim();

    if (!(imageURIString.startsWith("http://") || imageURIString.startsWith("https://"))) {
      imageURIString = "http://" + imageURIString;
    }

    URI imageURI;
    try {
      imageURI = new URI(imageURIString);
    } catch (URISyntaxException urise) {
      log.fine("URI was not valid: " + imageURIString);
      response.sendRedirect("badurl.jspx");
      return;
    }

    ClientConnectionManager connectionManager = new SingleClientConnManager(params, registry);
    HttpClient client = new DefaultHttpClient(connectionManager, params);

    HttpUriRequest getRequest = new HttpGet(imageURI);
    getRequest.addHeader("Connection", "close"); // Avoids CLOSE_WAIT socket issue?

    try {

      HttpResponse getResponse;
      try {
        getResponse = client.execute(getRequest);
      } catch (IllegalArgumentException iae) {
        // Thrown if hostname is bad or null
        log.fine(iae.toString());
        getRequest.abort();
        response.sendRedirect("badurl.jspx");
        return;
      } catch (IOException ioe) {
        // Encompasses lots of stuff, including
        //  java.net.SocketException, java.net.UnknownHostException,
        //  javax.net.ssl.SSLPeerUnverifiedException,
        //  org.apache.http.NoHttpResponseException,
        //  org.apache.http.client.ClientProtocolException,
        log.fine(ioe.toString());
        getRequest.abort();
        response.sendRedirect("badurl.jspx");
        return;
      }

      if (getResponse.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
        log.fine("Unsuccessful return code: " + getResponse.getStatusLine().getStatusCode());
        response.sendRedirect("badurl.jspx");
        return;
      }
      if (!isSizeOK(getResponse)) {
        log.fine("Too large");
        response.sendRedirect("badimage.jspx");
        return;
      }

      log.info("Decoding " + imageURI);
      HttpEntity entity = getResponse.getEntity();
      InputStream is = entity.getContent();
      try {
        processStream(is, request, response);
      } finally {
        entity.consumeContent();
        is.close();
      }

    } finally {
      connectionManager.shutdown();
    }

  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {

    if (!ServletFileUpload.isMultipartContent(request)) {
      log.fine("File upload was not multipart");
      response.sendRedirect("badimage.jspx");
      return;
    }

    ServletFileUpload upload = new ServletFileUpload(diskFileItemFactory);
    upload.setFileSizeMax(MAX_IMAGE_SIZE);

    // Parse the request
    try {
      for (FileItem item : (List<FileItem>) upload.parseRequest(request)) {
        if (!item.isFormField()) {
          if (item.getSize() <= MAX_IMAGE_SIZE) {
            log.info("Decoding uploaded file");
            InputStream is = item.getInputStream();
            try {
              processStream(is, request, response);
            } finally {
              is.close();
            }
          } else {
            log.fine("Too large");
            response.sendRedirect("badimage.jspx");
          }
          break;
        }
      }
    } catch (FileUploadException fue) {
      log.fine(fue.toString());
      response.sendRedirect("badimage.jspx");
    }

  }

  private static void processStream(InputStream is, ServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    BufferedImage image;
    try {
      image = ImageIO.read(is);
    } catch (IOException ioe) {
      log.fine(ioe.toString());
      // Includes javax.imageio.IIOException
      response.sendRedirect("badimage.jspx");
      return;
    } catch (CMMException cmme) {
      log.fine(cmme.toString());
      // Have seen this in logs
      response.sendRedirect("badimage.jspx");
      return;
    } catch (IllegalArgumentException iae) {
      log.fine(iae.toString());
      // Have seen this in logs for some JPEGs
      response.sendRedirect("badimage.jspx");
      return;
    }
    if (image == null) {
      response.sendRedirect("badimage.jspx");
      return;
    }
    if (image.getHeight() <= 1 || image.getWidth() <= 1 ||
        image.getHeight() * image.getWidth() > MAX_PIXELS) {
      log.fine("Dimensions too large: " + image.getWidth() + 'x' + image.getHeight());
      response.sendRedirect("badimage.jspx");
      return;
    }

    Reader reader = new MultiFormatReader();
    LuminanceSource source = new BufferedImageLuminanceSource(image);
    BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
    Collection<Result> results = new ArrayList<Result>(1);
    ReaderException savedException = null;

    try {
      // Look for multiple barcodes
      MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(reader);
      Result[] theResults = multiReader.decodeMultiple(bitmap, HINTS);
      if (theResults != null) {
        results.addAll(Arrays.asList(theResults));
      }
    } catch (ReaderException re) {
      savedException = re;
    }

    if (results.isEmpty()) {
      try {
        // Look for pure barcode
        Result theResult = reader.decode(bitmap, HINTS_PURE);
        if (theResult != null) {
          results.add(theResult);
        }
      } catch (ReaderException re) {
        savedException = re;
      }
    }

    if (results.isEmpty()) {
      try {
        // Look for normal barcode in photo
        Result theResult = reader.decode(bitmap, HINTS);
        if (theResult != null) {
          results.add(theResult);
        }
      } catch (ReaderException re) {
        savedException = re;
      }
    }

    if (results.isEmpty()) {
      try {
        // Try again with other binarizer
        BinaryBitmap hybridBitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result theResult = reader.decode(hybridBitmap, HINTS);
        if (theResult != null) {
          results.add(theResult);
        }
      } catch (ReaderException re) {
        savedException = re;
      }
    }

    if (results.isEmpty()) {
      handleException(savedException, response);
      return;
    }

    if (request.getParameter("full") == null) {
      response.setContentType("text/plain");
      response.setCharacterEncoding("UTF8");
      Writer out = new OutputStreamWriter(response.getOutputStream(), "UTF8");
      try {
        for (Result result : results) {
          out.write(result.getText());
          out.write('\n');
        }
      } finally {
        out.close();
      }
    } else {
      request.setAttribute("results", results);
      request.getRequestDispatcher("decoderesult.jspx").forward(request, response);
    }
  }

  private static void handleException(ReaderException re, HttpServletResponse response) throws IOException {
    if (re instanceof NotFoundException) {
      log.info("Not found: " + re);
      response.sendRedirect("notfound.jspx");
    } else if (re instanceof FormatException) {
      log.info("Format problem: " + re);
      response.sendRedirect("format.jspx");
    } else if (re instanceof ChecksumException) {
      log.info("Checksum problem: " + re);
      response.sendRedirect("format.jspx");
    } else {
      log.info("Unknown problem: " + re);
      response.sendRedirect("notfound.jspx");
    }
  }

  private static boolean isSizeOK(HttpMessage getResponse) {
    Header lengthHeader = getResponse.getLastHeader("Content-Length");
    if (lengthHeader != null) {
      long length = Long.parseLong(lengthHeader.getValue());
      if (length > MAX_IMAGE_SIZE) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void destroy() {
    log.config("DecodeServlet shutting down...");
  }

}
