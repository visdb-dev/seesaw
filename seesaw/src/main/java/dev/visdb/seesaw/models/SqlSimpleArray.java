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
package dev.visdb.seesaw.models;

import java.sql.Array;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of JDBC array that holds data; most methods unsupported.
 * Useful to update an array in a Table.
 * Array data is not copied/safe.
 */
public class SqlSimpleArray implements Array {
  /**
   * Underlying database type name for array elements
   */
  final private JDBCType baseType;
  
  /**
   * an array containing elements for an sql array
   */
  private Object data;
  
  /**
   * Creates SsJDBCArray with the object array and data base type.
   * <em>The data is not copied</em>; the caller should not modify the array.
   *
   * @param data     object array of SsJDBCArray
   * @param baseType Array elements database type
   */
  public SqlSimpleArray(Object data, JDBCType baseType) {
    Objects.requireNonNull(data);
    if (!data.getClass().isArray())
      throw new IllegalArgumentException("Must be an array");
    this.data = data;
    this.baseType = baseType;
  }
  
  /** {@inheritDoc } */
  @Override
  public void free() throws SQLException {
    data = null;
  }
  
  /** {@inheritDoc } */
  @Override
  public Object getArray() throws SQLException {
    return data;
  }
  
  /**
   * throws UnsupportedOperationException
   * {@inheritDoc }
   */
  @Override
  public Object getArray(long index, int count) throws SQLException {
    throw new UnsupportedOperationException();
  }
  
  /**
   * throws UnsupportedOperationException
   * {@inheritDoc }
   */
  @Override
  public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
    throw new UnsupportedOperationException();
  }
  
  /**
   * throws UnsupportedOperationException
   * {@inheritDoc }
   */
  @Override
  public Object getArray(Map<String, Class<?>> map) throws SQLException {
    throw new UnsupportedOperationException();
  }
  
  /** {@inheritDoc } */
  @Override
  public int getBaseType() throws SQLException {
    return baseType.getVendorTypeNumber();
  }
  
  /** {@inheritDoc } */
  @Override
  public String getBaseTypeName() throws SQLException {
    return baseType.getName();
  }
  
  /**
   * throws UnsupportedOperationException
   * {@inheritDoc }
   */
  @Override
  public ResultSet getResultSet() throws SQLException {
    throw new UnsupportedOperationException();
  }
  
  /**
   * throws UnsupportedOperationException
   * {@inheritDoc }
   */
  @Override
  public ResultSet getResultSet(long index, int count) throws SQLException {
    throw new UnsupportedOperationException();
  }
  
  /**
   * throws UnsupportedOperationException
   * {@inheritDoc }
   */
  @Override
  public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map)
      throws SQLException {
    throw new UnsupportedOperationException();
  }
  
  /**
   * throws UnsupportedOperationException
   * {@inheritDoc }
   */
  @Override
  public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a string value with comma separated values. e.g. "{100,200,300}"
   * @return
   */
  @Override
  public String toString() {
    String text = "SqlSimpleArray " + Arrays.asList(data);
    return text;
  }
}
// vi: sw=2 ts=8
