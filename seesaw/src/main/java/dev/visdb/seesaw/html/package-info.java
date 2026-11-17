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

/**
 * Deciding how text that may be HTML is displayed.
 * <p>
 * Swing renders text that starts with {@code <html>} as HTML. For text
 * from a database that is a risk: it can fetch URLs, put components in a
 * label and make {@code setText} throw.
 * {@link dev.visdb.seesaw.html.SsHtml} decides how such text is shown,
 * {@link dev.visdb.seesaw.html.Rules} says what is allowed, and
 * {@link dev.visdb.seesaw.html.SsHtmlChecker} checks text against rules
 * using the JDK's own HTML parser.
 */
package dev.visdb.seesaw.html;
// vi: sw=2 ts=8
