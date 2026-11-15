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
 * copyright (C) 2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package dev.visdb.seesaw.table;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.swing.JOptionPane;
import javax.swing.JTable;

import dev.visdb.seesaw.SsTable;
import dev.visdb.seesaw.datasources.RowSetOps;
import dev.visdb.seesaw.utils.JStuff;

import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * Key adapter for JTable and SsTable that manages cut and paste functionality
 * between a table and either another table or a spreadsheet.
 */
public class SsTableKeyAdapter extends KeyAdapter {
  /** Logger for component */
  private static final Logger logger = JStuff.getLogger();

  /**
   * Takes the column number and string value to be set for that column and
   * converts the string in to appropriate class. The class is found by calling
   * the getColumnClass() method of the JTable.
   *
   * @param jTable JTable containing target object
   * @param column the column number for which new value has to be set.
   * @param value  string representation of the new value.
   *
   * @return returns the value as a column class object.
   * @throws Exception catch all exception
   */
  protected static Object getObjectToSet(JTable jTable, int column, String value) throws Exception {
    // GET THE COLUMN CLASS
    final Class<?> objectClass = jTable.getColumnClass(column);
    Object newValue = null;
    try {
      // CONSTRUCT THE OBJECT ONLY IF THE STRING IS NOT NULL
      if (value != null) {
        // DATE CLASS DOESN'T HAVE A CONSTRUCTOR THAT TAKES A STRING
        if (objectClass.equals(java.sql.Date.class)) {
          newValue = Date.valueOf(value);
        } else if (objectClass.equals(Object.class)) {
          newValue = value;
        } else {
          // GET THE CONSTRUCTOR FOR THE CLASS WHICH TAKES A STRING
          final Constructor<?> constructor
              = objectClass.getConstructor(new Class<?>[] {String.class});

          // CREATE AN INSTANCE OF THE OBJECT
          newValue = constructor.newInstance(new Object[] {value});
        }
      }
    } catch (final NoSuchMethodException nsme) {
      logger.log(WARNING, "No Such Method Exception. Failed to copy data.", nsme);
      newValue = value;
    }

    // RETURN THE NEWLY CREATED OBJECT.
    return newValue;
  }

  /**
   * Indicates whether or not row insertions are allowed via cut/copy and paste.
   */
  protected boolean allowInsertion = false;

  /**
   * Indicates row used for insertion in SsTable.
   */
  protected boolean forSsDataGrid = false;

  /**
   * Off state for copying or pasting.
   */
  protected int offMask = InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

  /**
   * On state for copying or pasting.
   */
  protected int onMask = InputEvent.CTRL_DOWN_MASK;

  /**
   * Constructs a KeyAdapter for the JTable.
   *
   * @param jTable JTable for which copy and paste support should be added.
   */
  @SuppressWarnings("OverridableMethodCallInConstructor")
  public SsTableKeyAdapter(JTable jTable) {
    init(jTable);
  }

  /**
   * Adds the key listener for the specified JTable.
   * @param jTable table for which listener is to be added
   */
  protected void init(JTable jTable) {
    jTable.addKeyListener(this);
  }

