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

import java.io.StringReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.openide.util.lookup.ServiceProvider;

import dev.visdb.seesaw.html.Rules.OnRefusal;
import dev.visdb.seesaw.utils.JStuff;

/**
 * An {@link SsHtml} that allows HTML which uses only the elements and
 * attributes in a {@link Rules}.
 * <p>
 * Text that passes is shown exactly as it is. Text that does not pass is
 * shown literally: either the whole original or, with
 * {@link OnRefusal#TEXT_ONLY}, only its text content.
 * This class never produces HTML of its own.
 * <p>
 * The text is parsed with the JDK's {@link ParserDelegator}, which is the
 * parser that Swing's label renderer uses. So what is checked here is
 * what the renderer will be given; the two can not read the markup
 * differently.
 * <p>
 * Get a checker with {@link #get(Rules)}. For the {@code Rules} constants
 * that is a shared instance.
 * To do something else with refused text, such as rewriting it,
 * extend this class and override {@link #refused(String, Parsed)}.
 * <p>
 * A checker for each {@code Rules} constant is registered at
 * {@link SsHtml#PATH}: {@link Inline}, {@link Multiline} and {@link Broad}.
 * The registered instance is equal to the one {@code get} returns.
 */
public class SsHtmlChecker implements SsHtml {
  private static final Logger logger = JStuff.getLogger();

  private static final SsHtmlChecker INLINE = new Inline();
  private static final SsHtmlChecker MULTILINE = new Multiline();
  private static final SsHtmlChecker BROAD = new Broad();
  private static final List<SsHtmlChecker> NAMED = List.of(INLINE, MULTILINE, BROAD);

  /** In {@link Display#notAllowed()} when the text is longer than the rules allow. */
  public static final String REFUSED_LENGTH = "(length)";
  /** In {@link Display#notAllowed()} when elements are nested deeper than the rules allow. */
  public static final String REFUSED_DEPTH = "(depth)";
  /** In {@link Display#notAllowed()} when the text could not be parsed. */
  public static final String REFUSED_ERROR = "(error)";

  private final Rules rules;

  /**
   * A checker that uses the given rules.
   * For {@link Rules#INLINE}, {@link Rules#MULTILINE} and {@link Rules#BROAD},
   * or rules equal to one of them, this is a shared instance.
   * @param rules what is allowed
   * @return a checker
   */
  public static SsHtmlChecker get(Rules rules) {
    Objects.requireNonNull(rules, "rules");
    for (SsHtmlChecker checker : NAMED) {
      if (checker.rules.equals(rules))
        return checker;
    }
    return new SsHtmlChecker(rules);
  }

  /** The registered checker for {@link Rules#INLINE}. */
  @ServiceProvider(path = SsHtml.PATH, service = SsHtml.class)
  public static final class Inline extends SsHtmlChecker {
    /** For registration; use {@code SsHtmlChecker.get(Rules.INLINE)}. */
    public Inline() {
      super(Rules.INLINE);
    }
  }

  /** The registered checker for {@link Rules#MULTILINE}. */
  @ServiceProvider(path = SsHtml.PATH, service = SsHtml.class)
  public static final class Multiline extends SsHtmlChecker {
    /** For registration; use {@code SsHtmlChecker.get(Rules.MULTILINE)}. */
    public Multiline() {
      super(Rules.MULTILINE);
    }
  }

  /** The registered checker for {@link Rules#BROAD}. */
  @ServiceProvider(path = SsHtml.PATH, service = SsHtml.class)
  public static final class Broad extends SsHtmlChecker {
    /** For registration; use {@code SsHtmlChecker.get(Rules.BROAD)}. */
    public Broad() {
      super(Rules.BROAD);
    }
  }

  /**
   * Create a checker; for a subclass. Otherwise use {@link #get(Rules)}.
   * @param rules what is allowed
   */
  protected SsHtmlChecker(Rules rules) {
    Objects.requireNonNull(rules, "rules");
    this.rules = rules;
  }

  /**
   * The rules this checker uses.
   * @return the rules
   */
  public final Rules getRules() {
    return rules;
  }

  /**
   * {@code CHECKER} with the rules in parentheses: {@code INLINE},
   * {@code MULTILINE} or {@code BROAD} if the allowed elements and attributes
   * are those of that constant, otherwise {@code custom}; then
   * {@code TEXT_ONLY} if that is what is shown on refusal.
   * For example {@code CHECKER(MULTILINE)}, {@code CHECKER(custom,TEXT_ONLY)}.
   * A subclass that shows text differently should override this.
   */
  @Override
  public String getName() {
    String name = "custom";
    if (sameAllowed(Rules.MULTILINE))
      name = "MULTILINE";
    else if (sameAllowed(Rules.INLINE))
      name = "INLINE";
    else if (sameAllowed(Rules.BROAD))
      name = "BROAD";
    if (rules.onRefusal() != OnRefusal.LITERAL)
      name += "," + rules.onRefusal();
    return "CHECKER(" + name + ")";
  }

