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
package com.nqadmin.swingset.navigate;

import java.awt.Component;
import java.awt.Window;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.ref.WeakReference;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.RowSet;
import javax.sql.RowSetEvent;
import javax.sql.RowSetListener;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.openide.util.WeakListeners;

import com.google.common.collect.MapMaker;
import com.google.common.eventbus.EventBus;
import com.nqadmin.swingset.core.DBComboBox2;
import com.nqadmin.swingset.datasources.DbOps;
import com.nqadmin.swingset.datasources.DbSupport;
import com.nqadmin.swingset.datasources.RowSetOps;
import com.nqadmin.swingset.datasources.products.DbOpsCreator;
import com.nqadmin.swingset.navigate.RowsEvent.OperatorKind;
import com.nqadmin.swingset.navigate.RowsEvent.RowSetEventType;
import com.nqadmin.swingset.navigate.RowsModelEventHandling.RowsEventSource;
import com.nqadmin.swingset.navigate.RowsModelEventHandling.SimpleEvents;
import com.nqadmin.swingset.utils.CentralLookup;
import com.nqadmin.swingset.utils.JStuff;
import com.nqadmin.swingset.utils.LookupDefaults;
import com.nqadmin.swingset.utils.SSComponent;
import com.nqadmin.swingset.utils.SSUtils;
import com.nqadmin.swingset.utils.SyncManager;
import com.raelity.lib.eventbus.WeakEventBus;

import static com.nqadmin.swingset.navigate.RowsAction.*;
import static com.nqadmin.swingset.navigate.RowsModelEventHandling.postAsync;
import static com.nqadmin.swingset.navigate.Utils.getGlobalEventBus;
import static com.nqadmin.swingset.utils.JStuff.sf;
import static com.nqadmin.swingset.utils.SSUtils.JDBCTypeMismatch;
import static com.nqadmin.swingset.utils.SSUtils.NullabilityMismatch;
import static com.nqadmin.swingset.utils.SSUtils.objectID;
import static java.lang.System.Logger.Level.*;

/**
 * The RowsModel is associated with a {@link javax.sql.RowSet}.
 * Using {@link NavigateState} and {@link RowsAction} it encapsulates RowSet
 * traversal and their associated {@link javax.swing.Action}s which can be
 * plugged into UI components; it broadcasts RowSet events.<p>
 * {@link RowsModelEvent}s are broadcast when the model's RowSet is changed
 * and when the current RowSet notifies of eventsNextQ. RowSet eventsNextQ are coalesced
 * when they occur in operations which are delineated by
 <ul>
 * <li> {@link #startRowsEvent }
 * <li> {@link #finishRowsEvent }
 * </ul>
 Other/stray RowSet eventsNextQ are sent individually as soon as they are received.
 */
//
// NOTE: The RowSet must be "executable".
//       Execution happens in NavigateState::setupRowSet() via RowsModel.
//       Should RowsModel try to execute if no command?
//       Handle an "empty"/"null"/non-executable RowSet?
//       Is there a way to tell if current command has been executed?
//
public final class RowsModel {
  static { LookupDefaults.init(); }
  /** Logger for component */
  private static final Logger logger = JStuff.getLogger();

  // Used like a WeakHashSet.
  private static final Map<RowsModel, Boolean> activeRowModels
      = new MapMaker().weakKeys().makeMap();

  // Track the component bound column names.
  private final Map<SSComponent, String> bindings = new MapMaker().weakKeys().makeMap();

  private NavigateState navState;
  private final RowsActions rowsActions;

  /**
   * Create and return a new RowsModel for the specified RowSet.
   * <p>
   * Note the RowSet, if not null, must have a query to execute.
   * @param rs
   * @return
   * @deprecated use {@link #create(RowSet, DbOps) }
   */
  // TODO: Is it ok to require a query?
  //       Previously, the query was executed by SSDataNavigator.
  @Deprecated
  public static RowsModel create(RowSet rs) {
    RowsModel rowsModel = create(rs, null);
    return rowsModel;
  }

