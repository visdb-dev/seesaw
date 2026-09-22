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

import dev.visdb.seesaw.datasources.DbSupport;

/**
 * Should be at least one of these for a supported database. It creates a
 * {@link DbSupport} that works with the database specified by the
 * DatabaseMetaData.
 * These are placed in {@code META-INF} using an annotation like
 * {@snippet lang="java":
 * @ServiceProvider(path="SS/DbSupport/H2", service=DbSupportCreator.class)
 * }
 * Note in this example that {@code dbMeta.getDatabaseProductName().equals("H2")}.
 * <p>
 * Any {@code DbSupportCreator}s found are tried in order.
 * The first creator that matches the metadata returns non null, the search
 * is terminated and that value is returned.
 * Normally {@code position} is not used with the annotation, but if there are
 * multiple creators that match, it's use might be needed.
 * <a href="https://bits.netbeans.org/dev/javadoc/org-openide-util-lookup/org/openide/util/lookup/ServiceProvider.html">
 * See @ServiceProvider javadoc</a>.
 */
public interface DbSupportCreator {
  /**
   * Create a DbSupport for the connection. The DatabaseMetaData belongs
   * to the connection; use to refer to dbMeta productVersion or ...
   * If this creator can't construct a DbSupport for the meta data return null;
   *
   * @param sharedConnection
   * @param dbMeta
   * @return may be null
   */
  DbSupport create(Connection sharedConnection, DatabaseMetaData dbMeta);
}
// vi: sw=2 ts=8
