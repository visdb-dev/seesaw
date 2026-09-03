/* *****************************************************************************
 * Copyright (C) 2025-2026, Ernie R Rael. All rights reserved.
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
package dev.visdb.seesaw.navigate;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.System.Logger;
import java.sql.SQLException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import dev.visdb.seesaw.datasources.RowSetOps;
import dev.visdb.seesaw.navigate.NavigateState.RowPositioning;
import dev.visdb.seesaw.navigate.RowsAction.Navigation;
import dev.visdb.seesaw.utils.JStuff;

import static dev.visdb.seesaw.navigate.RowSetState.setInserting;
import static dev.visdb.seesaw.navigate.RowsAction.*;
import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * {@code RowsActions} contains Actions used on a RowSet associated with
 * a {@link RowsModel}. They are accessed through {@link RowsModel}.
 * One or more of these Actions is generally put into
 * a {@code NavigationBar}.
 */
final class RowsActions {
  /** Logger for component */
  static final Logger logger = JStuff.getLogger();

  private final RowsModel rowsModel;

  RowsActions(RowsModel rowsModel) {
    this.rowsModel = rowsModel;
  }

  private RowSet getRowSet() {
    return rowsModel.getRowSet();
  }

  private NavigateState getNavState() {
    return rowsModel.getNavState();
  }

  private static final int countActionPerform[] = new int[RowsAction.values().length];
  /** Test/Debug. */
  static int getCount(RowsAction navAction) {
    return countActionPerform[navAction.ordinal()];
  }

  void startNavigationAction(RowsAction navAction) {
    logger.log(DEBUG, () -> sf("%s action (usually button clicked)", navAction));
    countActionPerform[navAction.ordinal()]++;
    getNavState().disableRowsetListeningFlag(
        navAction.toString()); // TODO: not needed for RowsModel???
    RowsModel.startRowsEvent(rowsModel, navAction);
  }

  void finishNavigationAction(RowsAction navAction) {
    RowsModel.finishRowsEvent(rowsModel);
    getNavState().enableRowsetListeningFlag(
        navAction.toString()); // TODO: not needed for RowsModel???
  }

  static Component dlgParent(EventObject e) {
    return e != null && (e.getSource() instanceof Component c) ? c : null;
  }

  private final Map<RowsAction, Action> actions = new HashMap<>();
  void run(RowsAction navAction) {
    get(navAction).actionPerformed(null);
  }
  Action get(RowsAction navAction) {
    Action action = actions.computeIfAbsent(navAction, key -> switch (key) {
      case ACT_FIRST -> new NavFirstAction();
      case ACT_PREVIOUS -> new NavPreviousAction();
      case ACT_NEXT -> new NavNextAction();
      case ACT_LAST -> new NavLastAction();
      case ACT_COMMIT -> new NavCommitAction();
      case ACT_REVERT -> new NavRevertRecordAction();
      case ACT_REFRESH -> new NavRefreshAction();
      case ACT_ADD -> new NavAddAction();
      case ACT_DELETE -> new NavDeleteAction();
      case ACT_GOTOROW -> new NavGotoRowAction();
      default -> throw new IllegalStateException();
    });
    if (rowsModel.getRowSet() == null)
      action.setEnabled(false);
    return action;
  }

  void disableAllActions() {
    actions.forEach((k, v) -> { v.setEnabled(false); });
  }

  private boolean disableVerifyEnabledCheck;
  void setVerifyEnabledFlag_DEBUG(boolean flag) {
    disableVerifyEnabledCheck = !flag;
  }

  private boolean verifyEnabled(AbstractAction act) {
    if (act.isEnabled() || disableVerifyEnabledCheck)
      return true;
    logger.log(ERROR, sf("disabled action '%s' called", act.getClass().getSimpleName()),
               new Exception("disabled action called"));
    return false;
  }

  /**
   * Action for the "First" button on the navigator.
   */
  @SuppressWarnings("serial")
  private final class NavFirstAction extends AbstractAction {
    /**
     * Constructor for the "First" Action.
     */
    public NavFirstAction() {
      super("First");
      putValue(LARGE_ICON_KEY,
               new ImageIcon(this.getClass().getClassLoader().getResource("images/first.gif")));
      putValue(SHORT_DESCRIPTION, "Navigate to First Record");
      //	        putValue(MNEMONIC_KEY, mnemonic);
    }

