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

import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;

import org.openide.util.lookup.Lookups;

import dev.visdb.seesaw.datasources.DbSupport;
import dev.visdb.seesaw.utils.CentralLookup;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsUtils;

import static java.lang.System.Logger.Level.ERROR;

/**
 * Generate DbSupport instances;
 * Searches for classes that can create them.
 * See {@link DbSupportCreator}.
 */
// Would be nice to have this run automatically. Might need a "using" method
// somewhere that initializes database related stuff.
public class DbSupportFactory {
  private DbSupportFactory() {}
  private static final System.Logger logger = JStuff.getLogger();

  /**
   * Create a DbSupport that works with the specified connection according
   * to its metadata; put it into the CentralLookup.
   * It replaces any
   * {@code DbSupport} that might already be in there.
   * See {@link SsUtils#dbSupport()} for best access method.
   * The connection is used as the shared connection and to
   * examine DatabaseMetaData.
   * @param sharedConnection
   * @return an DbSupport instance that is put into CentralLookup or null.
   */
  public static DbSupport addDbSupportToLookup(Connection sharedConnection) {
    DbSupport dbSupport = createDbSupport(sharedConnection);
    if (dbSupport != null)
      CentralLookup.getDefault().replace(DbSupport.class, dbSupport);
    return dbSupport;
  }

  /**
   * Create a DbSupport that works with the specified connection according
   * to its metadata.
   * The connection is used as the shared connection and to
   * examine DatabaseMetaData.
   * @param sharedConnection
   * @return an DbSupport instance that is put into CentralLookup or null.
   */
  public static DbSupport createDbSupport(Connection sharedConnection) {
    DatabaseMetaData dbMeta;
    String productName;
    try {
      dbMeta = sharedConnection.getMetaData();
      productName = dbMeta.getDatabaseProductName();
    } catch (SQLException ex) {
      logger.log(ERROR, (String) null, ex);
      return null;
    }
    logger.log(Level.INFO, () -> "Creating DbSupport for " + productName);
    Collection<? extends DbSupportCreator> creators
        = Lookups.forPath("SS/DbSupport/" + productName).lookupAll(DbSupportCreator.class);
    if (creators.isEmpty())
      logger.log(ERROR, () -> "No DbSupportCreator for " + productName);

    DbSupport dbSupport = null;
    for (DbSupportCreator creator : creators) {
      if ((dbSupport = creator.create(sharedConnection, dbMeta)) != null)
        break;
    }
    if (dbSupport == null) {
      logger.log(ERROR, () -> "Failed to create DbSupport for " + productName);
      return null;
    }
    return dbSupport;
  }
}
// vi: sw=2 ts=8
