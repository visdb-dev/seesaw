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

package dev.visdb.seesaw.contrib.simplevalidation;

import java.awt.event.FocusEvent;

import javax.swing.JComponent;
import javax.swing.border.Border;

import dev.visdb.seesaw.decorators.BorderDecorator;
import dev.visdb.seesaw.decorators.ComponentState;
import dev.visdb.seesaw.utils.SsComponent;


/**
 * The SimpleValidation decorator can do a focus border.
 * The border color comes from {@link BorderDecorator.BorderDecoratorPaint}
 */
public class SVFocusBorderFactory {
  private SVFocusBorderFactory() { }

  /** creates a border decorator helper for the component.
   * @param ssComp
   * @return  */
  static SVFocusBorder get(SsComponent ssComp) {
    return new SVFocusBorder(ssComp);
  }
  
  /** Paint a border for the component when focused */
  static class SVFocusBorder { // implements FocusListener {
    private final SsComponent ssComp;
    private final Border defaultBorder;

    /**  */
    public SVFocusBorder(SsComponent ssComp) {
      this.ssComp = ssComp;
      defaultBorder = BorderDecorator.setupDefaultBorder(decoComp(), null);
    }

    private JComponent decoComp() { return ssComp.getDecorateTarget(); }

    //@Override
    public void focusGained(FocusEvent e) {
      Border b = BorderDecorator.getBorder(ComponentState.FOCUSED_CLEAN, decoComp());
      decoComp().setBorder(b);
    }

    //@Override
    public void focusLost(FocusEvent e) {
      decoComp().setBorder(defaultBorder);
    }
  }
}
// vi: sw=2 ts=8