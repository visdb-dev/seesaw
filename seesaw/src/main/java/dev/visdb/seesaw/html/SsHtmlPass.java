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

import org.openide.util.lookup.ServiceProvider;

/**
 * An {@link SsHtml} that accepts everything; text is shown as Swing would
 * show it, so HTML is rendered whatever it contains.
 * Use {@link SsHtml#pass()} to get one.
 */
@ServiceProvider(path = SsHtml.PATH, service = SsHtml.class)
public final class SsHtmlPass implements SsHtml {
  static final SsHtmlPass INSTANCE = new SsHtmlPass();

  /** For registration; use {@link SsHtml#pass()}. */
  public SsHtmlPass() {}

  /** @return {@code PASS} */
  @Override
  public String getName() {
    return "PASS";
  }

  /** {@inheritDoc} */
  @Override
  public Display display(String text) {
    return Display.asIs(text);
  }

  /** {@inheritDoc} */
  @Override
  public Report inspect(String text) {
    return new NotParsedReport(SsHtml.isHtml(text), display(text));
  }

  /** All of these are equal. */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof SsHtmlPass;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return SsHtmlPass.class.hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "SsHtml." + getName();
  }
}
// vi: sw=2 ts=8
