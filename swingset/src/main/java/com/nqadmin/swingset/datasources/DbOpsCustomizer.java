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
package com.nqadmin.swingset.datasources;

import com.nqadmin.swingset.SSDataNavigator;
import com.nqadmin.swingset.navigate.RowsAction;
import com.nqadmin.swingset.utils.SSEnums.Navigation;

/**
 * Interface that provides a set of methods to perform custom operations before
 * and/or after certain database related operations;
 * for example add record, delete record.
 * There are also other methods to prevent/allow or assist
 * certain actions, for example {@code allowDeletion()} or {@code performCancelOps()}.
 * <p>
 * In this class' documentation there are references to {@link RowsAction}
 * enum values which have associated actions. The actions
 * are typically invoked by a <b>navigator button push</b> which invokes the
 * associated action,
 * for example see {@link SSDataNavigator}
 * <p>
 * This interface has only default methods, none of which do anything; it
 * can be instantiated by doing {@code new CustomizeDbOps() {}}.
 * <p>
 * Generally the user will want to use/extend {@link DbOpsCustomizerImpl} as it has an
 * implementation of {@link DbOpsCustomizerImpl#performPreInsertOps()}
 * that will clear/reset {@link com.nqadmin.swingset.utils.SSComponent SSComponent}
 * values when a new record is added.
 */
// TODO: rename SSDBCustomOps
public interface DbOpsCustomizer {
	/**
	 * This function is called as the first step, before inserting the row into
	 * the database, of {@link RowsAction#ACT_COMMIT}.
	 *
	 * @return true if row can be inserted, false aborts the operation
	 */
	default boolean allowInsertion() {
		return true;
	}

	/**
	 * This function is called as the first step, before performPreDeletionOps
	 * is called, of {@link RowsAction#ACT_DELETE}.
	 *
	 * @return true if the row can be deleted else false and the action is aborted
	 */
	default boolean allowDeletion() {
		return true;
	}

	/**
	 * This functions is called just before doing something that is sensitive
	 * to a row being dirty. When it returns true, it is followed by
	 * rowSet.updateRow(). Note that the default for {@code AutoCommit} is false.
	 * So the behavior of {@link SSDataNavigator} in conjunction with
	 * {@link RowsAction} is that when the current row is dirty only
	 * commit and undo are enabled.
	 *
	 * @return true is the row can be updated else false.
	 */
	default boolean allowUpdate() {
		return true;
	}

	/**
	 * This method is invoked by {@link RowsAction#ACT_REVERT}, the undo
	 * button, to perform operations after
	 * either rowSet.cancelRowUpdates or rowSet.moveToCurrentRow.
	 * Only meaningful if either the current row
	 * is modified, or a row is being inserted.
	 */
	default void performCancelOps() { }

	/**
	 * Method to perform operations when the user hits the refresh button.
	 * This method is invoked at the end of {@link RowsAction#ACT_REFRESH};
	 * note the rowSet's query is re-executed and the cursor is positioned
	 * at the first row.
	 */
	default void performRefreshOps() { }

	/**
	 * Method to perform navigation-related operations, in particular at the
	 * end of
	 * {@link RowsAction#ACT_FIRST}, {@link RowsAction#ACT_LAST},
	 * {@link RowsAction#ACT_NEXT}, and {@link RowsAction#ACT_PREVIOUS}.
	 *
	 * @param navigationType type of navigation that was done
	 */
	default void performNavigationOps(final Navigation navigationType) { }

	/**
	 * Method to perform pre-insertion operations by {@link RowsAction#ACT_ADD}
	 * after rowSet.moveToInsertRow. Typically initializes all the columns'
	 * {@code SSComponent}s. See {@link DbOpsCustomizerImpl#performPreInsertOps()}.
	 */
	default void performPreInsertOps() { }
	
	/**
	 * Method to perform post-insertion operations during 
	 * {@link RowsAction#ACT_COMMIT} after rowSet.insertRow.
	 * If the insert is aborted, {@link RowsAction#ACT_REVERT} cancelling
	 * a pending insert, this is not called.
	 * <p>
	 * In addition to this, a listener on the RowSet is
	 * notified after the RowSet is modified.
	 */
	default void performPostInsertOps() { }

	/**
	 * Method to perform pre-deletion operations; it is used for
	 * {@link RowsAction#ACT_DELETE}, invoked just before rowSet.deleteRow().
	 */
	default void performPreDeletionOps() { }

	/**
	 * Method to perform post-deletion operations; it is used for
	 * {@link RowsAction#ACT_DELETE}, invoked just after rowSet.deleteRow().
	 * <p>
	 * In addition to this, a listener on the RowSet is
	 * notified after the RowSet is modified.
	 */
	default void performPostDeletionOps() { }

	/**
	 * Method to perform operations at the end of {@link RowsAction#ACT_COMMIT}
	 * when the current row is modified, i.e. not on the insert row.
	 * It is invoked after rowSet.updateRow().
	 */
	default void performPostUpdateOps() { }
}

