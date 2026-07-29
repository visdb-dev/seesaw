/*******************************************************************************
 * Copyright (C) 2003-2021, Prasanth R. Pasala, Brian E. Pangburn, & The Pangburn Group
 * All rights reserved.
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
 *
 * Contributors:
 *   Prasanth R. Pasala
 *   Brian E. Pangburn
 *   Diego Gil
 *   Man "Bee" Vo
 *   Ernie R. Rael
 ******************************************************************************/
/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2024-2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package com.nqadmin.swingset.datasources;

import java.lang.System.Logger;
import java.sql.Array;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.spi.SyncProviderException;
import javax.sql.rowset.spi.SyncResolver;

import com.nqadmin.swingset.datasources.Utils.ConflictRow;
import com.nqadmin.swingset.navigate.RowSetState;
import com.nqadmin.swingset.navigate.UndoRedo;
import com.nqadmin.swingset.utils.JStuff;
import com.nqadmin.swingset.utils.SSComponent;
import com.nqadmin.swingset.utils.SSUtils;

import static com.google.common.collect.Sets.immutableEnumSet;
import static com.nqadmin.swingset.datasources.ConvertType.convertToType;
import static com.nqadmin.swingset.datasources.ConvertType.findJavaTypeClass;
import static com.nqadmin.swingset.datasources.ConvertType.getJDBCType;
import static com.nqadmin.swingset.datasources.DateTime.getSQLDateTimeObject;
import static com.nqadmin.swingset.datasources.JdbcDataTypeConversionTables.jdbcTypeToClass;
import static com.nqadmin.swingset.utils.CentralLookup.defLookup;
import static com.nqadmin.swingset.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * Utility class for working with {@link RowSet}s and {@link ResultSet}s.
 * Some methods for converting to/from text from/to objects according
 * to database type. Several convenience methods for accessing metadata.
 *
 * @since 4.0.0
 */
public class RowSetOps {
  private RowSetOps() {}

  private static final Logger logger = JStuff.getLogger();

  // TODO Audit type handling based on http://www.java2s.com/Code/Java/Database-SQL-JDBC/StandardSQLDataTypeswithTheirJavaEquivalents.htm

  /**
   * Inserts the context of the insert row into this {@linkplain ResultSet}
   * and into the database. Handle CachedRowSet.
   * @param resultSet
   * @throws SQLException
   */
  public static void insertRow(ResultSet resultSet) throws SQLException {
    resultSet.insertRow();
    if (!(resultSet instanceof CachedRowSet crs)) return;

    logger.log(DEBUG, "using CachedRowSet");
    resultSet.moveToCurrentRow();
    try {
      RowSetState.acceptCachedRowSetChanges(crs);
    } catch (SyncProviderException ex) {
      //
      // TODO: test CRS undoInsert after accept changes
      //
      crs.undoInsert();
      throw ex;
    }
  }

  private static class ResetRowPosition {
    SQLException ex;

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    static void doit(ResultSet rs, int targetRow) {
      new ResetRowPosition(rs, targetRow);
    }

    static void doit(ResultSet rs, int targetRow, boolean mayThrow) throws SQLException {
      ResetRowPosition rrp = new ResetRowPosition(rs, targetRow);
      if (mayThrow && rrp.ex != null) throw rrp.ex;
    }

    ResetRowPosition(ResultSet rs, int targetRow) {
      try {
        rs.absolute(targetRow);
      } catch (SQLException ex01) {
        logger.log(ERROR, "resetting row after acceptChanges", ex01);
        this.ex = ex01;
      }
    }
  }

