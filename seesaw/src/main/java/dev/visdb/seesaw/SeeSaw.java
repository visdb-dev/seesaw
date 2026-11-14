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

package dev.visdb.seesaw;

import dev.visdb.seesaw.utils.LookupDefaults;
import dev.visdb.seesaw.utils.SeeSawInitialization;

/**
 * Library initialization.
 */
public final class SeeSaw {
  private SeeSaw() {}

  /**
   * Initialize the library. <b>An application must call this</b>, once,
   * before using the library; typically near the start of {@code main()}.
   * {@snippet lang="java":
   * public static void main(String[] args) {
   *   // Optionally, add application defaults to the lookup first.
   *   SeeSaw.init();
   *   // ...
   * }
   * }
   * It runs all the registered {@link SeeSawInitialization}; there may be
   * any number of them, in the library and in other jars. For example, that
   * is where the JVM system properties used by the library are read. Then
   * it sets up the library defaults which the application has not already
   * set, see {@link LookupDefaults}.
   * <p>
   * The initialization is complete when this returns, with two exceptions:
   * <ul>
   * <li>If it is called from the event dispatch thread, the default text
   *     styles are loaded in the background and may not be available yet.
   *     This is logged as an error; call this from the main thread, before
   *     any GUI is created.
   * <li>If it is called from within the initialization, for example from a
   *     {@code SeeSawInitialization}, it returns immediately; the
   *     initialization that is running continues after that.
   * </ul>
   * Only the first call does the initialization. A call from another
   * thread while the initialization is running waits for it to finish.
   * <p>
   * If the library is used before this is called an error is logged,
   * a dialog is shown, and the initialization is done at that time.
   * The dialog is not shown if the JVM is headless or if
   * {@code -Dseesaw.init.noDialog=true}, see
   * {@link LookupDefaults#PROP_NO_INIT_DIALOG}.
   */
  public static void init() {
    LookupDefaults.init();
  }
}
// vi: sw=2 ts=8
