/* *****************************************************************************
 * Copyright (C) 2026, Ernie R Rael. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