  /**
   * Updates the underlying database with the new contents of the current row
   * of this {@linkplain ResultSet} object. Handle CachedRowSet.
   * @param resultSet
   * @throws SQLException
   */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public static void updateRow(ResultSet resultSet) throws SQLException {
    resultSet.updateRow();
    if (!(resultSet instanceof CachedRowSet crs)) return;

    final int maxTry = 2;
    List<ConflictRow> conflictRows = null;
    SyncProviderException srEx = null;
    int currentRow = resultSet.getRow();

    int tryCount = 1;
    for (; tryCount <= maxTry; ++tryCount) {
      logger.log(DEBUG, sf("CachedRowSet.acceptChanges: try %d", tryCount));
      try {
        RowSetState.acceptCachedRowSetChanges(crs);
        ResetRowPosition.doit(resultSet, currentRow, true);
        break;
      } catch (SyncProviderException syncEx) {
        if (Boolean.TRUE) {
          // Just log and re-throw the exception and
          // try looping to do it again and see how conflicts go.
          // In a real-life implementation, collect the conflicts
          // and imediately re-run commit. Then use the collected
          // conflicts for the UI to pick and chose, then use
          // UI results to resolve.
          List<ConflictRow> cRows = Utils.collectConflictNoThrow(syncEx.getSyncResolver(), crs);
          Utils.dumpConflict((s) -> logger.log(DEBUG, s), cRows);
          Utils.dumpConflict((s) -> System.err.printf("%s\n", s), cRows);
          if (conflictRows == null) {
            conflictRows = cRows;
            continue;
          }

          // The following only guaranteed in controlled/debug situation.
          if (!Objects.equals(conflictRows, cRows))
            throw new IllegalStateException("Conflicts should be equal");

          ResetRowPosition.doit(resultSet, currentRow);
          throw syncEx;
        }
        // ============================================================

        srEx = syncEx;
        //
        // TODO: test CRS undoUpdate after accept changes
        //
        SyncResolver sr = srEx.getSyncResolver();
        //
        // TODO: acceptChanges resolve persist DB
        //		If the CRS value is persisted, then all correct.
        //		If DB values persisted, then still dirty.
        //
        boolean persistDB = Boolean.FALSE;
        Utils.processConflict(System.err, sr, crs, false);

        // HACK TODO: acceptChanges resolve persist DB
        //		There are still issues if the DB was selected.
        //		After the exception, if the user does
        //		cancel row update, then the value reverts
        //		to what's in the CRS, which is probably NOT
        //		what's in the DB. There is no indication that
        //		there's a difference. But maybe it's not really
        //		a problem because if the DB is async changed,
        //		there's no indication.
        //
        //		Could have a "diff" button, or maybe keep
        //		the row as "changed", and then the "diff"
        //		just sends it all back.
        //
        //		Maybe set a visible state/status indicating that
        //		the CRS should be reloaded from the database.
        //

        ResetRowPosition rrp = new ResetRowPosition(resultSet, currentRow);
        if (persistDB) throw new SQLException("These value not persisted");
        if (rrp.ex != null) throw rrp.ex;
        break;
      }
      // if AfterChanges...:w
      // if AfterChanges...:w
    }

    if (tryCount > maxTry && srEx != null) throw srEx;
  }

  /**
   * Deletes the current row from this {@linkplain ResultSet} and from the
   * underlying database. Handle CachedRowSet.
   * @param resultSet
   * @throws SQLException
   */
  public static void deleteRow(ResultSet resultSet) throws SQLException {
    resultSet.deleteRow();
    if (!(resultSet instanceof CachedRowSet crs)) return;

    logger.log(DEBUG, "using CachedRowSet");
    try {
      RowSetState.acceptCachedRowSetChanges(crs);
    } catch (SyncProviderException ex) {
      crs.undoDelete();
      throw ex;
    }
  }

  /**
   * Returns the number of columns in the underlying ResultSet object
   *
   * @param resultSet ResultSet on which to operate
   * @return the number of columns
   * @throws SQLException - if a database access error occurs
   */
  public static int getColumnCount(final ResultSet resultSet) throws SQLException {
    return resultSet.getMetaData().getColumnCount();
  }

  /**
   * Get the designated column's index
   *
   * @param resultSet ResultSet on which to operate
   * @param columnName - name of the column
   *
   * @return returns the corresponding column index (starting from 1)
   *
   * @throws SQLException - if a database access error occurs
   */
  public static int getColumnIndex(final ResultSet resultSet, final String columnName)
      throws SQLException {
    return resultSet.findColumn(columnName);
  }

  /**
   * Returns the column name for the column index provided
   *
   * @param resultSet ResultSet on which to operate
   * @param columnIndex - the column index where the first column is 1, second
   *                     column is 2, etc.
   * @return the column name of the given column index
   *
   * @throws SQLException - if a database access error occurs
   */
  public static String getColumnName(final ResultSet resultSet, final int columnIndex)
      throws SQLException {
    return resultSet.getMetaData().getColumnName(columnIndex);
  }

  /**
   * Determine if the specified column is nullable. If the nullability
   * of the column is unknown, then an empty Optional is returned.
   * @param resultSet RowSet on which to operate
   * @param columnIndex column index
   * @return Optional of true if nullable, empty Optional if unknown.
   */
  public static Optional<Boolean> isNullable(final ResultSet resultSet, final int columnIndex) {
    try {
      int nullable = resultSet.getMetaData().isNullable(columnIndex);
      return nullable == ResultSetMetaData.columnNullableUnknown
          ? Optional.empty()
          : Optional.of(nullable == ResultSetMetaData.columnNullable);
    } catch (SQLException ex) {
      logger.log(ERROR, () -> sf("SQL Exception for column %d.", columnIndex, ex));
      return Optional.empty();
    }
  }

