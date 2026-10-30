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
package dev.visdb.seesaw.datasources;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import dev.visdb.seesaw.utils.SsUtils;

/**
 * Metadata cache. Only static and Driver info, capabilities, and limits should
 * be referenced. Information that is subject to change as the database is
 * running should not be referenced.
 * <p>
 * <b>NOTE:</b> There's only one database, found in SsUtils.dbSupport.getSharedConnection.
 *
 */
public class DbMetaDataCache {
  private DbMetaDataCache() {}

  //
  // The databasemetadata is cached, so only the first call connects to
  // the database. Note: some information is not necessarily safe to cache;
  // google search: "with jdbc is it safe to cache databasemetadata"
  //
  // Make this a selective cache. In particular Static/Driver info,
  // capabilities/Limits. But not tables & Column details.
  // Then no worry about needing to flush in some cases.
  //
  // TODO: this class could provide methods that are "safe", with exactly
  //       the same names as used in DatabaseMetaData, and return this
  //       rather than the metadata itself. Then there would be no chance
  //       of referencing non-cachable data.
  //

  private static DatabaseMetaData metaData;

  // TODO: flush/refresh/enable, other maintenance methods?
  // All accesses to metadata could go through here by adding
  // a "refresh" flag. Is that useful or confusing?

  /**
   * @return the MetaData cache for the default database from getSharedConnection().
   * @throws java.sql.SQLException
   */
  public static DatabaseMetaData get() throws SQLException {
    if (metaData == null) {
      Connection shconn = SsUtils.dbSupport().getSharedConnection();
      if (shconn == null)
        return null;
      metaData = shconn.getMetaData();
    }
    return metaData;
  }
}
// vi: sw=2 ts=8
