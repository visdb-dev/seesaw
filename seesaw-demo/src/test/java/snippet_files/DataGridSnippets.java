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

import java.sql.SQLException;
import java.util.List;

import javax.sql.RowSet;
import javax.swing.JFrame;

import dev.visdb.seesaw.SsTable;
import dev.visdb.seesaw.navigate.RowsModel;

/**
 *
 */
@SuppressWarnings("serial")
public class DataGridSnippets extends JFrame {
  // SsTable dataGrid;
  // RowSet rowSet;
  RowsModel rowsModel;

  // @start region=init1
  /**
   * Set up a data table with customizations.
   * @param table initialize this table
   * @throws SQLException
   */
  void init(SsTable table) throws SQLException {
    // Set the header before setting the RowsModel.
    table.setHeaders(new String[] {"Part Name", "Color Code", " Weight", "City"});
    table.setRowsModel(rowsModel);

    // HIDE THE PART ID COLUMN
    table.setHiddenColumnsByName(List.of("part_id"));

    table.setDialogParent(this); // Defaults to the SsTable
    table.setUneditableColumns(new String[] {"part_id"});

    table.setComboRenderer("color_code", new String[] {"Red", "Green", "Blue"},
                           new Integer[] {0, 1, 2});
    table.setDefaultValues(new int[] {1, 2, 3}, new Object[] {0, 20, "New Orleans"});

    table.setPrimaryColumn("part_id");

    // Setup the primary key generation as needed.
    table.setSsDataValue(() -> {
      Integer key = null;

      // your primary key value generation goes here.
      // If it's something the user enters then no problem.
      // If it's an auto increment field then it depends on
      // the database driver you are using.
      // If the updaterow can retrieve the values for the row
      // without knowing the primary key value its fine.

      return key;
    });
  }
  // @end region=init1
}
// vi: sw=2 ts=8
