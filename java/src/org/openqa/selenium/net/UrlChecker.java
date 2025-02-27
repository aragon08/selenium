// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.net;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/** Polls a URL until a HTTP 200 response is received. */
public class UrlChecker {

  private static final Logger LOG = Logger.getLogger(UrlChecker.class.getName());

  static final int CONNECT_TIMEOUT_MS = 500;
  private static final int READ_TIMEOUT_MS = 1000;
  private static final long MAX_POLL_INTERVAL_MS = 320;
  private static final long MIN_POLL_INTERVAL_MS = 10;

  private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
  private static final ExecutorService EXECUTOR =
      Executors.newCachedThreadPool(
          r -> {
            Thread t =
                new Thread(
                    r, "UrlChecker-" + THREAD_COUNTER.incrementAndGet()); // Thread safety reviewed
            t.setDaemon(true);
            return t;
          });

  public void waitUntilAvailable(long timeout, TimeUnit unit, final URL... urls)
      throws TimeoutException {
    long start = System.currentTimeMillis();
    LOG.fine("Waiting for " + Arrays.toString(urls));
    try {
      Future<Void> callback =
          EXECUTOR.submit(
              () -> {
                HttpURLConnection connection = null;

                long sleepMillis = MIN_POLL_INTERVAL_MS;
                while (!Thread.interrupted()) {
                  for (URL url : urls) {
                    try {
                      LOG.fine("Polling " + url);
                      connection = connectToUrl(url);
                      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        return null;
                      }
                    } catch (IOException e) {
                      // Ok, try again.
                    } finally {
                      if (connection != null) {
                        consume(connection);
                        connection.disconnect();
                      }
                    }
                  }
                  MILLISECONDS.sleep(sleepMillis);
                  sleepMillis =
                      (sleepMillis >= MAX_POLL_INTERVAL_MS) ? sleepMillis : sleepMillis * 2;
                }
                throw new InterruptedException();
              });
      try {
        callback.get(timeout, unit);
      } finally {
        // if already completed cancel is ignored
        callback.cancel(true);
      }
    } catch (java.util.concurrent.TimeoutException e) {
      throw new TimeoutException(
          String.format(
              "Timed out waiting for %s to be available after %d ms",
              Arrays.toString(urls), System.currentTimeMillis() - start),
          e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public void waitUntilUnavailable(long timeout, TimeUnit unit, final URL url)
      throws TimeoutException {
    long start = System.currentTimeMillis();
    LOG.fine("Waiting for " + url);
    try {
      Future<Void> callback =
          EXECUTOR.submit(
              () -> {
                HttpURLConnection connection = null;

                long sleepMillis = MIN_POLL_INTERVAL_MS;
                while (!Thread.interrupted()) {
                  try {
                    LOG.fine("Polling " + url);
                    connection = connectToUrl(url);
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                      return null;
                    }
                  } catch (IOException e) {
                    return null;
                  } finally {
                    if (connection != null) {
                      consume(connection);
                      connection.disconnect();
                    }
                  }

                  MILLISECONDS.sleep(sleepMillis);
                  sleepMillis =
                      (sleepMillis >= MAX_POLL_INTERVAL_MS) ? sleepMillis : sleepMillis * 2;
                }
                throw new InterruptedException();
              });
      try {
        callback.get(timeout, unit);
      } finally {
        // if already completed cancel is ignored
        callback.cancel(true);
      }
    } catch (java.util.concurrent.TimeoutException e) {
      throw new TimeoutException(
          String.format(
              "Timed out waiting for %s to become unavailable after %d ms",
              url, System.currentTimeMillis() - start),
          e);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Read and closes the ErrorStream / InputStream of the HttpURLConnection to allow Java reusing
   * the open socket.
   *
   * @param connection the connection to consume the input
   */
  private static void consume(HttpURLConnection connection) {
    try {
      InputStream data = connection.getErrorStream();
      if (data == null) {
        data = connection.getInputStream();
      }
      if (data != null) {
        data.readAllBytes();
        data.close();
      }
    } catch (IOException e) {
      // swallow
    }
  }

  private HttpURLConnection connectToUrl(URL url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(READ_TIMEOUT_MS);
    connection.connect();
    return connection;
  }

  public static class TimeoutException extends Exception {
    public TimeoutException(String s, Throwable throwable) {
      super(s, throwable);
    }
  }
}
