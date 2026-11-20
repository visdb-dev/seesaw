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

import java.util.Locale;
import java.util.Optional;

/**
 * A database server for which the demo has scripts in its resources.
 * It picks a script's file name; see {@link DemoScript#fileName(DbServer)}.
 * External databases, whose scripts are given with {@code -s},
 * are not a {@code DbServer}.
 */
enum DbServer {
  H2("h2"),
  MYSQL("mysql");

  private final String cmdName;

  DbServer(String cmdName) {
    this.cmdName = cmdName;
  }

  /** @return the name used on the command line */
  String cmdName() {
    return cmdName;
  }

  /**
   * @param cmdName a name from the command line
   * @return the server, empty if there is none with that name
   */
  static Optional<DbServer> forName(String cmdName) {
    String name = cmdName.toLowerCase(Locale.ROOT);
    for (DbServer server : values()) {
      if (server.cmdName.equals(name))
        return Optional.of(server);
    }
    return Optional.empty();
  }
}
// vi: sw=2 ts=8
