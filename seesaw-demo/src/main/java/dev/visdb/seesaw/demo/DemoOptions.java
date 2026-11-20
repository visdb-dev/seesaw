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

import java.util.List;

/**
 * The options from the command line, after {@code MainClass.main} has
 * parsed them, for the database setup.
 *
 * @param verbose output initialization sql as it runs
 * @param dump write the sql scripts to local files and exit
 * @param noLoadImages do not load the images
 * @param noInitializeDb do not run the scripts on an external database
 * @param propertyFile jdbc connection properties, may be null
 * @param userSqlFiles sql files given with {@code -s}
 * @param dataset where the demo's scripts come from
 */
record DemoOptions(boolean verbose, boolean dump, boolean noLoadImages, boolean noInitializeDb,
                   String propertyFile, List<String> userSqlFiles, DemoDataset dataset) {
  DemoOptions {
    userSqlFiles = List.copyOf(userSqlFiles);
  }
}
// vi: sw=2 ts=8
