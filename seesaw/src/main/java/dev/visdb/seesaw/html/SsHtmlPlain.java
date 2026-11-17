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

import java.util.Set;

import org.openide.util.lookup.ServiceProvider;

/**
 * An {@link SsHtml} that never renders HTML; text that is HTML is shown
 * literally, with the markup visible.
 * Use {@link SsHtml#plain()} to get one.
 */
@ServiceProvider(path = SsHtml.PATH, service = SsHtml.class)
public final class SsHtmlPlain implements SsHtml {
  static final SsHtmlPlain INSTANCE = new SsHtmlPlain();

  /** What this refuses: the html element itself. */
  private static final Set<String> REFUSE_HTML = Set.of("html");

  /** For registration; use {@link SsHtml#plain()}. */
  public SsHtmlPlain() {}

  /** @return {@code PLAIN} */
  @Override
  public String getName() {
    return "PLAIN";
  }

  /** {@inheritDoc} */
  @Override
  public Display display(String text) {
    return SsHtml.isHtml(text) ? new Display(text, false, REFUSE_HTML) : Display.asIs(text);
  }

  /** {@inheritDoc} */
  @Override
  public Report inspect(String text) {
    return new NotParsedReport(SsHtml.isHtml(text), display(text));
  }

  /** All of these are equal. */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof SsHtmlPlain;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return SsHtmlPlain.class.hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "SsHtml." + getName();
  }
}
// vi: sw=2 ts=8
