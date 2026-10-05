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

package dev.visdb.seesaw.decorators;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.lang.System.Logger;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import dev.visdb.seesaw.utils.Globals;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsComponent.ValidationResult;
import dev.visdb.seesaw.utils.SsUtils;

import static java.lang.System.Logger.Level.*;

/**
 * Decorate the border when SSComponent has focus, chose color dependent
 * on having valid data. A state dependent one line border is drawn.
 * The size of the original border is preserved.
 * If the border is null, then set a border of width 1.
 */
// NOTE: TODO: if there is a border and an element is 0, then may not quite right.
// TODO: could listen to components border property and adjust accordingly.
public class BorderDecorator extends FocusDecorator {
  private static final Logger logger = JStuff.getLogger();

  /**
   * Typically the border that the component started with;
   * not focus, no error.
   */
  protected Border defaultBorder;

  // ???: allow each border instance to have it's own painter
  private static BorderDecoratorPaint bdp;

  /**
   * Listens to lookup for the value.
   * @return the current value from {@link Globals}.
   */
  public static BorderDecoratorPaint getBorderDecoratorPaint() {
    if (bdp == null) {
      Globals.notifyOptionChange(BorderDecoratorPaint.class, (optionValue -> {
        if (optionValue != null) // typically two events, one for remove, one for add.
          bdp = optionValue;
      }));
      if (bdp == null)
        throw new IllegalStateException("BorderDecoratorPaint not found");
    }
    return bdp;
  }

  /**
   * Creates a Border that provides the visual state information; used by
   * BorderDecorator. Typically one line wide, colored and possibly dashed.
   */
  public static class BorderDecoratorPaint {
    /**
     * Determine color for specified BorderState; null return means
     * use the defaultBorder.
     * @param state
     * @return
     */
    public Color getBorderColor(ComponentState state) {
      return switch (state) {
        case CLEAN -> null;
        case FOCUSED_CLEAN -> Color.GREEN;
        case MODIFIED, FOCUSED_MODIFIED -> Color.ORANGE;
        case ERROR, FOCUSED_ERROR -> Color.RED;
      };
    }

    /**
     * Create a border for specified ComponentState; typically a colored
     * solid or dashed border. This single line width border is typically
     * one component of a compound border.
     * @param state
     * @return Border
     */
    public Border getDecoratingBorder(ComponentState state) {
      Color color = getBorderColor(state);
      Border decoratingBorder = state.isFocused() && state != ComponentState.FOCUSED_CLEAN
                                    ? BorderFactory.createDashedBorder(color, 13, 7)
                                    : BorderFactory.createLineBorder(color);
      return decoratingBorder;
    }
  }

  @SuppressWarnings({"UseOfSystemOutOrSystemErr", "unused"})
  void debugCheck(ComponentState borderState) {
    if (logger.isLoggable(TRACE)) {
      String caller = JStuff.getCaller(3);
      if (caller.contains("focusGained") || caller.contains("focusLost"))
        ; //System.out.println("");
      else
        ; //System.out.println("");
    }
  }

  /** Decorate the component using current state. */
  @Override
  public boolean decorate() {
    final ValidationResult valid = getSsComponent().allValidate();
    logger.log(TRACE,
               () -> String.format("%s focus: %s, compValid %s, allValid: %s",
                                   decoComp().getClass().getSimpleName(),
                                   hasFocus(), valid.comp(), valid.all()));
    if (SsUtils.findDecoratorPanel((Component)getSsComponent()) == null)
      return valid.all();
    Border b;
    ComponentState borderState = ComponentState.getComponentState(getSsComponent(), valid);
    //debugCheck(borderState);
    b = getBorder(borderState, decoComp());

    decoComp().setBorder(b != null ? b : defaultBorder);

    getSsComponent().decorateText(valid);

    return valid.all();
  }

  /** {@inheritDoc } */
  @Override
  public void install(SsComponent component) {
    super.install(component);
    defaultBorder = setupDefaultBorder(decoComp(), defaultBorder);
  }

  /** {@inheritDoc } */
  @Override
  public void uninstall() {
    defaultBorder = null;
    super.uninstall();
  }

