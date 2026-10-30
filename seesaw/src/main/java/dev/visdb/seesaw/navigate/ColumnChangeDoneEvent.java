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
package dev.visdb.seesaw.navigate;

/**
 * The modification has propagated, in particular to the undo/redo stack.
 */
@SuppressWarnings("serial")
public class ColumnChangeDoneEvent extends ColumnChangeEvent {
  /**
   * Create a modification done event.
   *
   * @param ev the event that started the modification
   */
  public ColumnChangeDoneEvent(ChangeEventData ev) {
    super(ev);
  }
}
// vi: sw=2 ts=8
