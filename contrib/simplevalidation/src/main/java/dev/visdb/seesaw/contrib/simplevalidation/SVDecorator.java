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


import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ui.ValidationListener;

import dev.visdb.seesaw.decorators.BaseDecorator;
import dev.visdb.seesaw.decorators.Decorator;
import dev.visdb.seesaw.decorators.SsCompFocusListener;
import dev.visdb.seesaw.utils.SsComponent;

/**
 * A SeeSaw decorator using the 
 * <a href="https://github.com/timboudreau/simplevalidation">Simple Validation</a>
 * framework to perform swing component decoration and problem reporting.
 * The decorate() method optionally invokes
 * {@link SsComponent#decorateText(dev.visdb.seesaw.utils.SsComponent.ValidationResult)
 * ssComp.decorateText(result)}, see {@link #setDoDecorateText(boolean) }.
 */
public class SVDecorator extends BaseDecorator {

  private final ValidationListener<?> valItem;
  private boolean doDecorateText;

  /**
   * Create SeeSaw decorator which uses a ValidationItem
   * for decoration/validation. {@code doDecorateText} is false.
   * decorateText() is typically invoked in a custom {@link Validator}
   * invoked via valItem.performValidation().
   * @param valItem
   */
  public SVDecorator(ValidationListener<?> valItem) {
    this(valItem, false);
  }

  /**
   * Create an SVDecorator which uses any ValidationListener
   * for decoration/validation. The {@code doDeorateText}
   * may be set.
   * @param valItem
   * @param doDecorateText if true, do it during the decorate() method
   */
  SVDecorator(ValidationListener<?> valItem, boolean doDecorateText) {
    this.valItem = valItem;
    this.doDecorateText = doDecorateText;
  }

  private SVBorder border;
  private SsCompFocusListener focusListener;

  /**
   * {@inheritDoc }
   */
  @Override
  public void install(SsComponent ssComp) {
    super.install(ssComp);
    border = SVBorder.get(getSsComponent());
    focusListener = new SsCompFocusListener(ssComp, (c) -> {
      // System.err.printf("*** DefaultSVDecorator (%s) - notifyFocusChange: %s, focused: %s\n",
      //                     objectID(component), objectID(c), c.isFocusOwner());
      if (c.isFocusOwner())
        border.draw();
      else
        border.clear();
    });
    focusListener.addFocusListener();
  }

  /**
   * Remove focus listening.
   */
  @Override
  public void uninstall() {
    focusListener.removeFocusListener();
    super.uninstall();
  }

  /**
   * Decorate the component using current state.
   * @throws IllegalStateException if component doesn't match validation item.
   */
  @Override
  public boolean decorate() {
    Problem problem = valItem.performValidation();
    if (doDecorateText)
      getSsComponent().decorateText(null);
    return problem == null || !problem.isFatal();
  }

  /**
   * @return if this decorator invokes decorateText().
   */
  public boolean isDoDecorateText() {
    return doDecorateText;
  }

  /**
   * Set whether or not this decorator invokes decorateText().
   *
   * @param doDecorateText flag
   */
  public void setDoDecorateText(boolean doDecorateText) {
    this.doDecorateText = doDecorateText;
  }

  /**
   * SimpleValidatorDecorator style
   * @return
   */
  @Override
  public Decorator.DecoratorStyle getDecoratorStyle() {
    return SVUtils.SIMPLE_VALIDATION;
  }
}
// vi: sw=2 ts=8
