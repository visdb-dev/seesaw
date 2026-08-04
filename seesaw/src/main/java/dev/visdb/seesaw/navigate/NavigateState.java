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
package dev.visdb.seesaw.navigate;

import java.lang.System.Logger;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.sql.RowSet;
import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.SpinnerNumberModel;

import com.google.common.collect.MapMaker;
import com.nqadmin.swingset.*;

import dev.visdb.seesaw.core.DBComboBox2;

import dev.visdb.seesaw.datasources.DbOps;
import dev.visdb.seesaw.datasources.RSC;
import dev.visdb.seesaw.datasources.RowSetOps;
import dev.visdb.seesaw.navigate.RowsEvent.OperatorKind;
import dev.visdb.seesaw.navigate.RowsEvent.RowSetEventType;
import dev.visdb.seesaw.navigate.UndoRedo.Change;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SSComponent;
import dev.visdb.seesaw.utils.SSUtils;
import dev.visdb.seesaw.utils.SyncManager;

import com.raelity.lib.eventbus.WeakEventBus;
import com.raelity.lib.eventbus.WeakSubscribe;

import static dev.visdb.seesaw.navigate.RowsAction.*;
import static dev.visdb.seesaw.navigate.Utils.getGlobalEventBus;
import static dev.visdb.seesaw.utils.JStuff.sf;
import static dev.visdb.seesaw.utils.SSUtils.objectID;
import static java.lang.System.Logger.Level.*;

//TODO: Handle CachedRowSet Paging

/*
 * External controls
 *     - confirmDeletes
 *     - DbOps
 *     - allowDelete (enableDeletion, deleteOK)
 *     - allowInsert (enableInsertion, insertOK)
 *     - writeable (writeOK)
 *     - NavCombo
 */

/**
* {@link NavigateState} contains RowSet state which get reflected in RowsActions.
 * There are {@linkplain Action}s for navigation, allowInsert, and allowDelete of
records in a RowSet.
There are {@linkplain ButtonModel}s for state, such as row dirty
 * that could be connected to a commit UI component. Readonly versions of the
 * state buttons are available.
 * <p>
 * This class listens for events about RowSet modifications and sends navigation
 * events (IN THE FUTURE). When navigating to a row, this class stashes the
 * current values (the values fetched from the datase); these are used for undo.
 * TODO: undo/redo history.
 * NOTE: undo and refresh are not the same thing.
 * NOTE: FetchSize
 *
 * They are used by {@linkplain SSDataNavigator}.

There are various navigation management parameters that may be set.
- auto commit









Component that can be used for data navigation. It provides buttons for
navigation, allowInsert, and allowDelete of records in a RowSet. The
allowWrite of a RowSet can be prevented using the setModificaton()
method. Any changes made to the columns of a record will be updated whenever
there is a navigation.
<p>
 * For example if you are displaying three columns using the JTextField and the
 * user changes the text in the text fields then the columns will be updated to
 * the new values when the user navigates the RowSet. If the user wants to
 * revert the changes he made he can press the Undo button, however this must be
 * done before any navigation. Once navigation takes place changes can't be
 * reverted using Undo button (has to be done manually by the user).
 */
//
// TODO: this is a package class, there shouldn't be public methods.
//
final class NavigateState {
  /**
   * Which key does increment/decrement.
   */
  public enum UpDownKeysAction {
    /** This is like data in a grid. */
    UP_DECREMENT,
    /** Up key increments. */
    UP_INCREMENT,
  }

  /** Logger for component */
  private static final Logger logger = JStuff.getLogger();

  //
  // TODO:
  //     For now, have the defaults here. In the future,
  //     probably want to set the defaults from some
  //     configurable spot, via CentralLookup?
  //     Maybe: interface SwingSetConfiguration {}
  //
  private static final boolean V3_BUTTONS_DEFAULT = false;
  private static final boolean AUTO_COMMIT_DEFAULT = false;

  /** logger for package use. */
  static Logger getLogger() { return logger; }

  ////////////////////////////////////////////////////////////////////////////
  //
  // TODO: Consider
  // The usage of these static access functions should probably
  // be reduced, if not eliminated, in favor of including a navAction
  // reference in the SSComponent.
  //

  // TODO: Should the static methods have instance counterparts,
  //		 e.g. onActiveRow. Then could make direct queries when
  //		 a Navigation is available.

