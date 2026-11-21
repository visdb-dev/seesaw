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

import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.visdb.seesaw.html.Rules.OnRefusal;
import dev.visdb.seesaw.html.SsHtml.Display;
import dev.visdb.seesaw.html.SsHtml.Report;
import dev.visdb.seesaw.html.SsHtmlChecker.Parsed;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SsHtmlChecker: what it accepts, what it refuses and what it shows then.
 */
public class SsHtmlCheckerTest {
  private static final SsHtml CHECKER = SsHtmlChecker.get(Rules.MULTILINE);
  private static final SsHtml TEXT_ONLY
      = SsHtmlChecker.get(Rules.MULTILINE.withOnRefusal(OnRefusal.TEXT_ONLY));

  private static void assertAsIs(SsHtml ssHtml, String text) {
    Display display = ssHtml.display(text);
    assertThat(display.notAllowed()).as(text).isEmpty();
    assertThat(display.text()).isSameAs(text);
    assertThat(display.renderHtml()).isTrue();
    assertThat(display.hasRisk()).isFalse();
  }

  private static void assertRefused(SsHtml ssHtml, String text, String... notAllowed) {
    Display display = ssHtml.display(text);
    assertThat(display.notAllowed()).as(text).containsExactlyInAnyOrder(notAllowed);
    assertThat(display.text()).isSameAs(text);
    assertThat(display.renderHtml()).isFalse();
    assertThat(display.hasRisk()).isTrue();
  }

  @Test
  void testNotHtml() {
    for (String text : new String[] {null, "", "plain", "a <img src=x> b", " <html><img src=x>",
                                     "<html lang=en><img src=x>"}) {
      assertAsIs(CHECKER, text);
      Report report = CHECKER.inspect(text);
      assertThat(report.isHtml()).isFalse();
      assertThat(report.elements()).contains(Set.of());
      assertThat(report.attributes()).contains(Set.of());
    }
  }

  @Test
  void testAccepted() {
    assertAsIs(CHECKER, "<html>");
    assertAsIs(CHECKER, "<html>just text");
    assertAsIs(CHECKER, "<html><b>Balance</b><br><font color=red size=+1>$10</font></html>");
    assertAsIs(CHECKER, "<HTML><BODY BGCOLOR=white><P ALIGN=center>x<TABLE BORDER=1>"
                        + "<TR><TD COLSPAN=2 width=10>a</TD></TR></TABLE></BODY></HTML>");
    assertAsIs(CHECKER, "<html><head></head><body><ul><li>a<li>b</ul><h1>t</h1><!-- c -->");
    assertAsIs(CHECKER, "<html>a &lt;img src=x&gt; &amp; b");
  }

  @Test
  void testRefused() {
    assertRefused(CHECKER, "<html><img src='http://h/x'>", "img", "img@src");
    assertRefused(CHECKER, "<html><ImG SrC=x>", "img", "img@src");
    assertRefused(CHECKER, "<html><b>ok</b><a href='javascript:x' onclick=y>c</a>",
                  "a", "a@href", "a@onclick");
    assertRefused(CHECKER, "<html><body background='http://h/b'>x", "body@background");
    assertRefused(CHECKER, "<html><table background=x><tr background=y><td>z",
                  "table@background", "tr@background");
    assertRefused(CHECKER, "<html><p style='background-image:url(x)'>x", "p@style");
    assertRefused(CHECKER, "<html><head><link rel=stylesheet href=x><base href=y>"
                           + "<meta http-equiv=refresh content=1><style>p{}</style></head>x",
                  "link", "link@rel", "link@href", "base", "base@href", "meta",
                  "meta@http-equiv", "meta@content", "style");
    assertRefused(CHECKER, "<html><script>alert(1)</script>x", "script");
    assertRefused(CHECKER, "<html><form><input type=password><select><option>a</select>"
                           + "<textarea></textarea></form>",
                  "form", "input", "input@type", "select", "option", "textarea");
    assertRefused(CHECKER, "<html><object classid=x><param name=a value=b></object>",
                  "object", "object@classid", "param", "param@name", "param@value");
    assertRefused(CHECKER, "<html><frameset><frame src=x></frameset>",
                  "frameset", "frame", "frame@src");
    // An element the parser does not know.
    assertRefused(CHECKER, "<html><blink q=1>x</blink>", "blink", "blink@q");
    // A title is refused when written; the one the parser adds to a head is not.
    assertRefused(CHECKER, "<html><head><title>t</title></head>x", "title");
    assertRefused(CHECKER, "<html><blockquote><title>t</title>", "title");
    // Elements that the parser adds are checked.
    assertRefused(SsHtmlChecker.get(Rules.INLINE.withElements("li")), "<html><li>x", "ul");
    // One bad thing spoils good text.
    assertRefused(CHECKER, "<html><b>fine</b><i>fine</i><b onmouseover=x>no</b>",
                  "b@onmouseover");
  }

