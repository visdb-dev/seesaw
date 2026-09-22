/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */
package snippet_files;

import javax.swing.JCheckBox;

import dev.visdb.seesaw.datasources.RowSetOps;
import dev.visdb.seesaw.utils.SsComponent;

/**
 * x
 */
@SuppressWarnings("serial")
public class DbReaderDbUpdater extends JCheckBox implements SsComponent {
  @SuppressWarnings("unused")
  void F() {
    // @start region=setColumnReader
    setColumnReader((rs, cidx, _) -> { return rs.getBytes(cidx); });
    // @end region=setColumnReader
    // @start region=setColumnUpdater
    setColumnUpdater((rs, cidx, _, value) -> {
      if (value == null) {
        rs.updateNull(cidx);
        return RowSetOps.UPDATE_NULL;
      } else {
        rs.updateBytes(cidx, (byte[]) value);
        return new RowSetOps.DbUpdate(value);
      }
    });
    // @end region=setColumnUpdater
  }

  /**
   *x
   */
  @Override
  public void cleanField() {}

  /**
   *x
   * @return
   */
  @Override
  public Hook getSsComponentHook() {
    return null;
  }
}
// vi: sw=2 ts=8
