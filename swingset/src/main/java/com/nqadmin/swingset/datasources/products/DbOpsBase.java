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
import java.sql.SQLException;

import javax.sql.RowSet;

import com.nqadmin.swingset.datasources.DbMetaDataCache;
import com.nqadmin.swingset.datasources.DbOpsImpl;
import com.nqadmin.swingset.navigate.RowsModel;

/**
 * Use metadata where applicable.
 * In particular to decide whether or not to re-execute the RowSset command.
 */
public class DbOpsBase extends DbOpsImpl {
  /**
   * Constructs a DbOpsBase with the specified container.
   *
   * @param container	GUI Container to scan for Swing components to clear/reset
   */
  public DbOpsBase(Container container) {
    super(container);
  }
  
  /**
   * Make sure the RowSet has the inserted row.
   * It may re-execute the RowSet's command.
   * @param rm
   * @throws SQLException 
   */
  @Override
  public void performPostInsertOps(RowsModel rm) throws SQLException {
    super.performPostInsertOps(rm);
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
    super.performPostDeletionOps(rm);
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
    super.performPostUpdateOps(rm);
    RowSet rs = rm.getRowSet();
    if (!DbMetaDataCache.get().ownUpdatesAreVisible(rs.getType()))
      rs.execute();
  }
}