  /**
   * Create and return a new RowsModel for the specified RowSet.
   * If dbOps is null, find the navigator using
   * {@code findDbOps(rowSet, null)}.
   * <p>
   * Note the RowSet, if not null, must have a query to execute.
   * If the RowSet is null then dbOps is ignored/discarded.
   * @param rs
   * @param dbOps navigator for the RowSet
   * @return
   */
  public static RowsModel create(RowSet rs, DbOps dbOps) {
    RowsModel rowsModel = new RowsModel(rs, dbOps == null ? findDbOps(rs, null) : dbOps);
    return rowsModel;
  }

  /**
   * Find {@link DbOps} for the specified RowSet.
   * Looks for {@link DbOpsCreator} to create it; if not found
   * or its {@code create(rowSet)} method returns null,
   * returns {@code new DbOps() {}}.
   *
   * @param rs
   * @param rowsModel where the RowSet is going, null if new RowsModel
   * @return DbOps to use with this RowSet and RowsModel
   */
  public static DbOps findDbOps(RowSet rs, RowsModel rowsModel) {
    DbOps dbOps = null;
    // creator may check by DB/TBL/whatever
    DbOpsCreator creator = CentralLookup.defLookup(DbOpsCreator.class);
    if (creator != null) dbOps = creator.create(rs, rowsModel);

    if (dbOps == null) {
      dbOps = new DbOps() {};
    } // no-op
    return dbOps;
  }

  /**
   * Find the RowsModels that currently hold the specified RowSet.
   * @param rs
   * @return
   */
  // TODO: should this really be public?
  static List<RowsModel> getActiveRowModels(RowSet rs) {
    return activeRowModels.keySet()
        .stream()
        .filter(rowsModel -> rowsModel.getRowSet() == rs)
        .toList();
  }

  /**
   * TEMPORARY; typically for debug/transition; find any RowModel for the RowSet.
   * @param rs
   * @return
   */
  public static RowsModel getActiveRowModel(RowSet rs) {
    return activeRowModels.keySet()
        .stream()
        .filter(rowsModel -> rowsModel.getRowSet() == rs)
        .findAny()
        .orElse(null);
  }

  /**
   * Create one associated with the given RowSet.
   * @param rs
   */
  // TODO: handle null RowSet; important, consider empty DataNavigator, build UI first.
  private RowsModel(RowSet rs, DbOps dbOps) {
    Objects.requireNonNull(dbOps);
    // TODO: get rid of junit test after tests are fully RowsModel ported.
    if (!SSUtils.isJunit() && rs != null && !verifyExecuted(rs))
      logger.log(Level.ERROR, "RowSet not executed", new Exception());
    logger.log(Level.INFO, () -> sf("new RowsModel %s for %s", objectID(this), objectID(rs)));

    activeRowModels.putIfAbsent(this, true);

    this.rowsActions = new RowsActions(this);

    setNavState(rs, dbOps);

    rowSetListener = new SimpleRowSetListener();
    rowSetListener.registerTo(rs);
  }

  /**
   * Get and set NavigationState. Two step process because when navState hooks into
   * the RowSet, it uses the RowsModel.
   *
   * <br>TODO: clean up the RowsModel/NavState initialization.
   *
   * @param rs
   * @param dbOps
   */
  private void setNavState(RowSet rs, DbOps dbOps) {
    Objects.requireNonNull(dbOps);
    if (rs == null) {
      navState = null;
      rowsActions.disableAllActions();
      return;
    }
    navState = NavigateState.getOrCreate(rs);
    navState.setDbOps(dbOps);

    if (navState.getRowSet() == null) navState.setupRowSet(rs);
  }

