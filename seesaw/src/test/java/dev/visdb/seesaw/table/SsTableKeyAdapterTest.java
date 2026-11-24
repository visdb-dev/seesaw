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
import javax.swing.JTextField;
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
 * Copy and paste in SsTableKeyAdapter, the SsTableModel methods it uses,
 * and SsTable.editingStopped.
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
  void testPlainPasteDate() {
    // Not an SsTable: a java.sql.Date column takes only yyyy-m-d.
    DefaultTableModel model = new DefaultTableModel(
        new Object[][] {{null}, {null}}, new Object[] {"D"}) {
      @Override
      public Class<?> getColumnClass(int column) {
        return java.sql.Date.class;
      }
    };
    JTable table = new JTable(model);
    TestAdapter adapter = new TestAdapter(table);
    select(table, 0, 0);
    adapter.paste(table, "2026-1-5\n11/23/2026\n");
    assertThat(table.getValueAt(0, 0)).isEqualTo(java.sql.Date.valueOf("2026-01-05"));
    assertThat(table.getValueAt(1, 0)).isNull();
    assertThat(adapter.messages).hasSize(1);
    assertThat(adapter.messages.get(0)).contains("11/23/2026");
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
    withSsTable("tbl_pk", body);
  }

  private static void withSsTable(String orderBy, Body body) throws Exception {
    SsTable[] table = new SsTable[1];
    Exception[] ex = new Exception[1];
    EventQueue.invokeAndWait(() -> {
      try {
        table[0] = ssTable(orderBy);
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

  private static SsTable ssTable(String orderBy) throws SQLException, ClassNotFoundException {
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
    rs.setCommand("SELECT * FROM tbl ORDER BY " + orderBy);
    SsTable table = new SsTable(RowsModel.create(rs)) {
      @Override
      protected void cellNoLongerEditable(int row, int column) {
        noLongerEditable.add(row + "," + column);
      }
    };
    table.setUneditableColumns(new int[] {0});
    table.getModel().setSsDataGridHandler(new Handler(rs));
    noLongerEditable.clear();
    return table;
  }

  /** Executes the query again after an insert; like the demo. */
  private static class Handler implements SsDataGridHandler {
    private final RowSet rs;

    Handler(RowSet rs) {
      this.rs = rs;
    }

    @Override
    public boolean allowDeletion(int row) {
      return true;
    }

    @Override
    public void performPreInsertOps(int row) {}

    @Override
    public void performPostInsertOps(int row) {
      try {
        rs.execute();
      } catch (SQLException ex) {
        throw new IllegalStateException(ex);
      }
    }

    @Override
    public void performPreDeletionOps(int row) {}

    @Override
    public void performPostDeletionOps(int row) {}
  }

  /** The cells for which SsTable.cellNoLongerEditable was invoked. */
  private static final List<String> noLongerEditable = new ArrayList<>();

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
      assertThat(model.isInsertRow(1)).isFalse();
      assertThat(model.isInsertRow(2)).isTrue();

      assertThatThrownBy(() -> model.setValueAt(99, 0, 0))
          .isInstanceOf(IllegalStateException.class);
      assertThat(model.trySetValueAt(99, 0, 0)).isNotNull();
      assertThat(model.trySetValueAt("changed", 0, 1)).isNull();
      assertThat(dbRows()).containsExactly("changed|3", "text-2|7");
    });
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // SsTable.editingStopped
  //

  @Test
  void testEditCommits() throws Exception {
    withSsTable((table) -> {
      assertThat(table.editCellAt(0, 1)).isTrue();
      ((JTextField)table.getEditorComponent()).setText("edited");
      assertThat(table.getCellEditor().stopCellEditing()).isTrue();

      assertThat(table.isEditing()).isFalse();
      assertThat(noLongerEditable).isEmpty();
      assertThat(dbRows()).containsExactly("edited|3", "text-2|7");
    });
  }

  /** The cell stops being editable while it is being edited. */
  @Test
  void testEditCellNoLongerEditable() throws Exception {
    withSsTable((table) -> {
      boolean[] editable = {true};
      table.setSsCellEditing(new SsCellEditing() {
        @Override
        public boolean isCellEditable(int row, int column) {
          return editable[0];
        }
      });
      assertThat(table.editCellAt(0, 1)).isTrue();
      ((JTextField)table.getEditorComponent()).setText("edited");
      editable[0] = false;
      table.getCellEditor().stopCellEditing();

      // The editor is not left open, the user is told, nothing is written.
      assertThat(table.isEditing()).isFalse();
      assertThat(noLongerEditable).containsExactly("0,1");
      assertThat(dbRows()).containsExactly("text-1|3", "text-2|7");
    });
  }

  /** An exception from setting the value does not leave the editor open. */
  @Test
  void testEditExceptionRemovesEditor() throws Exception {
    withSsTable((table) -> {
      table.setSsCellEditing(new SsCellEditing() {
        @Override
        public boolean cellUpdateRequested(int row, int column, Object oldValue,
                                           Object newValue) {
          throw new UnsupportedOperationException("expected by the test");
        }
      });
      assertThat(table.editCellAt(0, 1)).isTrue();
      ((JTextField)table.getEditorComponent()).setText("edited");
      assertThatThrownBy(() -> table.getCellEditor().stopCellEditing())
          .isInstanceOf(UnsupportedOperationException.class);

      assertThat(table.isEditing()).isFalse();
      assertThat(dbRows()).containsExactly("text-1|3", "text-2|7");
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
  void testPasteDates() throws Exception {
    withSsTable((table) -> {
      TestAdapter adapter = new TestAdapter(table);
      // c_date is column 3. US, ISO and basic ISO dates; Feb 30 is refused.
      select(table, 0, 3);
      adapter.paste(table, "11/23/2026\n 2026-1-5 \n");
      assertThat(adapter.messages).isEmpty();
      select(table, 0, 3);
      adapter.paste(table, "20260105\n2/30/2026\n");
      assertThat(adapter.messages).hasSize(1);
      assertThat(adapter.messages.get(0)).contains("Row 2").contains("2/30/2026");
      assertThat(dbDates()).containsExactly("2026-01-05", "2026-01-05");
    });
  }

  @Test
  void testBadDateTextIsNotNull() throws Exception {
    withSsTable((table) -> {
      // Text that is not a date is refused; the cell is not set to null.
      assertThat(table.getModel().trySetValueAt("not a date", 0, 3))
          .contains("not a date").contains("DATE");
      assertThat(table.getModel().trySetValueAt("11/23/2026", 1, 3)).isNull();
      assertThat(dbDates()).containsExactly("2000-01-11", "2026-11-23");
    });
  }

  /** @return c_date for each row, by primary key */
  private static List<String> dbDates() throws SQLException, ClassNotFoundException {
    List<String> rows = new ArrayList<>();
    try (Statement stmt = H2.getCon().createStatement();
         ResultSet rs = stmt.executeQuery("SELECT c_date FROM tbl ORDER BY tbl_pk")) {
      while (rs.next()) {
        rows.add(String.valueOf(rs.getDate(1)));
      }
    }
    return rows;
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
  @Test
  void testPasteNewRowMultiColumn() throws Exception {
    withSsTable((table) -> {
      TestAdapter adapter = new TestAdapter(table);
      adapter.setAllowInsertion(true);
      table.setDefaultValues(new int[] {2}, new Object[] {42});
      select(table, 2, 1);
      // An empty cell in a new row gets the default value.
      adapter.paste(table, "ins-1\t55\nins-2\t\n");

      assertThat(adapter.messages).hasSize(1); // not enough rows
      assertThat(dbRows()).containsExactly("text-1|3", "text-2|7", "ins-1|55", "ins-2|42");
    });
  }

  /**
   * The query is sorted and is executed again after an insert, so the
   * new row is not the last row; like the demo.
   */
  @Test
  void testPasteNewRowMoves() throws Exception {
    withSsTable("c_text", (table) -> {
      TestAdapter adapter = new TestAdapter(table);
      adapter.setAllowInsertion(true);
      select(table, 2, 1);
      adapter.paste(table, "aaa\t55\nbbb\t66\n");

      assertThat(adapter.messages).hasSize(1); // not enough rows
      assertThat(dbRows()).containsExactly("text-1|3", "text-2|7", "aaa|55", "bbb|66");
      assertThat(table.getValueAt(0, 1)).isEqualTo("aaa");
    });
  }

  @Test
  void testPasteNewRowSqlError() throws Exception {
    withSsTable((table) -> {
      TestAdapter adapter = new TestAdapter(table);
      adapter.setAllowInsertion(true);
      select(table, 2, 1);
      // The second row's text is too long for the column; the row is not
      // inserted and the paste stops.
      adapter.paste(table, "ins\t55\n" + "x".repeat(60) + "\t66\nlater\t77\n");

      assertThat(adapter.messages).hasSize(2); // not enough rows, the failure
      assertThat(adapter.messages.get(1))
          .contains("2 of 6")
          .contains("Row 4, new row not inserted: Error while inserting row.")
          .contains("Value too long")
          .contains("Rows after it that were not pasted: 1");
      assertThat(dbRows()).containsExactly("text-1|3", "text-2|7", "ins|55");
    });
  }
  /** After a failed update the row shows the database value and can be edited. */
  @Test
  void testUpdateSqlError() throws Exception {
    withSsTable((table) -> {
      SsTableModel model = table.getModel();
      assertThat(model.trySetValueAt("x".repeat(60), 1, 1)).contains("Error while updating");
      assertThat(table.getValueAt(1, 1)).isEqualTo("text-2");
      // The failed value must not be part of the next update of the row.
      assertThat(model.trySetValueAt(9, 1, 2)).isNull();
      assertThat(table.getValueAt(1, 2)).isEqualTo(9);
      assertThat(dbRows()).containsExactly("text-1|3", "text-2|9");
    });
  }
  /** A value refused for a new row stops the paste. */
  @Test
  void testPasteNewRowRefusedStops() throws Exception {
    withSsTable((table) -> {
      table.setSsCellEditing(new SsCellEditing() {
        @Override
        public boolean cellUpdateRequested(int row, int column, Object oldValue,
                                           Object newValue) {
          return !"bad".equals(newValue);
        }
      });
      TestAdapter adapter = new TestAdapter(table);
      adapter.setAllowInsertion(true);
      select(table, 1, 1);
      adapter.paste(table, "upd\t1\nins\t2\nbad\t3\nlater-1\t4\nlater-2\t5\n");

      assertThat(adapter.messages).hasSize(2); // not enough rows, the failure
      assertThat(adapter.messages.get(1))
          .contains("2 of 10")
          .contains("Row 4, new row not inserted")
          .contains("Value not allowed for column 'C_TEXT' (SsCellEditing)")
          .contains("Rows after it that were not pasted: 2");
      assertThat(dbRows()).containsExactly("text-1|3", "upd|1", "ins|2");
    });
  }
  /** With insertion off there is no insert row, and paste does not insert. */
  @Test
  void testPasteInsertionOff() throws Exception {
    withSsTable((table) -> {
      table.setInsertion(false);
      TestAdapter adapter = new TestAdapter(table);
      adapter.setAllowInsertion(true);
      select(table, 1, 1);
      adapter.paste(table, "upd\t1\nins\t2\n");

      assertThat(adapter.messages).hasSize(1); // not enough rows
      assertThat(adapter.messages.get(0)).contains("insertion is off");
      assertThat(adapter.questions).hasSize(1);
      assertThat(adapter.questions.get(0))
          .contains("The clipboard has 2 rows, but only 1 row is available.")
          .contains("Paste only the first 1 row?");
      assertThat(dbRows()).containsExactly("text-1|3", "upd|1");
    });
  }

  /** Inserting a row without an SsDataGridHandler is an internal error. */
  @Test
  void testPasteNewRowNoHandler() throws Exception {
    withSsTable((table) -> {
      table.getModel().setSsDataGridHandler(null);
      TestAdapter adapter = new TestAdapter(table);
      adapter.setAllowInsertion(true);
      select(table, 1, 1);
      adapter.paste(table, "upd\t1\nins-1\t55\nins-2\t66\n");

      assertThat(adapter.messages).hasSize(2); // not enough rows, the failure
      assertThat(adapter.messages.get(1))
          .contains("Row 3, new row not inserted: Internal error")
          .contains("SsDataGridHandler")
          .contains("Rows after it that were not pasted: 1");
      assertThat(table.getRowCount()).isEqualTo(3);
      assertThat(dbRows()).containsExactly("text-1|3", "upd|1");
    });
  }

  /** An inserted row can be read. */
  @Test
  void testPasteNewRowIsReadable() throws Exception {
    withSsTable((table) -> {
      TestAdapter adapter = new TestAdapter(table);
      adapter.setAllowInsertion(true);
      select(table, 2, 1);
      adapter.paste(table, "ins-1\t55\nins-2\t66\n");

      assertThat(table.getRowCount()).isEqualTo(5);
      assertThat(table.getValueAt(2, 1)).isEqualTo("ins-1");
      assertThat(table.getValueAt(3, 2)).isEqualTo(66);
      assertThat(dbRows()).containsExactly("text-1|3", "text-2|7", "ins-1|55", "ins-2|66");
    });
  }
}
// vi: sw=2 ts=8
