/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2025-2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.navigate;

import javax.sql.RowSet;

import static dev.visdb.seesaw.utils.JStuff.sf;
import static dev.visdb.seesaw.utils.SsUtils.objectID;

/**
 * This event signals the source {@link RowsModel} has a different
 * associated {@link javax.sql.RowSet}.Use {@link RowsModel#getRowSet()}
 * to get the RowSet.
 */
@SuppressWarnings("serial")
public class RowsModelNewRowSetEvent extends EventObjectBacktrace implements RowsModelEvent {
  private final RowSet newRowSet;
  private final RowSet oldRowSet;

  /**
   * Constructs a RowsModelEvent.
   * @param source RowsModel got a different RowSet
   * @param oldRowSet
   */
  public RowsModelNewRowSetEvent(RowsModel source, RowSet oldRowSet) {
    super(source);
    this.newRowSet = source.getRowSet();
    this.oldRowSet = oldRowSet;
  }

  /**
   * A RowsModel.
   * @return RowsModel that issued the event
   */
  @Override
  public RowsModel getRowsModel() {
    return (RowsModel) getSource();
  }

  /**
   * The old {@linkplain RowSet}.
   * @return previous for this model
   */
  public RowSet getNewRowSet() {
    return newRowSet;
  }

  /**
   * The old {@linkplain RowSet}.
   * @return previous for this model
   */
  public RowSet getOldRowSet() {
    return oldRowSet;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return sf("RowsModelNewRowSetEvent{%s, %s, old %s}", objectID(getRowsModel()),
              objectID(getNewRowSet()), objectID(getOldRowSet()));
  }
}
// vi: sw=2 ts=8
