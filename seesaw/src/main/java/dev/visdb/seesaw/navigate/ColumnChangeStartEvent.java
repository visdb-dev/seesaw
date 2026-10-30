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

import dev.visdb.seesaw.datasources.RSC;

/**
 * Sent for a component when its value is modified using updateXxx
 * in {@link dev.visdb.seesaw.datasources.RowSetOps}.
 */
@SuppressWarnings("serial")
public class ColumnChangeStartEvent extends ColumnChangeEvent {
  /**
   * Create a modification event.
   * @param source the component making the modification
   * @param value the value written to the rowSet
   */
  public ColumnChangeStartEvent(RSC source, Object value) {
    this(source, value, false);
  }

  /**
   * Create a modification event.
   * @param source the component making the modification
   * @param value the value written to the rowSet
   * @param error true if the component value is in error
   */
  public ColumnChangeStartEvent(RSC source, Object value, boolean error) {
    super(source, value, error);
  }
}
// vi: sw=2 ts=8
