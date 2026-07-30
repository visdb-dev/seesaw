/* *****************************************************************************
 * Copyright (C) 2026, Ernie R Rael. All rights reserved.
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
package com.nqadmin.swingset.datasources.products;

import java.awt.Container;
import java.lang.System.Logger;
import java.sql.SQLException;
import java.util.function.Consumer;

import javax.sql.RowSet;

import com.nqadmin.swingset.datasources.DbMetaDataCache;
import com.nqadmin.swingset.datasources.DbOps;
import com.nqadmin.swingset.navigate.DbOpsChangeEvent;
import com.nqadmin.swingset.navigate.RowsModel;
import com.nqadmin.swingset.utils.JStuff;
import com.nqadmin.swingset.utils.SSComponent;
import com.nqadmin.swingset.utils.SSUtils;

import static com.nqadmin.swingset.navigate.Utils.postDbOpsChange;
import static java.lang.System.Logger.Level.DEBUG;

/**
 * Implementation of DbOps that implements performPreInsertOps() to
 * clear/initialize the various SSComponents on a screen before the
 * user edits the new record;
 * implements the {@code performPost*Ops} methods
 * to use metadata to decide whether or not to re-execute the RowSset command;
 * handles state for the {@code allow*} methods.
 * <p>
 * {@code DbOps} is associated with a RowsModel/RowSet, see
 * {@link com.nqadmin.swingset.navigate.RowsModel#create(javax.sql.RowSet, com.nqadmin.swingset.datasources.DbOps) RowsModel(RowSet, DbOps)}.
 * {@link #performPreInsertOps()} searches the container provided to the
 * constructor to find the {@link SSComponent}s to clean.
 */
public class DbOpsBase implements DbOps {
  /**
   * Logger for component
   */
  protected static final Logger logger = JStuff.getLogger();

  /**
   * Screen where components to be cleared are located.
   */
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
   * In the specified container, clear/initialize SSComponents.
   * Typically done for a new record/row. Uses
   * {@link SSUtils#visitSSComponents(Container, Consumer) }
   * to run {@link SSComponent#cleanField() }.
   * <p>
   * This is done for all SwingSet components,
   * for example text fields, and text areas,
   * recursively looking in to the JTabbedPanes and JPanels inside the given
   * container as needed.
   *
   * @param container container in which to recursively initialize components
   */
  protected void cleanComponents(final Container container) {
    logger.log(DEBUG, "Clear/clean container SSComponents recursively.");
    if (container == null) return;
    SSUtils.visitSSComponents(container, comp -> comp.cleanField());
  }
  
  /**
   * Make sure the RowSet has the inserted row.
   * It may re-execute the RowSet's command.
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
   * It may re-execute the RowSet's command.
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
   * It may re-execute the RowSet's command.
   * @param rm
   * @throws java.sql.SQLException
   */
  @Override
  public void performPostUpdateOps(RowsModel rm) throws SQLException {
    RowSet rs = rm.getRowSet();
    if (!DbMetaDataCache.get().ownUpdatesAreVisible(rs.getType()))
      rs.execute();
  }
}