  private boolean sameAllowed(Rules other) {
    return rules.elements().equals(other.elements())
           && rules.attributes().equals(other.attributes());
  }

  /** {@inheritDoc} */
  @Override
  public final Display display(String text) {
    if (!SsHtml.isHtml(text))
      return Display.asIs(text);
    return display(text, parse(text));
  }

  /** {@inheritDoc} */
  @Override
  public final Report inspect(String text) {
    boolean isHtml = SsHtml.isHtml(text);
    Parsed parsed = isHtml ? parse(text) : Parsed.EMPTY;
    Display display = isHtml ? display(text, parsed) : Display.asIs(text);
    return new Report() {
      @Override
      public boolean isHtml() {
        return isHtml;
      }

      @Override
      public Optional<Set<String>> elements() {
        return parsed.wasParsed() ? Optional.of(parsed.elements()) : Optional.empty();
      }

      @Override
      public Optional<Set<String>> attributes() {
        return parsed.wasParsed() ? Optional.of(parsed.attributes()) : Optional.empty();
      }

      @Override
      public Display display() {
        return display;
      }
    };
  }

  /**
   * Decide what to show for text that uses something that is not allowed.
   * <p>
   * This implementation follows {@link Rules#onRefusal()}: the original
   * shown literally, or {@link Parsed#textContent()} shown literally.
   * <p>
   * An override may return a replacement to be rendered as HTML.
   * Such a replacement is given to the renderer as returned; this class
   * does not check it. If an override throws, the original is shown literally.
   *
   * @param text the original text; it is HTML
   * @param parsed what was found in the text; {@link Parsed#notAllowed()}
   *        is not empty
   * @return what to show; {@link Display#notAllowed()} must not be empty
   */
  protected Display refused(String text, Parsed parsed) {
    if (rules.onRefusal() == OnRefusal.TEXT_ONLY && parsed.textContent() != null)
      return new Display(parsed.textContent(), false, parsed.notAllowed());
    return new Display(text, false, parsed.notAllowed());
  }

  private Display display(String text, Parsed parsed) {
    if (parsed.notAllowed().isEmpty())
      return Display.asIs(text);
    try {
      Display display = refused(text, parsed);
      if (display != null && display.hasRisk())
        return display;
      logger.log(Level.ERROR, "{0}.refused() returned an invalid Display: {1}",
                 getClass().getName(), display);
    } catch (RuntimeException ex) {
      logger.log(Level.ERROR, getClass().getName() + ".refused() failed", ex);
    }
    return new Display(text, false, parsed.notAllowed());
  }

  /** Never throws. */
  private Parsed parse(String text) {
    if (text.length() > rules.maxLength())
      return Parsed.notParsed(REFUSED_LENGTH);
    Callback callback = new Callback(rules);
    try {
      new ParserDelegator().parse(new StringReader(text), callback, true);
    } catch (Exception | StackOverflowError ex) {
      logger.log(Level.DEBUG, "parse failed", ex);
      return Parsed.notParsed(REFUSED_ERROR);
    }
    return callback.result();
  }

  /** Equal if the same class with equal rules. */
  @Override
  public boolean equals(Object obj) {
    return obj != null && obj.getClass() == getClass()
           && ((SsHtmlChecker) obj).rules.equals(rules);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return rules.hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getName() + "{" + rules + "}";
  }

  /**
   * What a checker found in a text.
   */
  public static final class Parsed {
    private static final Parsed EMPTY = new Parsed(true, Set.of(), Set.of(), Set.of(), "", 0);

    private final boolean wasParsed;
    private final Set<String> elements;
    private final Set<String> attributes;
    private final Set<String> notAllowed;
    private final String textContent;
    private final int depth;

    private Parsed(boolean wasParsed, Set<String> elements, Set<String> attributes,
                   Set<String> notAllowed, String textContent, int depth) {
      this.wasParsed = wasParsed;
      this.elements = Collections.unmodifiableSet(elements);
      this.attributes = Collections.unmodifiableSet(attributes);
      this.notAllowed = Collections.unmodifiableSet(notAllowed);
      this.textContent = textContent;
      this.depth = depth;
    }

    private static Parsed notParsed(String reason) {
      return new Parsed(false, Set.of(), Set.of(), Set.of(reason), null, 0);
    }

    /**
     * False if the text was too long or the parser failed; then the only
     * information is the reason in {@link #notAllowed()}.
     * @return true if the text was parsed
     */
    public boolean wasParsed() {
      return wasParsed;
    }

    /**
     * The elements used, in lower case.
     * Elements that the parser supplies itself are not included here,
     * but they are checked: one that is not allowed is in {@link #notAllowed()}.
     * For example {@code <li>} by itself implies {@code ul}.
     * @return element names
     */
    public Set<String> elements() {
      return elements;
    }

