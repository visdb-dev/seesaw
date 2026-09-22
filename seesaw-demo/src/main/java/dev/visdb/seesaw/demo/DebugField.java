/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2024-2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.demo;

import javax.swing.text.DefaultFormatterFactory;

import dev.visdb.seesaw.formatting.Field;
import dev.visdb.seesaw.formatting.MaskFormatterFactory;
import dev.visdb.seesaw.formatting.SsFormat;

import static dev.visdb.seesaw.formatting.SsFormat.CUSTOM;

/**
 * A simple field for debug that is not in the formatting package.
 */
@SuppressWarnings("serial")
public class DebugField extends Field {
  /**
   *  Creates a default SSDateField object using the default date format.
   */
  public DebugField() {
    this(CUSTOM);
  }

  /**
   *  Creates a new instance of SSDateField with the specified format.
   *  @param format - an enum format to be used while the date field is in edit mode
   */
  public DebugField(SsFormat format) {
    this(createFormatterFactory(format));
  }

  /**
   * Creates an object of SSDateField with the specified formatter factory
   * @param factory - formatter factory to be used
   */
  public DebugField(AbstractFormatterFactory factory) {
    super(factory);
  }

  @Override
  public void cleanField() {
    setValue(getAllowNull() ? null : 777);
  }

  /**
   * Create mask formatter factory with specified format pattern.
   * @param _format - Format to be used for date while in editing mode.
   * @return a DefaultFormatterFactory for the specified date format
   */
  public static DefaultFormatterFactory createFormatterFactory(SsFormat _format) {
    SsFormat format = SsFormat.getActualFormat(_format);
    String formatMask = "###";

    return new MaskFormatterFactory.Builder<>(formatMask).ssFormat(format).build();
  }
}
// vi: sw=2 ts=8
