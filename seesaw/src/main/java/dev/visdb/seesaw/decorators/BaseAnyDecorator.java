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

import javax.swing.JComponent;

import dev.visdb.seesaw.utils.SsComponent;

import static dev.visdb.seesaw.utils.JStuff.sf;

/**
 * Used for both Decorator and TextDecorator.
 */
public abstract class BaseAnyDecorator implements AnyDecorator {
  private SsComponent ssComponent;

  /**
   * Install this decorator into the component. Installs listeners
   * @param component the component
   */
  @Override
  public void install(SsComponent component) {
    if (this.ssComponent != null)
      throw new IllegalStateException(sf("'%s' allready installed in '%s'",
                                         this.getClass().getSimpleName(),
                                         ssComponent.getClass().getSimpleName()));
    this.ssComponent = component;
  }

  /** Remove decorator/listeners from component. */
  @Override
  public void uninstall() {
    this.ssComponent = null;
  }

  /**
   * Return the SsComponent associated with this decorator.
   *
   * @return the component
   */
  @Override
  public final SsComponent getSsComponent() {
    return ssComponent;
  }

  /**
   * Convenience method to get the SsComponent cast as a JComponent.
   *
   * @return the SsComponent as a JComponent
   */
  protected final JComponent jComp() {
    return (JComponent) getSsComponent();
  }

  /**
   * Return the JComponent that gets decorated and TextDecorated.
   * It may not be the same as whats returned by {@link #getSsComponent() }.
   *
   * @return the JComponent
   */
  protected final JComponent decoComp() {
    return getSsComponent().getDecorateTarget();
  }
}
// vi: sw=2 ts=8