    /**
     * The attributes used, each as {@code element@attribute} in lower case.
     * @return attribute names
     */
    public Set<String> attributes() {
      return attributes;
    }

    /**
     * What the rules do not allow: {@code element}, {@code element@attribute},
     * or one of the reasons {@link #REFUSED_LENGTH}, {@link #REFUSED_DEPTH},
     * {@link #REFUSED_ERROR}.
     * @return names and reasons; empty if the text is acceptable
     */
    public Set<String> notAllowed() {
      return notAllowed;
    }

    /**
     * The text with all markup removed and character entities decoded.
     * Text inside {@code head}, {@code script} and {@code style} is left out.
     * A line break or the edge of a block becomes a single space.
     * <p>
     * This is only safe to show literally; it may itself look like markup.
     * @return the text content; null if the text was not parsed
     */
    public String textContent() {
      return textContent;
    }

    /**
     * The deepest nesting of elements, not counting {@code html},
     * {@code head} and {@code body}.
     * @return the nesting depth
     */
    public int depth() {
      return depth;
    }
  }

  /** Collects what the parser reports. */
  private static final class Callback extends HTMLEditorKit.ParserCallback {
    private final Rules rules;
    private final Set<String> elements = new TreeSet<>();
    private final Set<String> attributes = new TreeSet<>();
    private final Set<String> notAllowed = new TreeSet<>();
    private final StringBuilder text = new StringBuilder();
    private boolean pendingSpace;
    /** Greater than zero while inside head, script or style. */
    private int noText;
    private int depth;
    private int maxDepth;

    Callback(Rules rules) {
      this.rules = rules;
    }

    Parsed result() {
      if (maxDepth > rules.maxDepth())
        notAllowed.add(REFUSED_DEPTH);
      return new Parsed(true, elements, attributes, notAllowed, text.toString().strip(),
                        maxDepth);
    }

    private static boolean hidesText(HTML.Tag tag) {
      return tag == HTML.Tag.HEAD || tag == HTML.Tag.SCRIPT || tag == HTML.Tag.STYLE;
    }

    private static boolean isStructure(String name) {
      return Rules.STRUCTURE.contains(name);
    }

    /**
     * True if the parser supplied the element itself, such as html, head,
     * body and p-implied when the text leaves them out.
     * The marker's key is a String, so an attribute written in the text can
     * have the same key; but then its value is a String, never Boolean.TRUE.
     */
    private static boolean isImplied(MutableAttributeSet attrs) {
      return attrs.getAttribute(IMPLIED) == Boolean.TRUE;
    }

    /** The implied paragraph, and the title the parser adds to a head. */
    private static boolean isHarmlessImplied(HTML.Tag tag) {
      return tag == HTML.Tag.IMPLIED || tag == HTML.Tag.TITLE;
    }

    private void tag(HTML.Tag tag, MutableAttributeSet attrs) {
      if (tag.breaksFlow() || tag == HTML.Tag.BR)
        pendingSpace = true;
      String name = tag.toString().toLowerCase(Locale.ROOT);
      boolean implied = isImplied(attrs);
      // Every element is checked, the implied ones too; the renderer gets them.
      // For example li by itself implies ul.
      if (!implied)
        elements.add(name);
      if (!rules.allowsElement(name) && !(implied && isHarmlessImplied(tag)))
        notAllowed.add(name);
      for (Enumeration<?> e = attrs.getAttributeNames(); e.hasMoreElements();) {
        Object key = e.nextElement();
        if (implied && IMPLIED.equals(key))
          continue;
        // The closing tag of an element the parser does not know arrives
        // as a simple tag with this marker. For a known element it can only
        // be an attribute written in the text, and that is checked.
        if (key == HTML.Attribute.ENDTAG && tag instanceof HTML.UnknownTag)
          continue;
        String attr = key.toString().toLowerCase(Locale.ROOT);
        String full = name + "@" + attr;
        attributes.add(full);
        if (!rules.allowsAttribute(name, attr))
          notAllowed.add(full);
      }
    }

    @Override
    public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
      tag(tag, attrs);
      if (hidesText(tag))
        noText++;
      if (!isStructure(tag.toString())) {
        depth++;
        if (depth > maxDepth)
          maxDepth = depth;
      }
    }

    @Override
    public void handleEndTag(HTML.Tag tag, int pos) {
      if (tag.breaksFlow())
        pendingSpace = true;
      if (hidesText(tag) && noText > 0)
        noText--;
      if (!isStructure(tag.toString()) && depth > 0)
        depth--;
    }

    @Override
    public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
      tag(tag, attrs);
    }

    @Override
    public void handleText(char[] data, int pos) {
      if (noText > 0)
        return;
      if (pendingSpace && !text.isEmpty()
          && !Character.isWhitespace(text.charAt(text.length() - 1)))
        text.append(' ');
      pendingSpace = false;
      text.append(data);
    }
  }
}
// vi: sw=2 ts=8
