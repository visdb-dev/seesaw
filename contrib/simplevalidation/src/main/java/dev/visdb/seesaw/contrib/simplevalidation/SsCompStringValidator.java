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

import org.netbeans.validation.api.AbstractValidator;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;

import dev.visdb.seesaw.decorators.ComponentState;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsComponent.Validation;
import dev.visdb.seesaw.utils.SsComponent.ValidationResult;

/**
 * A validator specialized for an SsComponent that's a subclass of
 * JTextComponent. It is used by
 * {@link SVUtils#setTextDecorator(SsComponent, String, Validator...) }
 * It does {@link SsComponent#allValidate()} and, if there's a failure,
 * it uses {@link SsComponent#validationMsg(Validation)} for the problem
 * description. Finally it invokes
 * {@link SsComponent#decorateText(ValidationResult)}.
 * <p>
 * The SsComponent is used in {@link #validate(Problems, String, String) validate(...)};
 * this is unusual.
 * It is a {@code Validator<String>} so it is easy to chain together with other
 * {@code Validator<String>}, see {@link StringValidators} and
 * {@link SVUtils#setTextDecorator(SsComponent, String, Validator...) }.
 * 
 */
// TODO: Should this be FIRST and not LAST?
public class SsCompStringValidator extends AbstractValidator<String> {
  private final SsComponent ssComp;

  /**
   * Create a validator that handles a string.
   * @param ssComp how weird is it to have this here?
   */
  public SsCompStringValidator(SsComponent ssComp) {
    super(String.class);
    this.ssComp = Objects.requireNonNull(ssComp);
  }
  
  /**
   * Validate using this' validationCondition and problemDescription.
   * The model is ignored, the relevant information derives from SsComponent.allValidate()
   * @param problems
   * @param compName
   * @param model
   */
  // TODO: use compName in message.
  // TODO: detect/prevent warnings in problems?
  @Override
  public void validate(Problems problems, String compName, String model) {
    ValidationResult vr = ssComp.allValidate();
    ComponentState borderState = ComponentState.getComponentState(ssComp, vr);
    if (borderState.isModified())
      problems.append("modified", Severity.INFO);
    
    Optional<Validation> fail = vr.firstFail();
    if (fail.isPresent())
      problems.append(ssComp.validationMsg(fail.get()));

    // This is normally at the end of the SeeSaw decorator.
    ssComp.decorateText(vr);
  }
}
// vi: sw=2 ts=8