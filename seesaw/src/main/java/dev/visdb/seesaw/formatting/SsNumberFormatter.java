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

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;

import javax.swing.text.NumberFormatter;

import static dev.visdb.seesaw.utils.JStuff.sf;

/**
 * Number formatter that requires a parse of the complete string to succeed.
 */
@SuppressWarnings("serial")
public class SsNumberFormatter extends NumberFormatter implements FormatterAssist {
  /**
   *
   * @param format
   */
  public SsNumberFormatter(NumberFormat format) {
    super(format);
    setCommitsOnValidEdit(true);
  }

  /**
   * The Format associated with the FormattedTextField.
   * @return format
   */
  @Override
  public SsFormat getSSFormat() {
    if (getFormattedTextField() instanceof SsFormattedTextField ftf)
      return ftf.getSsFormat();
    return null;
  }

  /**
   * If the value is not a String and there is a converter,
   * then first convert the value before super.valueToString.
   * @param value
   * @return String representation of the value
   * @throws ParseException
   */
  @Override
  public String valueToString(Object value) throws ParseException {
    String string;

    // TODO: handle a converter, see SSMaskFormatterFactory
    //s = assistValueToString(value);

    //Object v = value instanceof String s ? stringToValue(s) : value;
    Object v = value;

    string = super.valueToString(v);
    return string;
  }

  /**
   * First convert the string with super.stringToValue,
   * then use the converter (if there is one) to create
   * the value object.
   * @param s
   * @return
   * @throws ParseException
   */
  @Override
  public Object stringToValue(String s) throws ParseException {
    if (s == null || s.isBlank()) {
      if (getFormattedTextField() instanceof SsFormattedTextField ftf && !ftf.getAllowNull())
        throw new ParseException("Null value not allowed", 0);
      return null;
    }
    ParsePosition ppos = new ParsePosition(0);
    getFormat().parseObject(s, ppos);
    if (s.length() != ppos.getIndex())
      throw new ParseException(sf("In '%s' parse finished at %d ", s, ppos.getIndex()),
                               ppos.getIndex());
    // Only need to do following so that min/max constraints are checked.
    return super.stringToValue(s);
    // TODO: handle a converter, see SSMaskFormatterFactory
  }
}
// vi: sw=2 ts=8