  /**
   * Return NavState for the RowSet.
   * If existing one, for given rowSet, not found;
   * the returned navState.getRowSet is null;
   * setupRowSet must be invoked before other usage.
   * @param rowSet
   * @return
   */
  synchronized static NavigateState getOrCreate(RowSet rowSet) {
    Objects.requireNonNull(rowSet);
    //NavigateState navState = RowSetState.getNavigateState(rowSet);
    NavigateState navState = get(rowSet);
    if (navState == null) {
      // RowSetState.setNavigateState(rowSet, navState = new NavigateState(null));
      navState = new NavigateState(null); // TODO: why null?
      if (navState.rowSetState != null)
        throw new IllegalStateException("navState.rowSetState not null");

      // At least for now, some of the RowSetState can be there without NavState.
      // if (RowSetState.getExistingRowSetState(rowSet) != null)
      // 	throw new IllegalStateException("RowSetState.get... not null");

      navState.rowSetState = RowSetState.getRowSetState(rowSet);
    }
    return navState;
  }

  /**
   * Return NavState for the RowSet.
   * If existing one, for given rowSet, not found;
   * the returned navState.getRowSet is null;
   * setupRowSet must be invoked before other usage.
   * @param rowSet
   * @return
   */
  synchronized static NavigateState get(RowSet rowSet) {
    Objects.requireNonNull(rowSet);
    //return RowSetState.getNavigateState(rowSet);
    return navigateState.get(rowSet);
  }

  private static final Map<RowSet, NavigateState> navigateState
      = new MapMaker().weakKeys().weakValues().makeMap();

  /**
   * @return
   */
  static int count() {
    // Can't depend on size() method when weakKeys.
    return SSUtils.size(navigateState);
  }

  //////////////////////////////////////////////////////////////////////
  //
  // INSTANCE starts here
  //

  BusReceiver busReceiver; // Must have a strong reference.
  private void setupEventBus() {
    busReceiver = new BusReceiver();
    WeakEventBus.register(busReceiver, getGlobalEventBus());
  }

  //Weak Subscriber notes: {@link com.nqadmin.swingset.navigate.Utils}.

  /**
   * Listener(s) for the underlying RowSet used to update the bound SwingSet
   * component. When working with a {@linkplain javax.sql.rowset.CachedRowSet} there are
   * extra steps involved which require the listener to ignore some events, see
   * {@link RowSetState#acceptCachedRowSetChanges(javax.sql.rowset.CachedRowSet, java.lang.Runnable) }.
   */

  class BusReceiver {
    /**
     * Ignore events generated by this class: OperatorKind.ACTION,
     * and only process events for this RowSet.
     */
    @WeakSubscribe
    public void handleRowSetEvent(RowsEvent ev) {
      logger.log(DEBUG, () -> sf("%s %s", objectID(getRowSet()), ev.toString()));

      // TODO: ev.getModel != getModel /// not RowSet
      if (ev.getKindOperator() == OperatorKind.ACTION || ev.getRowSet() != getRowSet()) {
        // System.err.println("     ***** SKIP *****");
        return;
      }
      if (ev.getEventTypes().contains(RowSetEventType.ROW_SET_CHANGED)) {
        // Update the record count. Leave positioned at first row.
        try {
          logger.log(DEBUG, "Updating row count.");
          establishRowCountCurrentRow(RowPositioning.AT_FIRST);
        } catch (final SQLException se) { logger.log(ERROR, "SQL Exception.", se); }
      }
      try {
        logger.log(DEBUG, "Calling updateNavigator().");
        freshRow();
        updateNavigatorRowAndCount();
      } catch (final SQLException se) { logger.log(ERROR, "SQL Exception.", se); }
    }

    @WeakSubscribe
    public void handleNewRowSetEvent(RowsModelNewRowSetEvent ev) {
      logger.log(DEBUG, () -> sf("%s %s", objectID(getRowSet()), ev.toString()));

      // Return if not our row set.
      if (ev.getRowsModel().getRowSet() != getRowSet()) return;

      try {
        updateNavigatorRowAndCount();
      } catch (SQLException ex) { logger.log(ERROR, (String) null, ex); }
    }

    // Following are typically from RowSetOps.

    @WeakSubscribe
    public void handleColumnChangeStart(ColumnChangeStartEvent ev) {
      if (!ev.matches(getRowSet())) return;

      // Our RowSet's row has changed
      logger.log(TRACE, () -> ev.toString());
      try {
        // TODO what about ev.getSource == null ?
        ((SSComponent) ev.getSource()).addUndoableChange(ev);
        // TODO: don't do the rest of this stuff if exception?
      } catch (SQLException ex) { logger.log(ERROR, "Undo/redo exception", ex); }

      adjustErrorComponentState(ev.getRSC(), ev.isError());
      Utils.postColumnChangeDone(ev);
    }

