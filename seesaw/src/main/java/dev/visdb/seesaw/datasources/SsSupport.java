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

import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.FilteredRowSet;
import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.JoinRowSet;
import javax.sql.rowset.WebRowSet;

/**
 * Specifies specific things that are supported by SS.
 * <p>
 * Experimental, this class may go away.
 */
public interface SsSupport {
  /**
   * Does SS handle the specified type of RowSet.
   * @param rs
   * @return
   */
  default boolean isWritableRowSetType(RowSet rs) {
    return switch (rs) {
      case FilteredRowSet _ -> false;
      case JoinRowSet _ -> false;
      case WebRowSet _ -> false;
      case JdbcRowSet _ -> true;
      case CachedRowSet _ -> true;
      default -> false;
    };
  }
}
// vi: sw=2 ts=8
