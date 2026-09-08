/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
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
