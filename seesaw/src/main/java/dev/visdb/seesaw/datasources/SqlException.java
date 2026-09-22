/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2024,2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.datasources;

import java.sql.SQLException;

/**
 * Base of all SS specific exceptions
 */
@SuppressWarnings("serial")
public class SqlException extends SQLException {
  /**
   * SSException
   * @param reason reason
   */
  public SqlException(String reason) {
    super(reason);
  }

  /**
   * SSException
   */
  public SqlException() {}

  /**
   * SSException
   * @param cause cause
   */
  public SqlException(Throwable cause) {
    super(cause);
  }

  /**
   * SSException
   * @param reason reason
   * @param cause cause
   */
  public SqlException(String reason, Throwable cause) {
    super(reason, cause);
  }
}
// vi: sw=2 ts=8