    @WeakSubscribe
    public void handleColumnUndoRedo(ColumnUndoRedoEvent ev) {
      if (!ev.matches(getRowSet())) return;

      // Our RowSet's row had an undo/redo.
      logger.log(TRACE, () -> ev.toString());
      adjustErrorComponentState(ev.getRSC(), ev.isError());
      Utils.postColumnChangeDone(ev);
    }

    @WeakSubscribe
    public void handleFocusChangeEvent(FocusChangeEvent ev) {
      undoRow.focusChange(ev);
    }

    @WeakSubscribe
    public void handleDbOpsChange(DbOpsChangeEvent ev) {
      if (Objects.equals(getDbOps(), ev.getDbOps())) updateActionState();
    }
  }

  /** return true if number of components in error changed. */
  boolean adjustErrorComponentState(RSC rsc, boolean isError) {
    int sz = errorComponents.size();
    if (isError) errorComponents.add(rsc);
    else errorComponents.remove(rsc);
    logger.log(TRACE,
               ()
                   -> sf("{%s} %s error: %b, sz: %d -> %d", JStuff.getCaller(4),
                         rsc.getColumnForLog(), isError, sz, errorComponents.size()));

    boolean compsChange = sz != errorComponents.size();
    if (compsChange) {
      if (rsc instanceof SSComponent comp) comp.decorate();
      else throw new IllegalStateException("Not SSComponent");
    }
    updateActionState();
    return compsChange;
  }

  // TODO: Also have Set<SSComponentInterface> modifiedComponents.
  //		 Paint modified/OK fields with identifying color, e.g. yellow.
  private final Set<RSC> errorComponents;

  /** Undo/redo this this rowset */
  final UndoRow undoRow;

  /** Row number for current record in RowSet. */ // TODO:
  // TODO: is this needed? Save much? Just use rs.getRow() always.
  // currentRow is written by updateNavigatorRowAndCount to getRow()
  private int currentRow = 0;

  /** Container (frame or internal frame) which contains the navigator. */
  private DbOps dbOps = new DbOps() {};

  /**
   * SSDBComboBox used for navigation if applicable.
   * <p>
   * Allows Navigator to disable it when a row is inserted and enable it
   * when that row is saved.
   * <p>
   * TODO Consider writing a PropertyChangeListener for onInsertRow instead.
   */
  private DBComboBox2<?, ?, ?> navCombo = null;

  private SyncManager<?> syncer = null;

  /** Number of rows in RowSet. Set to zero if next() method returns false. */
  /*RowsActions*/ int rowCount = 0;

  /** RowSet from which component will create/set values. */
  private /*final*/ RowSet rowSet;

  /** Indicates if rowset listener is added (or removed) */
  // XXX
  /*RowsActions*/ boolean rowsetListenerAdded = false;

  private RowSetState rowSetState;

  /**
   * Create actions and models for the RowSet.Note that _rowSet may be null for a Dummy.
   *
   * @param rowSet   the RowSet to which the navigator is bound to
   */
  //
  // TODO: RowsModel
  // TODO: create actions on demand; could have a supplier in RowsAction enum
  //
  @SuppressWarnings("LeakingThisInConstructor")
  private NavigateState(RowSet rowSet) {
    v3Buttons = V3_BUTTONS_DEFAULT;
    autoCommit = AUTO_COMMIT_DEFAULT;

    // TODO: should we be listening to SpinnerNumberModel
    //       and not using an Action?

    rowNumberModel = new SpinnerNumberModel(1, 1, 1, 1);
    setUpDownKeysAction(UpDownKeysAction.UP_DECREMENT);

    undoRow = new UndoRow();
    errorComponents = new HashSet<>();

    setupEventBus();

    if (rowSet == null) return;
    setupRowSet(rowSet);
  }

  void setupRowSet(RowSet rowSet) {
    if (this.rowSet != null) throw new IllegalStateException("NavState already has a RowSet");
    this.rowSet = rowSet;
    navigateState.put(rowSet, this);
    setupRowSet();
  }