  /**
   * Determine if the specified column is nullable. If the nullability
   * of the column is unknown, then an empty Optional is returned.
   * @param resultSet RowSet on which to operate
   * @param columnName column name
   * @return Optional of true if nullable, empty Optional if unknown.
   */
  public static Optional<Boolean> isNullable(final ResultSet resultSet, final String columnName) {
    try {
      return isNullable(resultSet, getColumnIndex(resultSet, columnName));
    } catch (SQLException ex) {
      logger.log(ERROR, () -> sf("SQL Exception for column %s.", columnName, ex));
      return Optional.empty();
    }
  }

  /**
   * Retrieves an integer corresponding to the designated column's type based on
   * the column index (starting from 1)
   *
   * @see "https://docs.oracle.com/javase/7/docs/api/java/sql/Types.html"
   *
   * @param resultSet ResultSet on which to operate
   * @param columnIndex - the column index where the first column is 1, second
   *                     column is 2, etc.
   * @return SQL type from java.sql.Types
   *
   * @throws SQLException - if a database access error occurs
   */
  public static int getColumnType(final ResultSet resultSet, final int columnIndex)
      throws SQLException {
    return resultSet.getMetaData().getColumnType(columnIndex);
  }

  /**
   * Retrieves JDBCType corresponding to the designated column's type based on
   * the column index (starting from 1)
   *
   * @see java.sql.JDBCType
   *
   * @param resultSet ResultSet on which to operate
   * @param columnIndex - the column index where the first column is 1, second
   *                     column is 2, etc.
   * @return JDBCType of the column
   *
   * @throws SQLException - if a database access error occurs
   */
  public static JDBCType getJDBCColumnType(final ResultSet resultSet, final int columnIndex)
      throws SQLException {
    return getJDBCType(getColumnType(resultSet, columnIndex));
  }

  /**
   * Retrieves an int corresponding to the designated column's type based on the
   * column name
   *
   * @see "https://docs.oracle.com/javase/7/docs/api/java/sql/Types.html"
   *
   * @param columnName - name of the column
   *
   * @param resultSet ResultSet on which to operate
   * @return JDBCType of the column
   *
   * @throws SQLException - if a database access error occurs
   */
  public static int getColumnType(ResultSet resultSet, String columnName) throws SQLException {
    return resultSet.getMetaData().getColumnType(getColumnIndex(resultSet, columnName));
  }

  /**
   * Retrieves an int corresponding to the designated column's type based on the
   * column name
   *
   * @see java.sql.JDBCType
   *
   * @param columnName - name of the column
   *
   * @param resultSet ResultSet on which to operate
   * @return JDBCType of the column
   *
   * @throws SQLException - if a database access error occurs
   */
  public static JDBCType getJDBCColumnType(ResultSet resultSet, String columnName)
      throws SQLException {
    return getJDBCType(getColumnType(resultSet, columnName));
  }

  /**
   * Retrieve the Java Class corresponding to the designated column
   * based on the column name.
   *
   * @param columnName - name of the column
   *
   * @param resultSet ResultSet on which to operate
   * @return
   * @throws SQLException
   */
  public static Class<?> getClassColumnType(final ResultSet resultSet, final String columnName)
      throws SQLException {
    return getClassColumnType(resultSet, getColumnIndex(resultSet, columnName));
  }

  /**
   * Retrieve the Java Class corresponding to the designated column
   * based on the column index (starting from 1).
   *
   * @param columnIndex - index of the column
   *
   * @param resultSet ResultSet on which to operate
   * @return
   * @throws SQLException
   */
  public static Class<?> getClassColumnType(final ResultSet resultSet, final int columnIndex)
      throws SQLException {
    JDBCType type = RowSetOps.getJDBCColumnType(resultSet, columnIndex);
    return findJavaTypeClass(type);
  }

  /**
   * Jdbc types that can be set to an empty string.
   */
  // clang-format off
  public static final Set<JDBCType> textUpdateEmptyOK = immutableEnumSet(
      JDBCType.CHAR,
      JDBCType.VARCHAR,
      JDBCType.LONGVARCHAR,
      
      JDBCType.NCHAR,
      JDBCType.NVARCHAR,
      JDBCType.LONGNVARCHAR
  );
  
