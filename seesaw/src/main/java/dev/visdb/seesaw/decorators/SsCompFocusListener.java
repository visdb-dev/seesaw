/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package dev.visdb.seesaw.decorators;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.raelity.lib.eventbus.WeakEventBus;
import com.raelity.lib.eventbus.WeakSubscribe;

import dev.visdb.seesaw.navigate.FocusChangeEvent;
import dev.visdb.seesaw.utils.SsComponent;

import static dev.visdb.seesaw.navigate.Utils.getGlobalEventBus;
import static dev.visdb.seesaw.utils.JStuff.sf;
import static dev.visdb.seesaw.utils.SsUtils.objectID;
import static javax.swing.SwingUtilities.isDescendingFrom;

/**
 * Assist detecting focus lost/gained in an SsComponent. Most cases, like a
 * SsCheckBox, are easy. Simply add a focus listener to the component. But
 * some components, like SsImage, SsCombobox*, are composites (they have
 * nested components that may get focus) and require special handling,
 * see {@link SsComponent#isComposite() }.
 */
public class SsCompFocusListener {
  private final SsComponent ssComp;
  /** only components that are contained in an SsComponent are notified */
  private final Consumer<Component> notifyFocusChange;
  private final Supplier<Logger> loggerSupplier;

  /**
   * Create SsComponent focus lost/gained detector.
   * Only focus changes for SsComponent are notified.
   * The actual component that lost/gained focus is sent with the notifier.
   * Use {@link Component#isFocusOwner() } to determine lost or gained.
   * @param ssComp
   * @param notifyFocusChange 
   */
  public SsCompFocusListener(SsComponent ssComp, Consumer<Component> notifyFocusChange) {
    this(ssComp, notifyFocusChange, null);
  }

  /**
   * Create SsComponent focus lost/gained detector.
   * Only focus changes for SsComponent are notified.
   * Logger.
   * @param ssComp
   * @param notifyFocusChange 
   * @param loggerSupplier 
   */
  public SsCompFocusListener(SsComponent ssComp, Consumer<Component> notifyFocusChange,
                             Supplier<Logger> loggerSupplier) {
    this.ssComp = ssComp;
    this.notifyFocusChange = notifyFocusChange;
    this.loggerSupplier = loggerSupplier;
  }

  /**
   * Arrange for notification of focus lost/gained.
   */
  public void addFocusListener() {
    if (!ssComp.isComposite()) {
      ((Component)ssComp).addFocusListener(getFocusListener());
    } else {
      busReceiver = new BusReceiver();
      WeakEventBus.register(busReceiver, getGlobalEventBus());
    }
  }

  /**
   * Remove listeners.
   */
  public void removeFocusListener() {
    ((Component)ssComp).removeFocusListener(focusListener);
    if (busReceiver != null) {
      WeakEventBus.unregister(busReceiver, getGlobalEventBus());
      busReceiver = null;
    }
  }

  FocusListener focusListener;
  private FocusListener getFocusListener() {
    // Only created if !ssComp.isComposite
    if (focusListener == null) {
      focusListener = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
          notifyFocusChange.accept((Component)ssComp);
        }

        @Override
        public void focusLost(FocusEvent e) {
          notifyFocusChange.accept((Component)ssComp);
        }
      };
    }
    return focusListener;
  }

  /**
   * @param ssComp
   * @return true if the ssComp, or something it conains, has focus
   */
  public static boolean hasFocus(SsComponent ssComp) {
    Component own = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    return own != null ? isDescendingFrom(own, (Component) ssComp) : false;
  }

  //
  // The following is only used when ssComp.isComposite().
  //

  private BusReceiver busReceiver; // Must have a strong reference.

  class BusReceiver {
    /**
     * via KeyboardFocusManager.
     * @param ev
     */
    @WeakSubscribe
    public void handleFocusChangeEvent(FocusChangeEvent ev) {
      // If both are contained in the same SsComponent then only need the 2nd,
      // too much work right now. And note: if both, already focused and focusGained.
      checkFocusChange((Component) ev.getPce().getOldValue());
      checkFocusChange((Component) ev.getPce().getNewValue());
    }
  }

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  private void checkFocusChange(Component c) {
    if (c == null || c instanceof SsComponent && c != ssComp) {
      log(Level.TRACE, () ->sf("Quick exit: focused '%s', ssComp '%s'", objectID(c),
                               objectID(ssComp)));
      return;
    }
    dumpCheckFocusInfo(Level.INFO, c, ssComp, loggerSupplier);

    if (isDescendingFrom(c, (Component) ssComp)) {
      log(Level.TRACE, () ->sf("%s, focused: %s\n", objectID(c), c.isFocusOwner()));
      notifyFocusChange.accept(c);
    }
  }

  private void log(Level level, Supplier<String> msg) {
    Logger logger = loggerSupplier != null ? loggerSupplier.get() : null;
    if (logger != null)
      logger.log(level, msg);
  }
  @SuppressWarnings("unused")
  private boolean isLoggable(Level level) {
    Logger logger = loggerSupplier != null ? loggerSupplier.get() : null;
    return logger != null ? logger.isLoggable(level) : false;
  }

  /**
   * Historical.
   * @param level
   * @param c
   * @param ssComp
   * @param logger
   */
  @SuppressWarnings({"UseOfSystemOutOrSystemErr", "unused"})
  public static void dumpCheckFocusInfo(Level level, Component c, SsComponent ssComp,
                                        Supplier<Logger> logger) {
    if (logger == null)
      return;
    if (!logger.get().isLoggable(level))
      return;
    String nam = "";
    if (c != null) {
      nam = c.getClass().getSimpleName();
      if (nam.isBlank())
        nam = c.getClass().getName();
    }
    String namF = nam;
    logger.get().log(level, () -> sf("focused %s, SSComp %s", c == null ? "null" : namF,
                                          ssComp.getClass().getSimpleName()));
  }
}
// vi: sw=2 ts=8