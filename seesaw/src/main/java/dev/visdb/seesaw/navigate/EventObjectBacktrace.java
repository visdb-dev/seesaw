/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2025-2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.navigate;

import java.util.EventObject;

import static dev.visdb.seesaw.navigate.Utils.recordEventBacktrace;

/**
 * Provides a backtrace of when the event is generated
 */
@SuppressWarnings("serial")
public class EventObjectBacktrace extends EventObject {
  private final Throwable backtrace;

  /**
   * @param source
   */
  public EventObjectBacktrace(Object source) {
    super(source);
    this.backtrace = recordEventBacktrace() ? new Throwable("EventObjectBacktrace") : null;
  }

  /**
   * @return where the event was created, may be null.
   */
  Throwable getEventBacktrace() {
    return backtrace;
  }
}
// vi: sw=2 ts=8