  /**
   * Jdbc types that can be set to a non-empty string.
   */
  public static final Set<JDBCType> textUpdateOK = immutableEnumSet(
      JDBCType.INTEGER,
      JDBCType.SMALLINT,
      JDBCType.TINYINT,
      JDBCType.BIGINT,
      JDBCType.FLOAT,
      JDBCType.DOUBLE,
      JDBCType.REAL,
      JDBCType.NUMERIC,
      JDBCType.DECIMAL,
      JDBCType.BOOLEAN,
      JDBCType.BIT,
      JDBCType.DATE,
      JDBCType.TIME,
      JDBCType.TIMESTAMP,
      JDBCType.CHAR,
      JDBCType.VARCHAR,
      JDBCType.LONGVARCHAR,
      
      JDBCType.NCHAR,
      JDBCType.NVARCHAR,
      JDBCType.LONGNVARCHAR
  );
  // clang-format on
  
  /**
   * Fetch the current raw value from the database, the undo/redo stack is
   * not referenced; use columnReader if available.
   * Initial capture for undo/redo uses this method.
   *
   * @param rsc
   * @return
   * @throws SQLException
   */
  public static Object getColumnDirect(RSC rsc) throws SQLException {
    Objects.requireNonNull(rsc);

    if (rsc instanceof SSComponent comp) {
      DbSupport.DbReader<RowSet, Integer, SSComponent> columnReader = comp.getColumnReader();
      if (columnReader != null)
        return comp.getColumnReader().apply(comp.getRowSet(), comp.getColumnIndex(), comp);
    }

    return rsc.getRowSet().getObject(rsc.getColumnIndex());
  }

  ///////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////
  //
  // getColumn methods, fetch data from the current row.
  //

  /**
   * Get the column text from the current undo/redo value.
   * @param comp this components rowset/column text
   * @return
   */
  public static String getColumnObjectText(RSC comp) {
    final RowSet rowSet = comp.getRowSet();
    final String columnName = comp.getColumnName();

    String value = null;
    try {
      // IF THE COLUMN IS NULL SO RETURN NULL
      if (getColumnCount(rowSet) == 0) { return null; }

      Object objectValue = UndoRedo.isUndoRedoEnabled(comp)
                               ? UndoRedo.fetchCurrentChange(comp).value()
                               : comp.getRowSet().getObject(comp.getColumnIndex());
      if (objectValue == null) return null;

      if (objectValue instanceof String s) return s;

      final JDBCType jdbcType = getJDBCType(getColumnType(rowSet, columnName));

      // clang-format off
      switch (jdbcType) {
        // the CHAR... cases already handled, but...
        case INTEGER, SMALLINT, TINYINT, BIGINT,
            REAL, DOUBLE, FLOAT, NUMERIC, DECIMAL,
            BIT, BOOLEAN,
            CHAR, VARCHAR, LONGVARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR ->
          value = objectValue.toString();
        case DATE, TIME, TIMESTAMP -> value = DateTime.getDateTimeText(objectValue, comp);
        default -> // TODO: SSSQLExceptionUnhandledType
          logger.log(ERROR,
              () -> "Unsupported data type of " + jdbcType.getName() + " for column "
                  + columnName + ".");
      } // end switch
      // clang-format on

      //
      // TODO: Convert this to use java.time.LocalDate, LocalTime,
      //		 or LocalDateTime as needed.
      //

    } catch (final SQLException se) {
      logger.log(ERROR, "SQL Exception for column " + columnName + ".", se);
    }

    return value;

  } // end protected String getColumnObjectText(RowSet rs, String _columnName) {

  /**
   * Returns the Object from the rowset's specified column;
   * no object conversion.
   * There is no filtering, for example null conversion.
   * @param comp component
   * @return value
   * @throws java.sql.SQLException
   * @see <a href="https://download.oracle.com/otn-pub/jcp/jdbc-4_3-mrel3-eval-spec/jdbc4.3-fr-spec.pdf">JDBC 4.3 Specification</a> Appendix B-1
   */
  public static Object getColumnObject(RSC comp) throws SQLException {
    return UndoRedo.isUndoRedoEnabled(comp) ? UndoRedo.fetchCurrentChange(comp).value()
                                            : comp.getRowSet().getObject(comp.getColumnIndex());
  }

  //
  // TODO: revisit these ColumnObject methods
  //
  /**
   * Returns an Object of the specified type
   * representing the value in the component's bound database column.
   * This may involve a conversion.
   * <p>
   * Note that if a String type is specified, a null is not automatically
   * turned into "" use getColumnObjectText for that.
   * @param <T> type to return
   * @param comp component
   * @param type Class of returned type
   * @return object
   * @throws java.sql.SQLException
   */
  public static <T> T getColumnObject(RSC comp, Class<T> type) throws SQLException {
    // If there are no columns, return null.
    if (getColumnCount(comp.getRowSet()) == 0) return null;

    if (Boolean.TRUE) {
      //return getColumnObject1(comp, type); // undo/redo,convert
      Object objectValue = getColumnObject(comp);
      return convertToType(objectValue, type);
    } else return RowSetOps_NOT_USED.getColumnObject2(comp, type); // getObject direct
  }