  /**
   * Sets the RowSet for the navigator; the RowSet's current row is preserved.
   * The RowSet's select query should already be executed.
   *
   * @param _rowSet data source for navigator
   */
  private void setupRowSet() {
    try {
      RowsModel.verifyExecuted(getRowSet());
      if (getRowSet().getRow() != 0) {
        establishRowCountCurrentRow(RowPositioning.AT_CURRENT);
      } else {
        // At position 0, are there any rows in the given rowset.
        if (!getRowSet().next()) {
          rowCount = 0;
          currentRow = 0;
        } else {
          // There are rows
          establishRowCountCurrentRow(RowPositioning.AT_FIRST);
        }
      }
    } catch (final SQLException se) { logger.log(ERROR, "SQL Exception.", se); }

    // Add rowset listener.
    enableRowsetListeningFlag("setupRowSet");

    try {
      // freshRow();	// ************************** remove, setupRow only happens once.
      updateNavigatorRowAndCount();
    } catch (final SQLException se) { logger.log(ERROR, "SQL Exception.", se); }

    // TODO: This is new since first time NavGroupState was implemented.
    //       I think that doing setRowModified(false) a few lines up
    //       before the updateNavigatorRowAndCount() should take care of button state
    //       that the following is supposed to handle.
    //
    // // ENABLE OTHER BUTTONS IF NEED BE.

    // // THIS IS NEEDED TO HANDLE USER LEAVING THE SCREEN IN AN INCONSISTENT
    // // STATE EXAMPLE: USER CLICKS ADD BUTTON, THIS DISABLES ALL THE BUTTONS
    // // EXCEPT COMMIT & UNDO. WITH OUT COMMITING OR UNDOING THE ADD USER
    // // CLOSES THE SCREEN. NOW IF THE SCREEN IS OPENED WITH A NEW SSROWSET.
    // // THE REFRESH, ADD & DELETE WILL BE DISABLED.
    // // 2019-11-11: only enabling add/delete if this.allowWrite==true
    // refreshButton.setEnabled(true);
    // if (allowInsert && allowWrite) {
    // 	addButton.setEnabled(true);
    // }
    // if (allowDelete && allowWrite) {
    // 	deleteButton.setEnabled(true);
    // }
  }

  /**
   * Returns the RowSet being used.
   *
   * @return returns the RowSet being used.
   */
  final RowSet getRowSet() { return rowSet; }

  /**
   * @return returns the RowSet's current row being used.
   */
  final int getRow() {
    return currentRow;
    // try {
    // 	return getRowSet().getRow();
    // } catch (SQLException ex) {
    // 	SSUtils.randomSQLException(ex, logger);
    // 	return 0;
    // }
  }

  enum RowPositioning { AT_CURRENT, AT_FIRST, AT_LAST }
  /**
   * Determine how many rows in row set.
   *
   * @param pos where to leave the cursor
   * @throws SQLException
   */
  void establishRowCountCurrentRow(RowPositioning pos) throws SQLException {
    switch (pos) {
      case AT_CURRENT -> {
        int initial_row = getRowSet().getRow();
        getRowSet().last();
        rowCount = getRowSet().getRow();
        getRowSet().absolute(initial_row);
        currentRow = initial_row;
      }
      case AT_FIRST -> {
        getRowSet().last();
        rowCount = getRowSet().getRow();
        getRowSet().first();
        currentRow = getRowSet().getRow(); // Should be 1.
      }
      case AT_LAST -> {
        getRowSet().last();
        rowCount = getRowSet().getRow();
        currentRow = getRowSet().getRow();
      }
    }
  }

  /**
   * Specifies how the up/down arrows increment/decrement.
   * However the up key is specified, the down key does the opposite.
   * @param act the up key behavior
   */
  public final void setUpDownKeysAction(UpDownKeysAction act) {
    int stepsize = act == UpDownKeysAction.UP_DECREMENT   ? -1
                   : act == UpDownKeysAction.UP_INCREMENT ? 1
                                                          : 0;
    if (stepsize != 0) rowNumberModel.setStepSize(stepsize);
  }

  /**
   * Perform the specified undo/redo cmd on the specified component.
   * @param comp ssComponent
   * @param cmd undo or redo
   * @return new value, only for logging
   * @throws java.sql.SQLException
   */
  // TODO: Should this be public? NO, go through the static method in this class
  // TODO: SSComponent vs RSC
  Change doUndoRedo(SSComponent comp, UndoRedo cmd) throws SQLException {
    if (!UndoRedo.isUndoRedoEnabled(comp)) throw new IllegalStateException("UNDO/REDO disabled");
    Change change = undoRow.undoRedoChange(comp, cmd);
    if (change == UndoRedo.NO_CHANGE) SSUtils.beep();
    else {
      if (change.isError()) errorComponents.add(comp);
      else errorComponents.remove(comp);
      comp.undoRedoUpdateObject(cmd, change);
    }
    updateActionState();
    return change;
  }

