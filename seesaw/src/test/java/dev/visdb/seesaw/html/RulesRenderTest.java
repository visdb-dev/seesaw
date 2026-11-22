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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.visdb.seesaw.mock.FetchServer;
import dev.visdb.seesaw.mock.TestMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Give the JDK's label renderer everything that the Rules constants allow
 * and check that nothing is fetched, no component is put in the label and
 * nothing is thrown. This is what backs the claim in {@link Rules}; it runs
 * against the JDK the tests run on.
 * <p>
 * Rendering does not depend on the Rules, only the checker does. Each Rules
 * constant includes the one before it, so the texts that are the same for
 * all of them, each element with one attribute and each element inside
 * each other element, are rendered only for {@link Rules#BROAD}. The
 * checker is asked about every text for every Rules.
 * <p>
 * A quick run, see {@link TestMode}, renders an eighth of those texts.
 */
public class RulesRenderTest {
  private static FetchServer server;
  private static String url;

  @BeforeAll
  @SuppressWarnings("unused")
  static void startServer() throws IOException {
    server = FetchServer.start();
    url = server.url();
  }

  @AfterAll
  @SuppressWarnings("unused")
  static void stopServer() throws Exception {
    // In a full run, wait once more for anything loaded in the background.
    List<String> others = server.awaitOthers(1000);
    server.stop();
    assertThat(others).isEmpty();
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

  /** First make sure this test can see the things it claims do not happen. */
  @Test
  void testDetects() throws Exception {
    server.clear();
    render("<html><img src='" + url + "detect.img'>");
    server.awaitAny();
    assertThat(server.fetched()).contains("/detect.img");

    server.clear();
    render("<html><table background='" + url + "detect.bg'><tr><td>x</table>");
    server.awaitAny();
    assertThat(server.fetched()).contains("/detect.bg");

    server.clear();
    render("<html><ul style='list-style-image:url(" + url + "detect.css)'><li>x</ul>");
    server.awaitAny();
    assertThat(server.fetched()).contains("/detect.css");

    assertThat(render("<html><input type=text>").components()).isGreaterThan(0);
    assertThat(render("<html><frameset><frame></frameset>").thrown()).isNotNull();
    server.clear();
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
    // The shared texts are the same as BROAD's, apart from the names in the URLs;
    // make sure of that, so that rendering them only for BROAD loses nothing.
    boolean isBroad = rules.equals(Rules.BROAD);
    if (!isBroad) {
      assertThat(Rules.BROAD.elements()).containsAll(rules.elements());
      assertThat(Rules.BROAD.attributes()).containsAll(rules.attributes());
      for (String element : elements) {
        assertThat(content(element, rules)).as(element)
            .isEqualTo(content(element, Rules.BROAD));
      }
    }
    List<String> shared = new ArrayList<>();
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
        shared.add("<html><" + element + value + ">" + content + "</" + element + ">w");
        all.append(value);
      }
      texts.add("<html><" + element + all + ">" + content + "</" + element + ">w");
      texts.add("<html><head><" + element + all + ">x</" + element + "></head>w");
      // Each element inside each other element.
      for (String inner : elements) {
        shared.add("<html><" + element + ">a<" + inner + ">b</" + inner + ">c</" + element + ">");
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

    // The shared texts are rendered for BROAD; a quick run renders an eighth of them.
    // The checker is asked about all of them.
    int step = TestMode.fullOrQuick(1, 8);
    for (int i = 0; i < shared.size(); i++) {
      String text = shared.get(i);
      if (isBroad && i % step == 0)
        texts.add(text);
      else
        assertThat(checker.display(text).notAllowed()).as(text).isEmpty();
    }
    for (String text : texts) {
      // The test is about what the checker accepts; it must accept all of these.
      assertThat(checker.display(text).notAllowed()).as(text).isEmpty();
      Rendered rendered = render(text);
      assertThat(rendered.thrown()).as(text).isNull();
      assertThat(rendered.components()).as(text).isZero();
    }
    // Fetched is not cleared, so a late fetch is seen by a later check, or in stopServer.
    assertThat(server.awaitOthers(0)).as("fetched with Rules.%s", name).isEmpty();
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
