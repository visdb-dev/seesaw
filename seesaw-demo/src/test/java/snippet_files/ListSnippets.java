/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2025 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */
package snippet_files;

import java.sql.JDBCType;
import java.util.List;

import dev.visdb.seesaw.SsList1;
import dev.visdb.seesaw.navigate.RowsModel;

/**
 * xxx
 */
public class ListSnippets {
  SsList1<Object, String> list;
  RowsModel rowsModel;

  // @start region=init1
  /**
   * Create an SsList1, initialize its contents,
bind the list selection to a column in the RowsModel.
   */
  @SuppressWarnings("unused")
  void init() {
    list = new SsList1<>(JDBCType.DOUBLE);
    List<String> options = List.of("VLarge", "large", "medium", "small", "VSmall");
    List<Object> mappings = List.of(100.0, 10.0, 5.0, 1.0, 0.1);
    list.setDisplayValues(options, mappings);
    rowsModel.bind(list, "my_column");
  }
  // @end region=init1
}
// vi: sw=2 ts=8
