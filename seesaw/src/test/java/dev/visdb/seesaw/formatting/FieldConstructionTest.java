/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package dev.visdb.seesaw.formatting;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.SwingUtilities;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.visdb.seesaw.mock.Util;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Field and formatter factory construction.
 */
public class FieldConstructionTest {
  @BeforeAll
  public static void setUpClass() {
    Util.seesawInit();
  }

  @Test
  public void testMaskInvalidCharacters() {
    MaskFormatterFactory factory = new MaskFormatterFactory.Builder<>("****")
        .ssFormat(SsFormat.CUSTOM)
        .invalidCharacters("x")
        .build();
    MaskFormatter mf = (MaskFormatter)factory.getDefaultFormatter();
    assertEquals("x", mf.getInvalidCharacters());
    assertNull(mf.getValidCharacters());
  }

  @Test
  public void testCurrencyEditLocale() throws ParseException {
    DefaultFormatterFactory factory = SsCurrencyField.createFormatterFactory(
        SsFormat.CUSTOM, null, 2, Locale.GERMANY, Locale.US);
    assertEquals(new BigDecimal("1234.50"),
                 factory.getEditFormatter().stringToValue("1.234,50"));
    assertEquals("$1,234.50",
                 factory.getDisplayFormatter().valueToString(new BigDecimal("1234.5")));
  }

  @Test
  public void testCurrencyEditLocaleDefaultsToDisplay() throws ParseException {
    DefaultFormatterFactory factory = SsCurrencyField.createFormatterFactory(
        SsFormat.CUSTOM, null, 2, null, Locale.GERMANY);
    assertEquals(new BigDecimal("1234.50"),
                 factory.getEditFormatter().stringToValue("1.234,50"));
  }

  @Test
  public void testNewTimeFieldIsEmpty() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      assertNull(new SsTimeField().getValue());
    });
  }
}
// vi: sw=2 ts=8
