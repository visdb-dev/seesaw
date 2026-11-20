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

import dev.visdb.seesaw.SsComboBox1;
import dev.visdb.seesaw.SsComboBox2;
import dev.visdb.seesaw.SsDbComboBox2;
import dev.visdb.seesaw.SsLabel;
import dev.visdb.seesaw.SsList1;
import dev.visdb.seesaw.SsTable;
import dev.visdb.seesaw.html.Rules.OnRefusal;
import dev.visdb.seesaw.html.SsHtml.Display;
import dev.visdb.seesaw.html.SsHtml.Report;
import dev.visdb.seesaw.mock.Util;
import dev.visdb.seesaw.utils.Globals;
import dev.visdb.seesaw.utils.LookupDefaults;
import dev.visdb.seesaw.utils.SsUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
    Util.seesawInit();
    // With no override, the default is the library's choice for the type.
    assertThat(Globals.getOption(SsHtml.class)).isNull();
    JLabel other = new JLabel();
    assertThat(SsHtml.getDefault(null)).isSameAs(LookupDefaults.getSsHtmlDefault(null));
    assertThat(SsHtml.getDefault(other)).isSameAs(LookupDefaults.getSsHtmlDefault(other));
    assertThat(LookupDefaults.getSsHtmlDefault(other))
        .isSameAs(LookupDefaults.getSsHtmlDefault(null)).isNotNull();
    // Each subclass is listed before its super class.
    var types = List.copyOf(LookupDefaults.getSsHtmlDefaults().keySet());
    for (int i = 0; i < types.size(); i++)
      for (int j = i + 1; j < types.size(); j++)
        assertThat(types.get(i).isAssignableFrom(types.get(j)))
            .as("%s before %s", types.get(i), types.get(j)).isFalse();
    // The application's SsHtml in Globals overrides the default for every component.
    try {
      Globals.setOption(SsHtml.class, SsHtml.plain());
      assertThat(SsHtml.getDefault(null)).isSameAs(SsHtml.plain());
      assertThat(SsHtml.getDefault(other)).isSameAs(SsHtml.plain());
    } finally {
      Globals.setOption(SsHtml.class, null);
    }
    assertThat(SsHtml.getDefault(null)).isSameAs(LookupDefaults.getSsHtmlDefault(null));
  }

  /**
   * The library's defaults are as shipped. When they are edited in
   * {@code LookupDefaults} to try something, this is skipped, loudly,
   * instead of failing.
   */
  @Test
  void testShippedDefaults() {
    SsHtml multiline = SsHtmlChecker.get(Rules.MULTILINE);
    var defaults = LookupDefaults.getSsHtmlDefaults();
    List<Class<?>> shippedTypes = List.of(SsLabel.class, SsTable.class, SsList1.class,
                                          SsComboBox1.class, SsDbComboBox2.class,
                                          SsComboBox2.class);
    boolean shipped = List.copyOf(defaults.keySet()).equals(shippedTypes)
                      && defaults.values().stream().allMatch(multiline::equals)
                      && LookupDefaults.getSsHtmlDefault(null).equals(multiline);
    if (!shipped) {
      StringBuilder sb = new StringBuilder();
      defaults.forEach((type, ssHtml) -> sb.append(
          "*   %s: %s%n".formatted(type.getSimpleName(), ssHtml.getName())));
      System.err.printf("""

          ************************************************************
          * SKIPPED SsHtmlTest.testShippedDefaults:
          * LookupDefaults SsHtml defaults are not as shipped
          %s*   others: %s
          ************************************************************
          %n""", sb, LookupDefaults.getSsHtmlDefault(null).getName());
    }
    assumeTrue(shipped, "LookupDefaults SsHtml defaults are not as shipped");
    assertThat(LookupDefaults.getSsHtmlDefault(new JLabel())).isSameAs(multiline);
  }

  @Test
  void testAvailable() {
    assertThat(SsHtml.getAvailable()).extracting(SsHtml::getName)
        .containsExactlyInAnyOrder("PASS", "PLAIN",
                                   "CHECKER(INLINE)", "CHECKER(MULTILINE)", "CHECKER(BROAD)");
    // A registered instance is equal to the one from the static method.
    assertThat(List.<SsHtml>copyOf(SsHtml.getAvailable()))
        .containsExactlyInAnyOrder(SsHtml.pass(), SsHtml.plain(),
                                   SsHtmlChecker.get(Rules.INLINE),
                                   SsHtmlChecker.get(Rules.MULTILINE),
                                   SsHtmlChecker.get(Rules.BROAD));
  }

  @Test
  void testCheckerGet() {
    // The constants get shared instances.
    for (Rules named : new Rules[] {Rules.INLINE, Rules.MULTILINE, Rules.BROAD}) {
      SsHtmlChecker checker = SsHtmlChecker.get(named);
      assertThat(checker.getRules()).isSameAs(named);
      assertThat(SsHtmlChecker.get(named)).isSameAs(checker);
      // Equal rules made another way get the same instance.
      assertThat(SsHtmlChecker.get(Rules.of(named.elements(), named.attributes())))
          .isSameAs(checker);
    }
    assertThat(SsHtmlChecker.get(Rules.INLINE)).isInstanceOf(SsHtmlChecker.Inline.class);
    assertThat(SsHtmlChecker.get(Rules.MULTILINE)).isEqualTo(new SsHtmlChecker.Multiline());
    assertThat(SsHtmlChecker.get(Rules.INLINE)).isNotEqualTo(SsHtmlChecker.get(Rules.MULTILINE));
    // Other rules get a new instance each time.
    Rules other = Rules.MULTILINE.withOnRefusal(OnRefusal.TEXT_ONLY);
    SsHtmlChecker checker = SsHtmlChecker.get(other);
    assertThat(checker.getRules()).isSameAs(other);
    assertThat(SsHtmlChecker.get(other)).isNotSameAs(checker).isEqualTo(checker);
    // A subclass is not equal to the shared instance.
    assertThat(new SsHtmlChecker(Rules.INLINE) { }).isNotEqualTo(SsHtmlChecker.get(Rules.INLINE));
    assertThatThrownBy(() -> SsHtmlChecker.get(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void testName() {
    assertThat(SsHtml.pass().getName()).isEqualTo("PASS");
    assertThat(SsHtml.plain().getName()).isEqualTo("PLAIN");
    assertThat(SsHtmlChecker.get(Rules.MULTILINE).getName()).isEqualTo("CHECKER(MULTILINE)");
    assertThat(SsHtmlChecker.get(Rules.INLINE).getName()).isEqualTo("CHECKER(INLINE)");
    assertThat(SsHtmlChecker.get(Rules.BROAD.withMaxLength(100)).getName())
        .isEqualTo("CHECKER(BROAD)");
    assertThat(SsHtmlChecker.get(Rules.MULTILINE.withOnRefusal(OnRefusal.TEXT_ONLY)).getName())
        .isEqualTo("CHECKER(MULTILINE,TEXT_ONLY)");
    assertThat(SsHtmlChecker.get(Rules.INLINE.withElements("br")).getName())
        .isEqualTo("CHECKER(custom)");
    assertThat(SsHtmlChecker.get(Rules.MULTILINE).toString()).startsWith("CHECKER(MULTILINE){");
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
    assertThat(Rules.MULTILINE.elements()).containsAll(Rules.INLINE.elements());
    assertThat(Rules.MULTILINE.attributes()).containsAll(Rules.INLINE.attributes());
    assertThat(Rules.BROAD.elements()).containsAll(Rules.MULTILINE.elements());
    assertThat(Rules.BROAD.attributes()).containsAll(Rules.MULTILINE.attributes());
    assertThat(Rules.INLINE.elements()).contains("b", "font").doesNotContain("br", "p", "table");
    assertThat(Rules.MULTILINE.elements()).contains("br", "p", "table", "td");

    for (Rules rules : new Rules[] {Rules.INLINE, Rules.MULTILINE, Rules.BROAD}) {
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
    Rules base = Rules.MULTILINE;
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
