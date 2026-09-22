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
package dev.visdb.seesaw.contrib.simplevalidation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.netbeans.validation.api.AbstractValidator;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;

import dev.visdb.seesaw.decorators.ComponentState;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsComponent.Validation;
import dev.visdb.seesaw.utils.SsComponent.ValidationResult;

/**
 * A validator specialized for an SsComponent.
 * It has a validationCondition, like a pluginValidator,
 * and description, that handles a string.
 * It sets the SsComponent's pluginValidator to nullValidator.
 * It does SsComponent.allValidate.
 */
// TODO: Turn this into an SsComponentValidator.
//       Should this be Validator<SsComponent>?
//       Needs converter SsComponent to text or to Document.
public class StringSsComponentValidator extends AbstractValidator<String> {
  private final Function<String, Boolean> validationCondition;
  private final Supplier<String> problemDescription;
  private final SsComponent comp;

  /**
   * Create a validator that handles a string.
   * @param validationCondition returns true if string is valid.
   * @param problemDescription produces error message if validationCondition is false.
   * @param comp how weird is it to have this here?
   */
  public StringSsComponentValidator(Function<String, Boolean> validationCondition,
				    Supplier<String> problemDescription,
				    SsComponent comp) {
    super(String.class);
    this.validationCondition = Objects.requireNonNull(validationCondition);
    this.problemDescription = Objects.requireNonNull(problemDescription);
    this.comp = Objects.requireNonNull(comp);
    comp.setPluginValidator(dev.visdb.seesaw.decorators.Validator.nullValidator);
  }
  
  /**
   * Validate using this' validationCondition and problemDescription.
   * @param problems
   * @param compName
   * @param model
   */
  // TODO: use compName in message.
  // TODO: handle condition in pluginValidate.
  // TODO: detect/prevent warnings in problems?
  @Override
  public void validate(Problems problems, String compName, String model) {
    ValidationResult vr = comp.allValidate();
    ComponentState borderState = ComponentState.getComponentState(comp, vr);
    if (borderState.isModified())
      problems.append("modified", Severity.INFO);
    
    Optional<Validation> fail = vr.firstFail();
    if (fail.isPresent()) {
      problems.append(comp.validationMsg(fail.get()));
      return;
    }
    // AT LEAST FOR NOW plugin can't fail; check it here.
    if (!validationCondition.apply(model)) {
      problems.append(problemDescription.get());
    }
  }
}
// vi: sw=2 ts=8
