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
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JPanel;

// @start region=validation_example_import

import org.netbeans.validation.api.AbstractValidator;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;

import dev.visdb.seesaw.SsImage;
import dev.visdb.seesaw.contrib.simplevalidation.SVUtils;
// @end region=validation_example_import
import dev.visdb.seesaw.SsTextField;
import dev.visdb.seesaw.contrib.simplevalidation.SVDecorator;
import dev.visdb.seesaw.decorators.ComponentState;
import dev.visdb.seesaw.decorators.Decorator;
import dev.visdb.seesaw.utils.Globals;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsComponent.Validation;
import dev.visdb.seesaw.utils.SsComponent.ValidationResult;
import dev.visdb.seesaw.utils.SsUtils;

public class SimpleValidation {
  // @start region=validation_example
  void createFrameUI(JFrame frame) {
    SsTextField txtSupplierName = new SsTextField();
    SsTextField txtSupplierCity = new SsTextField();

    // Set some validation criteria; note it's independent of validation style.
    txtSupplierName.setTextValidationCondition(
        (str) -> str == null || !str.matches("(?i).*oops.{0,2}$"),
        (ssComp) -> "Supplier name can not end with 'oops..'");
    txtSupplierCity.setTextValidationCondition(
        (str) -> str == null || !str.matches(".*X"),
        (ssComp) -> "City can not end in 'X'");

    // Put our components in a JPanel and build the UI as usual.
    final Container uiPanel = new JPanel();
    uiPanel.add(txtSupplierName);
    uiPanel.add(txtSupplierCity);
    
    // Wrap the uiPanel in a ValidationPanel,
    // and add ValidationItems to the ValidationPanel's ValidationGroup.
    JPanel validationPanel = SVUtils.createDecoratorPanel(uiPanel,
        SVUtils.setTextDecorator(txtSupplierName, "Supplier Name"),
        SVUtils.setTextDecorator(txtSupplierCity, "Supplier City"));
    
    // Put the ValidiationPanel in the frame's contentPane.
    frame.setContentPane(validationPanel);
    assert uiPanel == SsUtils.getInnerComponent(frame.getContentPane());
  }
  // @end region=validation_example

  void foo() {
    SsImage imgImage = new SsImage();
    SVUtils.setDecorator(imgImage, "test_images",
                         new AbstractValidator<SsImage>(SsImage.class) {
        @Override
        public void validate(Problems problems, String compName, SsImage model) {
          SsComponent ssComp = imgImage;
          // @start region=validate
          ValidationResult result = ssComp.allValidate();
          ComponentState state = ComponentState.getComponentState(ssComp, result);
          if (state.isModified())
            problems.append("modified", Severity.INFO);
          
          Optional<Validation> fail = result.firstFail();
          if (fail.isPresent())
            problems.append(ssComp.validationMsg(fail.get()));
          ssComp.decorateText(result);
          // @end region=validate

          // @start region=setDoDecorateText
          ((SVDecorator)ssComp.getTextDecorator()).setDoDecorateText(true);
          // @end region=setDoDecorateText
        }
      });
  }

  void bar() {
    // @start region=sv_border_enable
    Globals.setOption(SVUtils.SimpleValidationBorderEnable.class,
                      new SVUtils.SimpleValidationBorderEnable(false));
    // @end region=sv_border_enable

    // @start region=sv_global
    Globals.setOption(Decorator.DecoratorStyle.class,
                      SVUtils.SIMPLE_VALIDATION);
    // @end region=sv_global

  }
}
// vi: sw=2 ts=8
