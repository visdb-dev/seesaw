# SsTable paste: design notes

2026-11-15

## Summary

Paste into an SsTable writes clipboard text cell by cell into existing rows, and inserts each new row with a single insert. It asks before skipping non-editable cells, reports every problem in one message at the end, and stops at the first new row that can not be inserted.

This describes the code on `main` after PR 52 (commit `5e65d924`, 2026-11-15). It records what paste does, the choices behind it, what is inconsistent or unfinished, and ideas for later work. Row and column numbers shown to the user and in logs are 1-based.

The main limits today: nothing is transactional, there is no undo, one paste can raise up to four dialogs, and whether rows may be inserted is tied to whether the insert row is shown.

## How a paste runs

Everything happens in `SsTableKeyAdapter.paste(JTable, String)`, on the event thread, when Ctrl-V is released.

1. **Parse.** The clipboard text is split into rows at line ends and into cells at tabs. Empty cells and blank lines keep their position. Rows may have different lengths.
2. **Find the target.** The start cell is the first selected row and first selected column. Target columns are the visible columns from there to the right. Hidden columns are skipped. If there are too few columns, a message is shown and nothing is pasted.
3. **Check the rows.** If the clipboard has more rows than the table has from the start row down, a "not enough rows" message is shown. It adds that new rows can not be inserted when that is the case.
4. **Decide about the extra rows.** If rows can be inserted, the user is asked "Do you want to insert new rows?". If the answer is no, or rows can not be inserted, the user is asked whether to paste only the rows that fit. No to that ends the paste with nothing written.
5. **Pre-check editability.** Every target cell is checked before anything is written. If none is editable, a message is shown and nothing is pasted. If some are not, the user is asked once: "Some target cells, N of M, are not editable. Continue with paste?". The answers are remembered, so each cell is asked about only once.
6. **Write existing rows.** Each editable cell is converted to the column's class and written with `SsTableModel.trySetValueAt`. Each cell is its own database update. A cell that fails is recorded and the paste carries on.
7. **Insert new rows.** For a clipboard row that lands on the insert row, the values are collected and inserted together with `SsTableModel.tryInsertRow`. If that fails, the paste stops.
8. **Report.** The table is refreshed. If anything was not written, one message lists the count, the stop reason if any, and up to 15 failed cells. The full list goes to the log.

Copy is simpler: `copyText` takes the selected rows and the selected visible columns, writes values with tabs and line ends, and writes null as an empty cell.

## Choices made and why

### Non-editable cells

- **Two things make a cell non-editable.** The column is in the table's uneditable-columns list, or `SsCellEditing.isCellEditable(row, column)` says no. `SsTableModel.isCellEditable` combines them, with the column list winning.
- **The model enforces it.** `SsTableModel.setValueAt` throws `IllegalStateException` for a non-editable cell. Before this work, paste could write into uneditable columns, which was the original security finding.
- **Paste asks first, once.** All target cells are checked before any write, and one YES/NO question covers them all. This replaced one dialog per cell. On YES the non-editable cells are left unchanged and the rest are pasted. On NO nothing is pasted.
- **A row that does not exist yet can only be checked by column.** `SsCellEditing` needs a row, so for rows that will be inserted the pre-check uses `isColumnEditable` only. `SsCellEditing` is consulted later, at insert time.
- **Paste never throws for this.** It writes through `trySetValueAt`, which returns a description of the problem and shows no dialog.

### Hidden columns

Hidden columns are ignored by both copy and paste. Pasted cells go into visible columns only, so a copy from one SsTable pastes into the same shape in another. View and model column indexes are converted at each use.

### Empty clipboard cells

- **In an existing row** an empty cell sets the target to null. The exception is a text column that does not allow null, which gets the empty string. A column whose nullability is unknown is treated as nullable. Nullability comes from `RowSetOps.isNullable`.
- **In a new row** an empty cell leaves the column's default value if it has one (`SsTableModel.getDefaultValue`). Without a default it follows the existing-row rule.
- Before this work an empty cell was dropped and the rest of the row shifted left. Parsing now keeps positions.

### New rows

- **One insert per clipboard row.** The values are collected and inserted together. Writing cell by cell wrote to the wrong row when the insert moved the new row, for example with a sorted query that is executed again after the insert.
- **All or nothing per row.** A new row is inserted with all its values or not at all.
- **Defaults and the primary key** are set by the existing `setDefaults()` and `setPrimaryColumn()` code when the insert row is entered.
- **A clipboard row with nothing to write inserts nothing.** The next clipboard row then uses the same new row.
- **An `SsDataGridHandler` is required to insert.** Without one, the insert is refused as an internal error. The model does not work around a missing handler and does not look at `ownInsertsAreVisible`.

