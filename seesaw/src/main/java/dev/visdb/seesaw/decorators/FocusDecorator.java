/* *****************************************************************************
 * Copyright (C) 2024, Prasanth R. Pasala, Brian E. Pangburn, & The Pangburn Group
 * All rights reserved.
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
 *
 * Contributors:
 *   Prasanth R. Pasala
 *   Brian E. Pangburn
 *   Diego Gil
 *   Man "Bee" Vo
 *   Ernie R. Rael
 * ****************************************************************************/
/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/

package dev.visdb.seesaw.decorators;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import com.raelity.lib.eventbus.WeakEventBus;
import com.raelity.lib.eventbus.WeakSubscribe;

import dev.visdb.seesaw.navigate.FocusChangeEvent;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsComponent;

import static dev.visdb.seesaw.navigate.Utils.getGlobalEventBus;
import static dev.visdb.seesaw.utils.JStuff.sf;
import static dev.visdb.seesaw.utils.SsUtils.objectID;
import static javax.swing.SwingUtilities.isDescendingFrom;

/**
 * Base class for decorators that use Focus.
 */
public abstract class FocusDecorator extends BaseDecorator implements Decorator, FocusListener {
  /** Apply decoration */
  @Override
  public void focusGained(FocusEvent e) {
    decorate();
  }

  /** Remove decoration */
  @Override
  public void focusLost(FocusEvent e) {
    decorate();
  }

  /** {@inheritDoc} */
  @Override
  public void install(SsComponent comp) {
    super.install(comp);
    if (!comp.isComposite())
      focusComp().addFocusListener(this);
    else {
      logger().log(Level.DEBUG, sf("Composite component %s", objectID(comp)));
      busReceiver = new BusReceiver();
      WeakEventBus.register(busReceiver, getGlobalEventBus());
    }
  }

  /** {@inheritDoc} */
  @Override
  public void uninstall() {
    super.uninstall();
    focusComp().removeFocusListener(this);
    if (busReceiver != null) {
      WeakEventBus.unregister(busReceiver, getGlobalEventBus());
      busReceiver = null;
    }
  }

  /**
   * Return the Component that gets focus when the associated SsComponent
is focused.
   *
   * @return focus target
   */
  protected Component focusComp() {
    return getSsComponent().getFocusTarget();
  }

  @SuppressWarnings("NonConstantLogger")
  private static Logger lazyLogger;
  private Logger logger() {
    if (lazyLogger == null)
      lazyLogger = JStuff.getLogger(getClass().getName());
    return lazyLogger;
  }

  private BusReceiver busReceiver; // Must have a strong reference.

  class BusReceiver {
    /**
     * via KeyboardFocusManager.
     * @param ev
     */
    @WeakSubscribe
    public void handleFocusChangeEvent(FocusChangeEvent ev) {
      checkFocusChange((Component) ev.getPce().getOldValue());
      checkFocusChange((Component) ev.getPce().getNewValue());
    }
  }

  /**
   * If the component is of the SsComponent, then decorate().
   * @param c
   */
  protected void checkFocusChange(Component c) {
    if (c instanceof SsComponent && c != getSsComponent()) {
      logger().log(Level.TRACE, sf("Quick exit: focused '%s', ssComp '%s'", objectID(c),
                                   objectID(getSsComponent())));
      return;
    }
    if (logger().isLoggable(Level.DEBUG))
      dumpCheckFocusInfo(c);

    if (c != null && isDescendingFrom(c, (Component) getSsComponent())) {
      decorate();
    }
  }

  @SuppressWarnings({"UseOfSystemOutOrSystemErr", "unused"})
  private void dumpCheckFocusInfo(Component c) {
    String nam = "";
    if (c != null) {
      nam = c.getClass().getSimpleName();
      if (nam.isBlank())
        nam = c.getClass().getName();
    }
    logger().log(Level.TRACE, sf("focused %s, SSComp %s", c == null ? "null" : nam,
                                 getSsComponent().getClass().getSimpleName()));
  }
}
