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

package dev.visdb.seesaw.decorators;

import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.lang.System.Logger;

import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsUtils;

/**
 * Base class for decorators that use Focus.
 * <p>
 * Decorating does nothing until the component is inside a decorator panel,
 * see {@link SsUtils#createDecoratorPanel(java.awt.Component)}; so
 * the component is also decorated when it, or a panel holding it, is
 * added to a decorator panel. A component bound, and so decorated,
 * before then would otherwise show no error until the next decorate.
 */
public abstract class FocusDecorator extends BaseDecorator implements Decorator {
  private SsCompFocusListener focusListener;
  private HierarchyListener hierarchyListener;

  /** {@inheritDoc} */
  @Override
  public void install(SsComponent comp) {
    super.install(comp);
    focusListener = new SsCompFocusListener(comp, (c) -> {
      decorate();
    }, () -> logger());
    focusListener.addFocusListener();
    hierarchyListener = (e) -> {
      if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0
          && SsUtils.findDecoratorPanel((Component)comp) != null)
        decorate();
    };
    ((Component)comp).addHierarchyListener(hierarchyListener);
  }

  /** {@inheritDoc} */
  @Override
  public void uninstall() {
    focusListener.removeFocusListener();
    if (getSsComponent() instanceof Component c)
      c.removeHierarchyListener(hierarchyListener);
    hierarchyListener = null;
    super.uninstall();
  }

  /**
   * For debug use.
   * @return true if the SsComponent is focused
   */
  public Boolean hasFocus() {
    if (getSsComponent() == null)
      return null;
    return SsCompFocusListener.hasFocus(getSsComponent());
  }


  @SuppressWarnings("NonConstantLogger")
  private static Logger lazyLogger;
  private Logger logger() {
    if (lazyLogger == null)
      lazyLogger = JStuff.getLogger(getClass().getName());
    return lazyLogger;
  }
}
// vi: sw=2 ts=8
