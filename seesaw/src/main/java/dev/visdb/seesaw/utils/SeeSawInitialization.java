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

package dev.visdb.seesaw.utils;

import dev.visdb.seesaw.SeeSaw;

/**
 * Initialization that is run once from {@link SeeSaw#init() }, before the
 * library's own defaults are set up. Typically used to read JVM properties
 * and do other initialization during startup. Any number of these may be
 * registered; they are all run. Register an implementation with
 * {@snippet lang="java":
 * @ServiceProvider(service = SeeSawInitialization.class, path = LookupDefaults.INIT_PATH)
 * public static class Init implements SeeSawInitialization {
 *   @Override
 *   public void init() { ... }
 * }
 * }
 * The implementation must be public and have a public no argument constructor.
 * If the order of initialization matters use the annotation's {@code position}.
 * <p>
 * <b>Keep {@code init()} simple.</b> It should only read properties, or
 * similar, and set fields of its own class. It runs while the library
 * initialization holds a lock, and may be running from the static
 * initializer of a library class. Anything more, in particular using
 * {@code RowsModel} or a SeeSaw component, risks seeing a class which is not
 * completely initialized, or a deadlock with a thread doing first use of
 * that class.
 * <p>
 * An exception thrown by {@code init()} is logged; it does not stop the
 * other initialization. The state the implementation sets up should have
 * a safe default for the case where {@code init()} has not run yet.
 *
 * @see LookupDefaults#INIT_PATH
 */
public interface SeeSawInitialization {
  /**
   * Do the initialization. Invoked once.
   */
  public void init();
}
// vi: sw=2 ts=8