  private final SpinnerNumberModel rowNumberModel;
  SpinnerNumberModel getRowNumberModel() { return rowNumberModel; }

  /*RowsActions*/ boolean autoCommitUpdateRowToDatabase() throws SQLException {
    if (!undoRow.isDirty()) return true; // all is OK
    return commitUpdateRowToDatabase();
  }

  /**
   * Common code to commit changes to the database from the rowset if
   * modifications are allowed; called before every action.
   * After committing, it performs any post-update operations.
   * <p>
   * If allowWrite==false, then skip the update and return as
   * successful, unless we have an empty rowset.
   * Checks {@link DbOps#allowUpdate() }.
   *
   * @param performPostUpdateOps true if performPostUpdateOps() should
   * 	be called after successful update, otherwise false
   *
   * @return true unless there are no records OR dbOps.allowUpdate() returns false
   * @throws SQLException SQL Exception if rowset call to updateRow() fails
   */
  /*RowsActions*/ boolean commitUpdateRowToDatabase() throws SQLException {
    // check for an empty rowset
    if (getRowSet().getRow() == 0) {
      return false; // weird state
    }

    boolean updateOK = true;

    // There is at least one row; update whether or not it is dirty.
    // If not read-only then continue attempt to update database
    // based on current rowset values
    if (Boolean.FALSE) {
      // There is a subtle difference. But reading the comments
      // There's special deference given to dbOps.allowUpdate().
      // WONDER WHAT THAT'S ABOUT.
      if (updateOK && getAllowWrite()) {
        if (!getDbOps().allowUpdate()) {
          updateOK = false;
        } else {
          RowSetOps.updateRow(getRowSet());
        }
      }
    }

    if (updateOK && canUpdate()) {
      RowSetOps.updateRow(getRowSet());
      getDbOps().performPostUpdateOps();
    } else updateOK = false; // can't update, so return false

    return updateOK;
  }

  // //
  // // NOTE: looking at h2, doing deleteRow also clears updateRow
  // //
  // /**
  //  * Before something like refresh/delete Actions should clear the updateRow.
  //  */
  // private void clearUpdateRow()
  // {
  // }

  /**
   * Returns true if the RowSet contains one or more rows, else false.
   *
   * @return return true if RowSet contains data else false.
   */
  public boolean containsRows() { return rowCount != 0; }

  /**
   * @return boolean indicating if the navigator is on an insert row
   */
  public boolean isOnInsertRow() { return RowSetState.isInserting(getRowSet()); }

  /**
   * Adds listener to the rowset
   */
  /*RowsActions*/ void enableRowsetListeningFlag(String tag) {
    // XXX
    if (!rowsetListenerAdded) {
      rowsetListenerAdded = true;
      logger.log(DEBUG, () -> sf("RowsetListener: %s: %s: is ON.", objectID(getRowSet()), tag));
    }
  }

  /**
   * Removes listener from the rowset
   */
  /*RowsActions*/ void disableRowsetListeningFlag(String tag) {
    // XXX
    if (rowsetListenerAdded) {
      rowsetListenerAdded = false;
      logger.log(DEBUG, () -> sf("RowsetListener: %s: %s: is OFF.", objectID(getRowSet()), tag));
    }
  }

  //////////////////////////////////////////////////////////////////////
  //
  // External control.
  //

  /**
   * Indicator to cause the navigator to skip the execute() function call on the
   * specified RowSet. Must be false for MySQL (see FAQ).
   */

  /** Indicator to force confirmation of RowSet deletions. */
  private boolean confirmDeletes = true;

  /** Indicator to allow/disallow deletions from the RowSet. */
  private boolean allowDelete = true;

  /** Indicator to allow/disallow insertions to the RowSet. */
  private boolean allowInsert = true;

  /** Indicator to allow/disallow changes to the RowSet. */
  private boolean allowWrite = true;

  /** Indicator to allow/disallow edits to a RowSets record. */
  private boolean allowUpdate = true;

