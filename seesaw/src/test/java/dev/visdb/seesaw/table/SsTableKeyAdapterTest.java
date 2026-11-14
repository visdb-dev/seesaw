/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package dev.visdb.seesaw.table;

import java.awt.EventQueue;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.RowSet;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.visdb.seesaw.SsTable;
import dev.visdb.seesaw.datasources.products.DbSupportFactory;
import dev.visdb.seesaw.mock.H2;
import dev.visdb.seesaw.mock.TestLogging;
import dev.visdb.seesaw.mock.Util;
import dev.visdb.seesaw.navigate.RowsModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Copy and paste in SsTableKeyAdapter, and the SsTableModel methods it uses.
 * The clipboard is not used; the tests work headless.
 */
public class SsTableKeyAdapterTest {

  @BeforeAll
  public static void setUpClass() throws ClassNotFoundException, SQLException {
    Util.seesawInit();
    TestLogging.load();
    DbSupportFactory.setDbSupportGlobalOption(H2.getCon());
  }

  /** Key adapter that records messages and answers questions. */
  private static class TestAdapter extends SsTableKeyAdapter {
    final List<String> messages = new ArrayList<>();
    final List<String> questions = new ArrayList<>();
    boolean answer = true;

    TestAdapter(JTable jTable) {
      super(jTable);
    }

    @Override
    protected void showMessage(JTable jTable, String message) {
      messages.add(message);
    }

    @Override
    protected boolean confirm(JTable jTable, String message, String title) {
      questions.add(message);
      return answer;
    }
  }