    /**
     * When the "first" button is pressed, the current record is saved and the user
     * is taken to the first row in the rowset.
     *
     * Since the rowset is on the first row, disable the "previous" button and
     * enable the "next" button.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      startNavigationAction(ACT_FIRST);
      try {
        if (!verifyEnabled(this))
          return;
        if (!getNavState().autoCommitUpdateRowToDatabase(rowsModel))
          return;

        getRowSet().first();

        getNavState().freshRow();
        getNavState().updateNavigatorRowAndCount();

        getNavState().getDbOps().performNavigationOps(Navigation.First);

      } catch (final SQLException se) {
        logger.log(ERROR, "SQL Exception.", se);
        JOptionPane.showMessageDialog(
            dlgParent(e),
            "Exception occured while updating row or moving the cursor.\n" + se.getMessage());
      } finally {
        finishNavigationAction(ACT_FIRST);
      }
    }
  } // end NavFirstAction

  /**
   * Action for the "Previous" button on the navigator.
   */
  @SuppressWarnings("serial")
  private final class NavPreviousAction extends AbstractAction {
    /**
     * Constructor for the "Previous" Action.
     */
    public NavPreviousAction() {
      super("Previous");
      putValue(LARGE_ICON_KEY,
               new ImageIcon(this.getClass().getClassLoader().getResource("images/prev.gif")));
      putValue(SHORT_DESCRIPTION, "Navigate to Previous Record");
      //	        putValue(MNEMONIC_KEY, mnemonic);
    }

    /**
     * When the "previous" button is pressed, the current record is saved and the
     * user is taken to the previous row in the rowset.
     *
     * If there are records in the rowset and the move to the previous record fails,
     * then the user is taken to the first row.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      startNavigationAction(ACT_PREVIOUS);
      try {
        if (!verifyEnabled(this))
          return;
        if (!getNavState().autoCommitUpdateRowToDatabase(rowsModel))
          return;

        if ((getRowSet().getRow() != 0) && !getRowSet().previous()) {
          getRowSet().first();
        }

        getNavState().freshRow();
        getNavState().updateNavigatorRowAndCount();

        getNavState().getDbOps().performNavigationOps(Navigation.Previous);

      } catch (final SQLException se) {
        logger.log(ERROR, "SQL Exception.", se);
        JOptionPane.showMessageDialog(
            dlgParent(e),
            "Exception occured while updating row or moving the cursor.\n" + se.getMessage());
      } finally {
        finishNavigationAction(ACT_PREVIOUS);
      }
    }
  } // end NavPreviousAction

  /**
   * Action for the "Next" button on the navigator.
   */
  @SuppressWarnings("serial")
  private final class NavNextAction extends AbstractAction {
    /** Constructor for the "Next" Action. */
    public NavNextAction() {
      super("Next");
      putValue(LARGE_ICON_KEY,
               new ImageIcon(this.getClass().getClassLoader().getResource("images/next.gif")));
      putValue(SHORT_DESCRIPTION, "Navigate to Next Record");
      //	        putValue(MNEMONIC_KEY, mnemonic);
    }

    /**
     * When the "next" button is pressed, the current record is saved and the user
     * is taken to the next row in the rowset.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      startNavigationAction(ACT_NEXT);
      try {
        if (!verifyEnabled(this))
          return;
        if (!getNavState().autoCommitUpdateRowToDatabase(rowsModel))
          return;

        getRowSet().next();

        getNavState().freshRow();
        getNavState().updateNavigatorRowAndCount();

        getNavState().getDbOps().performNavigationOps(Navigation.Next);

      } catch (final SQLException se) {
        logger.log(ERROR, "SQL Exception.", se);
        JOptionPane.showMessageDialog(
            dlgParent(e),
            "Exception occured while updating row or moving the cursor.\n" + se.getMessage());
      } finally {
        finishNavigationAction(ACT_NEXT);
      }
    }
  } // end NavNextAction

  /**
   * Action for the "Last" button on the navigator.
   */
  @SuppressWarnings("serial")
  private final class NavLastAction extends AbstractAction {
    /** Constructor for the "Last" Action. */
    public NavLastAction() {
      super("Last");
      putValue(LARGE_ICON_KEY,
               new ImageIcon(this.getClass().getClassLoader().getResource("images/last.gif")));
      putValue(SHORT_DESCRIPTION, "Navigate to Last Record");
      //	        putValue(MNEMONIC_KEY, mnemonic);
    }

