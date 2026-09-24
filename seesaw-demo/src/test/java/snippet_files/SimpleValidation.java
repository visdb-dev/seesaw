/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package snippet_files;

import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JPanel;

// @start region=validation_example_import

import org.netbeans.validation.api.ui.ValidationItem;

import dev.visdb.seesaw.contrib.simplevalidation.SVUtils;
// @end region=validation_example_import
import dev.visdb.seesaw.SsTextField;
import dev.visdb.seesaw.utils.SsUtils;

public class SimpleValidation {
  // @start region=validation_example
  void createFrameUI(JFrame frame) {
    SsTextField txtSupplierName = new SsTextField();
    SsTextField txtSupplierCity = new SsTextField();

    ValidationItem decoSupplierName = SVUtils.setDecoratorValidator(
        txtSupplierName, "Supplier Name",
        (str) -> str == null || !str.matches("(?i).*oops.{0,2}$"),
        () -> "Supplier name can not end with 'oops..'");

    ValidationItem decoSupplierCity = SVUtils.setDecoratorValidator(
        txtSupplierCity, "Supplier City",
        (str) -> str == null || !str.matches(".*X"),
        () -> "City can not end in 'X'");

    // Put our components in a JPanel as usual
    final Container uiPanel = new JPanel();
    uiPanel.add(txtSupplierName);
    uiPanel.add(txtSupplierCity);
    
    // Wrap the uiPanel in a ValidationPanel.
    JPanel validationPanel = SVUtils.createDecoratorPanel(
        uiPanel, decoSupplierName, decoSupplierCity);
    
    // Put the ValidiationPanel in the frame's contentPane.
    frame.setContentPane(validationPanel);
    assert uiPanel == SsUtils.getInnerComponent(frame.getContentPane());
  }
  // @end region=validation_example
}
// vi: sw=2 ts=8
