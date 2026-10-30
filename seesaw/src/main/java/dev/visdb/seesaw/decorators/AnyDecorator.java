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
package dev.visdb.seesaw.decorators;

import dev.visdb.seesaw.utils.SsComponent;

/**
 * Any kind of decorator.
 */
public interface AnyDecorator {
  /**
   * Install this decorator into the component. Installs listeners
   * @param component the componenet
   */
  void install(SsComponent component);

  /** Remove decorator/listeners from component. */
  void uninstall();

  /**
   * Return the SsComponent associated with this decorator.
   *
   * @return the component
   */
  SsComponent getSsComponent();
}
// vi: sw=2 ts=8
