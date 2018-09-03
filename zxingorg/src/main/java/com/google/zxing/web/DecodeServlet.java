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
import com.google.zxing.client.j2se.ImageReader;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;

import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * {@link HttpServlet} which decodes images containing barcodes. Given a URL, it will
 * retrieve the image and decode it. It can also process image files uploaded via POST.
 *
 * @author Sean Owen
 */
@MultipartConfig(
    maxFileSize = 1L << 26, // ~64MB
    maxRequestSize = 1L << 26, // ~64MB
    fileSizeThreshold = 1 << 23, // ~8MB
    location = "/tmp")
@WebServlet(value = "/w/decode", loadOnStartup = 1)
public final class DecodeServlet extends HttpServlet {

  private static final Logger log = Logger.getLogger(DecodeServlet.class.getName());

  private static final Pattern WHITESPACE = Pattern.compile("\\s+");
  // No real reason to let people upload more than ~64MB
  private static final long MAX_IMAGE_SIZE = 1L << 26;
  // No real reason to deal with more than ~32 megapixels
  private static final int MAX_PIXELS = 1 << 25;
  private static final byte[] REMAINDER_BUFFER = new byte[1 << 16];
  private static final Map<DecodeHintType,Object> HINTS;
  private static final Map<DecodeHintType,Object> HINTS_PURE;

  static {
    HINTS = new EnumMap<>(DecodeHintType.class);
    HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    HINTS.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
    HINTS_PURE = new EnumMap<>(HINTS);
    HINTS_PURE.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
  }

  private Collection<String> blockedURLSubstrings;
  private Timer timer;
  private DoSTracker destHostTracker;