### When rows can be inserted

Paste inserts only when `SsTable.getInsertion()` is true, which also means the insert row is shown. The key adapter has its own `allowInsertion` flag, but SsTable always sets it to true. This was a reading of the `setInsertion` javadoc and is flagged for rework below.

## Error handling

The rule today: a problem in an existing row skips that cell and continues, and a problem inserting a new row stops the paste.

| Situation | What happens | How it is reported |
| --- | --- | --- |
| Too few columns in the table | Nothing pasted | Message |
| Too few rows, user declines both questions | Nothing pasted | Nothing further |
| No target cell is editable | Nothing pasted | Message |
| Some target cells not editable | User chooses; on YES those cells are skipped | One question before writing; skipped cells are not listed afterwards |
| Text can not be converted to the column's class (existing row) | Cell skipped, paste continues | Line in the summary: "Can not use 'seven': ..." |
| `SsCellEditing.cellUpdateRequested` refuses a value (existing row) | Cell skipped, paste continues | Line in the summary |
| SQL error updating a cell (existing row) | Cell skipped, pending row update cancelled, paste continues | Line in the summary with the database message; ERROR in the log |
| Text can not be converted (new row) | That cell is left out, the row is still inserted | Line in the summary |
| `SsCellEditing` refuses a value or the cell (new row) | Row not inserted, paste stops | Stop section of the summary |
| SQL error inserting (new row), including a not-null column | Row not inserted, paste stops | Stop section, with the database message |
| No `SsDataGridHandler` (new row) | Row not inserted, paste stops | Stop section: "Internal error: can not insert a row; there is no SsDataGridHandler." |
| Inserted row not found in the RowSet afterwards | Paste continues | ERROR in the log only |

### The summary message

Shown once, after the paste, only if something was not written:

```
Paste: 2 of 6 cells were not written.

The paste was stopped.
Row 4, new row not inserted: Error while inserting row.
Value too long for column "C_TEXT CHARACTER VARYING(50)": ...
Rows after it that were not pasted: 1
```

Failed cells in existing rows follow as "Row N, column header: reason", up to 15, then "... and N more; see the log." The stop reason appears once and is not repeated in the cell list.

### Choices behind this

- **One message, not one dialog per cell.** `trySetValueAt` and `tryInsertRow` put the model in a quiet mode where its `messageDialog` records the first message and shows nothing.
- **Stop on a failed new row.** A failed insert usually means every following insert fails the same way, and the rows after it would land in the wrong place. Rows written before the stop stay written.
- **Cancel after a failed update.** After a failed `updateRow` the bad value stayed pending in the RowSet's row buffer, so the next edit of that row failed with the same error. `setValueAt` now calls `cancelRowUpdates()`.
- **After any SQL error the grid shows the old value.** It is read from the RowSet, which holds the value as of the last query. Nothing is re-queried.
- **Logging.** DEBUG entries cover the copy selection, the paste start (clipboard size, start cell, row count, can insert), the not-enough-rows outcome, each clipboard row, and the end totals. Failed cells are logged at WARNING and SQL errors at ERROR.

## Requirements and things to know

- **Handler.** The application must set an `SsDataGridHandler` on the model before rows can be inserted. With a database whose RowSet does not see its own inserts, H2 for one, `performPostInsertOps` must execute the query again. The demo's `DataGridExampleSupport` does this.
- **Cursor.** `SsTableModel` positions the RowSet with `absolute()` before each read, update and delete. It does not rely on a current row. After the handler's re-query the cursor is before the first row, which is why `insertRow` skips `refreshRow()` when there is no current row.
- **Row count.** The model does not listen for a re-query. Its `rowCount` is kept by hand and incremented on insert.
- **Sorted queries.** After the re-query a new row moves to its sorted position. Paste does not depend on where it went, because each new row is written in one insert.
- **Sorting and the insert row.** Turning insertion on removes the table's row sorter. With insertion off and sorting on, paste converts view rows to model rows for each write.
- **Event thread.** Paste runs synchronously on the event thread and does one database statement per existing-row cell.
- **Tests.** SsTable tests must create the table in one event and run the test body in a later one, because the RowsModel moves the cursor in queued events. See `withSsTable` in the test class.

