/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2024-2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.navigate;

import javax.sql.RowSet;

import dev.visdb.seesaw.datasources.RSC;

/**
 * Sent by a component when value changes from undo/redo.
 * The contents of the undo/redo stack is unchanged; note the error.
 */
@SuppressWarnings("serial")
public class ColumnUndoRedoEvent extends EventObjectBacktrace implements ChangeEventData {
  final private Object value;
  final private boolean error;

  /**
   * Create a undo/redo stack event.
   * Signals change in components current value and if newValue is an error.
   * @param source the component making the modification
   * @param value the value written to the rowSet
   * @param error true if the component value is in error
   */
  public ColumnUndoRedoEvent(RSC source, Object value, boolean error) {
    super(source);
    this.value = value;
    this.error = error;
  }

  /**
   * Test if this event is for the specified rowSet.
   * @param rowSet check against this rowSet
   * @return true if the event is for the specified rowSet
   */
  public boolean matches(RowSet rowSet) {
    return getSource().getRowSet() == rowSet;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public RSC getSource() {
    return (RSC) super.getSource();
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public RSC getRSC() {
    return getSource();
  }

  /**
   * Value written to rowSet.
   * @return value
   */
  @Override
  public Object getValue() {
    return value;
  }

  /**
   * Test if this event's component's value is in error.
   * @return true if in error.
   */
  @Override
  public boolean isError() {
    return error;
  }
}
// vi: sw=2 ts=8