  /**
   * Create a compound border the size of target's regular border.
   * Outside is empty, inside is 1 line.
   * @param state
   * @param decorateTarget
   * @return border for the state, null if state == CLEAN
   */
  public static Border getBorder(ComponentState state, JComponent decorateTarget) {
    if (state == ComponentState.CLEAN)
      return null;
    logger.log(DEBUG, () -> String.format("%s %s", state, asString(decorateTarget.getInsets())));
    Border b;
    if (decorateTarget.getBorder() instanceof CompoundBorder cb) {
      b = emptyLine_empty(cb.getOutsideBorder().getBorderInsets(decorateTarget),
                          cb.getInsideBorder().getBorderInsets(decorateTarget), state);
    } else {
      b = empty_line(decorateTarget.getInsets(), state);
    }
    return b;
  }

  /**
   * If a component has a border, then the defaultBorder is the
   * components original border.
   * If the component has no border, give it a default border
   * the size of it's insets, but with at least thickness 1.
   * If the defaultBorder is not null, then return it.
   * @param target
   * @param defaultBorder current state, returned if not null
   * @return 
   */
  public static Border setupDefaultBorder(JComponent target, Border defaultBorder) {
    if (defaultBorder != null)
      return defaultBorder;
    logger.log(DEBUG, () -> {
      Border b = target.getBorder();
      String bi = asString(target.getInsets());
      String bc = b != null ? b.getClass().getSimpleName() : null;
      String bs = asString(b, target);
      return String.format("%s-%s %s %s", target.getClass().getSimpleName(), bc, bi, bs);
    });
    Border b = target.getBorder();
    if (b == null) {
      //b = createDefaultBorder();
      Insets i = target.getInsets();
      b = BorderFactory.createEmptyBorder(Math.max(1, i.top), Math.max(1, i.left),
                                                                Math.max(1, i.bottom), Math.max(1, i.right));
      target.setBorder(b);
    }
    return b;
  }

  /**
   * Return a Border that displays the specified state.
   * See {@link BorderDecoratorPaint}.
   * @param state
   * @return
   */
  public static Border getDecoratingBorder(ComponentState state) {
    return getBorderDecoratorPaint().getDecoratingBorder(state);
  }