## Inconsistencies noticed

1. **Existing rows continue, new rows stop.** A failed cell in an existing row is skipped. A failed new row ends the paste. Both are defensible, but the user sees two different rules in one paste.
2. **A conversion failure in a new row does not stop the paste.** The cell is left out and the row is inserted without it. A refused value or an SQL error in the same row does stop it. So a new row can be inserted partly filled in one case and not at all in the other.
3. **`setInsertion` means three things.** It is the permission to insert, it shows the insert row, and it turns sorting off. The rest of SeeSaw has a plain permission, `RowsModel.setAllowInsert`, combined with `getAllowWrite()` and `DbOps.allowInsert()`. SsTable ignores all of those.
4. **The key adapter's `allowInsertion` flag is not a real switch.** SsTable always sets it to true, and there is no public way to turn paste inserts off separately.
5. **New rows are pre-checked by column only.** The user can answer YES to "continue with paste?" and still have the paste stop when `SsCellEditing` refuses the new row.
6. **A missing handler is found late.** It is detected at the first insert, after the existing rows have been written. It could be checked before anything is written.
7. **The summary count is partial.** "N of M cells were not written" does not count the cells of rows that were never reached after a stop, and cells skipped as non-editable are not listed.
8. **Column names differ within one message.** Failed cells use the column header. The stop reason from the model uses the database column name.
9. **Selection shows rows, paste uses a block.** SsTable keeps the JTable default of highlighting whole rows, while copy and paste use the selected column range. Only the first selected cell matters for paste. The size of the selection is ignored.
10. **Paste and typing report differently.** Typing shows one dialog per problem. Paste collects them. The wording for the same problem is not the same in both.
11. **Old wording remains.** Some messages still say "copy" for paste, such as "not enough columns in the table to copy into".
12. **Errors while setting defaults are swallowed.** `setDefaults()` and `setPrimaryColumn()` report an SQL error and the insert continues anyway.

## Open items and possible improvements

### Behaviour

- **Revisit when rows can be inserted.** Separate the permission from showing the insert row, and take the permission from the `RowsModel` like the rest of SeeSaw. The "can not be inserted because the table's insertion is off" wording depends on this.
- **Do not always abort after a new-row problem.** Options: skip the bad row and continue, ask the user at that point, or make it a setting. Skipping is safe now that each row is one insert.
- **Decide the rule for a bad cell in a new row.** Either the row is inserted without it, as today, or the row is refused like the other new-row failures.
- **Check everything that can be checked before writing.** Convert all values, run the editability checks, and look for the handler first. Most failures could then be shown before any change is made.
- **Default values for null in existing rows.** Deferred earlier. `SsTableModel.getDefaultValue(column)` is the hook.
- **Append.** Pasting as new rows works only by selecting the insert row. There is no append command, and the start column still decides where cells land.
- **One statement per row.** Existing rows are updated one cell at a time. Setting all the cells of a row and calling `updateRow` once would be faster and would make each row all-or-nothing.
- **Selection.** Show the selected block with `setCellSelectionEnabled(true)`. Consider filling a selected block from a single copied cell, as spreadsheets do.
- **After a re-query the view does not follow.** With a sorted query the new rows move, and the selection and scroll position stay where they were.
- **Keys.** Paste is tied to Ctrl-V on key release. There is no Cmd-V on macOS, no menu item or `Action`, and no cut.
- **Large pastes.** The work runs on the event thread with no progress display and no cancel.
- **Optional cleanup.** `refreshRow()` in `insertRow` has no reader and could be removed with its check.

### Error reporting

More could be reported, and there should be a way to get the rest.

- **Keep the full list available.** Only 15 failures are shown and the rest are in the log, which a user may not have. A details view with the whole list, and a button to copy it, would cover this.
- **Include the value.** Conversion failures show the clipboard text. SQL and refusal failures do not.
- **Identify the row.** A row number is weak once a sorted query has moved rows. Showing the primary key, where there is one, would help.
- **Database detail.** SQLState and the vendor error code are in the exception and are not shown.
- **Say what was written.** The summary reports failures only. A count of rows updated and rows inserted is logged at DEBUG and could be shown.
- **List skipped cells.** Cells skipped as non-editable could be listed on request.
- **Let SsCellEditing say why.** `cellUpdateRequested` returns only true or false, so a refusal has no reason attached.
- **Give the application the result.** Paste returns nothing. A result object, or a listener, would let an application show or log the outcome its own way.