  /** Attributes with the names of the parser's own markers do not hide an element. */
  @Test
  void testParserMarkers() {
    // The parser drops leading underscores, so this never is the marker.
    assertRefused(CHECKER, "<html><img _implied_ src=x>", "img", "img@implied_", "img@src");
    assertRefused(CHECKER, "<html><img _implied_=true src=x>",
                  "img", "img@implied_", "img@src");
    assertRefused(CHECKER, "<html><input endtag=true>", "input", "input@endtag");
    assertRefused(CHECKER, "<html><b endtag=true>x</b>", "b@endtag");
  }

  @Test
  void testRules() {
    SsHtml inline = SsHtmlChecker.get(Rules.INLINE);
    assertAsIs(inline, "<html><b>a</b><font color=red>b</font>");
    assertRefused(inline, "<html>a<br>b", "br");
    assertRefused(inline, "<html><p align=center>a", "p", "p@align");
    assertRefused(inline, "<html><table><tr><td>a", "table", "tr", "td");

    SsHtml broad = SsHtmlChecker.get(Rules.BROAD);
    assertAsIs(broad, "<html><kbd>a</kbd><ol start=3 type=a><li>x");
    assertRefused(CHECKER, "<html><kbd>a</kbd>", "kbd");

    SsHtml img = SsHtmlChecker.get(Rules.MULTILINE.withElements("img").withAttributes("img@src"));
    assertAsIs(img, "<html><img src=x>");
    // src is allowed on img only.
    assertRefused(img, "<html><img src=x><b src=y>z</b>", "b@src");
  }

  @Test
  void testTextOnly() {
    Display display = TEXT_ONLY.display(
        "<html><head><title>TTL</title><style>p{color:red}</style></head><body>"
        + "A<b>B</b>C<br>D<p>E</p>F<script>var x</script>G &amp; H<img src=x>"
        + "<table><tr><td>I</td><td>J</td></tr></table></body></html>");
    assertThat(display.renderHtml()).isFalse();
    assertThat(display.text()).isEqualTo("ABC D E FG & H I J");
    assertThat(display.notAllowed()).containsExactlyInAnyOrder("title", "style", "script",
                                                               "img", "img@src");

    // What is left may look like HTML; it is still shown literally.
    display = TEXT_ONLY.display("<html><img src=x>&lt;html&gt;&lt;img src=y&gt;");
    assertThat(display.text()).isEqualTo("<html><img src=y>");
    assertThat(display.renderHtml()).isFalse();

    // Accepted text is not touched.
    assertAsIs(TEXT_ONLY, "<html><b>a</b>");
  }

  @Test
  void testLimits() {
    SsHtml small = SsHtmlChecker.get(Rules.MULTILINE.withMaxLength(20).withMaxDepth(3));
    assertAsIs(small, "<html><b>12345</b>");
    assertRefused(small, "<html><b>123456789012</b>", SsHtmlChecker.REFUSED_LENGTH);
    Report report = small.inspect("<html><b>123456789012</b>");
    assertThat(report.isHtml()).isTrue();
    assertThat(report.elements()).isEmpty();
    assertThat(report.attributes()).isEmpty();

    SsHtml shallow = SsHtmlChecker.get(Rules.MULTILINE.withMaxDepth(3));
    assertAsIs(shallow, "<html><body><table><tr><td>x");
    assertAsIs(shallow, "<html><b><i><u>x</u></i></b><b><i><u>y");
    assertRefused(shallow, "<html><table><tr><td><b>x", SsHtmlChecker.REFUSED_DEPTH);

    // Too long with TEXT_ONLY: nothing was parsed, so the original is shown.
    SsHtml smallText = SsHtmlChecker.get(
        Rules.MULTILINE.withMaxLength(20).withOnRefusal(OnRefusal.TEXT_ONLY));
    assertRefused(smallText, "<html><b>123456789012</b>", SsHtmlChecker.REFUSED_LENGTH);

    // Deep nesting is refused, and does not break the parser.
    assertRefused(CHECKER, "<html>" + "<table><tr><td>".repeat(1000) + "x",
                  SsHtmlChecker.REFUSED_DEPTH);
    assertRefused(CHECKER, "<html>" + "<b><i>".repeat(Rules.DEFAULT_MAX_LENGTH / 6 - 2) + "x",
                  SsHtmlChecker.REFUSED_DEPTH);
  }