    /**
     * When the "last" button is pressed, the current record is saved and the user
     * is taken to the last row in the rowset.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      startNavigationAction(ACT_LAST);
      try {
        if (!verifyEnabled(this))
          return;
        if (!getNavState().autoCommitUpdateRowToDatabase(rowsModel))
          return;

        getRowSet().last();

        getNavState().freshRow();
        getNavState().updateNavigatorRowAndCount();

        getNavState().getDbOps().performNavigationOps(Navigation.Last);

      } catch (final SQLException se) {
        logger.log(ERROR, "SQL Exception.", se);
        JOptionPane.showMessageDialog(
            dlgParent(e),
            "Exception occured while updating row or moving the cursor.\n" + se.getMessage());
      } finally {
        finishNavigationAction(ACT_LAST);
      }
    }
  } // end NavLastAction

  /**
   * Action for the "Commit" button on the navigator.
   */
  @SuppressWarnings("serial")
  private final class NavCommitAction extends AbstractAction {
    /** Constructor for the "Commit" Action. */
    public NavCommitAction() {
      super("Commit");
      putValue(LARGE_ICON_KEY,
               new ImageIcon(this.getClass().getClassLoader().getResource("images/commit.gif")));
      putValue(SHORT_DESCRIPTION, "Commit/Save Current Record");
      //	        putValue(MNEMONIC_KEY, mnemonic);
    }

    /**
     * When the "commit" button is pressed, any changes are committed to the database.
     *
     * If the user is on the 'insert' row (new record) at the time of a commit,
     * the record is inserted and the rowset is moved to the newly added/inserted row,
     * and the other navigation buttons are re-enabled.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      startNavigationAction(ACT_COMMIT);
      try {
        if (!verifyEnabled(this))
          return;
        if (RowSetState.isInserting(getRowSet())) {
          // IF on insert row add the row.

          // Check if the row can be inserted.
          if (!getNavState().canInsert()) {
            // TODO: condition was "if (!getNavState().dbOps.allowInsert())"
            //       which seems to indicate that dbOps did some validity
            //       checking on the data. Since dbOps has the container
            //       maybe multiple fields checked; hm, I'm guessing not
            //       after all, you'd think the rowSet would be needed.

            // Do nothing. Stay on insert row. User must
            // fix the data and save the row or cancel the insertion.
            return;
          }

          RowSetOps.insertRow(getRowSet());
          setInserting(getRowSet(), false);
          getNavState().getDbOps().performPostInsertOps(rowsModel);

          // It's unspecified where the insertRow goes, but
          // performPosInsertOps typically re-executes the query.
          // This code is assuming the new row goes at the end. This
          // seems to be counting on the new row having a new primary
          // key that puts it at the end of the table. Probably should
          // be some assist, dbOps?, for finding the new row.
          // But, for now, assume new row is at the end.
          // Note there's Statement.getGeneratedKeys(); but that, in
          // itself, has assumptions.

          getNavState().establishRowCountCurrentRow(RowPositioning.AT_LAST);

          getNavState().freshRow();
          getNavState().updateNavigatorRowAndCount();
        } else {
          // ELSE update the database based on the present row values.
          if (!getNavState().commitUpdateRowToDatabase(rowsModel))
            return;

          // commit, resultSet.updateRow(), does not generate an event.
          // ... getRowSet().absolute(getRowSet().getRow()) ...
          // was used to create an event. Old comment says
          // it was neede so "other" SSComponent bound to same column
          // was updated correctly. Think that situation is taken
          // care of with SSCommon's handleColumnChangeDone.

          getNavState().freshRow();
          getNavState().updateActionState();
          getNavState().getDbOps().performPostUpdateOps(rowsModel);
        }
      } catch (final SQLException se) {
        logger.log(ERROR, "SQL Exception.", se);
        JOptionPane.showMessageDialog(dlgParent(e),
                                      "Exception occured while saving row.\n" + se.getMessage());
      } finally {
        finishNavigationAction(ACT_COMMIT);
      }
    }
  } // end NavCommitAction

  /**
   * Action for the "RevertRecord" button on the navigator.
   */
  // TODO: Rename to NavRevertRecordAction
  @SuppressWarnings("serial")
  private final class NavRevertRecordAction extends AbstractAction {
    /** Constructor for the "RevertRecord" Action. */
    public NavRevertRecordAction() {
      super("RevertRecord");
      putValue(LARGE_ICON_KEY,
               new ImageIcon(this.getClass().getClassLoader().getResource("images/undo.gif")));
      putValue(SHORT_DESCRIPTION, "Undo/Revert Changes to Current Record");
      //	        putValue(MNEMONIC_KEY, mnemonic);
    }

