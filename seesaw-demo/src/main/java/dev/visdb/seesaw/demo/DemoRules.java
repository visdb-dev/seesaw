/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.demo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import dev.visdb.seesaw.html.Rules;
import dev.visdb.seesaw.html.Rules.OnRefusal;
import dev.visdb.seesaw.html.SsHtml;
import dev.visdb.seesaw.html.SsHtmlChecker;

/**
 * The choices for {@code --rules}: how the demo shows database text that
 * may be HTML. Each name gives the {@link SsHtml} that {@code MainClass.main}
 * puts in {@code Globals}. Most are a checker with some {@link Rules};
 * {@code PASS} and {@code PLAIN} are the other kinds of {@code SsHtml}.
 * <p>
 * To add a choice, add one line to the static block.
 */
final class DemoRules {
  private static final Map<String, Supplier<SsHtml>> CHOICES = new LinkedHashMap<>();

  static {
    add("PASS",        SsHtml::pass);
    add("PLAIN",       SsHtml::plain);
    add("INLINE",      () -> SsHtmlChecker.get(Rules.INLINE));
    add("MULTILINE",   () -> SsHtmlChecker.get(Rules.MULTILINE));
    add("BROAD",       () -> SsHtmlChecker.get(Rules.BROAD));
    // An application that opts in to images.
    add("IMG",         () -> SsHtmlChecker.get(Rules.MULTILINE.withElements("img")
                                 .withAttributes("img@src", "img@width", "img@height")));
    // Only text, no markup. Note that invalid html outputs literally.
    add("PLAIN-TEXT", () -> SsHtmlChecker.get(Rules.of(List.of(), List.of())
        .withOnRefusal(OnRefusal.TEXT_ONLY)));
    // Refused text is shown with the markup removed, instead of literally.
    add("INLINE-TEXT", () -> SsHtmlChecker.get(Rules.INLINE.withOnRefusal(OnRefusal.TEXT_ONLY)));
  }

  private DemoRules() {
  }

  /**
   * Add a choice.
   * @param name the name for {@code --rules}; case does not matter
   * @param ssHtml makes the {@code SsHtml}
   */
  static void add(String name, Supplier<SsHtml> ssHtml) {
    CHOICES.put(name.toUpperCase(Locale.ROOT), ssHtml);
  }

  /**
   * @param name a name from {@link #names()}; case does not matter
   * @return the {@code SsHtml} for the name
   * @throws IllegalArgumentException if there is no such name
   */
  static SsHtml get(String name) {
    Supplier<SsHtml> ssHtml = CHOICES.get(name.toUpperCase(Locale.ROOT));
    if (ssHtml == null)
      throw new IllegalArgumentException("Unknown rules '" + name + "'; known rules: " + names());
    return ssHtml.get();
  }

  /** @return the names, in the order they were added */
  static Set<String> names() {
    return Collections.unmodifiableSet(CHOICES.keySet());
  }
}
// vi: sw=2 ts=8
