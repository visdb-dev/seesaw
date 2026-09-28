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
package dev.visdb.seesaw.mock;

import dev.visdb.seesaw.datasources.DbSupport;
import dev.visdb.seesaw.datasources.products.DbSupportBase;
import dev.visdb.seesaw.utils.Globals;

/**
 * x
 */
public class Util {
  /**
   * x
   */
  public static void initLookup() {
    Globals.setOption(DbSupport.class, new DbSupportBase(null));
  }

  private Util() {}
}
// vi: sw=2 ts=8