  /**
   * Sets the confirm deletion indicator. If set to true, every time delete button
   * is pressed, the navigator pops up a confirmation dialog to the user. Default
   * value is true.
   *
   * @param confirmDeletes indicates whether or not to confirm deletions
   */
  // TODO: WHAT?
  void setConfirmDeletes(boolean confirmDeletes) { this.confirmDeletes = confirmDeletes; }

  /**
   * Returns true if deletions must be confirmed by user, else false.
   *
   * @return returns true if a confirmation dialog is displayed when the user
   *         deletes a record, else false.
   */
  boolean getConfirmDeletes() { return confirmDeletes; }

  /**
   * Enables or disables the row deletion button. This method should be used if
   * row deletions are not allowed. True by default.
   *
   * @param deletion indicates whether or not to allow deletions
   */
  void setAllowDelete(boolean deletion) {
    this.allowDelete = deletion;
    updateActionState();
  }

  /**
   * Returns true if deletions are allowed, else false.
   *
   * @return returns true if deletions are allowed, else false.
   */
  boolean getAllowDelete() { return allowDelete; }

  /**
   * Enables or disables the row insertion button. This method should be used if
   * row insertions are not allowed. True by default.
   *
   * @param insertion indicates whether or not to allow insertions
   */
  void setAllowInsert(boolean insertion) {
    this.allowInsert = insertion;
    updateActionState();
  }

  /**
   * Returns true if insertions are allowed, else false.
   *
   * @return returns true if insertions are allowed, else false.
   */
  boolean getAllowInsert() { return allowInsert; }

  /**
   * Enables or disables the modification-related buttons on the SSDataNavigator.
   * If the user can only navigate through the records with out making any changes
   * set this to false. By default, the modification-related buttons are enabled.
   *
   * @param allowWrite indicates whether or not the allowWrite-related buttons are enabled.
   */
  void setAllowWrite(boolean allowWrite) {
    this.allowWrite = allowWrite;
    updateActionState();
  }

  /**
   * Returns true if the user can modify the data in the RowSet, else false.
   *
   * @return returns true if the user modifications are written back to the
   *         database, else false.
   */
  boolean getAllowWrite() { return allowWrite; }

  /** Allow/disallow edits to a RowSets record. */
  boolean getAllowUpdate() { return allowUpdate; }

  /* Enables or disables the modification-related buttons on the SSDataNavigator. */
  void setAllowUpdate(boolean allowUpdate) {
    this.allowUpdate = allowUpdate;
    updateActionState();
  }

  boolean hasError(SSComponent comp) { return errorComponents.contains(comp); }

  /**
   * Function that passes the implementation of the DbOps interface. This
   * interface can be implemented by the developer to perform custom actions when
   * the insert button is pressed
   *
   * @param dbOps implementation of the DbOps interface
   */
  //TODO: does this belong here
  void setDbOps(DbOps dbOps) {
    Objects.requireNonNull(dbOps);
    this.dbOps = dbOps;
  }

  /**
   * Returns any custom implementation of the DbOps interface, which is used
when the insert button is pressed to perform custom actions.
   *
   * @return any custom implementation of the DbOps interface
   */
  DbOps getDbOps() { return dbOps; }

  // TODO: handle multipble navCombo?
  <K> void setNavCombo(DBComboBox2<K, ?, ?> navCombo, SyncManager<K> syncer) {
    Objects.requireNonNull(navCombo);
    // TODO: Objects.requireNonNull(syncer);
    if (this.navCombo != null) throw new IllegalStateException("navCombo already set");
    this.navCombo = navCombo;
    this.syncer = syncer;
  }

  /**
   * @return the navCombo
   */
  // TODO: what's this about
  /*public*/ DBComboBox2<?, ?, ?> getNavCombo() { return navCombo; }

  void syncSyncManager() {
    if (syncer != null) syncer.sync();
  }

  //////////////////////////////////////////////////////////////////////
  //
  // State maintenance - set enable/disable on various actions
  //

  /** Indicator that current row is dirty. */
  //private boolean isRowModified = false;

  void setNavComboEnabled(boolean b) {
    if (navCombo != null) navCombo.setEnabled(b);
  }

  /** Going to a new row, or undo updates, or refresh row. */
  void freshRow() {
    logger.log(TRACE, "freshRow");
    undoRow.clear();
    errorComponents.clear();
    //isRowModified = false; // TODO: get rid of this
  }

  /** Moving to insertRow. */
  void freshInsertRow() {
    logger.log(TRACE, "freshInsertRow");
    undoRow.clearInsertRow(getRowSet());
    errorComponents.clear();
  }

