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
import org.netbeans.validation.api.ui.ValidationListener;

import dev.visdb.seesaw.contrib.simplevalidation.SVFocusBorderFactory.SVFocusBorder;
import dev.visdb.seesaw.decorators.BaseDecorator;
import dev.visdb.seesaw.decorators.Decorator;
import dev.visdb.seesaw.decorators.SsCompFocusListener;
import dev.visdb.seesaw.utils.SsComponent;

/**
 * A decorator using the 
 * <a href="https://github.com/timboudreau/simplevalidation">Simple Validation</a>
 * framework.
 * The decorate() method does {@snippet lang="java":
 *   Problem problem = valItem.performValidation();
 *   if (doTextDecoration)
 *     getSsComponent().handleTextDecorator(null);
 * }
 */
public class SVDecorator extends BaseDecorator {

  private final ValidationListener<?> valItem;
  private boolean doTextDecoration;

  /**
   * Create SimpleValidationDecorator which uses a ValidationITem
   * for decoration/validation. doTextDecoration is false.
   * @param valItem
   */
  public SVDecorator(ValidationListener<?> valItem) {
    this(valItem, false);
  }

  /**
   * Create SimpleValidationDecorator which uses any ValidationListener
   * for decoration/validation. In normal usage, doTextDecoration is false,
   * the parm valItem is expected to use a Validator which does 
   * {@snippet lang="java":
   *   result = getSsComponent().allValidate();
   *   getSsComponent().handleTextDecorator(result);
   * }
   * @param valItem
   * @param doTextDecoration during the decorate() method
   */
  public SVDecorator(ValidationListener<?> valItem, boolean doTextDecoration) {
    this.valItem = valItem;
    this.doTextDecoration = doTextDecoration;
  }

  private SVFocusBorder focusBorder;
  private SsCompFocusListener focusListener;

  /**
   * {@inheritDoc }
   * Create focusBorder and add focus listening.
   */
  @Override
  public void install(SsComponent component) {
    super.install(component);
    focusBorder = SVFocusBorderFactory.get(getSsComponent());
    focusListener = new SsCompFocusListener(component, (c) -> {
      // System.err.printf("*** DefaultSVDecorator (%s) - notifyFocusChange: %s, focused: %s\n",
      //                     objectID(component), objectID(c), c.isFocusOwner());
      if (c.isFocusOwner())
        focusBorder.focusGained(null);
      else
        focusBorder.focusLost(null);
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
    if (doTextDecoration)
      getSsComponent().handleTextDecorator(null);
    return problem == null || !problem.isFatal();
  }

  /**
   * @return if this decorator invokes handleTextDecorator.
   */
  public boolean isDoTextDecoration() {
    return doTextDecoration;
  }

  /**
   * Set whether or not this decorator invokes handleTextDecorator.
   *
   * @param doTextDecoration flag
   */
  public void setDoTextDecoration(boolean doTextDecoration) {
    this.doTextDecoration = doTextDecoration;
  }

  /**
   * SimpleValidatorDecorator style
   * @return
   */
  @Override
  public Decorator.DecoratorStyle getDecoratorStyle() {
    return SVUtils.SV_DECORATOR_STYLE;
  }
}
// vi: sw=2 ts=8
