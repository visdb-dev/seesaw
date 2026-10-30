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
package dev.visdb.seesaw.formatting;

/**
 * Base class for formatted fields.
 */
@SuppressWarnings("serial")
public abstract class Field extends SsFormattedTextField {
  /**
   * Constructor.
   * @param factory formatter factory
   */
  public Field(AbstractFormatterFactory factory) {
    super(factory);
  }

  /**
   * {@inheritDoc }
   * If a null value, make sure it's OK to have it.
   */

  @Override
  public boolean componentValidate() {
    return getValue() != null || getAllowNull();
  }
}
// vi: sw=2 ts=8