  //
  // TODO: These are called many times in a row;
  //       don't wan't to recompute rowsModel every time.
  private void updateEnable(RowsAction navAction, boolean enableFlag) {
    List<RowsModel> rowsModels = RowsModel.getActiveRowModels(getRowSet());
    for (RowsModel rowsModel : rowsModels) {
      Action act = rowsModel.getAction(navAction);
      if (act.isEnabled() != enableFlag) act.setEnabled(enableFlag);
    }
  }
  @SuppressWarnings("unused")
  private void checkEnableLog(List<RowsAction> navActions, boolean enableFlag) {
    List<RowsModel> rowsModels = RowsModel.getActiveRowModels(getRowSet());
    for (RowsModel rowsModel : rowsModels) {
      for (RowsAction navAction : navActions) {
        Action act = rowsModel.getAction(navAction);
        if (act.isEnabled() != enableFlag)
          logger.log(ERROR,
                     ()
                         -> sf("Wrong enable state for %s, expect %b", navAction, enableFlag),
                     new IllegalStateException());
      }
    }
  }

  /**
   * Set to true for original behavior
   */
  private boolean v3Buttons;

  /**
   * Return if Pre v4 button behavior.
   * @return true if pre v4
   */
  private boolean isV3Buttons() { return v3Buttons; }

  /**
   * Set whether or not the commit and cancel buttons are always enabled,
   * independent of whether or not there is a allowWrite.
   * @param v3Buttons true for pre v4 behavior
   */
  @SuppressWarnings("unused")
  private void setV3Buttons(boolean v3Buttons) {
    this.v3Buttons = v3Buttons;
  }

  /**
   * when false, navigation disabled when row is dirty
   */
  private boolean autoCommit;

  /**
   * Return autoCommit mode. In autoCommit mode, the buttons that move
   * the row, like first or prev, are enabled when row has a modification.
   * Navigating away from a modified row,
   * commits the changes to the rowSet.
   *
   * @return true if autoCommit mode
   */
  private boolean isAutoCommit() { return autoCommit; }

