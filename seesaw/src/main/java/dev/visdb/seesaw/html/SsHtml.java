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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.openide.util.lookup.Lookups;

import dev.visdb.seesaw.utils.Globals;

/**
 * Decides how text that may be HTML is displayed.
 * <p>
 * Swing renders text that starts with {@code <html>} as HTML in labels,
 * buttons, tooltips and the default table, list and combo renderers.
 * When the text comes from a database that can fetch URLs, imitate
 * parts of the UI and make {@code setText} throw.
 * An {@code SsHtml} looks at such text and says how to show it;
 * see {@link Display}. It does not know about components.
 * <p>
 * An {@code SsHtml} is immutable and may be shared by any number of
 * components and threads.
 * The implementations supplied are
 * <ul>
 * <li>{@link #pass()}, everything is shown as Swing would show it;
 * <li>{@link #plain()}, text is never rendered as HTML;
 * <li>{@link #checker()} and {@link #checker(Rules)}, HTML that uses only
 * the elements and attributes allowed by a {@link Rules} is shown as is,
 * anything else is shown literally; see {@link SsHtmlChecker}.
 * </ul>
 * The default, {@link #getDefault()}, is the one in {@link Globals}.
 * The library initialization sets it to {@link #checker()} unless the
 * application has set one. For example, to never render HTML
 * {@snippet :
 * Globals.setOption(SsHtml.class, SsHtml.plain());
 * }
 * <p>
 * Implementations register at the lookup path {@link #PATH} so that
 * {@link #getAvailable()} can list the choices. A registered instance is
 * what its no argument constructor makes; a checker with other rules is
 * made with {@link #checker(Rules)}.
 * <p>
 * Text that is not HTML according to {@link #isHtml(String)} is never
 * a concern; every implementation returns it unchanged.
 */
public interface SsHtml {

  /**
   * Lookup path where {@code SsHtml} implementations register, for example
   * {@snippet :
   * @ServiceProvider(path = SsHtml.PATH, service = SsHtml.class)
   * }
   */
  String PATH = "SS/Html";

  /**
   * The registered implementations. This shows what there is to choose
   * from; the library initialization logs them.
   * @return one instance of each registered implementation
   */
  static Collection<? extends SsHtml> getAvailable() {
    return Lookups.forPath(PATH).lookupAll(SsHtml.class);
  }

  /**
   * The test that Swing uses to decide if text is HTML: the first six
   * characters are {@code <html>}, ignoring case.
   * Leading whitespace, or an attribute in the tag, makes it not HTML.
   * @param text the text, may be null
   * @return true if Swing would render the text as HTML
   */
  static boolean isHtml(String text) {
    return text != null && text.length() >= 6 && text.regionMatches(true, 0, "<html>", 0, 6);
  }

  /**
   * The default {@code SsHtml}: the one set in {@link Globals}.
   * The library initialization sets {@link #checker()} if the application
   * has not set one; see {@code LookupDefaults}.
   * @return the default, never null; {@link #checker()} if none is set
   */
  static SsHtml getDefault() {
    SsHtml ssHtml = Globals.getOption(SsHtml.class);
    return ssHtml != null ? ssHtml : checker();
  }

  /**
   * Everything is accepted; text is shown as Swing would show it.
   * @return the pass everything {@code SsHtml}
   */
  static SsHtml pass() {
    return SsHtmlPass.INSTANCE;
  }

  /**
   * HTML is never rendered; text that is HTML is shown literally.
   * @return the plain text {@code SsHtml}
   */
  static SsHtml plain() {
    return SsHtmlPlain.INSTANCE;
  }

  /**
   * A checker that uses {@link Rules#DEFAULT}.
   * @return the default checker
   */
  static SsHtml checker() {
    return SsHtmlChecker.DEFAULT;
  }

  /**
   * A checker that uses the given rules.
   * @param rules what is allowed
   * @return a checker
   */
  static SsHtml checker(Rules rules) {
    return rules.equals(Rules.DEFAULT) ? checker() : new SsHtmlChecker(rules);
  }

