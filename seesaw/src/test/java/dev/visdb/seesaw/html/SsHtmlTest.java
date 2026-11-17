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

import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicHTML;

import org.junit.jupiter.api.Test;

import dev.visdb.seesaw.html.Rules.OnRefusal;
import dev.visdb.seesaw.html.SsHtml.Display;
import dev.visdb.seesaw.html.SsHtml.Report;
import dev.visdb.seesaw.mock.Util;
import dev.visdb.seesaw.utils.Globals;
import dev.visdb.seesaw.utils.SsUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SsHtml's static methods, Display, the pass and plain implementations,
 * Rules, and SsUtils.setHtmlEnabled.
 */
public class SsHtmlTest {
  private static final String HTML = "<html><b>bold</b>";

  @Test
  void testIsHtmlSameAsSwing() {
    String[] texts = {
      null, "", "<html", "<html>", "<HTML>", "<HtMl>x", " <html>x", "\n<html>", "<html >x",
      "<html lang=en>x", "<htm1>x", "x<html>", "<html>x</html>", "<body>x", "plain",
    };
    for (String text : texts) {
      assertThat(SsHtml.isHtml(text)).as("'%s'", text).isEqualTo(BasicHTML.isHTMLString(text));
      // And what a label really does with it.
      JLabel label = new JLabel(text);
      assertThat(SsHtml.isHtml(text)).as("'%s'", text)
          .isEqualTo(label.getClientProperty(BasicHTML.propertyKey) != null);
    }
    assertThat(SsHtml.isHtml("<html>")).isTrue();
    assertThat(SsHtml.isHtml(" <html>")).isFalse();
  }

  @Test
  void testPass() {
    SsHtml pass = SsHtml.pass();
    for (String text : new String[] {null, "", "plain", HTML, "<html><img src=x>"}) {
      Display display = pass.display(text);
      assertThat(display.text()).isSameAs(text);
      assertThat(display.renderHtml()).isTrue();
      assertThat(display.hasRisk()).isFalse();
    }
    Report report = pass.inspect(HTML);
    assertThat(report.isHtml()).isTrue();
    assertThat(report.elements()).isEmpty();
    assertThat(report.attributes()).isEmpty();
    assertThat(pass.inspect("plain").elements()).contains(Set.of());
  }

  @Test
  void testPlain() {
    SsHtml plain = SsHtml.plain();
    for (String text : new String[] {null, "", "plain", " <html>x"}) {
      assertThat(plain.display(text)).isEqualTo(Display.asIs(text));
    }
    Display display = plain.display(HTML);
    assertThat(display.text()).isSameAs(HTML);
    assertThat(display.renderHtml()).isFalse();
    assertThat(display.hasRisk()).isTrue();
    assertThat(display.notAllowed()).containsExactly("html");
    assertThat(plain.inspect(HTML).display()).isEqualTo(display);
  }

  @Test
  void testDefault() {
    // The initialization sets the default explicitly.
    Util.seesawInit();
    assertThat(Globals.getOption(SsHtml.class)).isSameAs(SsHtml.checker());
    assertThat(SsHtml.getDefault()).isSameAs(SsHtml.checker());
    try {
      Globals.setOption(SsHtml.class, SsHtml.plain());
      assertThat(SsHtml.getDefault()).isSameAs(SsHtml.plain());
    } finally {
      Globals.setOption(SsHtml.class, SsHtml.checker());
    }
    assertThat(SsHtml.getDefault()).isSameAs(SsHtml.checker());
  }

  @Test
  void testAvailable() {
    assertThat(SsHtml.getAvailable()).extracting(SsHtml::getName)
        .containsExactlyInAnyOrder("PASS", "PLAIN", "CHECKER(DEFAULT)");
    // A registered instance is equal to the one from the static method.
    assertThat(List.<SsHtml>copyOf(SsHtml.getAvailable()))
        .containsExactlyInAnyOrder(SsHtml.pass(), SsHtml.plain(), SsHtml.checker());
    assertThat(SsHtml.checker(Rules.INLINE)).isNotEqualTo(SsHtml.checker());
    assertThat(new SsHtmlChecker(Rules.INLINE)).isEqualTo(SsHtml.checker(Rules.INLINE));
  }

  @Test
  void testCheckerFactory() {
    assertThat(SsHtml.checker()).isInstanceOf(SsHtmlChecker.class);
    assertThat(((SsHtmlChecker) SsHtml.checker()).getRules()).isEqualTo(Rules.DEFAULT);
    assertThat(SsHtml.checker(Rules.DEFAULT)).isSameAs(SsHtml.checker());
    SsHtml inline = SsHtml.checker(Rules.INLINE);
    assertThat(((SsHtmlChecker) inline).getRules()).isSameAs(Rules.INLINE);
  }

  @Test
  void testName() {
    assertThat(SsHtml.pass().getName()).isEqualTo("PASS");
    assertThat(SsHtml.plain().getName()).isEqualTo("PLAIN");
    assertThat(SsHtml.checker().getName()).isEqualTo("CHECKER(DEFAULT)");
    assertThat(SsHtml.checker(Rules.INLINE).getName()).isEqualTo("CHECKER(INLINE)");
    assertThat(SsHtml.checker(Rules.BROAD.withMaxLength(100)).getName())
        .isEqualTo("CHECKER(BROAD)");
    assertThat(SsHtml.checker(Rules.DEFAULT.withOnRefusal(OnRefusal.TEXT_ONLY)).getName())
        .isEqualTo("CHECKER(DEFAULT,TEXT_ONLY)");
    assertThat(SsHtml.checker(Rules.INLINE.withElements("br")).getName())
        .isEqualTo("CHECKER(custom)");
    assertThat(SsHtml.checker().toString()).startsWith("CHECKER(DEFAULT){");
  }