  // TODO: verifyExecuted HACK, remove this and require executed row set
  /**
   * Check if RowSet is executed. If not executed, then execute it.
   * @param rs
   * @return true if was executed
   * @throws SQLException
   */
  static boolean verifyExecuted(RowSet rs) {
    String msg;
    try {
      boolean ok = rs.getMetaData() != null ? rs.getMetaData().getColumnCount() > 0
                                            : false; // CachedRowSet
      if (ok) return true;
      msg = "no exception";
    } catch (SQLException ex) { msg = ex.getMessage(); }
    String fMsg = msg;
    try {
      logger.log(Level.ERROR, () -> sf("%s. Will execute query", fMsg));
      // TODO: take out the callExecute error recovery, propogate the exception
      rs.execute();
    } catch (SQLException ex1) { logger.log(Level.ERROR, "execute() SQL Exception", ex1); }
    return false;
  }

  /**
   * Change the RowSet associated with this model; keep the current DbOps.
   * <p>
   * <em>Be careful</em> using this method. You must be certain that the
   * current DbOps is compatible with the new RowSet.
   *
   * @param rs new RowSet for this model
   */
  public void setRowSet(RowSet rs) { setRowSet(rs, getDbOps()); }

  /**
   * Change the RowSet associated with this model.
   * If _dbOps is null, find the default navigator with
   * {@link #findDbOps(javax.sql.RowSet, com.nqadmin.swingset.navigate.RowsModel) }.
   * @param rs new RowSet for this model
   * @param _dbOps
   * @return false if abort and rowSet not set/changed
   */
  // TODO: need programatic disable dialog if discarding uncommited changes? quiet flag.
  public boolean setRowSet(RowSet rs, DbOps _dbOps) {
    DbOps dbOps = _dbOps == null ? findDbOps(rs, this) : _dbOps;
    logger.log(Level.INFO,
               ()
                   -> sf("RowsModel %s change rowSet from %s to %s", objectID(this),
                         objectID(getRowSet()), objectID(rs)));

    if (isDirty()) {
      //throw new IllegalStateException("oldRS dirty"); ???
      logger.log(INFO, "oldRS dirty");
      // TODO: What if row set is also in a different rowsmodel?
      //           Then don't want dialog?
      //       Need a way to programatically disable dialog? quiet flag.
      //       Or caller/app should check?
      // parent dialog with enclosing Window. TODO: allow custom find parent
      Component parent = null;
      if (!bindings.isEmpty()) {
        parent = (Component) bindings.keySet().iterator().next();
        Window win = SwingUtilities.getWindowAncestor(parent);
        parent = win != null ? win : parent;
      }
      int response = JOptionPane.showConfirmDialog(
          parent,
          sf("Setting new RowSet discards\nuncommitted modifications\nto table \"%s\"",
             SSUtils.tableName(getRowSet())),
          null, JOptionPane.OK_CANCEL_OPTION);
      // TODO: Note the rowSet's undo/redo still has the modifications.
      //       Really discard?
      if (response != JOptionPane.OK_OPTION) return false;
    }

    if (rs != null) {
      verifyExecuted(rs);

      // TODO: May need a setRowSet variant that allows replacing dirty rowSet.
      NavigateState newNS = NavigateState.get(rs);
      if (newNS != null && newNS.undoRow.isDirty())
        //throw new IllegalStateException("newRS dirty"); ???
        logger.log(INFO, "newRS dirty");

      // Check for component binding compatibility here
      // to avoid exception buried in event handler.
      for (Map.Entry<SSComponent, String> entry : bindings.entrySet()) {
        SSComponent comp = entry.getKey();
        if (!comp.isFullyBound()) continue;

        // verify same column type and nullable.
        String colName = entry.getValue();
        JDBCType oldType = comp.getColumnJDBCType();
        JDBCType newType = JDBCType.NULL;
        try {
          newType = RowSetOps.getJDBCColumnType(rs, colName);
        } catch (SQLException ex) { logger.log(Level.ERROR, (String) null, ex); }
        if (newType != oldType)
          throw new IllegalArgumentException(JDBCTypeMismatch(oldType, newType));
        boolean oldVal = comp.getAllowNull();
        boolean newVal = RowSetOps.isNullable(rs, colName).get();
        if (oldVal != newVal)
          throw new IllegalArgumentException(NullabilityMismatch(oldVal, newVal));
      }
    }

    RowSet oldRowSet = getRowSet();
    rowSetListener.unregisterFrom(oldRowSet);
    setNavState(rs, dbOps);

    rowSetListener.registerTo(rs);
    enq.postNewRowSetEvent(this, oldRowSet);
    return true;
  }

