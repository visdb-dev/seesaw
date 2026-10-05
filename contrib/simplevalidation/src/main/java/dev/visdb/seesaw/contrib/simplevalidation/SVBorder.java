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


import javax.swing.JComponent;
import javax.swing.border.Border;

import dev.visdb.seesaw.decorators.BorderDecorator;
import dev.visdb.seesaw.decorators.BorderDecorator.BorderDecoratorPaint;
import dev.visdb.seesaw.decorators.ComponentState;
import dev.visdb.seesaw.utils.Globals;
import dev.visdb.seesaw.utils.SsComponent;

/**
 * Draw/clear a border for the component's decoration target on demand,
 * this doesn't listen.
 * The border, when drawn, uses the current color associated with
 * {@link ComponentState#FOCUSED_CLEAN} from the current {@link BorderDecoratorPaint}.
 * Honors the {@link Globals} option {@link SVUtils.SimpleValidationBorderEnable}.
 */
public class SVBorder {
  static SVBorder get(SsComponent ssComp) {
    return new SVBorder(ssComp);
  }

  private static boolean borderEnabled = true;
  static {
    Globals.notifyOptionChange(SVUtils.SimpleValidationBorderEnable.class, (optionValue -> {
      if (optionValue != null) // typically two events, one for remove, one for add.
        borderEnabled = optionValue.flag();
    }));
  }

  // implements FocusListener {
  private final SsComponent ssComp;
  private final Border defaultBorder;

  /**
   * For drawing/clearing a border for the specified component.
   * The constructor captures the current border.
   * @param ssComp 
   */
  public SVBorder(SsComponent ssComp) {
    this.ssComp = ssComp;
    defaultBorder = BorderDecorator.setupDefaultBorder(decoComp(), null);
  }

  private JComponent decoComp() {
    return ssComp.getDecorateTarget();
  }

  /**
   * Draw the border.
   */
  public void draw() {
    if (!borderEnabled)
      return;
    Border b = BorderDecorator.getBorder(ComponentState.FOCUSED_CLEAN, decoComp());
    decoComp().setBorder(b);
  }

  /**
   * Restore the original border.
   */
  public void clear() {
    decoComp().setBorder(defaultBorder);
  }
}
// vi: sw=2 ts=8