  @Test
  void testInspect() {
    Report report = CHECKER.inspect(
        "<html><b>x</b><font color=red>y</font><img src=z alt=a><p></p>");
    assertThat(report.isHtml()).isTrue();
    assertThat(report.elements()).contains(Set.of("html", "b", "font", "img", "p"));
    assertThat(report.attributes()).contains(Set.of("font@color", "img@src", "img@alt"));
    assertThat(report.display().notAllowed())
        .containsExactlyInAnyOrder("img", "img@src", "img@alt");
    assertThat(report.display()).isEqualTo(CHECKER.display(
        "<html><b>x</b><font color=red>y</font><img src=z alt=a><p></p>"));

    // Elements that the parser supplies are not reported.
    report = CHECKER.inspect("<html>x");
    assertThat(report.elements()).contains(Set.of("html"));
    assertThat(report.display().hasRisk()).isFalse();
  }

  @Test
  void testRefusedOverride() {
    class Rewriter extends SsHtmlChecker {
      Parsed seen;

      Rewriter() {
        super(Rules.INLINE);
      }

      @Override
      protected Display refused(String text, Parsed parsed) {
        seen = parsed;
        return new Display("<html><i>" + parsed.textContent() + "</i>", true,
                           parsed.notAllowed());
      }
    }

    Rewriter rewriter = new Rewriter();
    assertThat(rewriter.display("<html><b>ok</b>").hasRisk()).isFalse();
    assertThat(rewriter.seen).isNull();

    Display display = rewriter.display("<html><h1 align=left>T</h1><b>ok</b><br><br>more");
    assertThat(display.text()).isEqualTo("<html><i>T ok more</i>");
    assertThat(display.renderHtml()).isTrue();
    assertThat(display.hasRisk()).isTrue();
    assertThat(rewriter.seen.wasParsed()).isTrue();
    assertThat(rewriter.seen.elements()).containsExactlyInAnyOrder("html", "h1", "b", "br");
    assertThat(rewriter.seen.attributes()).containsExactly("h1@align");
    assertThat(rewriter.seen.notAllowed()).containsExactlyInAnyOrder("h1", "h1@align", "br");
    assertThat(rewriter.seen.depth()).isEqualTo(1);
  }

  /** A misbehaving override gets the original shown literally. */
  @Test
  void testRefusedOverrideFails() {
    String text = "<html><img src=x>";
    SsHtml throwing = new SsHtmlChecker(Rules.MULTILINE) {
      @Override
      protected Display refused(String text, Parsed parsed) {
        throw new IllegalStateException("test");
      }
    };
    assertRefused(throwing, text, "img", "img@src");

    SsHtml returnsNull = new SsHtmlChecker(Rules.MULTILINE) {
      @Override
      protected Display refused(String text, Parsed parsed) {
        return null;
      }
    };
    assertRefused(returnsNull, text, "img", "img@src");

    SsHtml saysFine = new SsHtmlChecker(Rules.MULTILINE) {
      @Override
      protected Display refused(String text, Parsed parsed) {
        return Display.asIs(text);
      }
    };
    assertRefused(saysFine, text, "img", "img@src");
  }

  /** Nothing thrown, whatever the text. */
  @Test
  void testGarbage() {
    String[] pieces = {
      "<", ">", "</", "/>", "<!--", "-->", "<![CDATA[", "]]>", "&", "&#", ";", "&lt;", "\"", "'",
      "=", " ", "\n", "\0", "￿", "b", "table", "tr", "td", "img", "src", "style", "script",
      "html", "body", "head", "frameset", "x", "<b>", "</b>", "<table>", "<p", "<?", "<!",
    };
    Random random = new Random(20261116);
    for (SsHtml ssHtml : new SsHtml[] {CHECKER, TEXT_ONLY}) {
      for (int i = 0; i < 3000; i++) {
        StringBuilder sb = new StringBuilder("<html>");
        int n = random.nextInt(40);
        for (int j = 0; j < n; j++) {
          sb.append(pieces[random.nextInt(pieces.length)]);
        }
        String text = sb.toString();
        Display display = ssHtml.display(text);
        assertThat(display).as(text).isNotNull();
        if (display.renderHtml())
          assertThat(display.text()).isSameAs(text);
        assertThat(ssHtml.inspect(text).display()).as(text).isEqualTo(display);
      }
    }
  }
}
// vi: sw=2 ts=8