  // TODO: is this path OK?
  void syncSyncManager() { getNavState().syncSyncManager(); }

  /**
   * Establish bindings.
   * @param binds
   */
  public void bind(Map<SSComponent, String> binds) {
    for (Map.Entry<SSComponent, String> binding : binds.entrySet()) {
      bind(binding.getKey(), binding.getValue());
    }
  }

  private String bindPairName(SSComponent comp, String columnName) {
    return sf("<%s,%s>", objectID(comp), columnName);
  }

  /**
   *
   * @param comp
   * @param columnName
   */
  // TODO: deprecate in favor of bind(Map)
  @SuppressWarnings("deprecation")
  public void bind(SSComponent comp, String columnName) {
    // Check that there's no existing binding for comp or columnName.
    for (Map.Entry<SSComponent, String> entry : bindings.entrySet()) {
      if (Objects.equals(comp, entry.getKey())) {
        throw new IllegalArgumentException(sf(
            "SSComponent of %s already bound in RowsModel %s of %s", bindPairName(comp, columnName),
            objectID(this), bindPairName(comp, entry.getValue())));
      }
      // ColumnName can be bound to multiple components
      // if (Objects.equals(columnName, entry.getValue())) {
      // 	throw new IllegalArgumentException(
      // 			sf("ColumnName of %s already bound in RowsModel %s of %s",
      // 					bindPairName(comp, columnName), objectID(this),
      // 					bindPairName(entry.getKey(), columnName)));
      // }
    }
    bindings.put(comp, columnName);
    try {
      comp.bind(this, columnName); // Only allowed from RowsModel
    } catch (Exception ex) {
      bindings.remove(comp);
      throw ex;
    }
  }

  /**
   * The event will typically cause all components to update.
   * @param type 
   */
  public void issueRowSetEvent(RowSetEventType type) {
    startRowsEvent(this, ACT_ROW_SET_EVENT);
    addRowSetEvent(type, getRowSet());
    finishRowsEvent(this);
  }

  /**
   * Return the associated RowSet.
   * @return row set
   */
  public RowSet getRowSet() { return navState != null ? navState.getRowSet() : null; }

  /**
   * Returns DbOps which is used when the insert action, 
   * and much more, is pressed, to perform custom actions.
   *
   * @return the DbOps
   */
  public DbOps getDbOps() { return navState != null ? navState.getDbOps() : null; }

  /**
   * Use rsOp to capture multiple RowSet eventsNextQ into a single event.
   *
   * @param operator
   * @param r code that operates on a RowSet
   * @throws java.sql.SQLException
   */
  // TODO: need javadoc examples.
  public void rsOp(Object operator, DbSupport.RunnableSQL r) throws SQLException {
    RowsModel.startRowsEvent(OperatorKind.OTHER, this, operator);
    try {
      r.run();
    } finally { RowsModel.finishRowsEvent(this); }
  }

  NavigateState getNavState() { return navState; }

  UndoRow getUndoRow() { return getNavState().undoRow; }

  /**
   * Return an action, associated with this model, that can be plugged
   * into a JComponent's {@link javax.swing.Action}.
   * @param navAction
   * @return
   */
  // TODO: javadoc says "action this model", but action is RowSet assoc.
  //       Wrap the action and go indirect to the navState.
  //       Cache the wrapped actions.
  public Action getAction(RowsAction navAction) { return rowsActions.get(navAction); }

  // These convenience methods may do nothing