  private static void select(JTable table, int row, int column) {
    table.setRowSelectionInterval(row, row);
    table.setColumnSelectionInterval(column, column);
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // parseClipboard
  //

  @Test
  void testParseKeepsEmptyCells() {
    List<String[]> rows = SsTableKeyAdapter.parseClipboard("a\t\tc\n\tb\t\n");
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0)).containsExactly("a", "", "c");
    assertThat(rows.get(1)).containsExactly("", "b", "");
  }

  @Test
  void testParseLines() {
    // A blank line is a row; the final line terminator is not.
    List<String[]> rows = SsTableKeyAdapter.parseClipboard("a\r\n\r\nc\r\n");
    assertThat(rows).hasSize(3);
    assertThat(rows.get(0)).containsExactly("a");
    assertThat(rows.get(1)).containsExactly("");
    assertThat(rows.get(2)).containsExactly("c");

    assertThat(SsTableKeyAdapter.parseClipboard("")).isEmpty();
    assertThat(SsTableKeyAdapter.parseClipboard("x")).hasSize(1);
    // A single empty cell.
    assertThat(SsTableKeyAdapter.parseClipboard("\n")).hasSize(1);
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // A plain JTable, the second column is not editable.
  //

  private static JTable plainTable() {
    DefaultTableModel model = new DefaultTableModel(
        new Object[][] {{"a0", "b0", "c0"}, {"a1", "b1", "c1"}, {"a2", "b2", "c2"}},
        new Object[] {"A", "B", "C"}) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return column != 1;
      }
    };
    return new JTable(model);
  }

  @Test
  void testPlainCopy() {
    JTable table = plainTable();
    TestAdapter adapter = new TestAdapter(table);
    table.setValueAt(null, 0, 1);
    table.setRowSelectionInterval(0, 1);
    table.setColumnSelectionInterval(0, 2);
    assertThat(adapter.copyText(table)).isEqualTo("a0\t\tc0\na1\tb1\tc1\n");
  }

  @Test
  void testPlainPasteNotEditableYes() {
    JTable table = plainTable();
    TestAdapter adapter = new TestAdapter(table);
    select(table, 0, 0);
    adapter.paste(table, "x0\ty0\tz0\nx1\ty1\tz1\n");

    assertThat(adapter.questions).hasSize(1);
    assertThat(adapter.questions.get(0)).contains("2 of 6");
    assertThat(adapter.messages).isEmpty();
    assertThat(table.getValueAt(0, 0)).isEqualTo("x0");
    assertThat(table.getValueAt(0, 1)).isEqualTo("b0");
    assertThat(table.getValueAt(0, 2)).isEqualTo("z0");
    assertThat(table.getValueAt(1, 0)).isEqualTo("x1");
    assertThat(table.getValueAt(1, 1)).isEqualTo("b1");
    assertThat(table.getValueAt(1, 2)).isEqualTo("z1");
    assertThat(table.getValueAt(2, 0)).isEqualTo("a2");
  }

  @Test
  void testPlainPasteNotEditableNo() {
    JTable table = plainTable();
    TestAdapter adapter = new TestAdapter(table);
    adapter.answer = false;
    select(table, 0, 0);
    adapter.paste(table, "x0\ty0\tz0\n");

    assertThat(adapter.questions).hasSize(1);
    assertThat(table.getValueAt(0, 0)).isEqualTo("a0");
    assertThat(table.getValueAt(0, 2)).isEqualTo("c0");
  }

  @Test
  void testPlainPasteNoneEditable() {
    JTable table = plainTable();
    TestAdapter adapter = new TestAdapter(table);
    select(table, 0, 1);
    adapter.paste(table, "y0\ny1\n");

    assertThat(adapter.questions).isEmpty();
    assertThat(adapter.messages).hasSize(1);
    assertThat(table.getValueAt(0, 1)).isEqualTo("b0");
  }

  @Test
  void testPlainPasteEmptyCellKeepsPosition() {
    JTable table = plainTable();
    TestAdapter adapter = new TestAdapter(table);
    adapter.answer = false; // not asked
    select(table, 1, 0);
    // The middle cell is empty, and is for the column that is not editable.
    adapter.paste(table, "x\t\tz\n");
    assertThat(adapter.questions).hasSize(1);

    // Only editable columns; the empty cell is first.
    adapter.questions.clear();
    select(table, 1, 2);
    adapter.paste(table, "\n");
    assertThat(adapter.questions).isEmpty();
    assertThat(table.getValueAt(1, 2)).isEqualTo("");
  }

  @Test
  void testPlainPasteTooManyColumnsOrRows() {
    JTable table = plainTable();
    TestAdapter adapter = new TestAdapter(table);
    select(table, 0, 2);
    adapter.paste(table, "x\ty\n");
    assertThat(adapter.messages).hasSize(1);
    assertThat(table.getValueAt(0, 2)).isEqualTo("c0");

    // Too many rows, answer no: nothing is pasted.
    adapter.messages.clear();
    adapter.answer = false;
    select(table, 2, 0);
    adapter.paste(table, "x\ny\n");
    assertThat(adapter.messages).hasSize(1);
    assertThat(table.getValueAt(2, 0)).isEqualTo("a2");

    // Too many rows, answer yes: just the rows present.
    adapter.answer = true;
    adapter.paste(table, "x\ny\n");
    assertThat(table.getValueAt(2, 0)).isEqualTo("x");
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // SsTable with a database.
  // Columns: 0 tbl_pk, 1 c_text, 2 c_int, 3 c_date
  // Rows: (1, 'text-1', 3, 2000-01-11), (2, 'text-2', 7, 2000-02-22)
  //

  private interface Body {
    void run(SsTable table) throws Exception;
  }

  /**
   * Create the table and run the test on the EDT. The RowsModel does some
   * of its work in events, which move the RowSet cursor; so the
   * table is created in one event and the test runs in a later one.
   */
  private static void withSsTable(Body body) throws Exception {
    SsTable[] table = new SsTable[1];
    Exception[] ex = new Exception[1];
    EventQueue.invokeAndWait(() -> {
      try {
        table[0] = ssTable();
      } catch (Exception ex01) {
        ex[0] = ex01;
      }
    });
    EventQueue.invokeAndWait(() -> {
      try {
        if (ex[0] == null)
          body.run(table[0]);
      } catch (Exception ex01) {
        ex[0] = ex01;
      }
    });
    if (ex[0] != null)
      throw ex[0];
  }

  private static SsTable ssTable() throws SQLException, ClassNotFoundException {
    RowSet rs = H2.getRowSetCleanDB("""
        CREATE TABLE tbl
        (
            tbl_pk INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 1000) PRIMARY KEY,
            c_text VARCHAR(50),
            c_int INT,
            c_date DATE
        );
        INSERT INTO tbl VALUES (1, 'text-1', 3, '2000-01-11');
        INSERT INTO tbl VALUES (2, 'text-2', 7, '2000-02-22');
        """);
    rs.setCommand("SELECT * FROM tbl ORDER BY tbl_pk");
    SsTable table = new SsTable(RowsModel.create(rs));
    table.setUneditableColumns(new int[] {0});
    return table;
  }

  /** @return "c_text|c_int" for each row, by primary key */
  private static List<String> dbRows() throws SQLException, ClassNotFoundException {
    List<String> rows = new ArrayList<>();
    try (Statement stmt = H2.getCon().createStatement();
         ResultSet rs = stmt.executeQuery("SELECT c_text, c_int FROM tbl ORDER BY tbl_pk")) {
      while (rs.next()) {
        rows.add(rs.getString(1) + "|" + rs.getObject(2));
      }
    }
    return rows;
  }

  @Test
  void testModelGuards() throws Exception {
    withSsTable((table) -> {
      SsTableModel model = table.getModel();

      assertThat(model.isColumnEditable(0)).isFalse();
      assertThat(model.isColumnEditable(1)).isTrue();
      assertThat(model.isColumnNullable(1)).isTrue();
      assertThat(model.isInsertRow(1)).isFalse();
      assertThat(model.isInsertRow(2)).isTrue();

      assertThatThrownBy(() -> model.setValueAt(99, 0, 0))
          .isInstanceOf(IllegalStateException.class);
      assertThat(model.trySetValueAt(99, 0, 0)).isNotNull();
      assertThat(model.trySetValueAt("changed", 0, 1)).isNull();
      assertThat(dbRows()).containsExactly("changed|3", "text-2|7");
    });
  }

  @Test
  void testPasteSkipsUneditableColumn() throws Exception {
    withSsTable((table) -> {
      TestAdapter adapter = new TestAdapter(table);
      select(table, 0, 0);
      adapter.paste(table, "55\tnew-1\t30\n66\tnew-2\t70\n");

      assertThat(adapter.questions).hasSize(1);
      assertThat(adapter.questions.get(0)).contains("2 of 6");
      assertThat(adapter.messages).isEmpty();
      assertThat(table.getValueAt(0, 0)).isEqualTo(1);
      assertThat(dbRows()).containsExactly("new-1|30", "new-2|70");
    });
  }

  @Test
  void testPasteEmptyCellIsNull() throws Exception {
    withSsTable((table) -> {
      TestAdapter adapter = new TestAdapter(table);
      select(table, 0, 1);
      adapter.paste(table, "\t\nx\t\n");

      assertThat(adapter.questions).isEmpty();
      assertThat(adapter.messages).isEmpty();
      assertThat(dbRows()).containsExactly("null|null", "x|null");
    });
  }

  @Test
  void testCopyPasteRoundTripWithNull() throws Exception {
    withSsTable((table) -> {
      TestAdapter adapter = new TestAdapter(table);
      assertThat(table.getModel().trySetValueAt(null, 0, 2)).isNull();

      table.setRowSelectionInterval(0, 0);
      table.setColumnSelectionInterval(1, 2);
      String text = adapter.copyText(table);
      assertThat(text).isEqualTo("text-1\t\n");

      select(table, 1, 1);
      adapter.paste(table, text);
      assertThat(adapter.messages).isEmpty();
      assertThat(dbRows()).containsExactly("text-1|null", "text-1|null");
    });
  }

  @Test
  void testPasteReportsFailedCells() throws Exception {
    withSsTable((table) -> {
      table.setSsCellEditing(new SsCellEditing() {
        @Override
        public boolean cellUpdateRequested(int row, int column, Object oldValue,
                                           Object newValue) {
          return !"bad".equals(newValue);
        }
      });
      TestAdapter adapter = new TestAdapter(table);
      select(table, 0, 1);
      // "bad" is refused by cellUpdateRequested, "seven" is not an integer.
      adapter.paste(table, "bad\t31\nok\tseven\n");

      assertThat(adapter.questions).isEmpty();
      // One message for the whole paste.
      assertThat(adapter.messages).hasSize(1);
      assertThat(adapter.messages.get(0))
          .contains("2 of 4")
          .contains("Row 1")
          .contains("Row 2")
          .contains("seven");
      // The other cells were written.
      assertThat(dbRows()).containsExactly("text-1|31", "ok|7");
    });
  }

  @Test
  void testPasteIgnoresHiddenColumn() throws Exception {
    withSsTable((table) -> {
      table.setHiddenColumns(List.of(2));
      TestAdapter adapter = new TestAdapter(table);

      // Copy leaves out the hidden column.
      table.setRowSelectionInterval(0, 0);
      table.setColumnSelectionInterval(1, 3);
      String text = adapter.copyText(table);
      assertThat(text).isEqualTo("text-1\t2000-01-11\n");

      // Paste steps over it; c_int in row 2 is not changed.
      select(table, 1, 1);
      adapter.paste(table, text);
      assertThat(adapter.questions).isEmpty();
      assertThat(adapter.messages).isEmpty();
      assertThat(dbRows()).containsExactly("text-1|3", "text-1|7");
      assertThat(String.valueOf(table.getValueAt(1, 3))).isEqualTo("2000-01-11");
    });
  }

  @Test
  void testPasteInsertsRows() throws Exception {
    withSsTable((table) -> {
      TestAdapter adapter = new TestAdapter(table);
      adapter.setAllowInsertion(true);
      select(table, 1, 1);
      // The second clipboard row has nothing to write, so it does not
      // insert a row; the third row is the first new row.
      adapter.paste(table, "upd\n\nins\n");

      assertThat(adapter.questions).hasSize(1); // insert new rows?
      assertThat(adapter.messages).hasSize(1); // not enough rows
      assertThat(dbRows()).containsExactly("text-1|3", "upd|7", "ins|null");
    });
  }
}
// vi: sw=2 ts=8
