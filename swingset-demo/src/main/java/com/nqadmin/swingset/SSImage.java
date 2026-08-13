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
 * copyright (C) 2025, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package com.nqadmin.swingset;

import javax.sql.RowSet;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import dev.visdb.seesaw.core.Image;
import dev.visdb.seesaw.navigate.RowsModel;
import dev.visdb.seesaw.utils.ZoomCanvas;

import static dev.visdb.seesaw.utils.SSUtils.findRowsModel;

/**
 * Used to load, store, and display images stored in a database.
 */
// TODO: SSImage make all the load/store buttons/capabilities optional.
@SuppressWarnings("serial")
public class SSImage extends Image {
  // Doesn't matter what this is, since there are no zoom controls. Always bestFit.
  private final ZoomCanvas.ResizeMode resizeMode = ZoomCanvas.ResizeMode.CENTER_PANNING;

  /**
   * Construct a default SSImage Object.
   */
  public SSImage() {
    setResizeMode(resizeMode);
    setImageToolTipText(null);
  }

  /**
   * Constructs a SSImage Object bound to the specified column in the specified
   * rowSet.
   *
   * @param rowsModel          - RowSet from/to which data has to be read/written
   * @param columnName - column in the rowSet to which the component should
   *                         be bound.
   */
  @SuppressWarnings("LeakingThisInConstructor")
  public SSImage(RowsModel rowsModel, String columnName) {
    this();
    rowsModel.bind(this, columnName);
  }

  /**
   * Only show the update button, don't show the zoom controls.
   * @return the "Update" button
   */
  @Override
  protected JComponent imageControls() {
    return getUpdateButton();
  }

  /**
   * @return null - no popup menu
   */
  @Override
  protected JPopupMenu createImagePopup() {
    return null;
  }

  /**
   * Constructs a SSImage Object bound to the specified column in the specified
   * rowSet.
   *
   * @param rowSet          - RowSet from/to which data has to be read/written
   * @param columnName - column in the rowSet to which the component should
   *                         be bound.
   * @deprecated use RowsModel insted of RowSet
   */
  @Deprecated
  public SSImage(RowSet rowSet, String columnName) {
    this(findRowsModel(rowSet), columnName);
  }
}
