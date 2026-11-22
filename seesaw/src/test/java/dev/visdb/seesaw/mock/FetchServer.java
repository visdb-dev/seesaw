/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package dev.visdb.seesaw.mock;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.sun.net.httpserver.HttpServer;

/**
 * A local HTTP server that records the path of every request and answers
 * 404. For tests that check that rendering some HTML fetches nothing.
 * <p>
 * {@link #awaitOthers()} is how such a test knows that it has waited long
 * enough: it renders a label that does fetch, a sentinel, and waits for
 * that request. The JDK's label renderer loads an image while the label
 * is being laid out and painted on the event thread, so when the sentinel
 * has arrived, so has anything rendered before it.
 */
public final class FetchServer {
  /** How long to wait for an expected request. */
  private static final long TIMEOUT_MS = 5000;
  private static final String SENTINEL = "sentinel-";

  private final HttpServer server;
  private final String url;
  private final List<String> fetched = new CopyOnWriteArrayList<>();
  private final AtomicInteger sentinels = new AtomicInteger();

  private FetchServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      try (exchange) {
        fetched.add(exchange.getRequestURI().getPath());
        exchange.sendResponseHeaders(404, -1);
      }
    });
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
  }

  /** Create and start a server on a free port.
   * @return
   * @throws java.io.IOException  */
  public static FetchServer start() throws IOException {
    return new FetchServer();
  }

  /** x */
  public void stop() {
    server.stop(0);
  }

  /** The server's URL, ending in "/".
   * @return  */
  public String url() {
    return url;
  }

  /** The paths that were requested, in the order they arrived.
   * @return  */
  public List<String> fetched() {
    return fetched;
  }

  /** x */
  public void clear() {
    fetched.clear();
  }

  /**
   * Wait until something has been fetched, or the timeout.
   * @return true if something was fetched
   * @throws java.lang.InterruptedException
   */
  @SuppressWarnings("SleepWhileInLoop")
  public boolean awaitAny() throws InterruptedException {
    long end = System.currentTimeMillis() + TIMEOUT_MS;
    while (fetched.isEmpty() && System.currentTimeMillis() < end) {
      Thread.sleep(10);
    }
    return !fetched.isEmpty();
  }

  /**
   * Render a label that fetches, wait for its request, and return what
   * else was fetched since the last {@link #clear()}.
   * <p>
   * In a full run, see {@link TestMode}, it then also waits graceMillis,
   * in case some JDK loads an image in the background.
   * @param graceMillis how long to wait after the sentinel in a full run
   * @return the paths fetched, other than the sentinels
   * @throws Exception from rendering the sentinel
   * @throws AssertionError if the sentinel's request does not arrive
   */
  @SuppressWarnings("SleepWhileInLoop")
  public List<String> awaitOthers(long graceMillis) throws Exception {
    String path = SENTINEL + sentinels.incrementAndGet() + ".png";
    SwingUtilities.invokeAndWait(() -> {
      JLabel label = new JLabel("<html><img src='" + url + path + "'>");
      label.setSize(100, 100);
      BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
      Graphics g = image.getGraphics();
      label.paint(g);
      g.dispose();
    });
    long end = System.currentTimeMillis() + TIMEOUT_MS;
    while (!fetched.contains("/" + path)) {
      if (System.currentTimeMillis() > end)
        throw new AssertionError("The sentinel " + path + " was not fetched,"
            + " so whether anything else was can not be told; fetched: " + fetched);
      Thread.sleep(10);
    }
    if (!TestMode.quick())
      Thread.sleep(graceMillis);
    List<String> others = new ArrayList<>();
    for (String p : fetched) {
      if (!p.startsWith("/" + SENTINEL))
        others.add(p);
    }
    return others;
  }
}
// vi: sw=2 ts=8
