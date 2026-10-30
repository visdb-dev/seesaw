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
package dev.visdb.seesaw.datasources.products;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import org.openide.util.lookup.ServiceProvider;

import dev.visdb.seesaw.datasources.DbSupport;

/**
 * For H2.
 */
@ServiceProvider(path = "SS/DbSupport/H2", service = DbSupportCreator.class)
public class H2DbSupportCreator implements DbSupportCreator {
  /**
   * Construct DbSupport for H2. MetaData doesn't matter.
   * @param sharedConnection
   * @param dbMeta
   * @return DbSupport
   */
  @Override
  public DbSupport create(Connection sharedConnection, DatabaseMetaData dbMeta) {
    return new H2DbSupport(sharedConnection);
  }
}
// vi: sw=2 ts=8