  // /**
  //  * Returns an Object of the specified type
  //  * representing the value in the component's bound database column.
  //  * @param <T> type to return
  //  * @param comp component
  //  * @param type Class of returned type
  //  * @return value
  //  */
  // private static <T> T getColumnObject1(RSC comp, Class<T> type)
  // 		throws SQLException
  // {
  // 	Object objectValue = getColumnObject(comp);
  // 	// Object objectValue = UndoRedo.isUndoRedoEnabled(comp)
  // 	// 		? UndoRedo.fetchCurrentChange(comp).value()
  // 	// 		: comp.getRowSet().getObject(comp.getColumnIndex());
  // 	return convertToType(objectValue, type);
  // }

  /**
   *
   * @param comp
   * @return
   */
  public static Array getColumnArray(SSComponent comp) {
    try {
      if (getColumnCount(comp.getRowSet()) == 0) return null;
      return (UndoRedo.isUndoRedoEnabled(comp) ? (Array) UndoRedo.fetchCurrentChange(comp).value()
                                               : comp.getRowSet().getArray(comp.getColumnIndex()));
    } catch (SQLException ex) {
      logger.log(ERROR, "SQL Exception for column " + comp.getColumnName() + ".", ex);
    }
    return null;
  }

  /**
   * Reads the data from the rowset's specified column
   * using the provided columnReader;
   * no object conversion.
   * There is no filtering, for example null conversion.
   * @param comp component
   * @return value
   * @throws java.sql.SQLException
   * @see <a href="https://download.oracle.com/otn-pub/jcp/jdbc-4_3-mrel3-eval-spec/jdbc4.3-fr-spec.pdf">JDBC 4.3 Specification</a> Appendix B-1
   */
  public static Object getColumn(SSComponent comp) throws SQLException {
    return UndoRedo.isUndoRedoEnabled(comp) ? UndoRedo.fetchCurrentChange(comp).value()
                                            : DbSupport.runDbReader(comp);
  }

  ///////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////
  //
  // update methods, will post Modified if did update
  //

  /**
   * DEBUG ASSIST: Use this to force "n" acceptChanges conflicts after
   * modifying a character column in the database.
   * Only works with CachedRowSet and after
   * {@linkplain #updateColumnText(com.nqadmin.swingset.utils.SSComponent, java.lang.String)}.
   */
  public static class ForceConflict {
    /** Atomic just in case */
    private final AtomicInteger nForce;

    /**
     * Argument is number of conflicts to create.
     * @param n
     */
    public ForceConflict(int n) { nForce = new AtomicInteger(n); }

    /**
     * Argument is number of conflicts to add.
     * @param n
     */
    public void force(int n) {
      if (n < 0) throw new IllegalArgumentException();
      nForce.getAndAdd(n);
    }
    /**
     * Check if conflict should be forced. If this returns true,
     * the nForce counter is decremented.
     * Return true to force a conflict
     */
    boolean doForce() {
      int n = nForce.getAndUpdate((val) -> (val > 0 ? val - 1 : 0));
      return n != 0;
    }
  }

  /**
   * If enabled, modifify the database table associated with comp using a different
   * ResultSet forcing a conflict. The modification is to comp's current row
   * with a different value. Does nothing if not a String column.
   * @param comp
   * @param updatedValue current value for the component
   * @throws SQLException
   */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public static void checkForceConflict(SSComponent comp, String updatedValue) throws SQLException {
    // Only do this for strings.
    if (jdbcTypeToClass(comp.getColumnJDBCType()) != String.class) return;

    ForceConflict fc = defLookup(ForceConflict.class);
    if (fc == null || !fc.doForce()) return;

    // Get a resultSet that is probably the same as the RowSet associated
    // with the param comp.
    Connection conn = SSUtils.dbSupport().getSharedConnection();
    try (Statement statement
         = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
      ResultSet rs = statement.executeQuery(comp.getRowSet().getCommand());
    ) {
      //ResultSet rs = statement.getResultSet();
      rs.absolute(comp.getRowSet().getRow());
      System.err.printf("FORCE_CONFLICT: %s\n", rs.getObject(comp.getColumnIndex()));
      rs.updateString(comp.getColumnIndex(), updatedValue + "_ForceConflict");
      rs.updateRow();
    }
  }

