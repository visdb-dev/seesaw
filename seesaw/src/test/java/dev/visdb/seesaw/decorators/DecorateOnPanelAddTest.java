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

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.visdb.seesaw.SsTextField;
import dev.visdb.seesaw.mock.Util;
import dev.visdb.seesaw.utils.SsUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A component that is decorated before it is in a decorator panel
 * shows its state once it, or a panel holding it, is added to one.
 */
public class DecorateOnPanelAddTest {
  @BeforeAll
  public static void setUpClass() {
    Util.seesawInit();
  }

  @SuppressWarnings("serial")
  private static SsTextField invalidField() {
    return new SsTextField() {
      @Override
      public boolean baseValidate() {
        return false;
      }
    };
  }

  /** The decorating border has a red line border in it. */
  private static boolean isErrorBorder(Border b) {
    return switch (b) {
      case LineBorder lb -> Color.RED.equals(lb.getLineColor());
      case CompoundBorder cb -> isErrorBorder(cb.getOutsideBorder())
                                || isErrorBorder(cb.getInsideBorder());
      case null, default -> false;
    };
  }

  @Test
  public void testWrapPanelAfterDecorate() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      SsTextField field = invalidField();
      JPanel uiPanel = new JPanel();
      uiPanel.add(field);
      field.decorate();
      assertFalse(isErrorBorder(field.getBorder()), "no decorator panel yet");
      SsUtils.createDecoratorPanel(uiPanel);
      assertTrue(isErrorBorder(field.getBorder()));
    });
  }

  @Test
  public void testAddToDecoratorPanelAfterDecorate() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      SsTextField field = invalidField();
      field.decorate();
      JPanel uiPanel = new JPanel();
      SsUtils.createDecoratorPanel(uiPanel);
      assertFalse(isErrorBorder(field.getBorder()), "not added yet");
      uiPanel.add(field);
      assertTrue(isErrorBorder(field.getBorder()));
    });
  }
}
// vi: sw=2 ts=8
