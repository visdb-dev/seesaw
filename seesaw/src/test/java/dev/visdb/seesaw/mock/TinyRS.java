/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2025-2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.mock;

import java.sql.SQLException;

import javax.sql.RowSet;

/**
 *
 * @author err
 */
public class TinyRS {
  private TinyRS() {}

  public static RowSet getRS1() throws SQLException, ClassNotFoundException {
    RowSet rs = H2.getRowSet("""
			CREATE TABLE tbl1
			( c_pk INTEGER DEFAULT NOT NULL PRIMARY KEY, some_int tinyint);
            INSERT INTO tbl1 VALUES
				(11, 1), (12, 1), (13, 1), (14, 1), (15, 1)
            ;
            """);
    rs.setCommand("SELECT * FROM tbl1");
    return rs;
  }

  public static RowSet getRSEmpty() throws SQLException, ClassNotFoundException {
    RowSet rs = H2.getRowSet("""
			CREATE TABLE tbl1Empty
			( c_pk INTEGER DEFAULT NOT NULL PRIMARY KEY, some_int tinyint);
            """);
    rs.setCommand("SELECT * FROM tbl1Empty");
    return rs;
  }

  public static RowSet getRS1NotNull() throws SQLException, ClassNotFoundException {
    RowSet rs = H2.getRowSet("""
			CREATE TABLE tbl1NN
			( c_pk INTEGER DEFAULT NOT NULL PRIMARY KEY, some_int tinyint not null);
            INSERT INTO tbl1NN VALUES
				(11, 1), (12, 1), (13, 1), (14, 1), (15, 1)
            ;
            """);
    rs.setCommand("SELECT * FROM tbl1NN");
    return rs;
  }

  public static RowSet getRS2() throws SQLException, ClassNotFoundException {
    // This table has a different type for some_int
    RowSet rs = H2.getRowSet("""
			CREATE TABLE tbl2
			( c_pk INTEGER DEFAULT NOT NULL PRIMARY KEY, some_int int);
            INSERT INTO tbl2 VALUES
            	(21, 1), (22, 1), (23, 1), (24, 1), (25, 1), (26, 1)
            ;
            """);
    rs.setCommand("SELECT * FROM tbl2");
    return rs;
  }

  /**
   * x
   * @return
   * @throws SQLException
   * @throws ClassNotFoundException
   */
  public static RowSet getRS1_4() throws SQLException, ClassNotFoundException {
    RowSet rs = H2.getRowSet("""
			CREATE TABLE tbl1
			( c_pk INTEGER DEFAULT NOT NULL PRIMARY KEY, c_tinyint tinyint, c_char varchar);
            INSERT INTO tbl1 VALUES
            	(11, 1, 'a1'), (12, 1, 'b1'), (13, 1, 'c1'), (14, 1, 'd1')
            ;
            """);
    rs.setCommand("SELECT * FROM tbl1");
    return rs;
  }

  /**
   * x
   * @return
   * @throws SQLException
   * @throws ClassNotFoundException
   */
  public static RowSet getRS2_5() throws SQLException, ClassNotFoundException {
    RowSet rs = H2.getRowSet("""
			CREATE TABLE tbl2
			( c_pk INTEGER DEFAULT NOT NULL PRIMARY KEY, c_tinyint tinyint, c_char varchar);
            INSERT INTO tbl2 VALUES
            	(21, 1, 'a2'), (22, 1, 'b2'), (23, 1, 'c2'), (24, 1, 'd2'), (25, 1, 'e2')
            ;
            """);
    rs.setCommand("SELECT * FROM tbl2");
    return rs;
  }
}
// vi: sw=2 ts=8
