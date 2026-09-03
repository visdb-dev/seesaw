/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2024, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/

package dev.visdb.seesaw.datasources;

/**
 * Null used as database value where not allowed.
 */
@SuppressWarnings(value = "serial")
public class SqlNullException extends SqlException {
  /**
   * Construct an SQLException with given reason.
   *
   * @param reason description of the exception
   */
  public SqlNullException(String reason) {
    super(reason);
  }
}
