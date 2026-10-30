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
package dev.visdb.seesaw.navigate;

import dev.visdb.seesaw.datasources.DbOps;

/**
 * An allow* flag changed in DbOps.
 */
@SuppressWarnings("serial")
public class DbOpsChangeEvent extends EventObjectBacktrace {
  private final DbOps.Allow allow;

  /**
   * the DbOps that changed
   * @param source
   * @param allow
   */
  public DbOpsChangeEvent(Object source, DbOps.Allow allow) {
    super(source);
    this.allow = allow;
  }

  /**
   * @return the DbOops that changed.
   */
  public DbOps getDbOps() {
    return (DbOps) getSource();
  }

  /**
   * which permission/allow changed
   *
   * @return
   */
  public DbOps.Allow getAllow() {
    return allow;
  }
}
// vi: sw=2 ts=8