    /**
     * When the "revert" button is pressed, revert any changes to the current record.
     *
     * This button can also be used to cancel an insertion so if on the insert row,
     * re-enable the other navigation buttons.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      RowsAction act_revert = getRowSet() instanceof CachedRowSet ? ACT_REVERT_FORCE : ACT_REVERT;
      startNavigationAction(act_revert);
      try {
        if (!verifyEnabled(this))
          return;
        // Call move to current row if on insert row.
        boolean wasInserting = RowSetState.isInserting(getRowSet());
        if (wasInserting) {
          getRowSet().moveToCurrentRow();
        }

        // THIS FUNCTION IS NOT NEED IF ON INSERT ROW
        // BUT MOVETOINSERTROW WILL NOT TRIGGER ANY EVENT SO FOR THE SCREEN
        // TO UPDATE WE NEED TO TRIGGER SOME THING.
        // SINCE USER IS MOVED TO CURRENT ROW PRIOR TO INSERT IT IS SAFE TO
        // CALL CANCELROWUPDATE TO GET A TRIGGER

        // TODO: could cleanup above comment and use issueRowChanged.

        getRowSet().cancelRowUpdates();

        setInserting(getRowSet(), false);
        getNavState().getDbOps().performCancelOps();

        // Only attempt to refresh row if we have at least one record
        if (getRowSet().getRow() > 0) {
          getRowSet().refreshRow();
        }

        getNavState().freshRow();
        getNavState().updateNavigatorRowAndCount();
        if (wasInserting)
          rowsModel.syncSyncManager(); // TODO does this seem right.
      } catch (final SQLException se) {
        logger.log(ERROR, "SQL Exception.", se);
        JOptionPane.showMessageDialog(
            dlgParent(e), "Exception occured while reverting changes.\n" + se.getMessage());
      } finally {
        finishNavigationAction(act_revert);
      }
    }
  } // end NavRevertRecordAction

  /**
   * Action for the "Refresh" button on the navigator.
   */
  @SuppressWarnings("serial")
  private final class NavRefreshAction extends AbstractAction {
    /** Constructor for the "Refresh" Action. */
    public NavRefreshAction() {
      super("Refresh");
      putValue(LARGE_ICON_KEY,
               new ImageIcon(this.getClass().getClassLoader().getResource("images/refresh.gif")));
      putValue(SHORT_DESCRIPTION, "Refresh/Reload All Records");
      //	        putValue(MNEMONIC_KEY, mnemonic);
    }

    /**
     * When the "refresh" button is pressed, re-fetch all rows from the database
     * and move the cursor to the first record. If there are no records, disable
     * the other navigation buttons. Disable the first and previous buttons
     * regardless because the rowset will be on the first record.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      startNavigationAction(ACT_REFRESH);
      // TODO: refresh: should this be enabled if callExecute if false?
      // TODO: use performRefreshOps? Let it return false if handle it here?
      try {
        //if (Boolean.TRUE /*|| getNavState().callExecute*/) {
        if (!verifyEnabled(this))
          return;

        getRowSet().execute();

        if (!getRowSet().next()) {
          // THERE ARE NO RECORDS IN THE ROWSET
          getNavState().rowCount = 0;
        } else {
          // WE HAVE ROWS GET THE ROW COUNT AND MOVE BACK TO FIRST ROW
          getRowSet().last();
          getNavState().rowCount = getRowSet().getRow();
          getRowSet().first();
        }

        getNavState().freshRow();
        getNavState().updateNavigatorRowAndCount();

        //}

        getNavState().getDbOps().performRefreshOps();

      } catch (final SQLException se) {
        logger.log(ERROR, "SQL Exception.", se);
        JOptionPane.showMessageDialog(dlgParent(e),
                                      "Exception occured refreshing the data.\n" + se.getMessage());
      } finally {
        finishNavigationAction(ACT_REFRESH);
      }
    }
  } // end NavRefreshAction

