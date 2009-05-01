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

package com.google.zxing.client.android;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <p>Subclass of the Apache {@link DefaultHttpClient} that is configured with
 * reasonable default settings and registered schemes for Android, and
 * also lets the user add {@link HttpRequestInterceptor} classes.
 * Don't create this directly, use the {@link #newInstance} factory method.</p>
 * <p/>
 * <p>This client processes cookies but does not retain them by default.
 * To retain cookies, simply add a cookie store to the HttpContext:
 * <pre>context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);</pre>
 * </p>
 */
public final class AndroidHttpClient implements HttpClient {

  // Gzip of data shorter than this probably won't be worthwhile
  private static final long DEFAULT_SYNC_MIN_GZIP_BYTES = 256;

  /**
   * Set if HTTP requests are blocked from being executed on this thread
   */
  private static final ThreadLocal<Boolean> sThreadBlocked =
      new ThreadLocal<Boolean>();

  /**
   * Interceptor throws an exception if the executing thread is blocked
   */
  private static final HttpRequestInterceptor sThreadCheckInterceptor =
      new HttpRequestInterceptor() {
        public void process(HttpRequest request, HttpContext context) {
          if (sThreadBlocked.get() != null && sThreadBlocked.get()) {
            throw new RuntimeException("This thread forbids HTTP requests");
          }
        }
      };

  /**
   * Create a new HttpClient with reasonable defaults (which you can update).
   *
   * @param userAgent to report in your HTTP requests.
   * @return AndroidHttpClient for you to use for all your requests.
   */
  public static AndroidHttpClient newInstance(String userAgent) {
    HttpParams params = new BasicHttpParams();

    // Turn off stale checking.  Our connections break all the time anyway,
    // and it's not worth it to pay the penalty of checking every time.
    HttpConnectionParams.setStaleCheckingEnabled(params, false);

    // Default connection and socket timeout of 20 seconds.  Tweak to taste.
    HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
    HttpConnectionParams.setSoTimeout(params, 20 * 1000);
    HttpConnectionParams.setSocketBufferSize(params, 8192);

    // Don't handle redirects -- return them to the caller.  Our code
    // often wants to re-POST after a redirect, which we must do ourselves.
    HttpClientParams.setRedirecting(params, false);

    // Set the specified user agent and register standard protocols.
    HttpProtocolParams.setUserAgent(params, userAgent);
    SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme("http",
        PlainSocketFactory.getSocketFactory(), 80));
    schemeRegistry.register(new Scheme("https",
        SSLSocketFactory.getSocketFactory(), 443));
    ClientConnectionManager manager =
        new ThreadSafeClientConnManager(params, schemeRegistry);

    // We use a factory method to modify superclass initialization
    // parameters without the funny call-a-static-method dance.
    return new AndroidHttpClient(manager, params);
  }

  private final HttpClient delegate;


  private AndroidHttpClient(ClientConnectionManager ccm, HttpParams params) {
    this.delegate = new DefaultHttpClient(ccm, params) {
      @Override
      protected BasicHttpProcessor createHttpProcessor() {
        // Add interceptor to prevent making requests from main thread.
        BasicHttpProcessor processor = super.createHttpProcessor();
        processor.addRequestInterceptor(sThreadCheckInterceptor);
        return processor;
      }

      @Override
      protected HttpContext createHttpContext() {
        // Same as DefaultHttpClient.createHttpContext() minus the
        // cookie store.
        HttpContext context = new BasicHttpContext();
        context.setAttribute(ClientContext.AUTHSCHEME_REGISTRY, getAuthSchemes());
        context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, getCookieSpecs());
        context.setAttribute(ClientContext.CREDS_PROVIDER, getCredentialsProvider());
        return context;
      }
    };
  }

  /**
   * Block this thread from executing HTTP requests.
   * Used to guard against HTTP requests blocking the main application thread.
   *
   * @param blocked if HTTP requests run on this thread should be denied
   */
  public static void setThreadBlocked(boolean blocked) {
    sThreadBlocked.set(blocked);
  }

  /**
   * Modifies a request to indicate to the server that we would like a
   * gzipped response.  (Uses the "Accept-Encoding" HTTP header.)
   *
   * @param request the request to modify
   * @see #getUngzippedContent
   */
  public static void modifyRequestToAcceptGzipResponse(HttpMessage request) {
    request.addHeader("Accept-Encoding", "gzip");
  }

  /**
   * Gets the input stream from a response entity.  If the entity is gzipped
   * then this will get a stream over the uncompressed data.
   *
   * @param entity the entity whose content should be read
   * @return the input stream to read from
   * @throws IOException
   */
  public static InputStream getUngzippedContent(HttpEntity entity) throws IOException {
    InputStream responseStream = entity.getContent();
    if (responseStream == null) {
      return responseStream;
    }
    Header header = entity.getContentEncoding();
    if (header == null) {
      return responseStream;
    }
    String contentEncoding = header.getValue();
    if (contentEncoding == null) {
      return responseStream;
    }
    if (contentEncoding.contains("gzip")) {
      responseStream = new GZIPInputStream(responseStream);
    }
    return responseStream;
  }

  public void close() {
    // do nothing
  }

  public HttpParams getParams() {
    return delegate.getParams();
  }

  public ClientConnectionManager getConnectionManager() {
    return delegate.getConnectionManager();
  }

  public HttpResponse execute(HttpUriRequest request) throws IOException {
    return delegate.execute(request);
  }

  public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
    return delegate.execute(request, context);
  }

  public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
    return delegate.execute(target, request);
  }

  public HttpResponse execute(HttpHost target, HttpRequest request,
                              HttpContext context) throws IOException {
    return delegate.execute(target, request, context);
  }

  public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
    return delegate.execute(request, responseHandler);
  }

  public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context)
      throws IOException {
    return delegate.execute(request, responseHandler, context);
  }

  public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler)
      throws IOException {
    return delegate.execute(target, request, responseHandler);
  }

  public <T> T execute(HttpHost target, HttpRequest request,
                       ResponseHandler<? extends T> responseHandler,
                       HttpContext context)
      throws IOException {
    return delegate.execute(target, request, responseHandler, context);
  }

  /**
   * Compress data to send to server.
   * Creates a Http Entity holding the gzipped data.
   * The data will not be compressed if it is too short.
   *
   * @param data The bytes to compress
   * @return Entity holding the data
   */
  public static AbstractHttpEntity getCompressedEntity(byte[] data) throws IOException {
    AbstractHttpEntity entity;
    if (data.length < DEFAULT_SYNC_MIN_GZIP_BYTES) {
      entity = new ByteArrayEntity(data);
    } else {
      ByteArrayOutputStream arr = new ByteArrayOutputStream();
      OutputStream zipper = new GZIPOutputStream(arr);
      try {
        zipper.write(data);
      } finally {
        zipper.close();
      }
      entity = new ByteArrayEntity(arr.toByteArray());
      entity.setContentEncoding("gzip");
    }
    return entity;
  }

}