  /**
   * This wraps a value written to the database. In some cases, especially
   * for text columns, before writing a string is converted. DbUpdate
   * holds the actual value, after conversion, written to the database.
   * Also there is a special value, {@link #UPDATE_NULL}, used when
   * {@link RowSet#updateNull(int)} is used to update the database.
   */
  public record DbUpdate(Object value) {}
  /** The actual value written to the database */
  public static DbUpdate UPDATE_NULL = new DbUpdate(JDBCType.NULL);

  /**
   * The String updatedValue is converted to an object and
   * {@link RowSet#updateObject(int, java.lang.Object) }
   * or {@link RowSet#updateNull(int) } is used.
   * <p>
   * When the user changes/edits the SwingSet column this method propagates the
   * change to the RowSet. A separate call is required to flush/commit the change
   * to the database.
   *
   * @param comp The SSComponent doing the update
   * @param updatedValue string to be type-converted as needed and updated in
   *                      underlying RowSet column
   * @return actual item written to the database, throws if nothing written
   * @throws SSSQLNullException thrown if null is not allowed
   * @throws SQLException  thrown if a database error is encountered
   * @throws NumberFormatException thrown if unable to parse a string to number format
   */
  public static DbUpdate updateColumnText(SSComponent comp, String updatedValue)
      throws SSSQLNullException, SQLException, NumberFormatException {
    checkForceConflict(comp, updatedValue); // TODO: This is only for debug

    return updateColumnText(comp, comp.getRowSet(), updatedValue, comp.getColumnIndex(),
                            comp.getAllowNull());
  }

  /**
   * Method used by SwingSet component listeners to update the underlying
   * RowSet.
   * <p>
   * When the user changes/edits the SwingSet column this method propagates the
   * change to the RowSet. A separate call is required to flush/commit the change
   * to the database.
   *
   * @param comp The SSComponent doing the update
   * @param rowSet RowSet on which to operate
   * @param updatedValue string to be type-converted as needed and updated in
   *                      underlying RowSet column
   * @param columnIndex   name of the database column
   * @param allowNull 	indicates if Component and underlying column can contain null values
   * @throws SSSQLNullException thrown if null is not allowed
   * @throws SQLException  thrown if a database error is encountered
   * @throws NumberFormatException thrown if unable to parse a string to number format
   * @see <a href="https://download.oracle.com/otn-pub/jcp/jdbc-4_3-mrel3-eval-spec/jdbc4.3-fr-spec.pdf">JDBC 4.3 Specification</a> Appendix B
   */
  // TODO: test this and conversions
  private static DbUpdate updateColumnText(SSComponent comp, RowSet rowSet, String updatedValue,
                                           int columnIndex, boolean allowNull)
      throws SSSQLNullException, SQLException, NumberFormatException {
    int row = logger.isLoggable(DEBUG) ? rowSet.getRow() : -1;
    logger.log(DEBUG,
               () -> sf("[%s] row %d. Update to: %s. Allow null? [%s]", comp.getColumnForLog(),
                         row, updatedValue, allowNull));

    JDBCType jdbcType = getJDBCType(getColumnType(rowSet, columnIndex));

    if (!textUpdateOK.contains(jdbcType)) {
      // TODO: internal error exception?
      logger.log(ERROR,
                 ()
                     -> "Unsupported data type of " + jdbcType.getName() + " for column "
                            + comp.getColumnForLog() + ".");
      throw new SSSQLUnhandledTypeException(sf("'%s' can't be used as text", jdbcType));
    }

    Object dbValue = null;
    // On insert row, write null if updatedValue is null or empty string,
    // and do not perform other checks.
    // TODO: isBlank???
    if ((updatedValue == null || updatedValue.isEmpty()) && RowSetState.isInserting(rowSet)) {
      rowSet.updateNull(columnIndex);
      return UPDATE_NULL;
    }

    /*
     * FIRST - NULL HANDLING:
     *
     * For character-based columns where _allowNull==true, we write null rather than an empty string
     * We do this because a column could allow null, but have a UNIQUE constraint and each null
     * should be unique.
     *
     * We want to enter this code under 3 conditions:
     *  1. updateColumnText() is passed a null string
     *  2. updateColumnText() is passed an empty string (any column type)
     *  3. updateColumnText() is passed a 'blank' (whitespace) string
     *     for a non-character-based field (e.g., "" or "   " for a double)
     *
     * If !allowNull then a character based field with 0 or more blank spaced
     * will be allowed and code will continue to the switch/case statement below.
     */
    if (updatedValue == null || updatedValue.isEmpty()
        || (updatedValue.isBlank() && !textUpdateEmptyOK.contains(jdbcType))) {
      if (allowNull) {
        rowSet.updateNull(columnIndex);
        return UPDATE_NULL;
      } else if (!textUpdateEmptyOK.contains(jdbcType)) {
        // This will throw an exception for a non-char type, but allow
        // a char-based type with an empty string to continue to the
        // switch/case below and write the empty string via
        // rowSet.updateString(_columnName, _updatedValue)
        //
        // Note that if there is a UNIQUE constraint on such a text
        // column then repeatedly writing the same number (0 to N)
        // spaces should throw an SQL exception
        // (as should any other duplicate string)

        // TODO: Have a method "CreateMessage(RSC) see also
        //		 SSFormattedTextField, SSCommon
        // NOTE: in following should mention column name
        throw new SSSQLNullException("Null values are not allowed for this field.");
      }
    }
    assert (updatedValue != null);

    /*
     * SECOND - update non-null values based on string conversions
     */
    switch (jdbcType) {
      // clang-format off
      case INTEGER, SMALLINT, TINYINT, BIGINT,
          REAL, DOUBLE, FLOAT, DECIMAL, NUMERIC,
          BOOLEAN, BIT,
          CHAR, VARCHAR, LONGVARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR ->
        dbValue = convertToType(updatedValue, jdbcType);
        //dbValue = updatedValue; // Let DB convert.
      // clang-format on
      case DATE, TIME, TIMESTAMP -> // TODO: use convertObjectType when...
        dbValue = getSQLDateTimeObject(updatedValue, comp);
      default ->
        // TODO: SSSQLExceptionUnhandledType
        throw new IllegalStateException("switch cases out of sync");
    } // end switch
    rowSet.updateObject(columnIndex, dbValue);
    return new DbUpdate(dbValue);
  } // end protected void updateColumnText(String _updatedValue, String _columnName)

