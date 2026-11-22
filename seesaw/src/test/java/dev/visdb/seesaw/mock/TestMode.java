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

package dev.visdb.seesaw.mock;

/**
 * Quick or full test runs.
 * <p>
 * The tests that mostly check the JDK, the label renderer and the image
 * decoders, and the fuzz tests, do less in a quick run. A run is full
 * unless the system property {@value #PROP_QUICK} is "true"; CI and an
 * IDE run the full tests. See IDE_SETUP.md for turning quick runs on
 * for a local Maven build.
 */
public final class TestMode {
  /** System property; "true" for a quick run. */
  public static final String PROP_QUICK = "seesaw.test.quick";

  private TestMode() {}

  /**
   * Is this a quick run.
   * @return true if {@value #PROP_QUICK} is "true"
   */
  public static boolean quick() {
    return Boolean.getBoolean(PROP_QUICK);
  }

  /**
   * The first value in a full run, the second in a quick one. What the
   * value means, a count or a step for example, is up to the caller.
   * @param full the value for a full run
   * @param quick the value for a quick run
   * @return full or quick, depending on the run
   */
  public static int fullOrQuick(int full, int quick) {
    return quick() ? quick : full;
  }
}
// vi: sw=2 ts=8