  /**
   * Programmaticaly move the ResultSet cursor to the first row.
   * @return true if on valid row
   * @throws java.sql.SQLException
   */
  public boolean first() throws SQLException {
    if (!getRowSet().isFirst()) rowsActions.run(ACT_FIRST);
    return getRowSet().getRow() != 0;
  }
  /**
   * Programmatically move the ResultSet cursor to the last row.
   * @return true if on valid row
   * @throws java.sql.SQLException
   */
  public boolean last() throws SQLException {
    if (!getRowSet().isLast()) rowsActions.run(ACT_LAST);
    return getRowSet().getRow() != 0;
  }
  /**
   * Programmaticaly move the ResultSet cursor to the next row.
   * @return true if on valid row
   * @throws java.sql.SQLException
   */
  public boolean next() throws SQLException {
    rowsActions.run(ACT_NEXT);
    return getRowSet().getRow() != 0;
  }
  /**
   * Programmaticaly move the ResultSet cursor to the previous row.
   * @return true if on valid row
   * @throws java.sql.SQLException
   */
  public boolean previous() throws SQLException {
    rowsActions.run(ACT_PREVIOUS);
    return getRowSet().getRow() != 0;
  }
  /** Programmaticaly write the database with the changes. */
  public void commit() {
    if (isDirty()) rowsActions.run(ACT_COMMIT);
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Some state read/write.
  //

  // NOTE: SpinnerModel locked to RowSet
  SpinnerNumberModel getRowNumberModel() { return navState.getRowNumberModel(); }

  /**
   * Use this after directly moving the cursor around, to get the
   * spinner back in sync
   * @return
   */
  // Maybe some of the Spinner logic should go into the SpinnerModel logic.
  // In particular the resync. Also maybe the model should have a reference
  // to the spinner, create a RowNumberSpinnerModel that only has reference.
  public int syncRowNumber() { return getRow(true); }

  /**
   * Return the associated RowSet's current row number.
   * @return row number
   */
  public int getRow() { return getRow(false); }

  private int getRow(boolean isSync) {
    int spin_row = getRowNumberModel().getNumber().intValue();
    try {
      int rs_row = getRowSet().getRow();
      if (spin_row != rs_row) {
        // TODO: Instruct to use rowsModel.setRow().
        if (!isSync)
          logger.log(ERROR, sf("spinner model, %d, out of sync with row set %d", spin_row, rs_row),
                     new IllegalStateException("getRow sync"));
        // RESYNC SPINNER
        setRow(rs_row);
        int spin_row2 = getRowNumberModel().getNumber().intValue();
        int rs_row2 = getRowSet().getRow();
        if (spin_row2 != rs_row || rs_row != rs_row2)
          throw new IllegalStateException("Spinner failed to sync");
        spin_row = rs_row;
      }
    } catch (SQLException ex) {
      // TODO: random sql exception
    }
    return spin_row;
  }

  /**
   * Move the RowSet cursor to the specified row.
   * @param row target cursor row
   */
  public void setRow(int row) {
    SpinnerNumberModel spinnerModel = getRowNumberModel();
    if (spinnerModel != null) { spinnerModel.setValue(row); }
  }

  /**
   * Return the count of rows in the ResultSet
   * @return count of rows
   */
  public int getRowCount() {
    SpinnerNumberModel spinnerModel = getRowNumberModel();
    if (spinnerModel != null) return (Integer) spinnerModel.getMaximum();
    return -1;
  }

  /**
   * Check if there are rows in the RowSet.
   * @return true if there are no rows
   */
  public boolean isEmpty() {
    //return !rs.isBeforeFirst() && rs.getRow() == 0;
    return getRowCount() == 0;
  }

  /**
   * Is the component in the current row of this dirty?
   * @param comp
   * @return is dirty
   */
  public boolean isDirty(SSComponent comp) {
    return getNavState() != null && getNavState().undoRow.isDirty(comp);
  }

  /**
   * Is the current row of this dirty?
   * @return is dirty
   */
  public boolean isDirty() { return getNavState() != null && getNavState().undoRow.isDirty(); }

  /**
   * Has navigation been disabled for the rowSet.
   * @return
   */
  public boolean canNavigate() { return getNavState().canNavigate(); }

  /**
   * Check if this RowsModel's rowSet's cursor is on any row or on the insert row.
   * @return true if cursor on a row or insert row
   * @throws SQLException
   */
  public boolean onActiveRow() throws SQLException { return getRow() != 0 || isOnInsertRow(); }

  /**
   * Returns true if the RowSet contains one or more rows, else false.
   *
   * @return return true if RowSet contains data else false.
   */
  public boolean containsRows() { return navState.containsRows(); }

  /**
   * @return boolean indicating if the rowSet is on an insert row
   */
  public boolean isOnInsertRow() { return navState.isOnInsertRow(); }

  /**
   * @param comp
   * @return true if the component is in an error state
   */
  public boolean hasError(SSComponent comp) {
    // TODO: check if row set has component's column name?
    // if (!bindings.containsKey(comp))
    // 	throw new IllegalArgumentException("Component not bound in RowsModel");
    return navState.hasError(comp);
  }

  /**
   * Adjust the components error state. Used when there are multiple
   * components for the same column.
   * This execution path doesn't feel right. This shouldn't be public.
   * @param comp
   * @param isError
   */
  public void adjustErrorState(SSComponent comp, boolean isError) {
    getNavState().adjustErrorComponentState(comp, isError);
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Behavioral control methods - taken from SSDataNavigation
  //

  /**
   * Returns true if deletions must be confirmed by user, else false.
   *
   * @return returns true if a confirmation dialog is displayed when the user
   *         deletes a record, else false.
   */
  public boolean getConfirmDeletes() { return navState.getConfirmDeletes(); }

  /**
   * Sets the confirm deletion indicator. If set to true, every time delete action
   * is pressed, the navigator pops up a confirmation dialog to the user. Default
   * value is true.
   *
   * @param confirmDeletes indicates whether or not to confirm deletions
   */
  public void setConfirmDeletes(boolean confirmDeletes) {
    navState.setConfirmDeletes(confirmDeletes);
  }

  /**
   * Returns true if deletions are allowed, else false.
   *
   * @return returns true if deletions are allowed, else false.
   */
  public boolean getAllowDelete() { return navState.getAllowDelete(); }

  /**
   * Enables or disables the row deletion action. This method should be used if
   * row deletions are not allowed. True by default.
   *
   * @param deletion indicates whether or not to allow deletions
   */
  public void setAllowDelete(boolean deletion) { navState.setAllowDelete(deletion); }

  /**
   * Returns true if insertions are allowed, else false.
   *
   * @return returns true if insertions are allowed, else false.
   */
  public boolean getAllowInsert() { return navState.getAllowInsert(); }

  /**
   * Enables or disables the row insertion action. This method should be used if
   * row insertions are not allowed. True by default.
   *
   * @param insertion indicates whether or not to allow insertions
   */
  public void setAllowInsert(boolean insertion) { navState.setAllowInsert(insertion); }

  /**
   * Returns true if the user can modify the data in the RowSet, else false.
   *
   * @return returns true if the user modifications are written back to the
   *         database, else false.
   */
  public boolean getAllowWrite() { return navState.getAllowWrite(); }

  /**
   * Enables or disables the modification-related action on the SSDataNavigator.
   * If the user can only navigate through the records with out making any changes set
   * this to false. By default, the modification-related action are enabled.
   *
   * @param writable true to enable writable-related actions; false to disable
   */
  public void setAllowWrite(boolean writable) { navState.setAllowWrite(writable); }

  /**
   * Can the rowset be updated?
   * @return
   */
  public boolean getAllowUpdate() { return navState.getAllowUpdate(); }

  /**
   * Enable/disable row edit
   * @param allowUpdate
   */
  public void setAllowUpdate(boolean allowUpdate) { navState.setAllowUpdate(allowUpdate); }

  //////////////////////////////////////////////////////////////////////
  //
  // Bus and event coalescing other random stuff
  //
  // TODO: put at least some of this in some other class. Maybe inner class.
  //       Don't like all the public stuff.
  //

  /**
   * Put all the navigation actions into the action map.
   * If the param is null, a new actionMap is constructed and filled
   * @param actionMap the actionMap to fill; may be null
   * @return the filled actionMap
   */
  public ActionMap fillNavActionMap(ActionMap actionMap) {
    ActionMap am = actionMap != null ? actionMap : new ActionMap();
    Arrays.stream(RowsAction.values()).forEach(key -> {
      if (!key.isVirtual()) am.put(key, getAction(key));
    });
    return am;
  }

  /**
   * Register the busReceiver to create RowsModel RowSet eventsNextQ;
   * only methods annotated with {@code @WeakSubscribe} are registered.
   *
   * @param busReceiver
   */
  public void registerBusReceiver(Object busReceiver) {
    WeakEventBus.register(busReceiver, getEventBus());
  }

  /**
   * Unregister the busReceiver to create RowsModel RowSet eventsNextQ.
   * @param busReceiver
   */
  public void unregisterBusReceiver(Object busReceiver) {
    WeakEventBus.unregister(busReceiver, getEventBus());
  }

  /**
   * Use a Weak listener. Otherwise remove from rowset easily doesn't happen.
   * If not weak, this model will not be collected while the
   * RowSet exists; the rowset's NavigationAction would not be collected.
   */
  private abstract class RowSetListenerBase implements RowSetListener {
    private WeakReference<RowSetListener> refWeakRowSetListener;

    protected void registerTo(RowSet rs) {
      if (refWeakRowSetListener != null) throw new IllegalStateException("Already using listener");
      RowSetListener wrsl = WeakListeners.create(RowSetListener.class, this, rs);
      refWeakRowSetListener = new WeakReference<>(wrsl);
      if (rs != null) rs.addRowSetListener(wrsl);
    }

    protected void unregisterFrom(RowSet rs) {
      if (refWeakRowSetListener != null) {
        if (rs != null) {
          RowSetListener wrsl = refWeakRowSetListener.get();
          rs.removeRowSetListener(wrsl);
        }
        refWeakRowSetListener.clear();
        refWeakRowSetListener = null;
      }
    }
  }

  private final RowSetListenerBase rowSetListener;

  private class SimpleRowSetListener extends RowSetListenerBase {
    @Override
    public void rowSetChanged(RowSetEvent event) {
      addRowSetEvent(RowSetEventType.ROW_SET_CHANGED, (RowSet) event.getSource());
    }

    @Override
    public void rowChanged(RowSetEvent event) {
      addRowSetEvent(RowSetEventType.ROW_CHANGED, (RowSet) event.getSource());
    }

    @Override
    public void cursorMoved(RowSetEvent event) {
      addRowSetEvent(RowSetEventType.CURSOR_MOVED, (RowSet) event.getSource());
    }
  };

  /**
   * Invoke this when starting an Operation that manipulates a RowSet.
   * @param model
   * @param compOrNav
   */
  public static void startRowsEvent(RowsModel model, Object compOrNav) {
    enq.startRowsEvent(model, compOrNav);
  }

  /**
   * Invoke this when starting an Operation that manipulates a RowSet.
   * @param operatorKind
   * @param model
   * @param compOrNav
   */
  public static void startRowsEvent(OperatorKind operatorKind, RowsModel model, Object compOrNav) {
    enq.startRowsEvent(operatorKind, model, compOrNav);
  }

  /** from the RowSet event */
  private static void addRowSetEvent(RowSetEventType rsEventType, RowSet rs) {
    enq.addRowSetEvent(rsEventType, rs);
  }

  /**
   * Invoke this when finishing an Operation that manipulates a RowSet.
   * All the RowSet events that occured during the operation are
   * coalesced into a single event.
   * @param model must match the model associated with startRowsEvent
   */
  public static void finishRowsEvent(RowsModel model) { enq.finishRowsEvent(model); }

  static void post(RowsModelEvent event) { postAsync(event); }

  private static final SimpleEvents enq = new SimpleEvents();

  //
  // TODO: How to find the right event bus for Navigation Model?
  //       Use the global event bus, at least for now
  //

  /**
   * The event bus used by this RowsModel.
   * @return the event bus
   */
  // TODO: per model event bus ???
  static EventBus getEventBus() { return getGlobalEventBus(); }

  interface EnqueueRowsModelEvent {
    void startRowsEvent(RowsModel model, Object compOrNav);
    void startRowsEvent(OperatorKind operatorKind, RowsModel model, Object compOrNav);
    void addRowSetEvent(RowSetEventType rsEventType, RowSet rs);
    RowsEventSource finishRowsEvent(RowsModel model);
    void postNewRowSetEvent(RowsModel model, RowSet oldRowSet);
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Debug
  //

  /**
   * The number of active RowsModel; for debug.
   * @return
   */
  public static int count() {
    // Can't depend on size() method when weakKeys.
    return SSUtils.size(activeRowModels);
  }

  /**
   * The number of active NavigateState; for debug.
   * @return
   */
  public static int navCount() { return NavigateState.count(); }

  /**
   * for debug.
   * @param flag
   */
  public void setVerifyEnabledFlag_DEBUG(boolean flag) {
    rowsActions.setVerifyEnabledFlag_DEBUG(flag);
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Deprecated
  //

  /**
   * Writes the present row back to the RowSet.
   *
   * This is typically done when commit it pressed,
   * but it may be done programmatically.
   *
   * @return returns true if update succeeds else false.
   * @deprecated use {@link #commit() }
   */
  @Deprecated
  public boolean updatePresentRow() {
    return navState.updatePresentRow();
  }

  /**
   * Function that passes the implementation of the SSDBNav interface. This
   * interface can be implemented by the developer to perform custom actions when
   * the insert action is pressed
   *
   * @param dBNav implementation of the SSDBNav interface
   * @deprecated use {@linkplain #setRowSet(javax.sql.RowSet,
   * com.nqadmin.swingset.datasources.DbOps) }
   */
  @Deprecated
  public void setDBNav(DbOps dBNav) {
    navState.setDbOps(dBNav);
  }

  /**
   * This is necessary for ancient MySQL jdbc driver (see FAQ).
   * @param callExecute
   * @deprecated need to define a new strategy
   */
  @Deprecated
  public void setCallExecute(final boolean callExecute) {}

  /**
   * This is necessary for ancient MySQL jdbc driver (see FAQ).
   * @return
   * @deprecated need to define a new strategy
   */
  @Deprecated
  public boolean getCallExecute() {
    return false;
  }

  /**
   * Enables or disables the modification-related action on the SSDataNavigator.
   * If the user can only navigate through the records with out making any changes
   * set this to false. By default, the modification-related action are enabled.
   *
   * @param modification true to enable modification-related actions; false to disable
   * @deprecated use setAllowWrite
   */
  @Deprecated
  public void setModification(boolean modification) {
    navState.setAllowWrite(modification);
  }

  /**
   * Returns true if the user can modify the data in the RowSet, else false.
   *
   * @return returns true if the user modifications are written back to the
   *         database, else false.
   * @deprecated use getAllowWrite
   */
  @Deprecated
  public boolean getModification() {
    return navState.getAllowWrite();
  }

  /**
   * @param navCombo the navCombo to set
   * @deprecated this shouldn't be public
   */
  @Deprecated
  public void setNavCombo(DBComboBox2<?, ?, ?> navCombo) {
    setNavCombo(navCombo, null);
  }

  /**
   * @param <K> comboBox and syncMangager key
   * @param navCombo the navCombo used with this RowsModel
   * @param syncer
   * @deprecated this shouldn't be public
   */
  @Deprecated
  public <K> void setNavCombo(DBComboBox2<K, ?, ?> navCombo, SyncManager<K> syncer) {
    navState.setNavCombo(navCombo, syncer);
  }

  /**
   * @return the navCombo
   * @deprecated this shouldn't be public, if it's needed at all
   */
  // TODO: what's this about? Remove it.
  @Deprecated
  public DBComboBox2<?, ?, ?> getNavCombo() {
    return navState.getNavCombo();
  }
}
