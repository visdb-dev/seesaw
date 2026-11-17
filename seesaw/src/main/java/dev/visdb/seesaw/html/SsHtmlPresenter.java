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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicHTML;

import dev.visdb.seesaw.html.SsHtml.Display;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsUtils;

import static dev.visdb.seesaw.utils.JStuff.sf;

/**
 * Shows text from one source, such as a database column, in components
 * according to an {@link SsHtml}, and logs what the {@code SsHtml} refuses.
 * <p>
 * An {@code SsHtml} only decides; this does the part that involves a
 * component. Before the component's {@code setText} use
 * {@snippet :
 * setText(presenter.prepare(this, text));
 * }
 * A renderer, which is one component used for many cells, does the same
 * for every cell.
 * <p>
 * The {@code SsHtml} used is the one given to {@link #setSsHtml(SsHtml)},
 * or else {@link SsHtml#getDefault()} at the time the text is shown.
 * <p>
 * Each thing that is refused is logged once, as a warning, the first time
 * it is seen from this source; see {@link #MAX_REPORTED}.
 * <p>
 * Use this on the event dispatch thread.
 */
public final class SsHtmlPresenter {
  private static final Logger logger = JStuff.getLogger();

  /**
   * After this many different refused things have been logged for a source
   * there is one more message, and then no more. The names of unknown
   * elements and attributes are whatever is in the text, so there is no
   * end to them.
   */
  public static final int MAX_REPORTED = 20;

  /** Client property: this class set "html.disable" on the component. */
  private static final String DISABLED_HERE = "SsHtmlPresenter.htmlDisabled";

  private static final int LOG_TEXT_LENGTH = 60;

  private final Supplier<String> source;
  private SsHtml ssHtml;
  private Set<String> reported;
  private boolean reportedSuppressed;

  /**
   * Create a presenter.
   * @param source names where the text comes from, for the log;
   *        for example a column name
   */
  public SsHtmlPresenter(Supplier<String> source) {
    this.source = source;
  }

  /**
   * Set the {@code SsHtml} to use for this source.
   * @param ssHtml the {@code SsHtml}; null to use {@link SsHtml#getDefault()}
   */
  public void setSsHtml(SsHtml ssHtml) {
    this.ssHtml = ssHtml;
    reported = null;
    reportedSuppressed = false;
  }

  /**
   * The {@code SsHtml} in use for this source.
   * @return the one that was set, or else {@link SsHtml#getDefault()}
   */
  public SsHtml getSsHtml() {
    return ssHtml != null ? ssHtml : SsHtml.getDefault();
  }

  /**
   * Set up the component for the text and return the text to give to the
   * component's {@code setText}. Call this before each {@code setText}.
   * <p>
   * If the text must be shown literally then Swing's {@code "html.disable"}
   * is set on the component, and it is cleared again when later text may be
   * rendered. If the component already had {@code "html.disable"} set,
   * it is left alone.
   *
   * @param comp the component that will show the text;
   *        see {@link SsUtils#setHtmlEnabled(JComponent, boolean)}
   * @param text the text to show, may be null
   * @return the text for {@code setText}
   */
  public String prepare(JComponent comp, String text) {
    Display display = display(text);
    boolean changed = false;
    if (!display.renderHtml()) {
      if (comp.getClientProperty("html.disable") != Boolean.TRUE) {
        SsUtils.setHtmlEnabled(comp, false);
        comp.putClientProperty(DISABLED_HERE, Boolean.TRUE);
        changed = true;
      }
    } else if (comp.getClientProperty(DISABLED_HERE) != null) {
      SsUtils.setHtmlEnabled(comp, true);
      comp.putClientProperty(DISABLED_HERE, null);
      changed = true;
    }
    // Swing reads "html.disable" only when the text changes. If the
    // component has this text already, setText does nothing; redo the view.
    if (changed && Objects.equals(currentText(comp), display.text()))
      BasicHTML.updateRenderer(comp, display.text());
    return display.text();
  }

  private static String currentText(JComponent comp) {
    return comp instanceof JLabel label ? label.getText()
           : comp instanceof AbstractButton button ? button.getText() : null;
  }

  /**
   * Decide how the text is displayed, and log what is refused.
   * This is {@link SsHtml#display(String)} of the {@code SsHtml} in use,
   * except that if that fails the text is shown literally.
   * @param text the text to show, may be null
   * @return the decision, never null
   */
  public Display display(String text) {
    // Every SsHtml returns text that is not HTML as it is.
    if (!SsHtml.isHtml(text))
      return Display.asIs(text);
    Display display = null;
    SsHtml current = getSsHtml();
    try {
      display = current.display(text);
    } catch (RuntimeException | StackOverflowError ex) {
      logger.log(Level.ERROR, sf("%s: %s failed", source.get(), current.getName()), ex);
    }
    if (display == null)
      display = new Display(text, false, Set.of(SsHtmlChecker.REFUSED_ERROR));
    if (display.hasRisk())
      report(current, text, display);
    return display;
  }

  private void report(SsHtml current, String text, Display display) {
    if (reportedSuppressed)
      return;
    if (reported == null)
      reported = new TreeSet<>();
    Set<String> fresh = new TreeSet<>(display.notAllowed());
    fresh.removeAll(reported);
    if (fresh.isEmpty())
      return;
    reported.addAll(fresh);
    String how = !display.renderHtml() ? "shown literally" : "replaced";
    logger.log(Level.WARNING,
               sf("%s: HTML text %s by %s; not allowed: %s; text starts '%s'",
                  source.get(), how, current.getName(), fresh, logText(text)));
    if (reported.size() >= MAX_REPORTED) {
      reportedSuppressed = true;
      logger.log(Level.WARNING,
                 sf("%s: no more reports of HTML that is not allowed", source.get()));
    }
  }

  /** The start of the text, on one line. */
  private static String logText(String text) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length() && i < LOG_TEXT_LENGTH; i++) {
      char c = text.charAt(i);
      sb.append(Character.isISOControl(c) ? ' ' : c);
    }
    if (text.length() > LOG_TEXT_LENGTH)
      sb.append("...");
    return sb.toString();
  }
}
// vi: sw=2 ts=8