### Related, outside paste

- SsTable and a navigator sharing one RowSet is an open SeeSaw issue.
- Requiring the handler from the start, or defaulting insertion to off, would remove the late internal error.
- The demo comments in Example 6 and 7 that insertion is "not currently working for H2" may be out of date.

## Optional behaviour for the future

### A paste coordinator

One paste can raise four dialogs in a row: not enough rows, insert new rows, continue past non-editable cells, and the summary. A single window, opened only when there is something to ask or report, could replace them.

- **Questions in one place.** It shows what the paste will do: rows to overwrite, rows to insert, cells that will be skipped and why. The choices sit beside that, with one Paste and one Cancel.
- **Table status.** It shows the settings that explain the choices, such as insert enabled, sorting enabled, deletion enabled, and whether a handler is set.
- **Progress and result.** The same window shows progress for a large paste and then the summary, with the full failure list and a copy button.
- **Follow-up actions.** After an error it is the natural place for Undo, or for "continue with the remaining rows".

This fits the code as it stands. The pre-check already computes most of the plan before writing, and `showMessage` and `confirm` are already separate overridable methods. The step needed first is to split `paste` into a plan phase that changes nothing and an execute phase. That split also makes "check everything before writing" possible, and gives tests and applications a result to inspect.

### Undo

Undo would be most useful after an error, when part of a paste has been written.

- **By recording.** Before each write, keep the old value. For each insert, keep the new row's key. Undo writes the old values back and deletes the inserted rows. This needs a reliable way to find the rows again, so it depends on a primary key, and on the row still being as paste left it.
- **By transaction.** Turn auto-commit off for the paste, then commit, or roll back on undo or error. This is simpler and exact, but the connection may be shared with other components, the result is only undoable until commit, and the RowSet has to be re-queried after a rollback.
- A transaction also gives "all or nothing" for a whole paste, which recording does not.

### Other options

| Option | What it would do |
| --- | --- |
| Stop on first error | End the paste at the first failed cell or row, not only at a failed new row |
| Continue after a failed new row | Skip the row and carry on, the opposite of today's rule |
| All or nothing | Write nothing unless every cell can be written; needs a transaction or a full pre-check |
| Skip the non-editable question | Always skip non-editable cells without asking, or always refuse |
| Skip the insert question | Always insert, or never insert, when the clipboard overflows the rows |
| Empty-cell policy | Choose per table: set null, leave unchanged, or use the default value |
| Dry run | Show the plan and the problems without writing anything |
| Header row | Treat the first clipboard row as column names and match columns by name |
| Append mode | Paste every clipboard row as a new row, from a chosen column |
| Paste listener | Tell the application before and after a paste, with the result |

Most of these are settings on the key adapter or the table. They would be easier to add once paste has the plan and execute phases, because each one changes either what the plan allows or what execute does on a failure.

## Where the code and tests are

| What | Where |
| --- | --- |
| Copy, paste, clipboard parsing, dialogs, summary | `seesaw/src/main/java/dev/visdb/seesaw/table/SsTableKeyAdapter.java` (`copyText`, `parseClipboard`, `paste`, `showMessage`, `confirm`, `isHiddenColumn`) |
| Cell writes, inserts, editability, defaults | `seesaw/src/main/java/dev/visdb/seesaw/table/SsTableModel.java` (`setValueAt`, `trySetValueAt`, `insertRow`, `tryInsertRow`, `isCellEditable`, `isColumnEditable`, `isInsertRow`, `getDefaultValue`) |
| Key adapter set-up, insertion property, stuck-editor guard | `seesaw/src/main/java/dev/visdb/seesaw/SsTable.java` (`setInsertion`, `editingStopped`, `cellNoLongerEditable`) |
| Hooks an application implements | `SsCellEditing` (`isCellEditable`, `cellUpdateRequested`) and `SsDataGridHandler` (`performPostInsertOps` and others), both in `table/` |
| Tests | `seesaw/src/test/java/dev/visdb/seesaw/table/SsTableKeyAdapterTest.java`, on H2 |
| Demo with a handler | `seesaw-demo`, Example 5 with `DataGridExampleSupport` |

History: the paste guard, pre-check and summary came in with the SEC-AUDIT merge. The single insert for new rows, the stop rule, the handler requirement, the logging and the dialog wording came in with [PR 52](https://github.com/visdb-dev/seesaw/pull/52).
