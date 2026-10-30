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
import java.sql.JDBCType;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * For H2 metadata DatabaseProductName.
 */
public class H2DbSupport extends DbSupportBase {
  /**
   * For H2.
   * @param sharedConnection
   */
  public H2DbSupport(Connection sharedConnection) {
    super(sharedConnection);
  }

  /**
   * Create a query that contains the row number of a non "order by" query.
   * The {@code H2} {@code DbSupport} uses the the {@code H2} builtin
   * function {@code ROWNUM()}.
   * It returns a string like {@snippet :
   * SELECT part_id, part_name, ROWNUM() AS rown
   * FROM part_data
   * ORDER BY part_name;
   * }
   *
   * @param selectColumns
   * @param rownumberColumn
   * @param tableName
   * @param trailingClause
   * @return
   */
  @Override
  public String createRownumQuery(String selectColumns, String rownumberColumn, String tableName,
                                  String trailingClause) {
    String query = """
                 SELECT {selectColumns}, ROWNUM() AS {rownumberColumn}
                 FROM {tableName}
                 {trailingClause};
                 """.replace("{selectColumns}", selectColumns)
                       .replace("{rownumberColumn}", rownumberColumn)
                       .replace("{tableName}", tableName)
                       .replace("{trailingClause}", trailingClause);
    return query;
  }

  /**
   * H2 specific. Should also work with HSQLDB, DuckDB.
   * <p>
   * Works with typeNames like "INTEGER ARRAY".
   * {@snippet lang="java":
   *     String typeName = rmd.getColumnTypeName(columnIndex);
   *     JDBCType elemtype = JDBCType.valueOf(typeName.split(" ")[0]);
   * }
   */
  @Override
  public JDBCType resolveArrayElementType(ResultSetMetaData rmd, int columnIndex)
      throws SQLException {
    JDBCType columnType = JDBCType.valueOf(rmd.getColumnType(columnIndex));
    if (columnType != JDBCType.ARRAY)
      throw new IllegalArgumentException("Column must be JDBCType.ARRAY, not " + columnType);
    // Assumes first word of column type is element type, eg "INTEGER ARRAY".
    String typeName = rmd.getColumnTypeName(columnIndex);
    JDBCType elemtype = JDBCType.valueOf(typeName.split(" ")[0]);
    return elemtype;
  }
}
// vi: sw=2 ts=8
