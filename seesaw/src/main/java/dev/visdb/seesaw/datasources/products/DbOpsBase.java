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
package dev.visdb.seesaw.datasources.products;

import java.awt.Container;
import java.lang.System.Logger;
import java.sql.SQLException;
import java.util.function.Consumer;

import javax.sql.RowSet;

import dev.visdb.seesaw.datasources.DbMetaDataCache;
import dev.visdb.seesaw.datasources.DbOps;
import dev.visdb.seesaw.navigate.DbOpsChangeEvent;
import dev.visdb.seesaw.navigate.RowsModel;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsUtils;

import static dev.visdb.seesaw.navigate.Utils.postDbOpsChange;
import static java.lang.System.Logger.Level.DEBUG;

/**
 * Implementation of DbOps that implements performPreInsertOps() to
 * clear/initialize the various SsComponents on a screen before the
 * user edits the new record.
 * Implements the {@code performPost*Ops} methods
 * to use metadata to decide whether or not to re-execute the RowSset command;
 * handles state for the {@code allow*} methods.
 * <p>
 * {@code DbOps} is associated with a RowsModel/RowSet, see
 * {@link dev.visdb.seesaw.navigate.RowsModel#create(javax.sql.RowSet, dev.visdb.seesaw.datasources.DbOps) RowsModel(RowSet, DbOps)}.
 * {@link #performPreInsertOps()} searches the container provided to the
 * constructor to find the {@link SsComponent}s to clean.
 */
public class DbOpsBase implements DbOps {
  /** Logger for component */
  protected static final Logger logger = JStuff.getLogger();

  /** Screen where components to be cleared are located. */
  // TODO: find out a way that this is not embedded in the class.
  protected Container container = null;

  /**
   * Constructs a DbOpsBase with the specified container.
   *
   * @param container	GUI Container to scan for Swing components to clear/reset
   */
  public DbOpsBase(Container container) {
    this.container = container;
  }

  private boolean allowInsert = true;
  private boolean allowDelete = true;
  private boolean allowUpdate = true;

  /**
   * Sub-classes should use this for proper posting of
   * {@link DbOpsChangeEvent}.
   * @param allow
   */
  protected void allowInsert(boolean allow) {
    allowInsert = allow;
    postDbOpsChange(this, Allow.INSERT);
  }

  /**
   * Sub-classes should use this for proper posting of
   * {@link DbOpsChangeEvent}.
   * @param allow
   */
  protected void allowDelete(boolean allow) {
    allowDelete = allow;
    postDbOpsChange(this, Allow.DELETE);
  }

  /**
   * Sub-classes should use this for proper posting of
   * {@link DbOpsChangeEvent}.
   * @param allow
   */
  protected void allowUpdate(boolean allow) {
    allowUpdate = allow;
    postDbOpsChange(this, Allow.UPDATE);
  }

  /** {@inheritDoc } */
  @Override
  public boolean allowInsert() {
    return allowInsert;
  }

  /** {@inheritDoc } */
  @Override
  public boolean allowDelete() {
    return allowDelete;
  }

  /** {@inheritDoc } */
  @Override
  public boolean allowUpdate() {
    return allowUpdate;
  }

  /**
   * Performs pre-insertion operations, in particular
   * {@link #cleanComponents(Container) }.
   */
  @Override
  public void performPreInsertOps() {
    cleanComponents(container);
  }

  /**
   * Make sure the RowSet has the inserted row.
   * It may re-execute the RowSet's command, depending on DatabaseMetaData.
   * @param rm
   * @throws SQLException
   */
  @Override
  public void performPostInsertOps(RowsModel rm) throws SQLException {
    RowSet rs = rm.getRowSet();
    if (!DbMetaDataCache.get().ownInsertsAreVisible(rs.getType()))
      rs.execute();
  }

  /**
   * Make sure the RowSet has the deleted row.
   * It may re-execute the RowSet's command, depending on DatabaseMetaData.
   * @param rm
   * @throws java.sql.SQLException
   */
  @Override
  public void performPostDeletionOps(RowsModel rm) throws SQLException {
    RowSet rs = rm.getRowSet();
    if (!DbMetaDataCache.get().ownDeletesAreVisible(rs.getType()))
      rs.execute();
  }

  /**
   * Make sure the RowSet has the updated row.
   * It may re-execute the RowSet's command, depending on DatabaseMetaData.
   * @param rm
   * @throws java.sql.SQLException
   */
  @Override
  public void performPostUpdateOps(RowsModel rm) throws SQLException {
    RowSet rs = rm.getRowSet();
    if (!DbMetaDataCache.get().ownUpdatesAreVisible(rs.getType()))
      rs.execute();
  }

  /**
   * In the specified container, clear/initialize SsComponents.
   * Typically done for a new record/row. Uses
   * {@link SsUtils#visitSsComponents(Container, Consumer) }
   * to run {@link SsComponent#cleanField() }.
   * <p>
   * This is done for all SwingSet components,
   * for example text fields, and text areas,
   * recursively looking in to the JTabbedPanes and JPanels inside the given
   * container as needed.
   *
   * @param container container in which to recursively initialize components
   */
  protected void cleanComponents(Container container) {
    logger.log(DEBUG, "Clear/clean container SsComponents recursively.");
    if (container == null)
      return;
    SsUtils.visitSsComponents(container, comp -> comp.cleanField());
  }
}
// vi: sw=2 ts=8