  /**
   * Method used by SwingSet component listeners to update the underlying
   * RowSet.
   * <p>
   * When the user changes/edits the SwingSet column this method propagates the
   * change to the RowSet. A separate call is required to flush/commit the change
   * to the database.
   *
   * @param comp The SSComponent doing the update
   * @param updatedValue value to write to underlying RowSet column
   * @return actual item written to the database, throws if nothing written
   * @throws SSSQLNullException thrown if null is not allowed
   * @throws SQLException  thrown if a database error is encountered
   */
  public static DbUpdate updateColumnObject(SSComponent comp, Object updatedValue)
      throws SSSQLNullException, SQLException, NumberFormatException {
    if (updatedValue instanceof String s) {
      // This method doesn't have all the string checks,
      // use updateColumnText if String Object.
      return updateColumnText(comp, s);
    }
    final RowSet rowSet = comp.getRowSet();
    final int columnIndex = comp.getColumnIndex();
    boolean allowNull = comp.getAllowNull();
    logger.log(DEBUG,
               () -> comp.getColumnForLog() + " Update to: " + updatedValue + ". Allow null? ["
                   + allowNull + "]");

    Object dbValue;
    // On insert row, write null and do not perform other checks.
    if (updatedValue == null) {
      if (RowSetState.isInserting(rowSet) || allowNull) {
        rowSet.updateNull(columnIndex);
        return UPDATE_NULL;
      } else throw new SSSQLNullException("NULL not allowed for this field.");
    }

    //_rowSet.updateObject(_columnIndex, _updatedValue);
    JDBCType jdbcType = comp.getColumnJDBCType();
    // TODO: Maybe a component field that says use jdbc conversion.
    //		 Better, checkDriverConvertToType(),
    //		 so "obj = convertObjectTypeIfNeeded(...)"
    // TODO: Why isn't updateObject(index, object, type) used anywhere?
    //		 Could always catch SQLFeatureNotSupportedException and do
    //		 manual conversions as a last resort.
    // TODO: It's weird that updateObject(idx,obj,type) javadoc says
    //		 "type to be sent to the database". Does that mean the specified
    //		 conversions for setObject kick in at that point?
    dbValue = convertToType(updatedValue, jdbcType);
    updateColumnObjectDirect(rowSet, columnIndex, dbValue, jdbcType);
    return new DbUpdate(dbValue);
  }

  /**
   * Method used by SwingSet component listeners to update the underlying
   * RowSet.
   * <p>
   * When the user changes/edits the SwingSet column this method propagates the
   * change to the RowSet. A separate call is required to flush/commit the change
   * to the database.
   *
   * @param comp The SSComponent doing the update
   * @param updatedValue Array
   * @return actual item written to the database, throws if nothing written
   * @throws SSSQLNullException thrown if null is not allowed
   * @throws SQLException  thrown if a database error is encountered
   */
  public static DbUpdate updateColumnArray(final SSComponent comp, final Array updatedValue)
      throws SSSQLNullException, SQLException {
    return updateColumnArray(comp, comp.getRowSet(), updatedValue, comp.getColumnName(),
                             comp.getAllowNull());
  }