  /**
   * A short name for this {@code SsHtml}, for logs and messages;
   * for example {@code PASS}, {@code PLAIN}, {@code CHECKER(DEFAULT)}.
   * It says which kind this is; it need not be unique.
   * @return the name
   */
  String getName();

  /**
   * Decide how the text is displayed.
   * This must be fast for text that is not HTML.
   * @param text the text to display; null is treated as not HTML
   * @return the decision, never null
   * @implSpec An implementation must not throw.
   */
  Display display(String text);

  /**
   * Describe the text. This is for applications and tests;
   * a component only needs {@link #display(String)}.
   * @param text the text to describe; null is treated as not HTML
   * @return the description, never null
   * @implSpec An implementation must not throw.
   */
  Report inspect(String text);

  /**
   * How to display a text.
   * <p>
   * Pass {@link #text()} to {@code setText}. If {@link #renderHtml()}
   * is false the component must also be made to show the text literally,
   * since the text may start with {@code <html>}.
   * The possible outcomes are
   * <table border="1">
   * <caption>Outcomes</caption>
   * <tr><th>Outcome</th><th>text</th><th>renderHtml</th></tr>
   * <tr><td>show as is</td><td>the original</td><td>true</td></tr>
   * <tr><td>show the original literally</td><td>the original</td><td>false</td></tr>
   * <tr><td>show cleaned HTML</td><td>a replacement</td><td>true</td></tr>
   * <tr><td>show stripped text literally</td><td>a replacement</td><td>false</td></tr>
   * </table>
   * <p>
   * When the outcome is not "show as is" then {@link #notAllowed()}
   * says why and is not empty.
   *
   * @param text what to pass to {@code setText}
   * @param renderHtml false means the component must show the text literally
   * @param notAllowed what was refused, as {@code element} or
   *        {@code element@attribute}, or a reason in parentheses such as
   *        {@code (length)}; empty if nothing was refused
   */
  record Display(String text, boolean renderHtml,
                 @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") Set<String> notAllowed) {

    /**
     * Create; a copy of {@code notAllowed} is kept.
     * @throws IllegalArgumentException if {@code renderHtml} is false
     *         and {@code notAllowed} is empty
     */
    @SuppressWarnings("AssignmentToMethodParameter")
    public Display {
      notAllowed = notAllowed.isEmpty()
          ? Set.of() : Collections.unmodifiableSet(new TreeSet<>(notAllowed));
      if (!renderHtml && notAllowed.isEmpty())
        throw new IllegalArgumentException("notAllowed is empty for text shown literally");
    }

    /**
     * The text is fine and is shown as it is.
     * @param text the original text
     * @return the display
     */
    public static Display asIs(String text) {
      return new Display(text, true, Set.of());
    }

    /**
     * True if the original text was not accepted as it was:
     * it is shown literally or it was replaced.
     * @return true if something was refused
     */
    public boolean hasRisk() {
      return !notAllowed.isEmpty();
    }
  }

  /**
   * A description of a text according to an {@code SsHtml}.
   */
  interface Report {
    /**
     * See {@link SsHtml#isHtml(String)}.
     * @return true if Swing would render the text as HTML
     */
    boolean isHtml();

    /**
     * The names of the elements used in the text, in lower case.
     * @return the names, may be an empty set;
     *         an empty Optional if this implementation can not tell
     */
    Optional<Set<String>> elements();

    /**
     * The attributes used in the text, each as {@code element@attribute}
     * in lower case. The attribute values are not included.
     * @return the names, may be an empty set;
     *         an empty Optional if this implementation can not tell
     */
    Optional<Set<String>> attributes();

    /**
     * The same as {@link SsHtml#display(String)} for the text.
     * @return how the text is displayed
     */
    Display display();
  }
}
// vi: sw=2 ts=8