  @Test
  void testDisplay() {
    Display asIs = Display.asIs("x");
    assertThat(asIs.renderHtml()).isTrue();
    assertThat(asIs.notAllowed()).isEmpty();
    assertThat(asIs.hasRisk()).isFalse();

    Display cleaned = new Display("<html>x", true, Set.of("img"));
    assertThat(cleaned.hasRisk()).isTrue();

    // Literal text needs a reason.
    assertThatThrownBy(() -> new Display("x", false, Set.of()))
        .isInstanceOf(IllegalArgumentException.class);

    // The set is copied, sorted and can not be changed.
    Set<String> names = new java.util.HashSet<>(Set.of("p@style", "img", "a@href"));
    Display display = new Display("x", false, names);
    names.clear();
    assertThat(display.notAllowed()).containsExactly("a@href", "img", "p@style");
    assertThatThrownBy(() -> display.notAllowed().add("x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void testRulesSets() {
    assertThat(Rules.DEFAULT.elements()).containsAll(Rules.INLINE.elements());
    assertThat(Rules.DEFAULT.attributes()).containsAll(Rules.INLINE.attributes());
    assertThat(Rules.BROAD.elements()).containsAll(Rules.DEFAULT.elements());
    assertThat(Rules.BROAD.attributes()).containsAll(Rules.DEFAULT.attributes());
    assertThat(Rules.INLINE.elements()).contains("b", "font").doesNotContain("br", "p", "table");
    assertThat(Rules.DEFAULT.elements()).contains("br", "p", "table", "td");

    for (Rules rules : new Rules[] {Rules.INLINE, Rules.DEFAULT, Rules.BROAD}) {
      assertThat(rules.elements())
          .doesNotContain("img", "a", "script", "style", "link", "base", "meta", "object",
                          "applet", "param", "form", "input", "select", "textarea", "frame",
                          "frameset", "noframes");
      assertThat(rules.attributes())
          .doesNotContain("src", "background", "style", "href", "lowsrc", "codebase",
                          "classid", "data", "action");
      assertThat(rules.onRefusal()).isEqualTo(OnRefusal.LITERAL);
      assertThat(rules.maxLength()).isEqualTo(Rules.DEFAULT_MAX_LENGTH);
      assertThat(rules.maxDepth()).isEqualTo(Rules.DEFAULT_MAX_DEPTH);
    }
  }

  @Test
  void testRulesImmutable() {
    Rules base = Rules.DEFAULT;
    Set<String> elements = Set.copyOf(base.elements());
    Set<String> attributes = Set.copyOf(base.attributes());

    Rules changed = base.withElements("IMG", " a ").withAttributes("img@SRC")
        .withoutElements("Table").withoutAttributes("WIDTH")
        .withOnRefusal(OnRefusal.TEXT_ONLY).withMaxLength(10).withMaxDepth(3);
    assertThat(changed.elements()).contains("img", "a").doesNotContain("table");
    assertThat(changed.attributes()).contains("img@src").doesNotContain("width");
    assertThat(changed.onRefusal()).isEqualTo(OnRefusal.TEXT_ONLY);
    assertThat(changed.maxLength()).isEqualTo(10);
    assertThat(changed.maxDepth()).isEqualTo(3);

    // The original is as it was.
    assertThat(base.elements()).isEqualTo(elements);
    assertThat(base.attributes()).isEqualTo(attributes);
    assertThat(base.onRefusal()).isEqualTo(OnRefusal.LITERAL);
    assertThatThrownBy(() -> base.elements().add("img"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> base.attributes().add("src"))
        .isInstanceOf(UnsupportedOperationException.class);

    assertThat(base.withElements("b")).isEqualTo(base);
    assertThatThrownBy(() -> base.withMaxLength(-1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> base.withOnRefusal(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void testRulesAllows() {
    Rules rules = Rules.of(Set.of("B", "td"), Set.of("color", "td@width"));
    assertThat(rules.allowsElement("b")).isTrue();
    assertThat(rules.allowsElement("i")).isFalse();
    // Always allowed.
    assertThat(rules.allowsElement("html")).isTrue();
    assertThat(rules.allowsElement("head")).isTrue();
    assertThat(rules.allowsElement("body")).isTrue();
    assertThat(rules.allowsAttribute("b", "color")).isTrue();
    assertThat(rules.allowsAttribute("td", "width")).isTrue();
    assertThat(rules.allowsAttribute("b", "width")).isFalse();
    assertThat(rules.allowsAttribute("body", "background")).isFalse();
  }

  @Test
  void testSetHtmlEnabled() {
    JLabel label = new JLabel();
    SsUtils.setHtmlEnabled(label, false);
    label.setText(HTML);
    assertThat(label.getClientProperty(BasicHTML.propertyKey)).isNull();

    // Read only when the text changes.
    SsUtils.setHtmlEnabled(label, true);
    assertThat(label.getClientProperty(BasicHTML.propertyKey)).isNull();
    label.setText(HTML + "x");
    assertThat(label.getClientProperty(BasicHTML.propertyKey)).isNotNull();

    SsUtils.setHtmlEnabled(label, false);
    label.setText(HTML);
    assertThat(label.getClientProperty(BasicHTML.propertyKey)).isNull();
  }
}
// vi: sw=2 ts=8