  /**
   * Set whether or not to enable autoCommit mode.
   * In autoCommit mode, the buttons that move
   * the row, like first or prev, are enabled when row has a modification.
   * Navigating away from a modified row,
   * commits the changes to the rowSet.
   *
   * @param autoCommit inidcates whether or not to enable autoCommit mode
   */
  // NOTE: this method is not referenced.
  @SuppressWarnings("unused")
  private void setAutoCommit(boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  private boolean lastCanNavigate;
  boolean canNavigate() { return lastCanNavigate; }

  private boolean canWrite() {
    DbOps ops = getDbOps();
    return getAllowWrite() && (getAllowUpdate() || getAllowInsert() || getAllowDelete())
        && (ops.allowUpdate() || ops.allowInsert() || ops.allowDelete());
  }

  boolean canInsert() { return getAllowWrite() && getAllowInsert() && getDbOps().allowInsert(); }

  boolean canDelete() { return getAllowWrite() && getAllowDelete() && getDbOps().allowDelete(); }

  boolean canUpdate() { return getAllowWrite() && getAllowUpdate() && getDbOps().allowUpdate(); }

  /**
   * Set the enable/disable state of each button according to
   * the Navigator state variables.
   * @see #updateActionStateWithDatabaseCheck()
   */
  /*RowsActions*/ void updateActionState() {
    logger.log(TRACE, () -> sf("rowCount=%d, currentRow=%d", rowCount, getRow()));
    List<RowsModel> rowsModels = RowsModel.getActiveRowModels(getRowSet());
    if (rowsModels.isEmpty())
      logger.log(DEBUG, () -> sf("No RowsModel for rowSet %s", objectID(getRowSet())));

    boolean isRowModified = undoRow.isDirty();

    boolean onInsertRow = RowSetState.isInserting(getRowSet());
    boolean hasError = !errorComponents.isEmpty();
    boolean isAutoCommit = isAutoCommit();
    boolean commitUndoAlwaysEnabled = false;

    if (isV3Buttons()) {
      // force some things for old style
      isAutoCommit = true;
      hasError = false;
      commitUndoAlwaysEnabled = true;
    }

    // True if row is modified and don't want implicit commit.
    // Disables first, prev, next, last, add, refresh.
    boolean disablingAutoCommit = isRowModified && !isAutoCommit;

    // Handle first, prev, next, last (but there's that option for later)
    boolean canNavigate = rowCount != 0 && !onInsertRow && !disablingAutoCommit;
    boolean atFirst = getRow() == 1;
    boolean atLast = getRow() == rowCount;

    updateEnable(ACT_FIRST, canNavigate && !atFirst);
    updateEnable(ACT_PREVIOUS, canNavigate && !atFirst);
    updateEnable(ACT_NEXT, canNavigate && !atLast);
    updateEnable(ACT_LAST, canNavigate && !atLast);
    updateEnable(ACT_GOTOROW, canNavigate);
    setNavComboEnabled(canNavigate);
    lastCanNavigate = canNavigate;

    // Handle commit, undo
    boolean commitOn = canWrite()
                       && (isRowModified && canUpdate() || onInsertRow && canInsert()
                           || commitUndoAlwaysEnabled);
    updateEnable(ACT_COMMIT, commitOn && !hasError);
    boolean undoOn = isRowModified || commitOn;
    updateEnable(ACT_REVERT, undoOn);

    // TODO: Consider if row is dirty, delete button makes sense,
    //			but, does the add button make sense?
    // Handle add, delete
    if (onInsertRow) {
      updateEnable(ACT_ADD, false);
      updateEnable(ACT_DELETE, false);
    } else {
      // Perhaps the following should only be "!isRowModified"
      updateEnable(ACT_ADD, canInsert() && !disablingAutoCommit);
      updateEnable(ACT_DELETE, canDelete() && rowCount != 0);
    }

    // refresh
    // TODO: Should refresh/reload ever be disabled?
    updateEnable(ACT_REFRESH, !onInsertRow && !disablingAutoCommit);
  }

  /**
   * Set the enable/disable state of each button according to
   * the Navigator state variables; additionally set the state
   * of the first, prev, next, and last buttons from the database.
   * @see #updateActionState()
   */
  // TODO: understand why this is needed? MOVE THIS INTO updateNavigatorRowAndCount.
  private void updateActionStateWithDatabaseCheck() {
    updateActionState();
    // Not sure why these are needed, updateActionState
    // compares getRow() to 1 and "rowCount".
    // This code doesn't hurt, if it changes anything, then there's a bug.

    // TRY TO GET RID OF THIS. SHOULDN'T BE NEEDED.

    try {
      if (getRowSet().isLast()) {
        checkEnableLog(List.of(ACT_NEXT, ACT_LAST), false);
        updateEnable(ACT_NEXT, false);
        updateEnable(ACT_LAST, false);
      }
      if (getRowSet().isFirst()) {
        checkEnableLog(List.of(ACT_FIRST, ACT_PREVIOUS), false);
        updateEnable(ACT_FIRST, false);
        updateEnable(ACT_PREVIOUS, false);
      }
    } catch (SQLException ex) { logger.log(ERROR, "SQL Exception.", ex); }
  }

  /**
   * Typically called after a rowSetEvent that may move the cursor
   * or set up a new rowSet; also called by many actions in RowsActions.
   * First updates currentRow and rowNumberModel's count/current.
   * Then updateActionStateWithDatabaseCheck().
   * @throws SQLException 	SQLException
   */
  void updateNavigatorRowAndCount() throws SQLException {
    // Usually currentRow is allready correct.
    currentRow = getRowSet().getRow();

    rowNumberModel.setMaximum(rowCount);
    rowNumberModel.setValue(currentRow);

    logger.log(DEBUG, () -> "Current Row: " + getRow() + ". Row Count: " + rowCount);
    //logger.debug("Stack trace:", new Throwable());

    // TODO: inline updateActionStateWithDatabaseCheck code then remove it
    updateActionStateWithDatabaseCheck();
  }

  /**
   * Writes the present row back to the RowSet. This is done automatically when
   * any navigation takes place, but can also be called manually.
   *
   * @return returns true if update succeeds else false.
   * @deprecated use RowsModel.commit()
   */
  @Deprecated
  public boolean updatePresentRow() {
    if (RowSetState.isInserting(getRowSet()) || (getRow() > 0)) {
      logger.log(DEBUG, "Doing NAV_COMMIT.");
      // TODO: minor optim getAnyRowModel(getRowSet())
      List<RowsModel> rowsModels = RowsModel.getActiveRowModels(getRowSet());
      if (rowsModels.isEmpty()) throw new IllegalStateException("No RowsModel for rowSet");
      // Do the commit through any action.
      rowsModels.get(0).getAction(ACT_COMMIT).actionPerformed(null);
    }
    return true;
  }
}
