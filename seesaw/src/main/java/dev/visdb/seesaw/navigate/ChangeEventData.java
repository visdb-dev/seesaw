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
package dev.visdb.seesaw.navigate;

import dev.visdb.seesaw.datasources.RSC;

/**
 * Implemented by several events.
 */
public interface ChangeEventData {
  /**
   * The SSComponent for the event.
   * @return
   */
  RSC getRSC();

  /**
   * The new value of the event.
   * @return
   */
  Object getValue();

  /**
   * Is the data in error.
   * @return
   */
  boolean isError();
}
// vi: sw=2 ts=8