  /**
   * Invoked when a key is released.
   * @param ke
   */
  @Override
  public void keyReleased(KeyEvent ke) {
    final JTable jTable = (JTable) ke.getSource();

    logger.log(DEBUG,
               () -> sf("Key Released on SsDataGrid. Key Released: %s %s.", ke.getKeyCode(),
                        ((ke.getModifiersEx() & (onMask | offMask)) == onMask)));

    // CONTROL-C or CONTROL-V; SHIFT OR ALT SHOULD NOT BE DOWN
    if ((ke.getModifiersEx() & (onMask | offMask)) != onMask)
      return;

    if (ke.getKeyCode() == KeyEvent.VK_C) {
      logger.log(DEBUG, "Going to handle copy");

      final String text = copyText(jTable);
      if (text == null)
        return;

      // COPY THE DATA TO THE SYSTEM CLIP BOARD
      final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      final StringSelection stringSelection = new StringSelection(text);
      clipboard.setContents(stringSelection, stringSelection);

    } else if (ke.getKeyCode() == KeyEvent.VK_V) {
      // GET THE CONTENTS OF THE CLIPBOARD
      final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      final Transferable transferable = clipboard.getContents(this);

      // IF THE CONTENT TYPE SUPPORTS STRING TYPE GET THE DATA.
      if (transferable == null || !transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
        return;
      String strData;
      try {
        strData = (String) transferable.getTransferData(DataFlavor.stringFlavor);
      } catch (final UnsupportedFlavorException ufe) {
        logger.log(ERROR, "Unsupported Flavor Exception.", ufe);
        return;
      } catch (final IOException ioe) {
        logger.log(ERROR, "IO Exception.", ioe);
        return;
      }
      paste(jTable, strData);
    }
  }

  /**
   * Returns the text for the selected cells: a tab between the cells of
   * a row and a new line at the end of each row. A null value is an
   * empty cell. Hidden columns are not included.
   *
   * @param jTable table with the selection
   * @return text for the clipboard, null if there is nothing selected
   */
  protected String copyText(JTable jTable) {
    // CHECK IF THERE IS ATLEAST ONE SELECTED CELL.
    if ((jTable.getSelectedRowCount() < 1) || (jTable.getSelectedColumnCount() < 1))
      return null;

    final int[] selectedRows = jTable.getSelectedRows();
    final int[] selectedColumns = IntStream.of(jTable.getSelectedColumns())
                                      .filter((col) -> !isHiddenColumn(jTable, col))
                                      .toArray();
    if (selectedColumns.length < 1)
      return null;

    logger.log(DEBUG, () -> sf("Copy: start row %d, column %d; %d rows x %d columns.",
                               selectedRows[0] + 1, selectedColumns[0] + 1,
                               selectedRows.length, selectedColumns.length));

    final StringBuilder strBuf = new StringBuilder();
    for (int row : selectedRows) {
      for (int j = 0; j < selectedColumns.length; j++) {
        if (j > 0)
          strBuf.append('\t');
        final Object value = jTable.getValueAt(row, selectedColumns[j]);
        if (value != null)
          strBuf.append(value);
      }
      strBuf.append('\n');
    }
    return strBuf.toString();
  }

  /**
   * Split clipboard text into rows of cells. Rows end with a line
   * terminator and cells are separated by a tab. An empty cell,
   * for example a blank spreadsheet cell, is an empty string;
   * empty cells and blank lines keep their position.
   *
   * @param strData clipboard text
   * @return the cells of each row
   */
  static List<String[]> parseClipboard(String strData) {
    final List<String[]> rows = new ArrayList<>();
    final String[] lines = strData.split("\\R", -1);
    int nLines = lines.length;
    // The text normally ends with a line terminator.
    if (lines[nLines - 1].isEmpty())
      nLines--;
    for (int i = 0; i < nLines; i++) {
      rows.add(lines[i].split("\t", -1));
    }
    return rows;
  }

  /**
   * Paste clipboard text into the table starting at the first selected cell.
   * <p>
   * Hidden columns are ignored, as they are for copy; the pasted cells go
   * into visible columns only. If some of the target cells are not editable
   * the user is asked whether to continue; if so those cells are left
   * unchanged. Cells that could not be written, for example because the
   * value could not be converted or was refused by
   * {@link SsCellEditing#cellUpdateRequested }, are listed in one message
   * after the paste.
   * <p>
   * An empty cell sets the target to null; except that a text column
   * which does not allow null is set to the empty string.
   * <p>
   * A clipboard row that goes into a new row of an SsTable is inserted
   * with a single insert, see {@link SsTableModel#tryInsertRow }; so the
   * row is inserted with all of its values or it is not inserted. In a new
   * row, an empty cell leaves the column's default value, if it has one.
   * If a new row can not be inserted, for example a value is refused by
   * SsCellEditing or there is an SQL error, then the paste stops; the
   * message after the paste has the reason and the number of rows not pasted.
   *
   * @param jTable table to paste into
   * @param strData clipboard text
   */
  protected void paste(JTable jTable, String strData) {
    // IF USER HAS NOT SELECTED ANY CELL THEN DO NOT COPY.
    if ((jTable.getSelectedRowCount() < 1) || (jTable.getSelectedColumnCount() < 1))
      return;

    // GET THE SELECTED ROWS AND COLUMNS
    final int[] selectedRows = jTable.getSelectedRows();
    final int[] selectedColumns = jTable.getSelectedColumns();
    if (selectedRows.length < 1 || selectedColumns.length < 1)
      return;

    final List<String[]> data = parseClipboard(strData);
    if (data.isEmpty())
      return;

    final int numRows = data.size();
    int numColumns = 0;
    for (String[] cells : data) {
      numColumns = Math.max(numColumns, cells.length);
    }

    // The target columns are the visible columns starting at the first
    // selected column.
    final int columnCount = jTable.getColumnCount();
    final int[] targetColumns = new int[numColumns];
    int nTargetColumns = 0;
    for (int col = selectedColumns[0]; col < columnCount && nTargetColumns < numColumns; col++) {
      if (!isHiddenColumn(jTable, col))
        targetColumns[nTargetColumns++] = col;
    }

    // IF THE NUMBER OF COLUMNS NEEDED TO COPY FROM CLIPBOARD
    // IS MORE THAN THAT IN JTABLE CANCEL COPY
    if (nTargetColumns < numColumns) {
      showMessage(jTable, "There are not enough columns in the table to copy into.\n");
      return;
    }

    // GET THE NUMBER OF ROWS IN THE JTABLE.
    // An SsTable has a row for inserting new rows after the database rows,
    // unless its insertion property is false; then rows can not be inserted.
    final boolean hasInsertRow = jTable instanceof SsTable ssTable ? ssTable.getInsertion()
                                                                   : forSsDataGrid;
    final boolean canInsert = allowInsertion && (hasInsertRow || !(jTable instanceof SsTable));
    int rowCount = jTable.getRowCount();
    if (hasInsertRow) {
      rowCount--;
    }

    int numRowsToCopy = numRows;
    {
      final int numColumnsF = numColumns;
      final int rowCountF = rowCount;
      logger.log(DEBUG, () -> sf("Paste: clipboard %d rows x %d columns; start row %d, column %d;"
                                 + " table has %d rows; can insert rows: %b.",
                                 numRows, numColumnsF, selectedRows[0] + 1,
                                 selectedColumns[0] + 1, rowCountF, canInsert));
    }

    // SEE IF SUFFICENT ROWS ARE PRESENT.
    if (rowCount < (numRows + selectedRows[0])) {
      final int available = rowCount - selectedRows[0];
      String message = "There are not enough rows in the table to paste into.\n";
      if (!canInsert) {
        message += allowInsertion
                   ? "New rows can not be inserted because the table's insertion is off.\n"
                   : "New rows can not be inserted.\n";
      }
      showMessage(jTable, message);
      if (canInsert
          && confirm(jTable, "Do you want to insert new rows?", "Add Rows")) {
        // SET THE NUMBER OF ROWS TO COPY EQUAL TO
        // NUMBER OF ROWS IN DATA.
        numRowsToCopy = numRows;
        logger.log(DEBUG, "Paste: not enough rows; will insert new rows.");
      } else if (available < 1) {
        logger.log(DEBUG, "Paste: no rows to paste into.");
        return;
      } else if (confirm(jTable,
                         sf("The clipboard has %s, but only %s available.\n"
                            + "Paste only the first %s?\n"
                            + "If you answer No, nothing is pasted.",
                            rows(numRows), available == 1 ? "1 row is" : available + " rows are",
                            rows(available)),
                         "Paste into existing rows?")) {
        // GET THE NUMBER OF ROWS FROM SELECTED ROW TO LAST ROW
        // NO NEED TO ADD A ONE
        numRowsToCopy = rowCount - selectedRows[0];
        final int numRowsToCopyF = numRowsToCopy;
        logger.log(DEBUG, () -> sf("Paste: not enough rows; only the first %d clipboard rows.",
                                   numRowsToCopyF));
      } else {
        logger.log(DEBUG, "Paste: not enough rows; cancelled.");
        return;
      }
    }
    if (numRowsToCopy < 1) {
      logger.log(DEBUG, "Paste: no rows to paste into.");
      return;
    }

    // Find the target cells that are not editable. The answer is saved
    // so that each cell is asked about only once.
    final int nViewRows = jTable.getRowCount();
    final boolean[][] editable = new boolean[numRowsToCopy][];
    int nCells = 0;
    int nNotEditable = 0;
    for (int i = 0; i < numRowsToCopy; i++) {
      final int row = selectedRows[0] + i;
      final int nRowCells = data.get(i).length;
      editable[i] = new boolean[nRowCells];
      for (int j = 0; j < nRowCells; j++) {
        // A row that will be inserted does not exist yet,
        // so only the column can be checked.
        editable[i][j] = row < nViewRows ? jTable.isCellEditable(row, targetColumns[j])
                                         : isNewRowCellEditable(jTable, targetColumns[j]);
        nCells++;
        if (!editable[i][j])
          nNotEditable++;
      }
    }
    if (nNotEditable == nCells) {
      showMessage(jTable, "None of the target cells are editable.\nNothing was pasted.");
      return;
    }
    if (nNotEditable > 0) {
      final int nNotEditableF = nNotEditable;
      final int nCellsF = nCells;
      logger.log(INFO, () -> sf("Paste: %d of %d target cells are not editable.",
                                nNotEditableF, nCellsF));
      if (!confirm(jTable,
                   sf("Some target cells, %d of %d, are not editable.\nContinue with paste?",
                      nNotEditable, nCells),
                   "Paste"))
        return;
    }

    // COPY THE DATA FROM CLIP BOARD TO JTABLE
    final SsTableModel ssModel = jTable.getModel() instanceof SsTableModel m ? m : null;
    final List<String> failures = new ArrayList<>();
    // The number of clipboard rows that were for a new row and did not insert one.
    int nNotInserted = 0;
    int nRowsInserted = 0;
    // Why the paste was stopped, null if it was not stopped.
    String stopped = null;
    // The number of cells in the new row that stopped the paste.
    int nRowCellsNotWritten = 0;
    // The number of clipboard rows left when the paste was stopped.
    int nRowsNotPasted = 0;
    for (int i = 0; i < numRowsToCopy; i++) {
      final int iF = i;
      final String[] cells = data.get(i);
      final int row = selectedRows[0] + i - nNotInserted;
      final boolean isInsertRow = ssModel != null && ssModel.isInsertRow(row);
      // The values for a new row, by model column. They are inserted
      // together, after the loop, because inserting the row can move it.
      final Map<Integer, Object> newRowValues = new LinkedHashMap<>();
      for (int j = 0; j < cells.length; j++) {
        if (!editable[i][j])
          continue;
        final int column = targetColumns[j];
        String problem;
        try {
          if (isInsertRow) {
            final int modelColumn = jTable.convertColumnIndexToModel(column);
            // An empty cell leaves the column's default value, if it has one.
            if (!cells[j].isEmpty() || ssModel.getDefaultValue(modelColumn) == null) {
              final Object newValue
                  = cells[j].isEmpty() ? getEmptyCellValue(jTable, column)
                                       : getObjectToSet(jTable, column, cells[j]);
              if (newValue != null)
                newRowValues.put(modelColumn, newValue);
            }
            continue;
          }
          // GET THE VALUE FOR THIS CELL IN THE FORM OF THIS COLUMN CLASS OBJECT
          final Object newValue = cells[j].isEmpty() ? getEmptyCellValue(jTable, column)
                                                     : getObjectToSet(jTable, column, cells[j]);
          // SET THE VALUE FOR THE COLUMN
          problem = setCell(jTable, newValue, row, column);
        } catch (final Exception ex) {
          final Throwable cause
              = ex instanceof InvocationTargetException && ex.getCause() != null ? ex.getCause()
                                                                                 : ex;
          logger.log(DEBUG, "Paste: cell not written.", ex);
          problem = sf("Can not use '%s': %s", cells[j], cause);
        }
        if (problem != null)
          failures.add(sf("Row %d, %s: %s", row + 1, getColumnLabel(jTable, column), problem));
      }
      if (isInsertRow && !newRowValues.isEmpty()) {
        final String problem = ssModel.tryInsertRow(newRowValues);
        if (problem != null) {
          // Do not go on to insert the rows that follow it.
          stopped = sf("Row %d, new row not inserted: %s", row + 1, problem);
          nRowCellsNotWritten += newRowValues.size();
          nRowsNotPasted = numRowsToCopy - i - 1;
          break;
        }
      }
      if (isInsertRow) {
        final boolean inserted = !ssModel.isInsertRow(row);
        if (inserted)
          nRowsInserted++;
        logger.log(DEBUG, () -> sf("Paste: clipboard row %d, new row at row %d: %s.", iF + 1,
                                   row + 1, inserted ? "inserted" : "nothing to insert"));
      } else {
        logger.log(DEBUG, () -> sf("Paste: clipboard row %d written to row %d.", iF + 1, row + 1));
      }
      // If every cell for a new row was skipped or empty, then no row was
      // inserted; the next clipboard row is for the same new row.
      if (isInsertRow && ssModel.isInsertRow(row))
        nNotInserted++;
    }

    // UPDATE THE UI AS WE HAVE UPDATED UNDERLIYING DATA
    // THIS HAS TO PROPOGATE TO THE SCREEN.
    jTable.updateUI();

    {
      final int nRowsInsertedF = nRowsInserted;
      final int nNotWritten = failures.size() + nRowCellsNotWritten;
      final String stoppedF = stopped;
      logger.log(DEBUG, () -> sf("Paste: done; %d rows inserted, %d cells not written%s.",
                                 nRowsInsertedF, nNotWritten,
                                 stoppedF == null ? "" : ", stopped early"));
    }

    if (!failures.isEmpty() || stopped != null) {
      final String stoppedF = stopped;
      final int nNotWritten = failures.size() + nRowCellsNotWritten;
      logger.log(WARNING, () -> sf("Paste: %d cells were not written:\n%s%s", nNotWritten,
                                   stoppedF == null ? "" : stoppedF + "\n",
                                   String.join("\n", failures)));
      final StringBuilder sb = new StringBuilder(
          sf("Paste: %d of %d cells were not written.\n", nNotWritten,
             nCells - nNotEditable));
      if (stopped != null) {
        sb.append("\nThe paste was stopped.\n").append(stopped).append('\n');
        if (nRowsNotPasted > 0)
          sb.append(sf("Rows after it that were not pasted: %d\n", nRowsNotPasted));
      }
      for (int i = 0; i < failures.size() && i < MAX_FAILURES_SHOWN; i++) {
        sb.append('\n').append(failures.get(i));
      }
      if (failures.size() > MAX_FAILURES_SHOWN)
        sb.append(sf("\n... and %d more; see the log.", failures.size() - MAX_FAILURES_SHOWN));
      showMessage(jTable, sb.toString());
    }
  }

  /** Returns "1 row" or "n rows". */
  private static String rows(int n) {
    return n == 1 ? "1 row" : n + " rows";
  }

  /** The maximum number of cells listed in the message after a paste. */
  private static final int MAX_FAILURES_SHOWN = 15;

  /**
   * Returns true if paste and copy should ignore the column.
   *
   * @param jTable the table
   * @param column the column, in the view, to be queried
   * @return true for a hidden column of an SsTable
   */
  protected boolean isHiddenColumn(JTable jTable, int column) {
    return jTable instanceof SsTable ssTable
        && ssTable.getHiddenColumns().contains(jTable.convertColumnIndexToModel(column));
  }

  /**
   * Returns the column's header, or its number if there is no header.
   */
  private String getColumnLabel(JTable jTable, int column) {
    final Object header = jTable.getColumnModel().getColumn(column).getHeaderValue();
    return header == null || header.toString().isBlank() ? sf("column %d", column + 1)
                                                         : header.toString();
  }

  /**
   * Returns true if the column is editable in a row that paste will insert.
   */
  private boolean isNewRowCellEditable(JTable jTable, int column) {
    return !(jTable.getModel() instanceof SsTableModel ssModel)
        || ssModel.isColumnEditable(jTable.convertColumnIndexToModel(column));
  }

  /**
   * Returns the value to set for an empty clipboard cell: null, except
   * the empty string for a text column of an SsTable that does not allow
   * null. A column whose nullability is not known is taken to allow null.
   */
  private Object getEmptyCellValue(JTable jTable, int column) {
    final Class<?> objectClass = jTable.getColumnClass(column);
    if (!objectClass.equals(String.class) && !objectClass.equals(Object.class))
      return null;
    return jTable instanceof SsTable ssTable
               && RowSetOps.isNullable(ssTable.getRowSet(),
                                       jTable.convertColumnIndexToModel(column) + 1)
                      .orElse(true)
           ? null : "";
  }

  /**
   * Set the value of a cell.
   * @return null if there was no problem, otherwise a description
   */
  private String setCell(JTable jTable, Object value, int row, int column) {
    if (jTable.getModel() instanceof SsTableModel ssModel) {
      return ssModel.trySetValueAt(value, jTable.convertRowIndexToModel(row),
                                   jTable.convertColumnIndexToModel(column));
    }
    jTable.setValueAt(value, row, column);
    return null;
  }

  /**
   * Show a message for copy or paste.
   *
   * @param jTable parent for the dialog
   * @param message the message
   */
  protected void showMessage(JTable jTable, String message) {
    JOptionPane.showMessageDialog(jTable, message);
  }

  /**
   * Ask a yes or no question for copy or paste.
   *
   * @param jTable parent for the dialog
   * @param message the question
   * @param title the dialog title
   * @return true if the answer is yes
   */
  protected boolean confirm(JTable jTable, String message, String title) {
    return JOptionPane.showConfirmDialog(jTable, message, title, JOptionPane.YES_NO_OPTION)
        == JOptionPane.YES_OPTION;
  }

  /**
   * Sets allowInsertion indicator. Set true if new rows can be added to JTable
   * via cut/copy and paste - otherwise false. False by default.
   *
   * @param allowInsertion true if new rows can be added when pasting data from
   *                        clipboard, else false.
   */
  public void setAllowInsertion(boolean allowInsertion) {
    this.allowInsertion = allowInsertion;
  }

  /**
   * Sets forSsDataGrid indicator. True if the key adapter is used for SsDataGrid
   * -- otherwise false. False by default.
   *
   * @param forSsDataGrid - true if this key adapter is used for SsTable, else
                     false.
   */
  public void setForSsDataGrid(boolean forSsDataGrid) {
    this.forSsDataGrid = forSsDataGrid;
  }
}
// vi: sw=2 ts=8
