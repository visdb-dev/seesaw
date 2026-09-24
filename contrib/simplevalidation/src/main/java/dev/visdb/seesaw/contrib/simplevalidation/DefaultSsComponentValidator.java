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

import java.util.Objects;
import java.util.Optional;

import org.netbeans.validation.api.AbstractValidator;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;

import dev.visdb.seesaw.decorators.ComponentState;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsComponent.Validation;
import dev.visdb.seesaw.utils.SsComponent.ValidationResult;

/**
 * This validator runs on any SsComponent. It contains no validation conditions
 * of its own. It exclusively uses comp.allValidate().
 */
public class DefaultSsComponentValidator extends AbstractValidator<SsComponent> {
  private final SsComponent comp;
  
  /** Create. */
  // TODO: remove param
  public DefaultSsComponentValidator(SsComponent comp) {
    super(SsComponent.class);
    this.comp = Objects.requireNonNull(comp);
  }
  
  /** Do compnent.allValidate */
  @Override
  public void validate(Problems problems, String compName, SsComponent model) {
    // TODO: comp --> model
    if (model != comp)
      throw new IllegalArgumentException();
    ValidationResult vr = comp.allValidate();
    ComponentState borderState = ComponentState.getComponentState(comp, vr);
    if (borderState.isModified())
      problems.append("modified", Severity.INFO);
    
    Optional<Validation> fail = vr.firstFail();
    if (fail.isPresent())
      problems.append(comp.validationMsg(fail.get()));
  }
}
// vi: sw=2 ts=8
