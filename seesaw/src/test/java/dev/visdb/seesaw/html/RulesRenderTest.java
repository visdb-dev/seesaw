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

package dev.visdb.seesaw.html;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Give the JDK's label renderer everything that the Rules constants allow
 * and check that nothing is fetched, no component is put in the label and
 * nothing is thrown. This is what backs the claim in {@link Rules}; it runs
 * against the JDK the tests run on.
 */
public class RulesRenderTest {
  private static HttpServer server;
  private static String url;
  private static final List<String> fetched = new CopyOnWriteArrayList<>();

  @BeforeAll
  @SuppressWarnings({"unused", "ConvertToTryWithResources"})
  static void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      fetched.add(exchange.getRequestURI().getPath());
      exchange.sendResponseHeaders(404, -1);
      exchange.close();
    });
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
  }

  @AfterAll
  @SuppressWarnings("unused")
  static void stopServer() {
    server.stop(0);
  }

  /** What rendering did, other than a fetch. */
  private record Rendered(int components, Throwable thrown) {}

  private static Rendered render(String html) throws Exception {
    Rendered[] result = new Rendered[1];
    SwingUtilities.invokeAndWait(() -> {
      try {
        JLabel label = new JLabel(html);
        label.setSize(300, 100);
        BufferedImage image = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        label.paint(g);
        g.dispose();
        result[0] = new Rendered(label.getComponentCount(), null);
      } catch (Throwable t) {
        result[0] = new Rendered(0, t);
      }
    });
    return result[0];
  }

  @SuppressWarnings("SleepWhileInLoop")
  private static void awaitFetch() throws InterruptedException {
    for (int i = 0; i < 100 && fetched.isEmpty(); i++) {
      Thread.sleep(50);
    }
  }

  /** First make sure this test can see the things it claims do not happen. */
  @Test
  void testDetects() throws Exception {
    fetched.clear();
    render("<html><img src='" + url + "detect.img'>");
    awaitFetch();
    assertThat(fetched).contains("/detect.img");

    fetched.clear();
    render("<html><table background='" + url + "detect.bg'><tr><td>x</table>");
    awaitFetch();
    assertThat(fetched).contains("/detect.bg");

    fetched.clear();
    render("<html><ul style='list-style-image:url(" + url + "detect.css)'><li>x</ul>");
    awaitFetch();
    assertThat(fetched).contains("/detect.css");

    assertThat(render("<html><input type=text>").components()).isGreaterThan(0);
    assertThat(render("<html><frameset><frame></frameset>").thrown()).isNotNull();
    fetched.clear();
  }

  @Test
  void testInline() throws Exception {
    checkRules("INLINE", Rules.INLINE);
  }

  @Test
  void testMultiline() throws Exception {
    checkRules("MULTILINE", Rules.MULTILINE);
  }

  @Test
  void testBroad() throws Exception {
    checkRules("BROAD", Rules.BROAD);
  }

  /** Something for the element to contain, so lists and tables are laid out. */
  private static String content(String element, Rules rules) {
    return switch (element) {
    case "ul", "ol", "dir", "menu" -> rules.allowsElement("li") ? "x<li>y" : "x";
    case "dl" -> "x<dt>y<dd>z";
    case "table" -> "<tr><td>x<th>y";
    case "tr" -> "<td>x";
    default -> "x";
    };
  }

  private void checkRules(String name, Rules rules) throws Exception {
    SsHtml checker = SsHtmlChecker.get(rules);
    Set<String> elements = new TreeSet<>(rules.elements());
    elements.addAll(Rules.STRUCTURE);
    List<String> texts = new ArrayList<>();
    for (String element : elements) {
      // The element with each allowed attribute by itself, then with all of them.
      String content = content(element, rules);
      StringBuilder all = new StringBuilder();
      for (String attribute : rules.attributes()) {
        int at = attribute.indexOf('@');
        if (at >= 0 && !attribute.substring(0, at).equals(element))
          continue;
        String attr = attribute.substring(at + 1);
        String value = " " + attr + "='" + url + name + "." + element + "." + attr + "'";
        texts.add("<html><" + element + value + ">" + content + "</" + element + ">w");
        all.append(value);
      }
      texts.add("<html><" + element + all + ">" + content + "</" + element + ">w");
      texts.add("<html><head><" + element + all + ">x</" + element + "></head>w");
      // Each element inside each other element.
      for (String inner : elements) {
        texts.add("<html><" + element + ">a<" + inner + ">b</" + inner + ">c</" + element + ">");
      }
    }
    // Many at once, nested.
    List<String> list = List.copyOf(rules.elements());
    for (int i = 0; i < list.size(); i += 8) {
      StringBuilder nested = new StringBuilder("<html>");
      for (String element : list.subList(i, Math.min(i + 8, list.size()))) {
        nested.append('<').append(element).append('>').append(element);
      }
      texts.add(nested.toString());
    }

    for (String text : texts) {
      // The test is about what the checker accepts; it must accept all of these.
      assertThat(checker.display(text).notAllowed()).as(text).isEmpty();
      Rendered rendered = render(text);
      assertThat(rendered.thrown()).as(text).isNull();
      assertThat(rendered.components()).as(text).isZero();
    }
    Thread.sleep(1000);
    assertThat(fetched).as("fetched with Rules.%s", name).isEmpty();
  }

  /** The deepest nesting that {@code DEFAULT_MAX_DEPTH} allows can be rendered. */
  @Test
  void testDepth() throws Exception {
    SsHtml checker = SsHtmlChecker.get(Rules.MULTILINE);
    int depth = Rules.DEFAULT_MAX_DEPTH;
    String[] texts = {
      "<html>" + "<table><tr><td>".repeat(depth / 3) + "x",
      "<html>" + "<ul><li>".repeat(depth / 2) + "x",
      "<html>" + "<blockquote>".repeat(depth) + "x",
      "<html>" + "<div><b><font color=red>".repeat(depth / 3) + "x",
    };
    for (String text : texts) {
      assertThat(checker.display(text).notAllowed()).as(text).isEmpty();
      assertThat(render(text).thrown()).as(text).isNull();
    }
    assertThat(checker.display("<html>" + "<blockquote>".repeat(depth + 1) + "x").notAllowed())
        .containsExactly(SsHtmlChecker.REFUSED_DEPTH);
  }
}
// vi: sw=2 ts=8