  @Override
  public void init(ServletConfig servletConfig) throws ServletException {
    Logger logger = Logger.getLogger("com.google.zxing");
    ServletContext context = servletConfig.getServletContext();
    logger.addHandler(new ServletContextLogHandler(context));

    URL blockURL = context.getClassLoader().getResource("/private/uri-block-substrings.txt");
    if (blockURL == null) {
      blockedURLSubstrings = Collections.emptyList();
    } else {
      try {
        blockedURLSubstrings = Resources.readLines(blockURL, StandardCharsets.UTF_8);
      } catch (IOException ioe) {
        throw new ServletException(ioe);
      }
      log.info("Blocking URIs containing: " + blockedURLSubstrings);
    }

    timer = new Timer("DecodeServlet");
    destHostTracker = new DoSTracker(timer, 500, TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES), 10_000);
  }

  @Override
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }

  @Override
  protected void doGet(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException, IOException {

    String imageURIString = request.getParameter("u");
    if (imageURIString == null || imageURIString.isEmpty()) {
      log.info("URI was empty");
      errorResponse(request, response, "badurl");
      return;
    }

    // Remove any whitespace to sanitize; none is valid anyway
    imageURIString = WHITESPACE.matcher(imageURIString).replaceAll("");

    if (!blockedURLSubstrings.isEmpty()) {
      for (CharSequence substring : blockedURLSubstrings) {
        if (imageURIString.contains(substring)) {
          log.info("Disallowed URI " + imageURIString);
          errorResponse(request, response, "badurl");
          return;
        }
      }
    }

    URI imageURI;
    try {
      imageURI = new URI(imageURIString);
      // Assume http: if not specified
      if (imageURI.getScheme() == null) {
        imageURI = new URI("http://" + imageURIString);
      }
    } catch (URISyntaxException e) {
      log.info("Error " + e + " while parsing URI: " + imageURIString);
      errorResponse(request, response, "badurl");
      return;
    }
    
    // Shortcut for data URI
    if ("data".equals(imageURI.getScheme())) {
      BufferedImage image;
      try {
        image = ImageReader.readDataURIImage(imageURI);
      } catch (Exception e) {
        log.info("Error " + e + " while reading data URI: " + imageURIString);
        errorResponse(request, response, "badurl");
        return;
      }
      if (image == null) {
        log.info("Couldn't read data URI: " + imageURIString);
        errorResponse(request, response, "badimage");
        return;
      }
      try {
        processImage(image, request, response);
      } finally {
        image.flush();
      }
      return;
    }

    if (destHostTracker.isBanned(imageURI.getHost())) {
      errorResponse(request, response, "badurl");
      return;
    }
    
    URL imageURL;    
    try {
      imageURL = imageURI.toURL();
    } catch (MalformedURLException ignored) {
      log.info("URI is not a URL: " + imageURIString);
      errorResponse(request, response, "badurl");
      return;
    }

    String protocol = imageURL.getProtocol();
    if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
      log.info("URL protocol was not valid: " + imageURIString);
      errorResponse(request, response, "badurl");
      return;
    }

    HttpURLConnection connection;
    try {
      connection = (HttpURLConnection) imageURL.openConnection();
    } catch (IllegalArgumentException ignored) {
      log.info("URL could not be opened: " + imageURIString);
      errorResponse(request, response, "badurl");
      return;
    }

    connection.setAllowUserInteraction(false);
    connection.setInstanceFollowRedirects(true);
    connection.setReadTimeout(5000);
    connection.setConnectTimeout(5000);
    connection.setRequestProperty(HttpHeaders.USER_AGENT, "zxing.org");
    connection.setRequestProperty(HttpHeaders.CONNECTION, "close");

    try {
      connection.connect();
    } catch (Exception e) {
      // Encompasses lots of stuff, including
      //  java.net.SocketException, java.net.UnknownHostException,
      //  javax.net.ssl.SSLPeerUnverifiedException,
      //  org.apache.http.NoHttpResponseException,
      //  org.apache.http.client.ClientProtocolException,
      log.info("Error " + e + " connecting to " + imageURIString);
      errorResponse(request, response, "badurl");
      return;
    }

    try (InputStream is = connection.getInputStream()) {
      try {
        if (connection.getResponseCode() != HttpServletResponse.SC_OK) {
          log.info("Unsuccessful return code " + connection.getResponseCode() + " from " + imageURIString);
          errorResponse(request, response, "badurl");
          return;
        }
        if (connection.getHeaderFieldInt(HttpHeaders.CONTENT_LENGTH, 0) > MAX_IMAGE_SIZE) {
          log.info("Too large: " + imageURIString);
          errorResponse(request, response, "badimage");
          return;
        }
        // Assume we'll only handle image/* content types
        String contentType = connection.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
          log.info("Wrong content type " + contentType + ": " + imageURIString);
          errorResponse(request, response, "badimage");
          return;
        }

        log.info("Decoding " + imageURIString);
        processStream(is, request, response);
      } finally {
        consumeRemainder(is);
      }
    } catch (IOException ioe) {
      log.info("Error " + ioe + " processing " + imageURIString);
      errorResponse(request, response, "badurl");
    } finally {
      connection.disconnect();
    }

  }

  private static void consumeRemainder(InputStream is) {
    try {
      while (is.read(REMAINDER_BUFFER) > 0) {
        // don't care about value, or collision
      }
    } catch (IOException | IndexOutOfBoundsException ioe) {
      // sun.net.www.http.ChunkedInputStream.read is throwing IndexOutOfBoundsException
      // continue
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Collection<Part> parts;
    try {
      parts = request.getParts();
    } catch (Exception e) {
      // Includes IOException, InvalidContentTypeException, other parsing IllegalStateException
      log.info(e.toString());
      errorResponse(request, response, "badimage");
      return;
    }
    Part fileUploadPart = null;
    for (Part part : parts) {
      if (part.getHeader(HttpHeaders.CONTENT_DISPOSITION) != null) {
        fileUploadPart = part;
        break;
      }
    }
    if (fileUploadPart == null) {
      log.info("File upload was not multipart");
      errorResponse(request, response, "badimage");
    } else {
      log.info("Decoding uploaded file " + fileUploadPart.getSubmittedFileName());
      try (InputStream is = fileUploadPart.getInputStream()) {
        processStream(is, request, response);
      }
    }
  }

  private static void processStream(InputStream is,
                                    HttpServletRequest request,
                                    HttpServletResponse response) throws ServletException, IOException {

    BufferedImage image;
    try {
      image = ImageIO.read(is);
    } catch (Exception e) {
      // Many possible failures from JAI, so just catch anything as a failure
      log.info(e.toString());
      errorResponse(request, response, "badimage");
      return;
    }
    if (image == null) {
      errorResponse(request, response, "badimage");
      return;
    }
    try {
      int height = image.getHeight();
      int width = image.getWidth();
      if (height <= 1 || width <= 1 || height * width > MAX_PIXELS) {
        log.info("Dimensions out of bounds: " + width + 'x' + height);
        errorResponse(request, response, "badimage");
        return;
      }

      processImage(image, request, response);
    } finally {
      image.flush();
    }
  }
  
  private static void processImage(BufferedImage image,
                                   HttpServletRequest request,
                                   HttpServletResponse response) throws IOException, ServletException {

    LuminanceSource source = new BufferedImageLuminanceSource(image);
    BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
    Collection<Result> results = new ArrayList<>(1);

    try {

      Reader reader = new MultiFormatReader();
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
        try {
          throw savedException == null ? NotFoundException.getNotFoundInstance() : savedException;
        } catch (FormatException | ChecksumException e) {
          log.info(e.toString());
          errorResponse(request, response, "format");
        } catch (ReaderException e) { // Including NotFoundException
          log.info(e.toString());
          errorResponse(request, response, "notfound");
        }
        return;
      }

    } catch (RuntimeException re) {
      // Call out unexpected errors in the log clearly
      log.log(Level.WARNING, "Unexpected exception from library", re);
      throw new ServletException(re);
    }

    String fullParameter = request.getParameter("full");
    boolean minimalOutput = fullParameter != null && !Boolean.parseBoolean(fullParameter);
    if (minimalOutput) {
      response.setContentType(MediaType.PLAIN_TEXT_UTF_8.toString());
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      try (Writer out = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
        for (Result result : results) {
          out.write(result.getText());
          out.write('\n');
        }
      }
    } else {
      request.setAttribute("results", results);
      request.getRequestDispatcher("decoderesult.jspx").forward(request, response);
    }
  }

  private static void errorResponse(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String key) throws ServletException, IOException {
    Locale locale = request.getLocale();
    if (locale == null) {
      locale = Locale.ENGLISH;
    }
    ResourceBundle bundle = ResourceBundle.getBundle("Strings", locale);
    String title = bundle.getString("response.error." + key + ".title");
    String text = bundle.getString("response.error." + key + ".text");
    request.setAttribute("title", title);
    request.setAttribute("text", text);
    RequestDispatcher dispatcher = request.getRequestDispatcher("response.jspx");
    if (dispatcher == null) {
      log.warning("Can't obtain RequestDispatcher");
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      dispatcher.forward(request, response);
    }
  }

}
