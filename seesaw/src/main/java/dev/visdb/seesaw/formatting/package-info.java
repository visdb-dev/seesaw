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

//package dev.visdb.seesaw.formatting;

// TODO: THIS NEEDS LOTS OF WORK.
//
// The initial text in here is copied from SSFormattedTextField.
//
// TODO Consider adding back context help and calculators via popups. See 2020-01-07 revisions or earlier.
// TODO Add JDatePicker support or something similar: https://www.codejava.net/java-se/swing/how-to-use-jdatepicker-to-display-calendar-component and https://github.com/JDatePicker/JDatePicker

/**
 * This package contains components based on {@link javax.swing.JFormattedTextField} and has
 * support/base classes for creating these components. The components interoperate
 * with {@link dev.visdb.seesaw.decorators.Decorator}
 * to provide visual feedback as data is entered into the component
 * and {@link dev.visdb.seesaw.decorators.Validator} to detect bogus values
 * as early as possible and avoid them getting into the database.
 * The components also respects {@link dev.visdb.seesaw.datasources.RSC#getAllowNull()}.
 *
 * Formatters and FormatterFactory
 *
 * <p>
 * OLD STUFF
 * <p>
 * Generally bound components are implemented by extending SsFormattedTextField and
 * instantiating with a custom FormatterFactory. E.g. {@link SsDateField }
 *
 * <p>
 * It would be possible to instead use a MaskFormatter, but custom code has to be
 * written if the field needs to be nullable/blanked
 * by the user. For a MaskFormatter, this triggers a ParseException,
 * which would need to be caught in the code and supplanted by
 * a call to setValue(null); Using a MaskFormatter still requires additional
 * validation of some sort. E.g. preventing a MM/dd/yyyy date of
 * 99/99/9999 from being entered.
 */
package dev.visdb.seesaw.formatting;

// public class package_info
// {
// }
// vi: sw=2 ts=8