  /**
   * For cases where the JComponent doesn't have a usable border.
   * @param comp
   * @return border to use with the SsComponent
   */
  public static Border createEmptyBorder(SsComponent comp) {
    JComponent jc = (JComponent) comp;
    Border b = jc.getBorder();
    if (b instanceof CompoundBorder cb) {
      Insets oInsets = toInsets(cb.getOutsideBorder(), jc);
      Insets iInsets = toInsets(cb.getInsideBorder(), jc);
      b = BorderFactory.createCompoundBorder(
          BorderFactory.createEmptyBorder(oInsets.top, oInsets.left, oInsets.bottom, oInsets.right),
          BorderFactory.createEmptyBorder(iInsets.top, iInsets.left, iInsets.bottom,
                                          iInsets.right));
    } else {
      Insets i = jc.getInsets();
      b = BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right);
    }
    return b;
  }
  private static Insets toInsets(Border b, JComponent jc) {
    return b.getBorderInsets(jc);
  }

  //
  // Below are a few methods that create a CompoundBorder
  // from either insets or a compound border.
  // Each direction, top,left,bottom,right is computed separately.
  //
  // The description of the output for each direction uses
  // "_" for empty or space, and "|" for a line
  // and the returned border is delineated with brackets
  // "[outside][inside]". Note the brackets take no space
  // The line is usually at, or near, the edge of either
  // the outside or indide.
  //
  // For example
  //         outside   inside
  //        [_______][_______]
  //

  /**
   * Create a simple compound border with size specified by param i,
   * and a line on the inside of the param color.
   * <p>
   * [_______][|]
   * <p>
   * @param i insets that specify size of output border
   * @param state components border state
   * @return border
   */
  public static Border empty_line(Insets i, ComponentState state) {
    if (state == ComponentState.CLEAN)
      throw new IllegalArgumentException();
    Border decoratingBorder = getDecoratingBorder(state);
    Border b = BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(Math.max(0, i.top - 1), Math.max(0, i.left - 1),
                                        Math.max(0, i.bottom - 1), Math.max(0, i.right - 1)),
        decoratingBorder);
    return b;
  }

  /**
   * Create a simple compound border with size specified by param i,
   * and a line on the inside of the param color.
   * <p>
   * [|][_______]
   * <p>
   * @param i insets that specify size of output border
   * @param color color of line
   * @return border
   */
  public static Border line_empty(Insets i, Color color) // TODO: BorderState
  {
    if (Boolean.TRUE)
      throw new IllegalCallerException("needs state/color fixup");
    Border b = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(color),
        BorderFactory.createEmptyBorder(Math.max(0, i.top - 1), Math.max(0, i.left - 1),
                                        Math.max(0, i.bottom - 1), Math.max(0, i.right - 1)));
    return b;
  }

  /**
   * Create a Compound border the same size as the specified Insets where the
   * inside is a compound border of a line and a space, and the outside
   * border is the remaining space.
   * <p>
   * [_______][|_]
   * <p>
   * @param i insets for size of border
   * @param color line color
   * @return border, null if problem
   */
  public static Border empty_lineSpace(Insets i, Color color) // TODO: BorderState
  {
    if (Boolean.TRUE)
      throw new IllegalCallerException("needs state/color fixup");
    Border b = BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(Math.max(0, i.top - 2), Math.max(0, i.left - 2),
                                        Math.max(0, i.bottom - 2), Math.max(0, i.right - 2)),
        BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color),
                                           BorderFactory.createEmptyBorder(1, 1, 1, 1)));
    return b;
  }

  /**
   * Create a CompoundBorder wth the sizes indicated by the outside-inside
   * parameters.
   * The top level outside-inside are the same sizes as the input;
   * the top level outside is a compound border where the inside is
   * a line and the outside is the remaining.
   * <p>
   * [______|][_______]
   * <p>
   * @param outside
   * @param inside
   * @param state components border state
   * @return border
   */
  public static Border emptyLine_empty(Insets outside, Insets inside, ComponentState state) {
    Objects.requireNonNull(outside);
    Objects.requireNonNull(inside);
    Objects.requireNonNull(state);
    //Insets inside = cb.getInsideBorder().getBorderInsets(decoComp());
    //Insets outside = cb.getOutsideBorder().getBorderInsets(decoComp());
    Border decoratingBorder = getDecoratingBorder(state);
    Border b = BorderFactory.createCompoundBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(
                Math.max(0, outside.top - 1), Math.max(0, outside.left - 1),
                Math.max(0, outside.bottom - 1), Math.max(0, outside.right - 1)),
            decoratingBorder),
        BorderFactory.createEmptyBorder(inside.top, inside.left, inside.bottom, inside.right));
    return b;
  }

  /**
   * Create a CompoundBorder wth the sizes indicated by the outside-inside
   * parameters.
   * The top level outside-inside are the same sizes as the input;
   * the top level outside is a compound border where the outside is
   * a line and the inside is the remaining.
   * <p>
   * [|______][_______]
   * <p>
   * @param outside
   * @param inside
   * @param state components border state
   * @return border
   */
  public static Border lineEmpty_empty(Insets outside, Insets inside, ComponentState state) {
    Objects.requireNonNull(outside);
    Objects.requireNonNull(inside);
    Objects.requireNonNull(state);
    //Insets inside = cb.getInsideBorder().getBorderInsets(decoComp());
    //Insets outside = cb.getOutsideBorder().getBorderInsets(decoComp());
    Border decoratingBorder = getDecoratingBorder(state);
    Border b = BorderFactory.createCompoundBorder(
        BorderFactory.createCompoundBorder(
            decoratingBorder, BorderFactory.createEmptyBorder(
                                  Math.max(0, outside.top - 1), Math.max(0, outside.left - 1),
                                  Math.max(0, outside.bottom - 1), Math.max(0, outside.right - 1))),
        BorderFactory.createEmptyBorder(inside.top, inside.left, inside.bottom, inside.right));
    return b;
  }

  /**
   * Convert a border to a display string which shows compound border nesting
   * and terminal border insets.
   * @param b a border
   * @param jc
   * @return String of border for output
   */
  public static String asString(Border b, JComponent jc) {
    if (b == null)
      return null;
    if (b instanceof CompoundBorder cb) {
      return String.format("[%s,%s]", asString(cb.getOutsideBorder(), jc),
                           asString(cb.getInsideBorder(), jc));
    }
    return asString(b.getBorderInsets(jc));
  }

  /**
   * Convert inset to a display String; like "[2,2,2,2]".
   * @param i inset
   * @return String for output
   */
  public static String asString(Insets i) {
    return String.format("[%d,%d,%d,%d]", i.top, i.left, i.bottom, i.right);
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public DecoratorStyle getDecoratorStyle() {
    return DecoratorStyle.BORDER;
  }
}
// vi: sw=2 ts=8
