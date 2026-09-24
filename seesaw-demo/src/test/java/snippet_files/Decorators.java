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

package snippet_files;


import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import dev.visdb.seesaw.utils.SsUtils;

/**
 *
 * @author err
 */
public class Decorators {
  JTextComponent textComponent;
  JFrame frame;
  void f1() {
    // @start region=decorator_panel_1
    JPanel uiPanel = new JPanel();
    uiPanel.add(textComponent);
    // ... create UI in uiPanel as usual
    frame.setContentPane(SsUtils.createDecoratorPanel(uiPanel));
    assert uiPanel == SsUtils.getInnerComponent(frame.getContentPane());
    // @end region=decorator_panel_1
  }
}
// vi: sw=2 ts=8
