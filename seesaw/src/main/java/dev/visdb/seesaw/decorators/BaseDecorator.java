/* *****************************************************************************
 * Copyright (C) 2026, Ernie R Rael. All rights reserved.
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
 * ****************************************************************************/
package dev.visdb.seesaw.decorators;

import dev.visdb.seesaw.utils.SsComponent.ValidationResult;

/**
 * Some handling for the TextDecorator; {@link #handleTextDecorator(ValidationResult)}
 * should be called at the end of subclass' decorate().
 */
public abstract class BaseDecorator extends BaseAnyDecorator implements Decorator {
  private boolean decorateTextEnabled = true;

  /**
   * Focus decorators typically decorate text as well;
   * this can be used to control that behavior.
   * @param flag
   */
  @Override
  public void setDecorateTextEnabled(boolean flag) {
    decorateTextEnabled = flag;
  }

  /**
   * Deal with a TextDecorator for this component.
   * @param valid
   */
  protected void handleTextDecorator(ValidationResult valid) {
    if (!decorateTextEnabled)
      return;
    TextDecorator td = getSsComponent().getTextDecorator();
    assert td != null;
    if (td instanceof ComponentStateTextDecorator std)
      std.decorateText(valid);
    else
      td.decorateText();
  }
}
