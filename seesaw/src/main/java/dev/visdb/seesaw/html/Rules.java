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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * What an {@link SsHtmlChecker} allows in HTML text:
 * a list of elements and a list of attributes, and what to do
 * with text that uses anything else.
 * <p>
 * A {@code Rules} is immutable. The {@code with} methods return a new
 * {@code Rules} and leave the original as it was, so a {@code Rules},
 * and a checker made from it, can be shared.
 * Start from one of the constants
 * {@snippet :
 * Rules.DEFAULT.withOnRefusal(OnRefusal.TEXT_ONLY);
 * Rules.INLINE.withElements("br").withMaxLength(4000);
 * Rules.DEFAULT.withoutElements("table", "tr", "td", "th", "caption");
 * }
 * <p>
 * The three constants each contain the one before:
 * {@link #INLINE}, {@link #DEFAULT}, {@link #BROAD}.
 * None of them allows anything that was seen to fetch a URL, to put a
 * component in the label or to make {@code setText} throw; that is checked
 * by a test against the JDK's renderer. In particular none allows
 * {@code img}, or the attributes {@code src}, {@code background}
 * and {@code style}.
 * A {@code Rules} that adds such things can be made, for example
 * {@code Rules.DEFAULT.withElements("img").withAttributes("img@src")};
 * the application is then responsible for where that text comes from.
 * <p>
 * Names are not case sensitive; they are kept in lower case.
 * The elements {@code html}, {@code head} and {@code body} are always
 * allowed; their attributes are checked like any other.
 * <p>
 * The parser adds elements that the text leaves out, and those must be
 * allowed too. For example {@code <li>} by itself implies a {@code ul},
 * so rules that allow {@code li} should allow {@code ul}.
 *
 * @param elements the allowed element names
 * @param attributes the allowed attributes; a plain name such as
 *        {@code color} is allowed on any allowed element, and
 *        {@code element@name} such as {@code td@width} is allowed
 *        on that element only
 * @param onRefusal what is shown when the text uses something not allowed
 * @param maxLength text longer than this is refused without looking at it
 * @param maxDepth text with elements nested deeper than this is refused;
 *        the renderer fails with a {@code StackOverflowError} on deeply
 *        nested tables and lists
 */
public record Rules(Set<String> elements, Set<String> attributes, OnRefusal onRefusal,
                    int maxLength, int maxDepth) {

  /** What a checker shows when the text uses something that is not allowed. */
  public enum OnRefusal {
    /** The whole original text, so the markup is visible. */
    LITERAL,
    /** The text content with all the markup removed. */
    TEXT_ONLY
  }

  /** The default for {@link #maxLength()}. */
  public static final int DEFAULT_MAX_LENGTH = 32 * 1024;

  /** The default for {@link #maxDepth()}. */
  public static final int DEFAULT_MAX_DEPTH = 32;

  /** These elements are always allowed. */
  static final Set<String> STRUCTURE = Set.of("html", "head", "body");

  /**
   * Character formatting only; meant for table cells, combo and list items
   * and one line labels.
   * <p>
   * Elements: {@code b i u s strike em strong small big sub sup tt code font span}.
   * <br>Attributes: {@code color size face}.
   * <p>
   * There is nothing that starts a new line or changes the layout.
   */
  public static final Rules INLINE = of(
      names("b i u s strike em strong small big sub sup tt code font span"),
      names("color size face"));

  /**
   * Formatted text; meant for labels. This is what {@link SsHtml#checker()} uses.
   * <p>
   * Adds to {@link #INLINE} the elements
   * {@code br p div center pre blockquote hr}, {@code h1} to {@code h6},
   * {@code ul ol li dl dt dd}, {@code table tr td th caption}
   * <br>and the attributes {@code align valign width height border bgcolor
   * colspan rowspan cellpadding cellspacing nowrap}.
   * <p>
   * It is a list of what is commonly useful, not of everything harmless.
   */
  public static final Rules DEFAULT = INLINE
      .withElements(names("br p div center pre blockquote hr h1 h2 h3 h4 h5 h6"
                          + " ul ol li dl dt dd table tr td th caption"))
      .withAttributes(names("align valign width height border bgcolor"
                            + " colspan rowspan cellpadding cellspacing nowrap"));

  /**
   * Nearly everything in which no harm was seen; meant for an application
   * that has existing rich HTML and wants to refuse only the known dangers.
   * <p>
   * Adds to {@link #DEFAULT} the elements
   * {@code address basefont cite dfn dir kbd menu nobr samp var}
   * <br>and the attributes {@code alink clear compact dir halign hspace lang link
   * noshade start text title type value vlink vspace}.
   * <p>
   * Still left out, although harmless by themselves in a label:
   * {@code a script style link base meta form option map area isindex}
   * and the attributes {@code href class id name}.
   * Also left out is {@code title}, which puts a component in the label
   * when it is written outside the {@code head}.
   */
  public static final Rules BROAD = DEFAULT
      .withElements(names("address basefont cite dfn dir kbd menu nobr samp var"))
      .withAttributes(names("alink clear compact dir halign hspace lang link"
                            + " noshade start text title type value vlink vspace"));

  /**
   * Create; the names are copied and converted to lower case.
   * @throws IllegalArgumentException if a limit is negative
   */
  @SuppressWarnings("AssignmentToMethodParameter")
  public Rules {
    elements = normalize(elements);
    attributes = normalize(attributes);
    if (onRefusal == null)
      throw new NullPointerException("onRefusal");
    if (maxLength < 0 || maxDepth < 0)
      throw new IllegalArgumentException("negative limit");
  }

  /**
   * A {@code Rules} that allows only the given elements and attributes,
   * with {@link OnRefusal#LITERAL} and the default limits.
   * @param elements the allowed element names
   * @param attributes the allowed attributes, see {@link Rules}
   * @return the rules
   */
  public static Rules of(Collection<String> elements, Collection<String> attributes) {
    return new Rules(Set.copyOf(elements), Set.copyOf(attributes), OnRefusal.LITERAL,
                     DEFAULT_MAX_LENGTH, DEFAULT_MAX_DEPTH);
  }

  /**
   * Also allow these elements.
   * @param names element names
   * @return a new {@code Rules}
   */
  public Rules withElements(String... names) {
    return withElements(Arrays.asList(names));
  }

  /**
   * Also allow these elements.
   * @param names element names
   * @return a new {@code Rules}
   */
  public Rules withElements(Collection<String> names) {
    return new Rules(plus(elements, names), attributes, onRefusal, maxLength, maxDepth);
  }

  /**
   * Do not allow these elements.
   * @param names element names
   * @return a new {@code Rules}
   */
  public Rules withoutElements(String... names) {
    return new Rules(minus(elements, names), attributes, onRefusal, maxLength, maxDepth);
  }

  /**
   * Also allow these attributes; see {@link Rules} for the two forms.
   * @param names attributes, as {@code name} or {@code element@name}
   * @return a new {@code Rules}
   */
  public Rules withAttributes(String... names) {
    return withAttributes(Arrays.asList(names));
  }

  /**
   * Also allow these attributes; see {@link Rules} for the two forms.
   * @param names attributes, as {@code name} or {@code element@name}
   * @return a new {@code Rules}
   */
  public Rules withAttributes(Collection<String> names) {
    return new Rules(elements, plus(attributes, names), onRefusal, maxLength, maxDepth);
  }

  /**
   * Do not allow these attributes. Each name is removed in the form given;
   * removing {@code width} does not remove {@code td@width}.
   * @param names attributes, as {@code name} or {@code element@name}
   * @return a new {@code Rules}
   */
  public Rules withoutAttributes(String... names) {
    return new Rules(elements, minus(attributes, names), onRefusal, maxLength, maxDepth);
  }

  /**
   * Change what is shown when the text uses something not allowed.
   * @param onRefusal what to show
   * @return a new {@code Rules}
   */
  public Rules withOnRefusal(OnRefusal onRefusal) {
    return new Rules(elements, attributes, onRefusal, maxLength, maxDepth);
  }

  /**
   * Change the longest text that is looked at.
   * @param maxLength number of characters
   * @return a new {@code Rules}
   */
  public Rules withMaxLength(int maxLength) {
    return new Rules(elements, attributes, onRefusal, maxLength, maxDepth);
  }

  /**
   * Change how deep elements may be nested.
   * @param maxDepth nesting depth, not counting {@code html} and {@code body}
   * @return a new {@code Rules}
   */
  public Rules withMaxDepth(int maxDepth) {
    return new Rules(elements, attributes, onRefusal, maxLength, maxDepth);
  }

  /**
   * Is the element allowed.
   * @param element element name, in lower case
   * @return true if allowed
   */
  public boolean allowsElement(String element) {
    return elements.contains(element) || STRUCTURE.contains(element);
  }

  /**
   * Is the attribute allowed on the element.
   * This does not check that the element is allowed.
   * @param element element name, in lower case
   * @param attribute attribute name, in lower case
   * @return true if allowed
   */
  public boolean allowsAttribute(String element, String attribute) {
    return attributes.contains(attribute) || attributes.contains(element + "@" + attribute);
  }

  private static Set<String> names(String names) {
    return Set.of(names.split(" "));
  }

  private static Set<String> normalize(Collection<String> names) {
    Set<String> set = new TreeSet<>();
    for (String name : names) {
      set.add(name.strip().toLowerCase(Locale.ROOT));
    }
    return Collections.unmodifiableSet(set);
  }

  private static Set<String> plus(Set<String> set, Collection<String> names) {
    Set<String> result = new TreeSet<>(set);
    result.addAll(names);
    return result;
  }

  private static Set<String> minus(Set<String> set, String... names) {
    Set<String> result = new TreeSet<>(set);
    for (String name : names) {
      result.remove(name.strip().toLowerCase(Locale.ROOT));
    }
    return result;
  }
}
// vi: sw=2 ts=8
