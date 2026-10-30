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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static dev.visdb.seesaw.datasources.ConvertType.castJDBCToJava;

/**
 *
 */
public class ConvertTypeSnippets {
  void castArray() throws SQLException {
    // @start region=convert_array
    Object[] arr = f(); // But I "know" the elements are Integer
    Integer[] newarr = (Integer[]) castJDBCToJava(JDBCType.INTEGER, arr);
    List<Integer> properList = Arrays.asList(newarr);
    // @end region=convert_array
  }

  Object[] f() {
    return null;
  }
}
// vi: sw=2 ts=8
