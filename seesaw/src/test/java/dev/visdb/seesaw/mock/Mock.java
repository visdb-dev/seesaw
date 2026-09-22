/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2024-2026 Ernie Rael.  All Rights Reserved.
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
import javax.sql.rowset.JdbcRowSet;

import org.mockito.Mockito;

/**
 * Some well known mocks for SS.
 */
public class Mock {
  private Mock() {}

  /**
   *	name		age
   *	VARCHAR		INTEGER
   * { "John"		30 }
   * @return rowset
   * @throws java.sql.SQLException
   */
  public static RowSet getRowSet() throws SQLException {
    JdbcRowSet mockRowSet = Mockito.mock(JdbcRowSet.class);
    Mockito.when(mockRowSet.next()).thenReturn(true).thenReturn(false);
    Mockito.when(mockRowSet.getString("name")).thenReturn("John");
    Mockito.when(mockRowSet.getInt("age")).thenReturn(30);
    return mockRowSet;
  }
}
/* https://www.baeldung.com/spring-jdbctemplate-testing
   Shows using H2 or Mock
*/
/*	google search AI
import javax.sql.RowSet;
import javax.sql.rowset.JdbcRowSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;

public class JdbcRowSetTest {

    @Test
    public void testRowSet() throws SQLException {
        // Create a mock JdbcRowSet
        JdbcRowSet mockRowSet = Mockito.mock(JdbcRowSet.class);

        // Set up expectations for the mock
        Mockito.when(mockRowSet.next()).thenReturn(true).thenReturn(false);
        Mockito.when(mockRowSet.getString("name")).thenReturn("John");
        Mockito.when(mockRowSet.getInt("age")).thenReturn(30);

        // Use the mock in your test
        while (mockRowSet.next()) {
            String name = mockRowSet.getString("name");
            int age = mockRowSet.getInt("age");

            // Assert the values
            assertEquals("John", name);
            assertEquals(30, age);
        }
    }
}
 */
// vi: sw=2 ts=8
