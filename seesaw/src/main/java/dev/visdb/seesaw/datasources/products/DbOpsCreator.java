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

import javax.sql.RowSet;

import dev.visdb.seesaw.datasources.DbOps;
import dev.visdb.seesaw.navigate.RowsModel;

/**
 * Stash one of these into lookup to create a {@link DbOps}. Only used if one
 * is not provided to
 * {@link RowsModel#create(javax.sql.RowSet, dev.visdb.seesaw.datasources.DbOps)
 * RowsModel.create(RowSet, DbOps)}
 * or
 * {@link RowsModel#setRowSet(javax.sql.RowSet, dev.visdb.seesaw.datasources.DbOps)
 * rowsModel.setRowSet(RowSet, DbOps)}
 */
public interface DbOpsCreator {
  /**
   * Return a {@link DbOps} that works with the specified RowSet.
   * Typically called when a RowSet is assigned to a RowsModel.
   *
   * @param rs needing a DbOps
   * @param rowsModel before the RowSet is assigned, null if not yet created.
   * @return DbOps may be null and caller should keep looking.
   */
  DbOps create(RowSet rs, RowsModel rowsModel);
}
// vi: sw=2 ts=8
