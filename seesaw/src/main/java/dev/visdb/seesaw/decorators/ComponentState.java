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

package dev.visdb.seesaw.decorators;

import java.awt.Component;

import javax.swing.SwingUtilities;

import dev.visdb.seesaw.navigate.Utils;
import dev.visdb.seesaw.utils.SsComponent;

/**
 * The state of a component, focused/dirty/modified.
 * Used to create a proper decoration
 */
// TODO: may want to treat as bit field for: error/focus/warning/dirty
public enum ComponentState {
  /** not focused, no error, not modified */
  CLEAN,
  /** focus gained, no error, not modified */
  FOCUSED_CLEAN,
  /** modified without focus */
  MODIFIED,
  /** modified with focus */
  FOCUSED_MODIFIED,
  /** error with/without focus */
  ERROR,
  /** error with/without focus */
  FOCUSED_ERROR;

  /**
   * focused?
   * @return
   */
  public boolean isFocused() {
    return this == FOCUSED_CLEAN || this == FOCUSED_MODIFIED || this == FOCUSED_ERROR;
  }

  /**
   * modified?
   * @return
   */
  public boolean isModified() {
    return this == MODIFIED || this == FOCUSED_MODIFIED;
  }

  /**
   * focused?
   * @return
   */
  public boolean isError() {
    return this == ERROR || this == FOCUSED_ERROR;
  }

  /**
   * Determine the state of the component about focus/clean/dirty/error.
   * @param comp
   * @param valid
   * @return the component state
   */
  public static ComponentState getComponentState(SsComponent comp,
                                                 SsComponent.ValidationResult valid) {
    ComponentState borderState;
    if (valid.all())
      borderState = comp.isDirty() ? MODIFIED : CLEAN;
    else
      borderState = ERROR;
    Component f = Utils.getKFM().getFocusOwner();
    if (f != null && SwingUtilities.isDescendingFrom(f, (Component) comp))
      borderState = switch (borderState) {
        case CLEAN -> FOCUSED_CLEAN;
        case MODIFIED -> FOCUSED_MODIFIED;
        case ERROR -> FOCUSED_ERROR;
        default -> throw new IllegalStateException("Unexpected value: " + (borderState));
      };
    return borderState;
  }
}
// vi: sw=2 ts=8
