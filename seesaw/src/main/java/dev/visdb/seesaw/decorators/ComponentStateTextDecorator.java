/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.decorators;

import java.lang.System.Logger;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.text.AttributeSet;

import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsComponent;

import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.DEBUG;

/**
 * This TextDecorator applies a named TextStyle based on ComponentState.
 * There is a map of ComponentState to text style name.
 * If no style name is available for a ComponentState, {@link TextStyles#RESET}
 * is used.
 */
public class ComponentStateTextDecorator extends BaseTextDecorator {
  private static final Logger logger = JStuff.getLogger();

  private final EnumMap<ComponentState, String> styleNames = new EnumMap<>(ComponentState.class);

  /**
   * Create using specified map.
   * @param map
   */
  public ComponentStateTextDecorator(Map<ComponentState, String> map) {
    styleNames.putAll(map);
  }

  /**
   * Decorate the text according to {@code valid}.
   * @param valid
   */
  public void decorateText(SsComponent.ValidationResult valid) {
    ComponentState state = ComponentState.getComponentState(getSsComponent(), valid);
    AttributeSet style = TextStyles.getStyle(styleNames.get(state));
    TextStyles.applyStyle(jComp(), style != null ? style : TextStyles.RESET);
    logger.log(DEBUG, sf("Style: %s", jComp().getClientProperty(TextStyles.STYLE_NAME)));
  }

  /**
   * Validate the component and decorate the text accordingly.
   */
  @Override
  public void decorateText() {
    SsComponent.ValidationResult valid = getSsComponent().allValidate();
    decorateText(valid);
  }
}
// vi: sw=2 ts=8
