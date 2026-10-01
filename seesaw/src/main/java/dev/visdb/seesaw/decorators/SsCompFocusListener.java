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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.function.Consumer;

import com.raelity.lib.eventbus.WeakEventBus;
import com.raelity.lib.eventbus.WeakSubscribe;

import dev.visdb.seesaw.navigate.FocusChangeEvent;
import dev.visdb.seesaw.utils.SsComponent;

import static dev.visdb.seesaw.navigate.Utils.getGlobalEventBus;
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

  /**
   * Create SsComponent focus lost/gained detector.
   * Only focus changes for SsComponent are notified.
   * The actual component that lost/gained focus is sent with the notifier.
   * Use {@link Component#isFocusOwner() } to determine lost or gained.
   * @param ssComp
   * @param notifyFocusChange 
   */
  public SsCompFocusListener(SsComponent ssComp, Consumer<Component> notifyFocusChange) {
    this.ssComp = ssComp;
    this.notifyFocusChange = notifyFocusChange;
  }

  /**
   * Arrange for notification of focus lost/gained.
   */
  // TODO: don't need focusTarget, use SsComponent
  public void addFocusListener(/*Component focusTarget*/) {
    if (!ssComp.isComposite()) {
      assert ssComp == ssComp.getFocusTarget();
      ((Component)ssComp).addFocusListener(getFocusListener());
    } else {
      busReceiver = new BusReceiver();
      WeakEventBus.register(busReceiver, getGlobalEventBus());
    }
  }

  /**
   * Remove listeners.
   */
  // TODO: don't need focusTarget, use SsComponent
  public void removeFocusListener(/*Component focusTarget*/) {
    ((Component)ssComp).removeFocusListener(focusListener);
    if (busReceiver != null) {
      WeakEventBus.unregister(busReceiver, getGlobalEventBus());
      busReceiver = null;
    }
  }

  FocusListener focusListener;
  private FocusListener getFocusListener() {
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

  private BusReceiver busReceiver; // Must have a strong reference.

  class BusReceiver {
    /**
     * via KeyboardFocusManager.
     * @param ev
     */
    @WeakSubscribe
    public void handleFocusChangeEvent(FocusChangeEvent ev) {
      // If both are contained in the same SsComponent then only need the 2nd,
      // Actually it is already focused and focusGained.
      checkFocusChange((Component) ev.getPce().getOldValue());
      checkFocusChange((Component) ev.getPce().getNewValue());
    }
  }

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  private void checkFocusChange(Component c) {
    if (c instanceof SsComponent && c != ssComp) {
      return;
    }

    if (c != null && isDescendingFrom(c, (Component) ssComp)) {
      // decorate();
      if (Boolean.FALSE)
        System.err.printf("*** checkFocusChange: %s, focused: %s\n",
                        objectID(c), c.isFocusOwner());
      notifyFocusChange.accept(c);
    }
  }
}
// vi: sw=2 ts=8