  /**
   * Method used by SwingSet component listeners to update the underlying
   * RowSet.
   * <p>
   * When the user changes/edits the SwingSet column this method propagates the
   * change to the RowSet. A separate call is required to flush/commit the change
   * to the database.
   *
   * @param comp The SSComponent doing the update
   * @param rowSet RowSet on which to operate
   * @param dbValue Array
   * @param columnName   name of the database column
   * @param allowNull 	indicates if Component and underlying column can contain null values
   * @throws SSSQLNullException thrown if null is not allowed
   * @throws SQLException  thrown if a database error is encountered
   */
  private static DbUpdate updateColumnArray(@SuppressWarnings("unused") SSComponent comp,
                                            RowSet rowSet, Array dbValue, String columnName,
                                            boolean allowNull)
      throws SSSQLNullException, SQLException {
    logger.log(
        DEBUG,
        () -> "[" + columnName + "]. Update to: " + dbValue + ". Allow null? [" + allowNull + "]");

    // On insert row, write null if dbValue is null,
    // and do not perform other checks.
    if (dbValue == null && RowSetState.isInserting(rowSet)) {
      rowSet.updateNull(columnName);
      return UPDATE_NULL;
    }

    if (dbValue == null) {
      if (allowNull) {
        rowSet.updateNull(columnName);
        return UPDATE_NULL;
      } else throw new SSSQLNullException("NULL not allowed for this field.");
    }

    rowSet.updateArray(columnName, dbValue);
    return new DbUpdate(dbValue);
  }

  /**
   * Update the RowSet using {@code columnUpdater}. columnUpdater is
   * expected to do a rowSet.update*.
   *
   * @param comp
   * @param value
   * @return actual item written to the database, throws if nothing written
   * @throws SQLException
   */
  public static DbUpdate updateColumn(SSComponent comp, Object value) throws SQLException {
    DbUpdate dbUpdate = DbSupport.runDbUpdater(comp, value);
    return dbUpdate;
  }

  ///////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////
  //
  // grid stuff
  //

  /**
   * Returns the Object from the rowset's specified column;
   * no object conversion.
   * There is no filtering, for example null conversion.
   * @param comp component
   * @return value
   * @throws java.sql.SQLException
   * @see <a href="https://download.oracle.com/otn-pub/jcp/jdbc-4_3-mrel3-eval-spec/jdbc4.3-fr-spec.pdf">JDBC 4.3 Specification</a> Appendix B-1
   */
  public static Object getColumnObjectDirect(RSC comp) throws SQLException {
    if (Boolean.TRUE) return comp.getRowSet().getObject(comp.getColumnIndex());
    else return RowSetOps_NOT_USED.getColumnObject2(comp);
  }

  // 2024/01/08
  // FOLLOWING ONLY USED FROM SSTableModel (AT LEAST FOR NOW)

  /**
   * Update the Grid's RowSet at the specified column index with the given Object value.
   * RowSet. Operate on the current row.
   * <p>
   * When the user changes/edits the SSDataGrid cell this method propagates the
   * change to the RowSet. A separate call is required to flush/commit the change
   * to the database.
   *
   * @param rowSet RowSet on which to operate
   * @param value updated in underlying RowSet column
   * @param columnIndex   index of the database column
   * @param type NOT USED, the jdbc driver does the conversion
   * @throws SQLException  thrown if a database error is encountered
   */
  private static void updateColumnObjectDirect(RowSet rowSet, int columnIndex, Object value,
                                               JDBCType type) throws SQLException {
    if (Boolean.TRUE) rowSet.updateObject(columnIndex, value);
    else RowSetOps_NOT_USED.updateColumnObject2(rowSet, columnIndex, value, type);
  }

  /**
   * Update the Grid's RowSet at the specified column index with the given Object value.
   * RowSet. Operate on the current row.
   * <p>
   * When the user changes/edits the SSDataGrid cell this method propagates the
   * change to the RowSet. A separate call is required to flush/commit the change
   * to the database.
   *
   * @param rowSet RowSet on which to operate
   * @param value string to be type-converted as needed and updated in
   *                      underlying RowSet column
   * @param columnIndex   index of the database column
   * @throws SQLException  thrown if a database error is encountered
   */
  public static void updateColumnObjectDirect(RowSet rowSet, int columnIndex, Object value)
      throws SQLException {
    if (Boolean.TRUE) rowSet.updateObject(columnIndex, value);
    else
      RowSetOps_NOT_USED.updateColumnObject2(rowSet, columnIndex, value,
                                             getJDBCColumnType(rowSet, columnIndex));
  }
}
