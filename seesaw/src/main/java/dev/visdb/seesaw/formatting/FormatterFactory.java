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

import java.text.ParseException;
import java.util.Objects;
import java.util.function.BiFunction;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

/**
 * Base for SS formatter factories; supports allow null and ssFormat.
 */
@SuppressWarnings("serial")
public abstract class FormatterFactory extends DefaultFormatterFactory {
  private final SsFormat ssFormat;
  private final AbstractFormatter converter;
  private final BiFunction<JFormattedTextField, AbstractFormatter, Boolean> containsUserText;

  /**
   * To build a new FormatterFactory with the specified parameters. Unless noted,
   * a parameter is used when constructing the MaskFormatter.<p>
   * <p>
   * @see <em>Effective Java</em> Item 2 about override.
   * @param <T>
   */
  abstract protected static class Builder<T extends Builder<T>> {
    private AbstractFormatter converter = null;
    private SsFormat ssFormat = null;
    private BiFunction<JFormattedTextField, AbstractFormatter, Boolean> containsUserText;

    /**
     * May be used by a formatter to assist string2Value and value2String.
     * For example, the Ss mask formatters work with strings, the converter
     * converts string to value; like a Date.
     * It's the last step in stringToValue; it produces the Value
     * in the formatted text field.
     * @param val
     * @return  builder
     */
    public T converter(AbstractFormatter val) {
      converter = val;
      return self();
    }
    /**
     * *  The {@link SsFormat} used when generating this format factory.
     * @param val
     * @return  builder
     */
    public T ssFormat(SsFormat val) {
      ssFormat = val;
      return self();
    }
    /**
     *  This overrides the default check for user input data present. The default
     * check is done using {@link FormatterAssist#userText(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.Character)}. If set, this
     * is used by {@link SsFormattedTextField#containsUserText() }.
     * @param val
     * @return  builder
     */
    public T containsUserText(BiFunction<JFormattedTextField, AbstractFormatter, Boolean> val) {
      containsUserText = val;
      return self();
    }

    /**
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    protected T self() {
      return (T) this;
    }

    /**
     * builder
     * @return converter
     */
    public AbstractFormatter getConverter() {
      return converter;
    }
  }

  /**
   * The Format used to create factory.
   * @param builder
   */
  public FormatterFactory(Builder<?> builder) {
    this.ssFormat = Objects.requireNonNull(builder.ssFormat, "format can not be null");
    this.converter = builder.converter;
    this.containsUserText = builder.containsUserText;
  }

  /**
   * The Format used to create factory.
   * @return format
   */
  public SsFormat getSSFormat() {
    return ssFormat;
  }

  /**
   * Convert the string as needed and do
   * {@link JFormattedTextField#setValue(java.lang.Object) }.
   * @param ftf set this text field's value
   * @param string convert to a value
   * @throws ParseException
   */
  public abstract void switchToNonNullValue(JFormattedTextField ftf, String string)
      throws ParseException;

  /**
   * Converter used with stringToValue and valueToString; typically null.
   * @return converter
   */
  public AbstractFormatter getConverter() {
    return converter;
  }

  /**
   * Function used to determine if text field has user input; typically null.
   * @return containsUserText
   */
  public BiFunction<JFormattedTextField, AbstractFormatter, Boolean> getContainsUserText() {
    return containsUserText;
  }

  /** use setEditValid method to check that formatter should flip */
  @SuppressWarnings("serial")
  protected static class NullFormatter extends DefaultFormatter {
    /**
     * The null formatter.
     */
    public NullFormatter() {
      // DO NOT CHANGE
      setValueClass(String.class);
      // DO NOT CHANGE
      setCommitsOnValidEdit(true);
    }

    /**
     * This method is invoked by the formatter when it is almost done.
     * After super.setEditValid, if the text field is a String
     * attempt to set the value (which switches the formatter).
     * @param valid
     */
    @Override
    protected void setEditValid(boolean valid) {
      super.setEditValid(valid);

      // if a character was added to the Null Formatter,
      // then set a value to flip to the  (presumably) edit formatter.
      final JFormattedTextField ftf = getFormattedTextField();
      Object value = ftf.getValue();
      // May not need to check for NullFormatter, but things change :-)
      if (value instanceof String stringValue && ftf.getFormatter() instanceof NullFormatter
          && ftf.getFormatterFactory() instanceof FormatterFactory ff) {
        try {
          ff.switchToNonNullValue(ftf, stringValue);
          // If ftf is still the null formatter,
          // then Formatter didn't like the value; notify and get out.
          if (ftf.getFormatter() instanceof NullFormatter) {
            invalidEdit();
            return;
          }

          // Attempt to put the caret after the character.
          try {
            ftf.setCaretPosition(((String) value).length());
          } catch (IllegalArgumentException ex) {
          }
        } catch (ParseException ex) {
          // mask formatter (probably) got an exception,
          // back to the null formatter
          ftf.setValue(null);
        }
      }
    }
  }
}
// vi: sw=2 ts=8
