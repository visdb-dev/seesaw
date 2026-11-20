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
package dev.visdb.seesaw.demo;

/**
 * The scripts and load lists in the demo's resources, by what they are for.
 * Each knows its file name for each {@link DbServer}, so the rest of the demo
 * does not use file names; renaming the files changes only this enum.
 * Where a file is found is decided by {@link DemoDataset}.
 */
enum DemoScript {
  //                  H2                         MySQL
  SUPPLIERS_AND_PARTS("suppliers_and_parts.sql", "mysql.demo-app.sql"),
  COMPONENTS(         "tests.sql",               "mysql.demo-components.sql"),
  /** Loads the images with H2's FILE_READ; only in the H2 dump. */
  COMPONENTS_BLOBS(   "tests_load_blobs.sql",    null),
  DEBUG(              "debug.sql",               null),
  /** The images and the keys they go with in base_test_data. */
  DEMO_IMAGES(        "demo-images.txt",         "demo-images.txt");

  private final String h2;
  private final String mysql;

  DemoScript(String h2, String mysql) {
    this.h2 = h2;
    this.mysql = mysql;
  }

  /**
   * @param server the database server
   * @return the file name, without a directory; null if there is none for the server
   */
  String fileName(DbServer server) {
    return switch (server) {
      case H2 -> h2;
      case MYSQL -> mysql;
    };
  }
}
// vi: sw=2 ts=8
