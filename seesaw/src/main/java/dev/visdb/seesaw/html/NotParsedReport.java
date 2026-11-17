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

import java.util.Optional;
import java.util.Set;

import dev.visdb.seesaw.html.SsHtml.Display;
import dev.visdb.seesaw.html.SsHtml.Report;

/**
 * The report of an {@link SsHtml} that does not parse the text,
 * so it can not tell which elements are used in HTML.
 *
 * @param isHtml is the text HTML
 * @param display how the text is displayed
 */
record NotParsedReport(boolean isHtml, Display display) implements Report {
  @Override
  public Optional<Set<String>> elements() {
    return isHtml ? Optional.empty() : Optional.of(Set.of());
  }

  @Override
  public Optional<Set<String>> attributes() {
    return isHtml ? Optional.empty() : Optional.of(Set.of());
  }
}
// vi: sw=2 ts=8
