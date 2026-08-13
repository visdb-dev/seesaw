/* *****************************************************************************
 * Copyright (C) 2026, Ernie R Rael. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * ****************************************************************************/
package dev.visdb.seesaw.decorators;

import javax.swing.JComponent;

import dev.visdb.seesaw.utils.SSComponent;

import static dev.visdb.seesaw.utils.JStuff.sf;

/**
 * Used for both Decorator and TextDecorator.
 */
public abstract class BaseAnyDecorator implements AnyDecorator {
  private SSComponent ssComponent;

  /**
   * Install this decorator into the component. Installs listeners
   * @param component the component
   */
  @Override
  public void install(SSComponent component) {
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
   * Return the SSComponent associated with this decorator.
   *
   * @return the component
   */
  @Override
  public final SSComponent getSSComponent() {
    return ssComponent;
  }

  /**
   * Convenience method to get the SSComponent cast as a JComponent.
   *
   * @return the SSComponent as a JComponent
   */
  protected final JComponent jComp() {
    return (JComponent) getSSComponent();
  }

  /**
   * Return the JComponent that gets decorated and TextDecorated.
   * It may not be the same as whats returned by {@link #getSSComponent() }.
   *
   * @return the JComponent
   */
  protected final JComponent decoComp() {
    return getSSComponent().getDecorateTarget();
  }
}