  /**
   * Action for the "Add" button on the navigator.
   */
  @SuppressWarnings("serial")
  private final class NavAddAction extends AbstractAction {
    /** Constructor for the "Add" Action. */
    public NavAddAction() {
      super("Add");
      putValue(LARGE_ICON_KEY,
               new ImageIcon(this.getClass().getClassLoader().getResource("images/add.gif")));
      putValue(SHORT_DESCRIPTION, "Add a New Record");
      //	        putValue(MNEMONIC_KEY, mnemonic);
    }

    /**
     * When the "add" button is pressed, move to the insert row and
     * disable other navigation buttons except for Commit and Undo (cancel).
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      startNavigationAction(ACT_ADD);
      try {
        if (!verifyEnabled(this))
          return;
        // Commit changes for current row to database
        // Ignore return since doesn't matter if there's nothing to do.
        getNavState().autoCommitUpdateRowToDatabase(rowsModel);

        // Move to insert row, update status, and update combo navigator (if applicable)
        getRowSet().moveToInsertRow();
        setInserting(getRowSet(), true);

        //
        // TODO:
        //		The following does not inspire confidence.
        //
        //		AFAICT, performPreInsertOps causes saving of the
        //		new fresh/empty values on top of the previous
        //		db record in the undo/redo stuff: e.g. "[Smith, ]"
        //
        //		So, can't do freshRow(), which clears undo/redo
        //		until after performPreInsertOps(), and any listeners
        //		that are triggered by it, is finished.
        //
        //		Ideally, nothing should be done with undo/redo
        //		during performPreInsertOps(). Should also take
        //		a detailed look at next/prev record and such.
        //

        // If we don't use invokeLater() here,
        // the values from the just-committed prior record
        // are displayed for the insert row.

        // quick in/out
        //SwingUtilities.invokeLater(() -> {
        //

        // The values set during preInsertOps are collected in the
        // undo/redo stack. (undo/redo must be enabled for PreInsertOps.)
        // The preInsertOps flag may be used to avoid DB access
        // related to setting up an empty undo/redo stack.
        RowSetState.setPreInsertOps(getRowSet(), true);
        try {
          getNavState().getDbOps().performPreInsertOps();
        } catch (Exception ex) {
          // Catch exception to insure that preInsertOps false.
          logger.log(ERROR, "SQL Exception in preInsertOps.", ex);
          JOptionPane.showMessageDialog(
              dlgParent(e), "Pleae report exception in preInsertOps.\n" + ex.getMessage());
        }
        SwingUtilities.invokeLater(() -> {
          RowSetState.setPreInsertOps(getRowSet(), false);
          getNavState().freshInsertRow();
        });

        //
        //});
        //

        getNavState().updateActionState();
      } catch (final SQLException se) {
        logger.log(ERROR, "SQL Exception.", se);
        JOptionPane.showMessageDialog(
            dlgParent(e), "Exception occured while moving to insert row.\n" + se.getMessage());
      } finally {
        finishNavigationAction(ACT_ADD);
      }
    }
  } // end NavAddAction

  /**
   * Action for the "Delete" button on the navigator.
   */
  @SuppressWarnings("serial")
  private final class NavDeleteAction extends AbstractAction {
    /** Action for the "Delete" Action. */
    public NavDeleteAction() {
      super("Delete");
      putValue(LARGE_ICON_KEY,
               new ImageIcon(this.getClass().getClassLoader().getResource("images/delete.gif")));
      putValue(SHORT_DESCRIPTION, "Delete the Current Record");
      //	        putValue(MNEMONIC_KEY, mnemonic);
    }

