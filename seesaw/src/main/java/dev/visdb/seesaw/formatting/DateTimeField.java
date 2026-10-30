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
package dev.visdb.seesaw.formatting;

import java.util.List;

import dev.visdb.seesaw.datasources.DateTime;
import dev.visdb.seesaw.datasources.RSC;

/**
 * Base class for date time fields; provides specialized component validation.
 */
@SuppressWarnings("serial")
abstract public class DateTimeField extends Field {
  /**
   * Create.
   * @param factory formatter factory
   */
  public DateTimeField(AbstractFormatterFactory factory) {
    super(factory);
  }

  /**
   * Sets the value of the field to an initial state consistent with
   * the AllowNull property. If not AllowNull then use the current system date.
   */
  @Override
  public void cleanField() {
    if (getAllowNull()) {
      setValue(null);
    } else {
      setValue(new java.util.Date());
    }
  }

  /**
   * Specialized Date/Time/Timestamp component validation.
   * @param strings text to check: list[0] is masked, list[1] is plain
   * @param comp
   * @return false if the component does not have valid data.
   */
  public static boolean stringValidator(List<String> strings, RSC comp) {
    if (!(comp instanceof DateTimeField dtfield))
      return false;
    if (DateTime.isHandledDateTimeComp(dtfield)) {
      if (!dtfield.containsUserText()) {
        if (DateTime.dateTimeColumnValidate("", dtfield))
          return true;
      }
      for (String string : strings) {
        if (DateTime.dateTimeColumnValidate(string, dtfield))
          return true;
      }
    }
    return false;
  }
}
// vi: sw=2 ts=8