    /**
     * When the "delete" button is pressed, delete the current row and move
     * to the next row. If the deleted row is the last row then move the the
     * record that was previous to the deleted row/record.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      startNavigationAction(ACT_DELETE);
      try {
        if (!verifyEnabled(this))
          return;
        if (getNavState().getConfirmDeletes()) {
          final int answer = JOptionPane.showConfirmDialog(
              dlgParent(e), "Are you sure you want to delete this record?", "Delete Present Record",
              JOptionPane.YES_NO_OPTION);
          if (answer != JOptionPane.YES_OPTION) {
            return;
          }
        }

        if (!getNavState().getDbOps().allowDelete()) {
          return;
        }

        // CAPTURE CURRENT ROW PRE-DELETION
        final int tmpPosition = getNavState().getRow();

        // SET ANTICIPATED ROW COUNT POST-DELETION
        final int tmpSize = getNavState().rowCount - 1;

        // PERFORM ANY PRE DELETION OPS
        getNavState().getDbOps().performPreDeletionOps();

        // DELETE ROW FROM ROWSET
        RowSetOps.deleteRow(getRowSet());

        // PERFORM ANY POST DELETION OPS (WHICH MAY INVOLVE REQUERYING WHICH IS NEEDED FOR H2)
        getNavState().getDbOps().performPostDeletionOps(rowsModel);

        // UPDATE TOTAL ROW COUNT
        getNavState().rowCount = tmpSize;

        // TRY TO NAVIGATE TO THE RECORD AFTER THE DELETED RECORD, OTHERWISE GO TO
        // WHATEVER IS THE LAST RECORD
        if ((tmpPosition <= getNavState().rowCount) && (tmpPosition > 0)) {
          getRowSet().absolute(tmpPosition);
        } else {
          getRowSet().last();
        }

        getNavState().freshRow();
        // UPDATE THE STATUS OF THE NAVIGATOR
        getNavState().updateNavigatorRowAndCount();

      } catch (final SQLException se) {
        logger.log(ERROR, "SQL Exception.", se);
        JOptionPane.showMessageDialog(dlgParent(e),
                                      "Exception occured while deleting row.\n" + se.getMessage());
      } finally {
        finishNavigationAction(ACT_DELETE);
      }
    }
  } // end NavDeleteAction

  /**
   * The {@linkplain NavGotoRowAction} is used with {@link RowNumberSpinner}.
   * This action embeds a {@link SpinnerNumberModel} that tracks
   * the row number and its limits.
   */
  // TODO: should this be listening to SpinnerNumberModel?
  @SuppressWarnings("serial")
  final class NavGotoRowAction extends AbstractAction {
    private NavGotoRowAction() {}

    @Override
    public void actionPerformed(ActionEvent e) {
      // Avoid re-entrancy. Unlike buttons, spinner changes go both ways.
      // While the rowset listeners are removed,
      // we're in the middle of processing, possibly  a row move.
      if (!getNavState().rowsetListenerAdded)
        return;

      startNavigationAction(ACT_GOTOROW);
      try {
        if (!verifyEnabled(this))
          return;
        int row = (int) getNavState().getRowNumberModel().getNumber();

        // TODO: autoCommitUpdateRowToDatabase returns false if the getRowSet()
        //       is not modifiable. Maybe a multi-state return one
        //       value could be OK, NOT_WRITEABLE, ERROR...
        //       Only return if error.
        //

        // TODO: check row is dirty.
        //       In normal operation control from autoCommit prevents getting here.
        //       Except that setRowSet gets in here from spinner.
        //       In here for *new* RowSet, the old has been stashed.
        // //       BUG: Skip the commit if part of setRowSet's new row,
        // //            otherwise when new rowSet is dirty it gets commited.
        // //boolean dirty = getNavState().undoRow.isDirty();

        logger.log(DEBUG, () -> "Record number manually updated to " + row + ".");
        // If row's in good range,
        //     and
        //        not already on current row or not ok to skip move
        if ((row <= getNavState().rowCount) && (row > 0)
            && !(getRowSet().getRow() == row && e != null
                 && RowsAction.OK_SKIP_CURSOR_MOVE.equals(e.getActionCommand()))) {
          if (!getNavState().autoCommitUpdateRowToDatabase(rowsModel))
            return;
          getRowSet().absolute(row);
          getNavState().freshRow(); // only do this if row changed and commit
        } else
          logger.log(WARNING, "skipping commit and cursor move");

        getNavState().updateNavigatorRowAndCount();
      } catch (final SQLException se) {
        logger.log(ERROR, "SQL Exception.", se);
        JOptionPane.showMessageDialog(null, //NavigateActions.this,
                                      "Exception occured while going to row.\n" + se.getMessage());
      } finally {
        finishNavigationAction(ACT_GOTOROW);
      }
    }
  }
}
